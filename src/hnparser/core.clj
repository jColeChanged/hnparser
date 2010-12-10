(ns hnparser.core
  (:require [net.cgrand.enlive-html :as html])
  (:require [clojure.zip :as zip])
  (:use [hnparser.rate-limit :only [defrlf]])
  (:use [clj-time.core]))

(def url "http://news.ycombinator.com/item?id=1951803")

;; Paul Graham told me he didn't want me to be doing more than one request
;; per five seconds. This bit of code is defining a url fetcher that adheres
;; to his request.
(defn fetch-url
  [url]
  (html/html-resource (java.net.URL. url)))
(defrlf rl-fetch-url fetch-url 1 5 true)

(defn parse-date
  [s]
  (let [[units unit-type] (rest (re-matches #"^(\d+) (minutes|hours|days) ago" s))]
    (plus (now)
	  (cond
	   (= unit-type "minutes") (minutes (- units))
	   (= unit-type "hours") (hours (- units))
	   (= unit-type "days") (days (- units))))))
    

(def flink (comp :href :attrs first))
(def fcontentf (comp first :content first))

(defn get-post
  [page]
  {:title (fcontentf (html/select page [:td.title [:a (html/attr? :href)]]))
   :title-link (flink (html/select page [:td.title [:a (html/attr? :href)]]))
   :score (fcontentf (html/select page [:td.subtext [:span]]))
   :user (fcontentf (html/select page [:td.subtext [:a (html/nth-of-type 1)]]))
   :link (flink (html/select page [:td.subtext [:a (html/nth-of-type 2)]]))
   :date (html/select page {[:td.subtext [:a (html/nth-of-type 1)]]
		     [:td.subtext [:a (html/nth-of-type 2)]]})})

(defn get-all-posts
  [page]
  (map (fn [x] (:href (:attrs x))) (html/select page [:span.comhead [:a (html/nth-of-type 2)]])))

(def page (rl-fetch-url url))

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
  (loop [loc (zip/vector-zip [root-id])
	 link-lists [(get-all-posts (rl-fetch-url (urlize root-id)))]]
    (do (println "root: " (zip/root loc))
	(println "node: " (zip/node loc))
	(println "link lists: " link-lists)
	(println "----------")
    (if (empty? link-lists)
      loc
      (let [links (get-all-posts (rl-fetch-url (urlize (ffirst link-lists))))
	    link (first links)
	    new-link-list (remove-matches link (cons links link-lists))
	    node (zip/make-node loc (zip/node loc) [link])]
	(recur
	 (zip/rightmost
	  (zip-vertically-n
	   (zip/append-child loc node)
	   (- (count link-lists) (count new-link-list))))
	 new-link-list))))))
	   