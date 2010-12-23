(ns hnparser.openkeyval)

(def *base-url* "http://openkeyval.org/")

(defn is-valid-key?
  [key]
  (< 5 (count key) 128))

(defn get-key
  [key]
  (if (is-valid-key? key)
    (java.net.URL (str *base-url* key))
    (throw (new java.security.InvalidKeyException
		"Key must be between 5 and 128 charachters"))))
