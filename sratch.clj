(ns screenscraping
  (:require [clojure.contrib.http.agent :as http-agent]))

(def *url* "http://news.ycombinator.com/")
(def resource (http-agent/http-agent *url*))


