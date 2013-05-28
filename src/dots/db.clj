(ns dots.db
  (:require [korma.core :as k]
            [korma.db :as db]
            [dots.field :as field]
            [clojure.pprint]
            ))

(db/defdb db (db/mysql {:host "localhost"
                        :port "3306"
                        :db "dots"
                        :user "sysadm"
                        :password "netcracker"}))

(k/defentity players
  (k/pk :player-id )
  (k/entity-fields :name ))

(k/defentity dots
  (k/pk :dot-id )
  (k/entity-fields :x :y :type ))

(k/defentity fields
  (k/pk :field-id )
  (k/entity-fields :width :height )
  (k/has-many dots {:fk :field-id}))

(k/defentity games
  (k/pk :game-id )
  (k/entity-fields :player1-id :player2-id )
  (k/has-one fields {:fk :game-id})
  (k/belongs-to players (:fk :player1-id ))
  (k/belongs-to players (:fk :player2-id )))

(defn load-game-field
  "Get game field by game id"
  [game-id]
  (let [query-result (first (k/select games (k/with fields (k/with dots)) (k/where {:games.game-id game-id})))
        field-size {:width (:width query-result) :height (:height query-result)}
        game-field {:size field-size}]
    (reduce #(field/put-dot %1 {:x (:x %2) :y (:y %2) :type (keyword (:type %2))}) game-field (:dots query-result))
    ))

(defn create-game
  [player1 player2]
  (k/insert games (k/values {:player1-id player1 :player2-id player2})))

(defn- save-dot
  [field dot]
  (k/insert dots
    (k/modifier "IGNORE")
    (k/values {:dot-id (:dot-id dot)
               :field-id (:field-id field)
               :x (:x dot) :y (:y dot)
               :type (name (:type dot))})))

(defn- save-game-field
  [field game-id]
  (let [saved-field (if (contains? field :field-id )
                      field
                      (k/insert fields (k/values {:width (:width field) :height (:height field) :game-id game-id})))]
    (reduce save-dot saved-field (:dots saved-field))
    ))

(defn save-game
  [game]
  (assoc game :field (save-game-field (:field game) (:game-id game))))

(defn create-player
  "Create new player"
  [player]
  (k/insert players
    (k/values {:name (:name player)})))

(defn update-player
  "Update existing player"
  [player]
  (k/update players
    (k/values {:player-id (:id player) :name (:name player)})))