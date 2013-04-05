(ns dots.field
  (:require [clojure.pprint]
            [dots.util :as util]))

(def test-field {:size {:width 5 :height 5}
                 ;map edge id to edge data
                 :edges {}
                 ;map dot id to dot data
                 :dots {1 {:x 1 :y 1 :type :red}
                        2 {:x 1 :y 2 :type :blue}}
                 ;map field position to dot id, use to find neighbours of a dot
                 :field-map {[1 1] 1
                             [1 2] 2}
                 ;list of cycles, where cycle is a lists of edges that create a cycle
                 :cycles '()
                 })

(defn- add-edge-to-adjacent-dots
  "Add edge to list of edges adjacent to a dot"
  [field dot-id edge-id]
  (update-in field [:dots dot-id :edges ] conj edge-id))

(defn- add-edge-to-field
  "Add new edge to the game field"
  [field vertex1 vertex2]
  (let [vertex1-id (get (:field-map field) vertex1)
        vertex2-id (get (:field-map field) vertex2)
        new-edge (list vertex1-id vertex2-id)
        new-edge-id (util/get-next-index (:edges field))
        field-with-new-edge (assoc-in field [:edges new-edge-id] new-edge)]
    ;add edge to underlying data map using both vertices of the edge as keys
    ;reduce over list of vertices (i.e. new-edge) with data map as initial value
    ;for reduction call a function(put-edge) that will add new-edge to a map(%1) under a key(%2)(vertex from new-edge)
    (reduce #(add-edge-to-adjacent-dots %1 %2 new-edge-id) field-with-new-edge new-edge)
    ))

(defn- add-edges-to-field
  "Add edges from source vertex to all destination vertices"
  [field source-vertex destination-vertices]
  (reduce #(add-edge-to-field %1 source-vertex %2) field destination-vertices))

(defn- valid-new-dot?
  "Check if new dot with specified coordinates
  can be added to the field"
  [field dot]
  (let [x (:x dot)
        y (:y dot)
        field-map (:field-map field)
        width (get-in field [:size :width ])
        height (get-in field [:size :height ])]
    (cond
      (and (contains? :id dot) (contains? (:dots field))) false ;dot has id set and a dot with same id already stored in field
      (contains? field-map [x y]) false ;a dot with same coordinates exists
      (or (> x width) (< x 1)) false ;dot is out of field
      (or (> y height) (< y 1)) false ;dot is out of field
      :else true)
    ))

(defn- get-neighbour-dots
  "Return coordinates of all neighbour dots that have same colour"
  [field dot]
  (let [x (:x dot)
        y (:y dot)
        field-map (:field-map field)
        ;dot left, dot right, dot bottom, dot top
        neighbour-coords (list [(- x 1) y] [(+ x 1) y] [x (- y 1)] [x (+ y 1)])]
    (filter #(and
               (contains? field-map %1)
               (let [dest-dot-id (field-map %1)
                     dest-dot ((:dots field) dest-dot-id)]
                 (= (dot :type ) (dest-dot :type ))
                 ))
      neighbour-coords)
    ))

(defn put-dot
  "Add a new dot with specified coordinates(1-based,
  x: left to right, y: bottom to top) to the field,
  and try to add edges to dots around."
  [field dot]
  (if (valid-new-dot? field dot)
    ;add four edges to field, connecting new dot with existing dots
    (let [x (:x dot)
          y (:y dot)
          destination-dots (get-neighbour-dots field dot)
          dot-index (util/get-next-index (:dots field))]
      (clojure.pprint/pprint destination-dots)
      (-> field
        (assoc-in [:dots dot-index] dot)
        (assoc-in [:field-map [x y]] dot-index)
        (add-edges-to-field [x y] destination-dots))
      )))

(clojure.pprint/pprint (-> test-field
                         (put-dot {:x 2 :y 2 :type :red})
                         (put-dot {:x 2 :y 1 :type :red})
                         ))
