(ns dots.core
  (:require [dots.util :as util]
            [dots.field :as field]
            [dots.db :as db]))

(def games (ref {}))
(def players (ref {}))

(defn create-game
  [player1-id player2-id width height]
  (let [game {:game-id (:GENERATED_KEY (db/create-game player1-id player2-id))
              player1-id :red
              player2-id :blue
              :field {:size {:width width
                             :height height}
                      }}]
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

(defn create-player
  "Create new player"
  [name]
  (let [player {:name name}
        created-player (assoc player :id (:GENERATED_KEY (db/create-player player)))]
    (dosync
      (ref-set players (assoc @players (:id created-player) created-player)))
    created-player
    ))

(defn update-player
  [player]
  (dosync
    (db/save-player player)
    (ref-set players (assoc @players (:id player) player))
    player
    ))

