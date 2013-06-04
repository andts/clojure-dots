(ns dots.db
  (:require [korma.core :as k]
            [korma.db :as db]
            [dots.field :as field]
            [dots.util :as util]))

(def config (util/load-properties (clojure.java.io/resource "dots.properties")))

(db/defdb db (db/mysql {:host (:dots.db.host config)
                        :port (:dots.db.port config)
                        :db "dots"
                        :user (:dots.db.user config)
                        :password (:dots.db.password config)}))

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
  (k/belongs-to players {:fk :player1-id})
  (k/belongs-to players {:fk :player2-id}))

(defn load-game-field
  "Get game field by game id"
  [game-id]
  (let [query-result (first (k/select games (k/with fields (k/with dots)) (k/where {:games.game-id game-id})))
        field-size {:width (:width query-result) :height (:height query-result)}
        game-field {:size field-size :field-id (:field-id query-result)}]
    (reduce #(field/put-dot %1 {:x (:x %2) :y (:y %2) :type (keyword (:type %2))}) game-field (:dots query-result))
    ))

(defn- save-dot
  "Save dot in db, if it doesn't already exist"
  [field dot-entry]
  (let [dot (second dot-entry)
        dot-id (first dot-entry)]
    (k/insert dots
      (k/modifier "IGNORE")
      (k/values {:dot-id dot-id
                 :field-id (:field-id field)
                 :x (:x dot) :y (:y dot)
                 :type (name (:type dot))}
        ))
    field
    ))

(defn- create-game-field
  [field game-id]
  (let [new-field-id (:GENERATED_KEY (k/insert fields
                                       (k/values [{:width (get-in field [:size :width ])
                                                   :height (get-in field [:size :height ])
                                                   :game-id game-id}])))]
    (assoc field :field-id new-field-id)
    ))

(defn- save-game-field
  "Save game field in db"
  [field game-id]
  (let [saved-field (if (contains? field :field-id )
                      field
                      (create-game-field field game-id))]
    (prn saved-field)
    (reduce save-dot saved-field (:dots saved-field))
    ))

(defn create-game
  [player1 player2]
  (k/insert games (k/values {:player1-id player1 :player2-id player2})))

(defn save-game
  "Update game in db: save field, history, etc."
  [game]
  (assoc game :field (save-game-field (:field game) (:game-id game))))

(defn load-game
  [game-id]
  (first (k/select games (k/where {:games.game-id game-id}))))

(defn create-player
  "Create new player"
  [player]
  (k/insert players
    (k/values {:name (:name player)})))

(defn save-player
  "Update existing player"
  [player]
  (k/update players
    (k/set-fields {:name (:name player)})
    (k/where (= :player-id (:id player)))
    ))