(ns dots.core.player
  (:require [dots.db :as db]))

(def players (ref {}))

(defn save-player-inmemory
  [player]
  (dosync
    (ref-set players (assoc @players (:player-id player) player))))

(defn create-player
  "Create new player"
  [name]
  (let [proto-player {:name name}
        created-player (assoc proto-player :player-id (:GENERATED_KEY (db/create-player proto-player)))]
    (save-player-inmemory created-player)
    created-player
    ))

(defn save-player
  [player]
  (do
    (db/save-player player)
    (save-player-inmemory player)
    player))

(defn load-player
  [player-id]
  (if-let [player-data (db/load-player player-id)]
    (let [player {:player-id (:player-id player-data)
                  :name (:name player-data)}]
      (save-player-inmemory player)
      player)
    ))

