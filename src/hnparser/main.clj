(ns hnparser.main
  (:use somnium.congomongo
	[hnparser.core :only [build-tree]])
  (:require [clojure.zip :as zip]))

(mongo! :db "hacker-archives")

(defn valid-link?
  [link]
  (not= nil (re-matches #"http://news\.ycombinator\.com/item\?id=\d+" link)))

(defn process-link
  [link]
  (if (valid-link? link)
    (build-tree link)))

(def test-link "http://news.ycombinator.com/item?id=1988804")
;;(def test-tree (process-link test-link))
(defn get-all-ids
  [tree]
  (loop [loc tree id-list []]
    (if (zip/end? loc)
      id-list
      (if (zip/branch? loc)
	(recur (zip/next loc) id-list)
	(recur (zip/next loc) (cons (:id (zip/node loc)) id-list))))))

(defn add-item-to-mongo
  [link]
  (let [item (process-link link)]
    (if item
      (insert! :posts {:keys (get-all-ids item) :tree item}))))

(def test-item (fetch-one :posts :where {:keys "1988942"}))