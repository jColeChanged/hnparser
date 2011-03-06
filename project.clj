(defproject hnparser "1.0.0-SNAPSHOT"
  :description "This is a parser for hacker news comment pages."
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
		 [enlive "1.0.0-SNAPSHOT"]
		 [clj-time "0.2.0-SNAPSHOT"]
		 [congomongo "0.1.3-SNAPSHOT"]]
  :dev-dependencies [[swank-clojure "1.3.0-SNAPSHOT"]]
  :main hnparser.server)