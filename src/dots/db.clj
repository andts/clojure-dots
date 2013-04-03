(ns dots.db
  (:require [korma.core :as k]
            [korma.db :as db]
            [dots.core]
            [clojure.pprint]
            ))

(db/defdb db (db/mysql {:host "localhost"
                        :port "3306"
                        :db "dots"
                        :user "sysadm"
                        :password "netcracker"}))

(k/defentity dots
  (k/pk :dot-id )
  (k/entity-fields :x :y ))

(k/defentity fields
  (k/pk :field-id )
  (k/entity-fields :width :height )
  (k/has-many dots {:fk :field-id}))

(k/defentity games
  (k/pk :game-id )
  (k/entity-fields :player1-id :player2-id )
  (k/has-one fields {:fk :game-id}))

(defn get-game-field
  "Get game field by game id"
  [game-id]
  (let [query-result (first (k/select games (k/with fields (k/with dots)) (k/where {:games.game-id game-id})))
        field-size {:width (:width query-result) :height (:height query-result)}
        field {:size field-size :edges {}}]
    (reduce #(dots.core/put-dot %1 (:x %2) (:y %2)) field (:dots query-result))
    ))
