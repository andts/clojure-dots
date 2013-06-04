(ns dots.core.player
  (:require [dots.db :as db]))

(def players (ref {}))

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
      player)
    ))

(defn load-player
  [player-id]
  )