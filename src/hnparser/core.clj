(ns hnparser.core
  (:use [hnparser.rate-limit :only [defrlf]]
	[clojure.contrib.string :only [join]]
	clj-time.coerce
	enlive-utils.core)
  (:require [net.cgrand.enlive-html :as html]
	    [clj-time.core :as dtime]))
 

;; Paul Graham told me he didn't want me to be doing more than one request
;; per five seconds. This bit of code is defining a url fetcher that adheres
;; to his request.
(defn fetch-url
  [url]
  (html/html-resource (java.net.URL. url)))
(defrlf rl-fetch-url fetch-url 1 5 true)

;; Some code to figure out the date a Hacker News comment was posted
;; I'm only bothering with post time to the day because that is the
;; highest resoultion time stamp you can get given HN's output format.
(defn strip-time
  "Takes a JodaTime Date and removes the hours/minute/second information."
  [dt]
  (let [y (dtime/year dt) m (dtime/month dt) d (dtime/day dt)]
    (dtime/date-time y m d)))

;; This is converting to a java Date because the database backend I'm using,
;; MongoDB, uses java.util.Date to handle date objects instead of using
;; JodaTime. Maybe patching congomongo would be a better option?
(defn parse-date
  [s]
  (to-date
   (let [reg-match (re-matches #"(\d+) (minute|hour|day)s? ago" s)
	 units (- (Integer/parseInt (second reg-match)))
	 unit-type (last reg-match)]
     (strip-time
      (dtime/plus (dtime/now)
		  (cond
		   (= unit-type "minute") (dtime/minutes units)
		   (= unit-type "hour") (dtime/hours units)
		   (= unit-type "day") (dtime/days units)))))))

(defn pparse-date [d] (parse-date (.substring d 1 (- (.length d) 4))))

(defn get-post
  [page]
  {
   :submission true
   :title (fcontentf
	   (html/select page [:td.title [:a (html/attr? :href)]]))
   :title-link (flink
		(html/select page [:td.title [:a (html/attr? :href)]]))
   :body (join
	  "\n\n"
	  (map
	   html/text
	   (:content (first
		      (html/select page [:td [:table (html/nth-child 1)]
					 [:tr (html/nth-child 4)]
					 [:td (html/nth-child 2)]])))))
   :score (Integer/parseInt
	   ((comp second #(re-matches #"(-?\d+) points?" %) fcontentf)
	    (html/select page [:td.subtext [:span]])))
   :user (fcontentf
	  (html/select page [:td.subtext [:a (html/nth-of-type 1)]]))
   :id ((comp second #(re-matches #"item\?id=(\d+)" %) flink)
	(html/select page [:td.subtext [:a (html/nth-of-type 2)]]))
   :date (pparse-date
	  ((comp second first)
	   (html/select page {[:td.subtext [:a (html/nth-of-type 1)]]
			      [:td.subtext [:a (html/nth-of-type 2)]]})))})

(defn get-comment
  [page]
  {
   :submission false
   :user (fcontentf
	  (html/select page [:span.comhead [:a (html/nth-of-type 1)]]))
   :id ((comp second #(re-matches #"item\?id=(\d+)" %) flink)
	(html/select page [:.comhead [:a (html/nth-child 3)]]))
   :score (Integer/parseInt
	   ((comp second #(re-matches #"(-?\d+) points?" %) fcontentf)
	    (html/select page [:span.comhead :span])))
   :date (pparse-date
	  ((comp second first)
	   (html/select page {[:span.comhead [:a (html/nth-of-type 1)]]
			      [:span.comhead [:a (html/nth-of-type 2)]]})))
   :parent ((comp second #(re-matches #"item\?id=(\d+)" %) flink)
	    (html/select page [:span.comhead [:a (html/nth-of-type 3)]]))
   :body (join
	  "\n\n"
	  (map html/text
	       (concat
		(html/select page [[:.default (html/nth-child 2)] :.comment])
		(html/select page [[:.default (html/nth-child 2)] :p]))))})

(defn get-all-posts
  [page]
  (map #(:href (:attrs %)) (html/select page [:span.comhead [:a (html/nth-of-type 2)]])))

(defn urlize [comment-id] (str "http://news.ycombinator.com/" comment-id))

(defn scrape-item
  [root-link]
  (do
    (println "Scraping item.")
    (let [root-page (rl-fetch-url root-link)]
      (loop [links (get-all-posts root-page)
	     items [(assoc (get-post root-page) :num-comments (count links))]]
	(if (empty? links)
	  items
	  (recur
	   (rest links)
	   (cons (get-comment (rl-fetch-url (urlize (first links)))) items)))))))