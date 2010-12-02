(ns hnparser.core
  (:require [net.cgrand.enlive-html :as html])
  (:use [hnparser.rate-limit :only [defrlf]]))

(def url "http://news.ycombinator.com/item?id=1951803")

;; Paul Graham told me he didn't want me to be doing more than one request
;; per five seconds. This bit of code is defining a url fetcher that adheres
;; to his request.
(defn fetch-url
  [url]
  (html/html-resource (java.net.URL. url)))
(defrlf rl-fetch-url fetch-url 1 5 true)

;; Grab the title and link
(defn get-title
  [page]
  (html/select page [:td.title [:a (html/attr? :href)]]))

;; Grab the title and link
(defn get-score
  [page]
  (html/select page [:td.subtext [:span]]))

(defn get-user
  [page]
  (html/select page [:td.subtext [:a (html/nth-of-type 1)]]))

(defn get-comment-link
  [page]
  (html/select page [:td.subtext [:a (html/nth-of-type 2)]]))

;; i can probably use some of what I got above to strip this if I don't find anything better suited to the task, just a reminder for mysef

(defn get-post-time
  [page]
  (html/select page {[:td.subtext [:a (html/nth-of-type 1)]]
		     [:td.subtext [:a (html/nth-of-type 2)]]}))


(defn get-all-posts
  [page]
  (html/select page [:span.comhead [:a (html/nth-of-type 2)]]))

(def page (rl-fetch-url url))
(defn print-links
  [page]
  (loop [links (get-all-posts page)]
    (do
      (println (first links))
      (recur (rest links)))))