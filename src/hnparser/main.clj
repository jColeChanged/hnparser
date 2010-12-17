(ns hnparser.main
  (:use somnium.congomongo)
  (:use [hnparser.core :only [build-tree]]))

(mongo! :db "hacker-archives")

(defn valid-link?
  [link]
  (not= nil (re-matches #"http://news\.ycombinator\.com/item\?id=\d+" link)))

(defn process-link
  [link]
  (if (valid-link? link)
    (build-tree link)))