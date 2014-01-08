(ns dots.web.websocket
  (:require [clojure.tools.logging :as log]
            [clj-wamp.server :as w]
            [dots.core.game :as game]
            [dots.core.invites :as invite]
            [dots.core.player :as player]))

;based on clj-wamp-example (https://github.com/cgmartin/clj-wamp-example)

;topic urls
(def ws-base-url "http://dots/")
(def call-base-url (str ws-base-url ""))
(def topic-base-url (str ws-base-url "/topic/"))

(defn call-url [path] (str call-base-url path))
(defn topic-url [path] (str topic-base-url path))

(def invites-list-url (topic-url "allInvites"))

;pre-game actions
(def register-url (call-url "register"))
(def create-invite-url (call-url "createInvite"))
(def join-invite-url (call-url "joinInvite"))
(def player-ready-url (call-url "playerReady"))
(def start-game-url (call-url "startGame"))

(def get-all-invites-url (call-url "getAllInvites"))

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
  (when (= topic invites-list-url)
    (w/emit-event! topic @game/games (list session-id))))

;define callbacks for default topics (only game-list for now)
(def publish-callbacks (ref {invites-list-url pass-through}))
(def subscribe-callbacks (ref {invites-list-url true
                               :on-after        send-games-list}))

(defn create-topic [name]
  (dosync
    (log/info "Create topic for new game: " name)
    (ref-set subscribe-callbacks (assoc @subscribe-callbacks name true))
    (ref-set publish-callbacks (assoc @publish-callbacks name pass-through))
    name))

(defn create-invite [game-settings]
  ;consider topic name == game-id
  (log/info "Create invite for settings: " game-settings)
  (let [{:keys [p1Id p1Name p1Color width height name filter]} (clojure.walk/keywordize-keys game-settings)
        new-invite (invite/create-invite p1Id name width height p1Color filter)
        id (:invite-id new-invite)
        player (get @player/players p1Id)]
    (when-not (= (:name player) p1Name)
      (player/save-player (assoc player :name p1Name)))
    (create-topic id)
    (w/send-event! invites-list-url new-invite)
    new-invite))

(defn register-player [name]
  (player/create-player (if (clojure.string/blank? name) "Anonymous" name) true))

(defn get-all-invites []
  ;(game/load-game)
  ;@game/games
  )

(defn join-invite [params] ;change to separate params when wamp web test tool will support it
  (let [{:keys [inviteId playerId]} (clojure.walk/keywordize-keys params)
        updated-invite (invite/join-invite inviteId playerId)]
    (w/send-event! inviteId updated-invite)
    (w/send-event! invites-list-url {:invite-id (updated-invite :invite-id)
                                     :state     (updated-invite :state)})
    updated-invite))

(defn player-ready [params] ;change to separate params when wamp web test tool will support it
  (let [{:keys [inviteId playerId]} (clojure.walk/keywordize-keys params)
        updated-invite (invite/set-player-ready inviteId playerId)]
    (w/send-event! inviteId updated-invite)
    updated-invite))

(defn start-game [params]
  (let [{:keys [inviteId playerId]} (clojure.walk/keywordize-keys params)
        new-game (game/create-game inviteId)
        updated-invite (get @invite/invites inviteId)
        id (new-game :game-id)]
    (w/send-event! inviteId updated-invite)
    updated-invite))

; Main http-kit/WAMP WebSocket handler
(defn wamp-handler
  "Returns a http-kit websocket handler with wamp subprotocol"
  [req]
  (w/with-channel-validation req channel #".*"
                             (w/http-kit-handler channel
                                                 {:on-open      ws-on-open
                                                  :on-close     ws-on-close
                                                  :on-call      {create-invite-url   create-invite
                                                                 register-url        register-player
                                                                 join-invite-url     join-invite
                                                                 player-ready-url    player-ready
                                                                 start-game-url      start-game
                                                                 get-all-invites-url get-all-invites
                                                                 }
                                                  :on-subscribe subscribe-callbacks
                                                  :on-publish   publish-callbacks})))