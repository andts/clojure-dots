(ns dots.util)

(defn get-next-index
  "Returns next index for a map where keys are numbers"
  [map]
  (if (empty? (keys map))
    1
    (inc (apply max (keys map))))
  )

(defn includes?
  "Check if the collection contain an item"
  [coll item]
  (some #(= item %) coll))