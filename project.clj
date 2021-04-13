(defproject pherograph "1.0.0-SNAPSHOT"
  :description "This is a parser for hacker news comment pages."
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [enlive "1.1.6"]
                 [clj-time "0.15.2"]
                 [congomongo "2.2.1"]
                 [environ "1.2.0"]]
  :dev-dependencies [[swank-clojure "1.3.0"]]
  :main pherograph.core)