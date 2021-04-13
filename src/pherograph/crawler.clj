;; A Clojure graph crawler.

(ns pherograph.sprinter)

(def *finished-sprint* "FINISHED SPRINT")


(defn crawler [nodes processor combiner successor]
  """Generalized graph walking.

  Args:
    nodes: A list of nodes to crawl.

    processor: when called on a node, processes that node, 
      potentially emitting environmental modification
    successors: successors, when called on a node, extends the nodes.
      combiner: A function that combines nodes with the res
  
    combiner: a function that combines nodes and the result of calling 
      successor
  """ 
  (loop [nodes nodes]
    (if (empty? nodes)
      *finished-crawl*
      (recur (combiner (rest nodes)
                       (successor (first nodes)))))))


(def new-set (comp set concat))

;; Starting the scrape has to begin somewhere. I chose an author at random.
;; (def seed-id "1750035")

