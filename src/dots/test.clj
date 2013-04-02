(ns dots.test
  [:require [dots.util :as util] [clojure.pprint]])

(def test-field {:size {:width 5 :height 5}
                 ;map vertex to list of edges it is included in
                 :edges {{:x 1 :y 1} '(({:x 1 :y 1} {:x 2 :y 2}))
                         {:x 2 :y 2} '(({:x 1 :y 1} {:x 2 :y 2}))}
                 })

(defn- put-edge
  "Put edge into the map. Using vertex as a key, add edge to the list corresponding the key"
  [map vertex edge]
  (update-in map [vertex] conj edge))

(defn- add-edge-to-field-graph
  "Add new edge to the game field"
  [field vertex1 vertex2]
  (let [new-edge (list vertex1 vertex2)
        edges (get field :edges )]
    ;add edge to underlying data map using both vertices of the edge as keys
    ;reduce over list of vertices (i.e. new-edge) with data map as initial value
    ;for reduction call a partial application of a function(put-edge) that will add new-edge to a map(%1) under a key(%2)(vertex from new-edge)
    (assoc field :edges (reduce #(put-edge %1 %2 new-edge) edges new-edge))
    ))

(defn- valid-new-dot?
  "Check if new dot with specified coordinates can be added to the field"
  [field x y]
  (let [data (get field :edges )
        width (get-in field [:size :width ])
        height (get-in field [:size :height ])]
    (cond
      (contains? data {:x x :y y}) false
      (or (> x width) (< x 1)) false
      (or (> y height) (< y 1)) false
      :else true
      )
    ))

(defn try-add-edge [field vertex1 vertex2]
  (let [data (get field :edges )]
    (if (contains? data vertex2)
      (add-edge-to-field-graph field vertex1 vertex2)
      field)
    ))

(defn put-dot [field x y]
  (if (valid-new-dot? field x y)
    ;add four edges to field, connecting new dot with existing dots
    (let [destination-dots (list {:x (- x 1) :y y} {:x (+ x 1) :y y} {:x x :y (- y 1)} {:x x :y (+ y 1)}) ;dot left, dot right, dot bottom, dot top
          field-with-new-dot (assoc-in field [:edges {:x x :y y}] '())]
      (reduce #(try-add-edge %1 {:x x :y y} %2) field-with-new-dot destination-dots))
    field
    ))

(clojure.pprint/pprint (put-dot (put-dot test-field 1 2) 8 2))