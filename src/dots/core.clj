(ns dots.core
  (:require [dots.util :as util]
            [dots.field :as field]
            [dots.db :as db]
            [clojure.pprint]))

(def games (ref {}))

(defn create-game
  [player1 player2 width height]
  (let [game {:game-id (:GENERATED_KEY (db/create-game-placeholder player1 player2))
              player1 :red
              player2 :blue
              :field {:size {:width width
                             :height height}}}]
    (dosync
      (ref-set games (assoc @games (:game-id game) game)))
    game
    ))

(defn put-dot
  [game-id x y player]
  (let [game (get @games game-id)]
    (field/put-dot (:field game) {:x x :y y :type (get game player)})
    ))

(clojure.pprint/pprint (create-game 10 11 5 5))
(clojure.pprint/pprint (create-game 12 14 2 2))
(clojure.pprint/pprint @games)