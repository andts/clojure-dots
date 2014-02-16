(ns dots.core.game
  (:require [dots.core.player :as player]
            [dots.field :as field]
            [dots.util :as util]
            [dots.db :as db]))

(def games
  "game structure
  {
    :game-id 123
    :players {100 :red
              101 :blue}
    :field   {:size      {:width 5 :height 5}

             ;map edge id to edge data (dots ids)
             :edges     {1 [1 2]
                         2 [2 3]}

             ;map dot id to dot data
             :dots      {1 {:x 1 :y 1 :type :red}
                         2 {:x 1 :y 2 :type :blue}}

             ;map field position to dot id, use to find neighbours of a dot
             :field-map {[1 1] 1
                         [1 2] 2}

             ;list of cycles, where cycle is a list of edge ids that create a cycle
             :cycles    '()}

    :state   #{:started :finished :closed}
  }"
  (ref {}))

(defn save-game-inmemory
  [game]
  (util/save-inmemory games :game-id game)
  game)

(defn create-game
  "Create a new game from supplied parameters, or from existing invite, and then delete the invite"
  ([player1-id player2-id width height]
   (let [game {:game-id (:GENERATED_KEY (db/create-game player1-id player2-id))
               :players {player1-id :red
                         player2-id :blue}
               :field   {:size {:width  width
                                :height height}}
               :state   :started
               }]
     (save-game-inmemory game)
     game))
  ([invite]
   (when (= (invite :state) :CLOSED)
     (let [player1-id (get-in invite [:gameInfo :player1 :id])
           player2-id (get-in invite [:gameInfo :player2 :id])
           player1-color (get-in invite [:gameInfo :player1Color])
           player2-color (if (= player1-color :RED) :BLUE :RED)
           game {:game-id (:GENERATED_KEY (db/create-game player1-id player2-id))
                 :players {player1-id player1-color
                           player2-id player2-color}
                 :field   {:size {:width  (get-in invite [:gameInfo :width])
                                  :height (get-in invite [:gameInfo :height])}}
                 :state   :STARTED
                 }]
       (save-game-inmemory game)
       game))))

(defn save-game
  [game]
  (if-let [saved-game (db/save-game game)]
    (do
      (save-game-inmemory saved-game))))

(defn load-game
  [game-id]
  (if-let [game-data (db/load-game game-id)]
    (do
      (player/load-player (:player1-id game-data))
      (player/load-player (:player2-id game-data))
      (let [game {:game-id (:game-id game-data)
                  :players {(:player1-id game-data) :red
                            (:player2-id game-data) :blue}
                  :field   (db/load-game-field game-id)}]
        (save-game-inmemory game)))))

(defn get-all-games-for-player
  [player-id]
  (db/get-all-games-for-player player-id))

(defn get-all-games []
  (let [games (db/get-all-games)]))

(defn put-dot
  "Add new dot to game field"
  [game-id x y player-id]
  (if-let [game (get @games game-id)]
    (let [field-with-dot (field/put-dot (:field game) {:x x :y y :type (get-in game [:players player-id])})
          new-game (assoc game :field field-with-dot)]
      (save-game-inmemory new-game))))