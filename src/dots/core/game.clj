(ns dots.core.game
  (:require [dots.core.player :as player]
            [dots.field :as field]
            [dots.util :as util]
            [dots.db :as db]))

(def games (ref {}))
(def invites (ref {}))

(defn save-inmemory
  [coll item-id-key item]
  (dosync
    (ref-set coll (assoc @coll (get item item-id-key) item))))

(defn save-game-inmemory
  [game]
  (save-inmemory games :game-id game))

(defn save-invite-inmemory
  [invite]
  (save-inmemory invites :invite-id invite))

(defn create-game
  "Create a new game from supplied parameters, or from existing invite, and then delete the invite"
  ([player1-id player2-id width height]
    (let [game {:game-id (:GENERATED_KEY (db/create-game player1-id player2-id))
                :players {player1-id :red
                          player2-id :blue}
                :field {:size {:width width
                               :height height}}
                :state :initiated
                }]
      (save-game-inmemory game)
      game))
  ([invite-id]
    (let [invite (get @invites invite-id)
          player1-id (:player1-id invite)
          player2-id (:player2-id invite)
          game {:game-id (:GENERATED_KEY (db/create-game player1-id player2-id))
                :players {player1-id :red
                          player2-id :blue}
                :field {:size {:width (:width invite)
                               :height (:height invite)}}
                }]
      ;      (dosync
      ;        (ref-set invites (assoc @invites (get item item-id-key) item))
      ;        (ref-set invites (dissoc @invites (get invite :invite-id )))
      ;        )
      game)))

(defn save-game
  [game]
  (if-let [saved-game (db/save-game game)]
    (do
      (save-game-inmemory saved-game)
      saved-game)))

(defn load-game
  [game-id]
  (if-let [game-data (db/load-game game-id)]
    (do
      (player/load-player (:player1-id game-data))
      (player/load-player (:player2-id game-data))
      (let [game {:game-id (:game-id game-data)
                  :players {(:player1-id game-data) :red
                            (:player2-id game-data) :blue}
                  :field (db/load-game-field game-id)}]
        (save-game-inmemory game)
        game))))

(defn get-all-games-for-player
  [player-id]
  (db/get-all-games-for-player player-id))

(defn put-dot
  "Add new dot to game field"
  [game-id x y player-id]
  (if-let [game (get @games game-id)]
    (let [field-with-dot (field/put-dot (:field game) {:x x :y y :type (get-in game [:players player-id])})
          new-game (assoc game :field field-with-dot)]
      (save-game-inmemory new-game)
      new-game)))

;create invites only inmemory for now
;(defn create-invite
;  [player-id width height]
;  (let [new-invite {:invite-id (util/get-next-index @invites)
;                    :player1-id player-id
;                    :width width
;                    :height height}]
;    (save-invite-inmemory new-invite)
;    new-invite))
;
;(defn get-all-invites [] @invites)
;
;(defn update-invite [invite]
;  (save-invite-inmemory invite))
;
;(defn remove-invite [invite-id]
;  (dosync
;    (ref-set invites (dissoc @invites (get @invites :invite-id )))))
;
;(defn join-invite [invite-id player-id]
;  (if-not (contains? (get @invites invite-id) :player2-id )
;    (let [new-invite (assoc invite :player2-id player-id)]
;      (save-invite-inmemory new-invite))))