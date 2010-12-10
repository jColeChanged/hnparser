(ns enlive-utils)
(defn fcontentf
  [x]
  (let [s (first (:content (first x)))]
    (if (instance? String s)
      (.replace s "," "") ; a null bit me earlier, this might be removable
      "")))