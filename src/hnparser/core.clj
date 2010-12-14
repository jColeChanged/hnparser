(ns hnparser.core
  (:require [net.cgrand.enlive-html :as html])
  (:require [clojure.zip :as zip])
  (:use [hnparser.rate-limit :only [defrlf]])
  (:require [clj-time.core :as dtime]))

;; These are some URLs and pages that I can use to test out how the algorithms
;; are working. Each of them are suitable for different tests. Link, Ask, Comment,
;; Comment w/ children
(def url "http://news.ycombinator.com/item?id=1951803")
(def url2 "http://news.ycombinator.com/item?id=1992137")
(def url3 "http://news.ycombinator.com/item?id=1998122")
(def url4 "http://news.ycombinator.com/item?id=1995212")
(def page (rl-fetch-url url))
(def page2 (rl-fetch-url url2))
(def page3 (rl-fetch-url url3))
(def page4 (rl-fetch-url url4))

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
  [dt]
  (let [y (dtime/year dt) m (dtime/month dt) d (dtime/day dt)]
    (dtime/date-time y m d)))

(defn parse-date
  [s]
  (let [reg-match (re-matches #"(\d+) (minute|hour|day)s? ago" s)
	units (- (Integer/parseInt (second reg-match))) ; need to go back in time
	unit-type (last reg-match)]
    (strip-time
     (dtime/plus (dtime/now)
		 (cond
		  (= unit-type "minute") (dtime/minutes units)
		  (= unit-type "hour") (dtime/hours units)
		  (= unit-type "day") (dtime/days units))))))
(defn pparse-date
  [d]
  (parse-date (.substring d 1 (- (.length d) 4))))

(def flink (comp :href :attrs first))
(def fcontentf (comp first :content first))

(defn get-post
  [page]
  {:title (fcontentf (html/select page [:td.title [:a (html/attr? :href)]]))
   :title-link (flink (html/select page [:td.title [:a (html/attr? :href)]]))
   :body (let [bod (:content (first (html/select page [:table :table [:tr (html/nth-of-type 4)] [:td (html/nth-of-type 2)]])))]
	   (if (= (:tag (first bod)) :form)
	     nil
	     (cons (first bod) (map (comp first :content) (rest bod)))))
   :score (fcontentf (html/select page [:td.subtext [:span]]))
   :user (fcontentf (html/select page [:td.subtext [:a (html/nth-of-type 1)]]))
   :link (flink (html/select page [:td.subtext [:a (html/nth-of-type 2)]]))
   :num-comments (fcontentf (html/select page [:td.subtext [:a (html/nth-of-type 2)]]))
   :date (pparse-date (second (first (html/select page {[:td.subtext [:a (html/nth-of-type 1)]]
						      [:td.subtext [:a (html/nth-of-type 2)]]}))))})

(defn get-comment
  [page]
  {:user (fcontentf (html/select page [:span.comhead [:a (html/nth-of-type 1)]]))
   :score (fcontentf (html/select page [:span.comhead :span]))
   :date (pparse-date (second (first (html/select page {[:span.comhead [:a (html/nth-of-type 1)]]
							[:span.comhead [:a (html/nth-of-type 2)]]}))))
   :parent (flink (html/select page [:span.comhead [:a (html/nth-of-type 3)]]))
   :body (html/select page [:span.comment])})

(defn get-all-posts
  [page]
  (map (fn [x] (:href (:attrs x))) (html/select page [:span.comhead [:a (html/nth-of-type 2)]])))



(defn urlize [comment-id] (str "http://news.ycombinator.com/" comment-id))
(def id "item?id=1988804")

(def node (zip/seq-zip (seq (get-all-posts (rl-fetch-url (urlize id))))))
(def branch-node (zip/make-node node (-> node zip/node) []))
(def testing-waters
     (zip/insert-right (-> node zip/down) branch-node))

(defn remove-matches
  [item lists]
  (remove empty? (map (fn [x] (remove (fn [y] (= y item)) x)) lists)))

(defn zip-vertically-n
  [loc n]
  (cond
   (zero? n) loc
   (pos? n) (if (zip/up loc)
	      (recur (zip/up loc) (dec n))
	      loc)
   (neg? n) (if (zip/down loc)
	      (recur (zip/down loc) (inc n))
	      loc)))

(defn build-tree
  [root-id]
  (loop [loc (zip/vector-zip [(get-post (rl-fetch-url (urlize root-id)))])
	 link-lists [(get-all-posts (rl-fetch-url (urlize root-id)))]]
    (if (empty? link-lists)
      loc
      (let [page (rl-fetch-url (urlize (ffirst link-lists)))
	    links (get-all-posts page)
	    link (first links)
	    new-link-list (remove-matches link (cons links link-lists))
	    node (zip/make-node loc (zip/node loc) [(get-comment page)])]
	(recur
	 (zip/rightmost
	  (zip-vertically-n
	   (zip/append-child loc node)
	   (- (count link-lists) (count new-link-list))))
	 new-link-list)))))
	   