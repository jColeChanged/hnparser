(ns hnparser.main
  (:use somnium.congomongo
	[hnparser.core :only [scrape-item]]
	[clojure.string :only [read-lines]]))

(mongo! :db "hacker-archives")

(defn un-urlize
  [link]
  (re-matches #"http://news\.ycombinator\.com/item\?id=(\d+)" link))

(defn valid-link?
  [link]
  (println "Validating link.")
  (not= nil (un-urlize link)))

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


;; Functions for putting a file into the database. Used to get seed content in
;; quickly.
(defn load-items
  [filename]
  (map
   (juxt first (comp #(clojure.contrib.string/split #" " %) second))
   (partition 2 (read-lines filename))))

(defn tag-item
  [item]
  (when-let [o (fetch-one :items
			  :where {:id (second (un-urlize (first item)))})]
    (update! :items {:id (:id o)} (assoc o :tags (second item)))))

(defn process-loaded-item
  [item]
  (process-link (first item))
  (tag-item item))

(defn process-loaded-items
  [items]
  (doseq [item items]
    (process-loaded-item item)))