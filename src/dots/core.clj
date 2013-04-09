(ns dots.core
  (:require [dots.util :as util]
            [dots.field :as field]
            [dots.db :as db]
            [clojure.pprint]))

(def games (ref {}))

(defn create-game
  [player1 player2 width height]
  (let [game {:game-id (:GENERATED_KEY (db/create-game player1 player2))
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
  (let [game (get @games game-id)
        field-with-dot (field/put-dot (:field game) {:x x :y y :type (get game player)})
        new-game (assoc game :field field-with-dot)]
    (dosync
      (ref-set games (assoc @games (:game-id new-game) new-game)))
    new-game
    ))
