(ns dots.core.game
  (:require [dots.core.player :as player]
            [dots.field :as field]
            [dots.db :as db]))

(def games (ref {}))

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
  (if-let [game-data (db/load-game game-id)]
    (player/load-player (:player1-id game-data))
    (player/load-player (:player2-id game-data))
    (let [game {:game-id (:game-id game-data)
                :players {(:player1-id game-data) :red
                          (:player2-id game-data) :blue}
                :field (db/load-game-field game-id)}]
      (dosync
        (ref-set games (assoc @games (:game-id game) game)))
      game
      )))

(defn put-dot
  "Add new dot to game field"
  [game-id x y player-id]
  (if-let [game (get @games game-id)]
    (let [field-with-dot (field/put-dot (:field game) {:x x :y y :type (get-in game [:players player-id])})
          new-game (assoc game :field field-with-dot)]
      (dosync
        (ref-set games (assoc @games (:game-id new-game) new-game)))
      new-game
      )))
