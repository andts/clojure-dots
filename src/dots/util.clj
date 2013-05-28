(ns dots.util)

(defn get-next-index
  "Returns next index for a map where keys are numbers"
  [map]
  (if (empty? (keys map))
    1
    (inc (apply max (keys map))))
  )

(defn includes?
  "Check if collection coll contains the item"
  [coll item]
  (some #(= item %) coll))

(defmacro try-run
  "Assuming that the body of code returns X, this macro returns [X nil]
  in the case of no error and [nil E] in event of an exception object E."
  [& body]
  `(try [(do ~@body) nil]
     (catch Exception e#
       [nil e#])))