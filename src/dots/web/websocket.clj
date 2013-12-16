(ns dots.web.websocket
  (:require [clojure.tools.logging :as log]
            [clj-wamp.server :as w]
            [dots.core.game :as game]
            [dots.core.player :as player]))
;based on clj-wamp-example (https://github.com/cgmartin/clj-wamp-example)

;topic urls
(def ws-base-url "http://dots/")
(def call-base-url (str ws-base-url ""))
(def topic-base-url (str ws-base-url "/topic/"))

(defn call-url [path] (str call-base-url path))
(defn topic-url [path] (str topic-base-url path))

(def games-list-url (topic-url "allGames"))
(def register-url (call-url "register"))
(def create-game-url (call-url "createGame"))
(def join-game-url (call-url "joinGame"))
(def get-all-games-url (call-url "getAllGames"))
(def get-game-info-url (call-url "getGameInfo"))

(defn ws-on-open
  "Log new user connected"
  [sess-id]
  (log/info "New websocket client connected [" sess-id "]"))

(defn ws-on-close
  "Log a user disconnected"
  [sess-id status]
  (log/info "Websocket client disconnected [" sess-id "] " status))

;TODO fix publish callbacks to support passing events through
(defn pass-through [sess-id topic event exclude eligible]
  [sess-id topic event exclude eligible])

(defn send-games-list [session-id topic]
  (when (= topic games-list-url)
    (w/emit-event! topic @game/games (list session-id))))

;define callbacks for default topics (only game-list for now)
(def publish-callbacks (ref {games-list-url pass-through}))
(def subscribe-callbacks (ref {games-list-url true
                               :on-after      send-games-list}))

(defn create-topic [name]
  (dosync
    (log/trace "Create topic for new game: " name)
    (ref-set subscribe-callbacks (assoc @subscribe-callbacks name true))
    (ref-set publish-callbacks (assoc @publish-callbacks name pass-through))
    name))

(defn create-game [game-settings]
  ;consider topic name == game-id
  (log/info "Create game for settings: " game-settings)
  (let [{:keys [p1Id p1Name p2Id width height name]} (clojure.walk/keywordize-keys game-settings)
        new-game (game/create-game p1Id p2Id width height)
        id (:game-id new-game)
        player (get @player/players p1Id)]
    (if-not (= (:name player) p1Name)
      (player/save-player (assoc player :name p1Name)))
    (create-topic id)
    (w/send-event! games-list-url @game/games)
    id))

(defn register-player []
  (let [new-player (player/create-player "Anonymous " true)]
    (assoc new-player :name (str (:name new-player) (:player-id new-player)))))

(defn get-all-games []
  (game/load-game)
  @game/games)

(defn join-game []
  )

;; Main http-kit/WAMP WebSocket handler
(defn wamp-handler
  "Returns a http-kit websocket handler with wamp subprotocol"
  [req]
  (w/with-channel-validation req channel #".*"
                             (w/http-kit-handler channel
                                                 {:on-open      ws-on-open
                                                  :on-close     ws-on-close
                                                  :on-call      {create-game-url   create-game
                                                                 join-game-url     join-game
                                                                 register-url      register-player
                                                                 get-all-games-url get-all-games
                                                                 get-game-info-url get-detailed-game-info
                                                                 }
                                                  :on-subscribe subscribe-callbacks
                                                  :on-publish   publish-callbacks})))