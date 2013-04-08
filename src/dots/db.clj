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
  (k/has-one fields {:fk :game-id}))

(defn load-game-field
  "Get game field by game id"
  [game-id]
  (let [query-result (first (k/select games (k/with fields (k/with dots)) (k/where {:games.game-id game-id})))
        field-size {:width (:width query-result) :height (:height query-result)}
        game-field {:size field-size}]
    (reduce #(field/put-dot %1 {:x (:x %2) :y (:y %2) :type (keyword (:type %2))}) game-field (:dots query-result))
    ))

(defn- create-game-placeholder
  [player1 player2]
  (k/insert games (k/values {:player1-id player1 :player2-id player2})))

(defn- save-dot
  [field dot]
  (if (:fresh dot)
    (update-in field [:dots ()])
    field)
  )

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