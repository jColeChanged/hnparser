(ns hnparser.server
  (:use clojure.contrib.server-socket
	hnparser.main)
  (:import [java.io PushbackReader InputStreamReader]))

(defn read-links
  [is os]
  (let [in-reader (PushbackReader. (InputStreamReader. is))
	input (str (read in-reader))]
    (println "Recieved: " input)
    (.close is)
    (.close os)
    (process-link input)))

(defn -main
  []
  (def *server* (create-server 4576 read-links)))