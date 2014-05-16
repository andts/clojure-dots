(ns dots.core.player
  (:require [dots.db :as db]
            [dots.util :as util]))

(def players (ref {}))

(defn save-player-inmemory
  [player]
  (util/save-inmemory players :id player))

(defn create-player
  "Create new player and return new generated id"
  [name anon]
  (let [proto-player {:name name :anon anon}
        created-player (assoc proto-player :id (:GENERATED_KEY (db/create-player proto-player)))]
    (save-player-inmemory created-player)
    created-player))

(defn save-player
  "Save existing player inmemory and in db"
  [player]
  (do
    (db/save-player player)
    (save-player-inmemory player)
    player))

(defn load-player
  [player-id]
  (if-let [player-data (db/load-player player-id)]
    (let [player {:id          (:id player-data)
                  :name        (:name player-data)
                  :anon        (:anon player-data)
                  :region      (:region player-data)
                  :skill       (:skill player-data)
                  :avatar      (:avatar player-data)
                  :facebookUid (:facebookUid player-data)
                  }]
      (save-player-inmemory player)
      player)))

(defn get-player [player-id]
  (if-let [player-info (get @players player-id)]
    player-info
    (load-player player-id)))

(defn load-all-players [] (db/load-all-players))
