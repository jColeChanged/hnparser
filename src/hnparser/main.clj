(ns hnparser.main
  (:use somnium.congomongo
	[hnparser.core :only [scrape-item]])
  (:require [clojure.zip :as zip]))

(mongo! :db "hacker-archives")

(defn valid-link?
  [link]
  (println "Validating link.")
  (not= nil (re-matches #"http://news\.ycombinator\.com/item\?id=\d+" link)))

(defn wikify [item]
  (dissoc (assoc item :wiki [{:title (:title item)
			      :author (:user item)
			      :body (:body item)
			      :date (:date item)
			      :reason "Original post."}])
	  :title :user :body :date))

(defn wikify-items [items]
  (println "Wikifying items.")
  (map wikify items))

(defn upload-item
  [item]
  (when (nil? (fetch-one :items :where {:id (:id item)}))
    (insert! :items (assoc item :scrape-date (java.util.Date.)))))
  

(defn upload-items
  [items]
  (println "Uploading items.")
  (doseq [item items] (upload-item item)))

(defn process-link
  [link]
  (println "Processing link.")
  (when (valid-link? link)
    (upload-items (wikify-items (scrape-item link)))))

(defn get-item
  [id]
  (fetch-one :items :where {:id id}))
(defn get-children
  [id]
  (fetch :items :where {:parent id} :sort {:score -1}))

(defn get-item-tree
  [item]
  (loop [items [item] stack []]
    (if (empty? items)
      stack
      (recur (concat
	      (get-children (:id (first items)))
	      (rest items))
	     (concat stack (first items))))))
