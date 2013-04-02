(ns dots.util)

(defn includes?
  "Check if the collection contain an item"
  [coll item]
  (some #(= item %) coll))