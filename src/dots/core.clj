(ns dots.core
  (:require [dots.util :as util]
            [dots.field :as field]
            [dots.db :as db]))

(def games (ref {}))
(def players (ref {}))

(defn create-game
  [player1-id player2-id width height]
  (let [game {:game-id (:GENERATED_KEY (db/create-game player1-id player2-id))
              :players {player1-id :red
                        player2-id :blue}
              :field {:size {:width width
                             :height height}
                      }}]
    (dosync
      (ref-set games (assoc @games (:game-id game) game)))
    game
    ))

(defn save-game
  [game]
  (let [saved-game (db/save-game game)]
    (dosync
      (ref-set games (assoc @games (:game-id saved-game) saved-game)))
    saved-game
    ))

(defn load-game
  [game-id]
  )

(defn put-dot
  "Add new dot to game field"
  [game-id x y player-id]
  (let [game (get @games game-id)
        field-with-dot (field/put-dot (:field game) {:x x :y y :type (get-in game [:players player-id])})
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

(defn save-player
  [player]
  (do
    (db/save-player player)
    (dosync
      (ref-set players (assoc @players (:id player) player))
      player
      )
    ))

(defn load-player
  [player-id]
  )