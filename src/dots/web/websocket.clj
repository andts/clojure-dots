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
(def topic-base-url (str ws-base-url "topic/"))

(defn call-url [path] (str call-base-url path))
(defn topic-url [path] (str topic-base-url path))

(def invites-list-url (topic-url "allInvites"))

;rpc actions
(def register-url (call-url "register"))
(def change-name-url (call-url "changeName"))
(def create-invite-url (call-url "createInvite"))
(def join-invite-url (call-url "joinInvite"))
(def leave-invite-url (call-url "leaveInvite"))
(def player-ready-url (call-url "playerReady"))
(def player-not-ready-url (call-url "playerNotReady"))
(def start-game-url (call-url "startGame"))
(def get-player-info-url (call-url "getPlayerInfo"))
(def get-invite-info-url (call-url "getInviteInfo"))
(def get-all-invites-url (call-url "getAllInvites"))
(def put-dot-url (call-url "putDot"))
(def add-loop-url (call-url "addLoop"))

(defn ws-on-open
  "Log new user connected"
  [sess-id]
  (log/info "New websocket client connected [" sess-id "]"))

(defn ws-on-close
  "Log a user disconnected"
  [sess-id status]
  (log/info "Websocket client disconnected [" sess-id "] " status))

(defn send-games-list [session-id topic]
  (when (= topic invites-list-url)
    (w/emit-event! topic @invite/invites (list session-id))))

;forward declaration of default topics
(declare subscribe-callbacks publish-callbacks pass-through)

(defn create-topic [name]
  (dosync
    (log/info "Create topic for new game: " name)
    (alter subscribe-callbacks assoc name true)
    (alter publish-callbacks assoc name pass-through)
    name))

(defn create-invite [{game-info :gameInfo search-filter :searchFilter :as invite-data}]
  ;consider topic name == game-id
  (log/info "Create invite for settings: " invite-data)
  (let [invite-player-name (get-in game-info [:player1 :name])
        player-id (get-in game-info [:player1 :id])
        current-player-info (get @player/players player-id)
        player-info (if (= invite-player-name (:name current-player-info))
                      current-player-info
                      (player/save-player (assoc current-player-info :name invite-player-name)))
        new-invite (invite/create-invite game-info search-filter player-info)
        invite-id (:inviteId new-invite)
        sess-id w/*call-sess-id*]
    (log/info "New Invite: " new-invite)
    (create-topic (topic-url invite-id))
    (log/info "Send event: " new-invite)
    (w/broadcast-event! invites-list-url {(:inviteId new-invite) new-invite} (list sess-id))
    (log/info "Event sent...")
    new-invite))

(defn register-player
  ([]
   (player/create-player "Anonymous" true))
  ([name]
   (player/create-player name true)))

(defn change-name
  [player-id player-name]
  (let [player-info (get @player/players player-id)]
    (player/save-player (assoc player-info :name player-name))))

(defn get-all-invites []
  ;(game/load-game)
  ;@game/games
  )

(defn join-invite [{invite-id :inviteId player-id :playerId :as params}]
  (let [updated-invite (invite/join-invite invite-id player-id)
        sess-id w/*call-sess-id*]
    (w/send-event! (topic-url invite-id) updated-invite)
    (w/broadcast-event! invites-list-url
                        {:inviteId (updated-invite :inviteId)
                         :state    (updated-invite :state)}
                        sess-id)
    updated-invite))

(defn leave-invite [{invite-id :inviteId player-id :playerId :as params}]
  (let [updated-invite (invite/leave-invite invite-id player-id)
        sess-id w/*call-sess-id*]
    (w/send-event! (topic-url invite-id) updated-invite)
    (w/broadcast-event! invites-list-url updated-invite sess-id)
    updated-invite))

(defn player-ready [{invite-id :inviteId player-id :playerId :as params}]
  (let [updated-invite (invite/set-player-ready invite-id player-id)
        sess-id w/*call-sess-id*]
    (w/broadcast-event! (topic-url invite-id) updated-invite sess-id)
    updated-invite))

(defn player-not-ready [{invite-id :inviteId player-id :playerId :as params}]
  (let [updated-invite (invite/set-player-not-ready invite-id player-id)
        sess-id w/*call-sess-id*]
    (w/broadcast-event! (topic-url invite-id) updated-invite sess-id)
    updated-invite))

(defn get-player-info [player-id]
  (dosync
    (player/get-player player-id)))

(defn get-invite-info [invite-id]
  (dosync
    (get @invite/invites invite-id)))

(defn put-dot [player-id game-id x y]
  (dosync
    (let
        [updated-game (game/put-dot game-id x y player-id)
         sess-id w/*call-sess-id*]
      (w/broadcast-event! (topic-url game-id) updated-game sess-id)
      updated-game)))

(defn add-loop [player-id game-id dots]
  {:response "Not yet implemented"})

;TODO: authentication logic goes here
(defn auth-permissions-fn [sess-id auth-key]
  {:all true})

(defn auth-secret-fn [sess-id auth-key auth-extra]
  nil)

;define WAMP calls/topics
(def rpc-actions {create-invite-url    create-invite
                  register-url         register-player
                  change-name-url      change-name
                  join-invite-url      join-invite
                  leave-invite-url     leave-invite
                  player-ready-url     player-ready
                  player-not-ready-url player-not-ready
                  get-player-info-url  get-player-info
                  get-invite-info-url  get-invite-info
                  put-dot-url          put-dot
                  add-loop-url         add-loop
                  get-all-invites-url  get-all-invites})

;TODO fix publish callbacks to support passing events through
(defn pass-through [sess-id topic event exclude eligible]
  [sess-id topic event exclude eligible])

;define callbacks for default topics (only invite-list for now)
(def publish-callbacks (ref {invites-list-url pass-through}))
(def subscribe-callbacks (ref {invites-list-url true
                               :on-after        send-games-list}))

; Main http-kit/WAMP WebSocket handler
(defn wamp-handler
  "Returns a http-kit websocket handler with wamp subprotocol"
  [req]
  (w/with-channel-validation req channel #".*"
                             (w/http-kit-handler channel
                                                 {:on-open      ws-on-open
                                                  :on-close     ws-on-close
                                                  :on-call      rpc-actions
                                                  :on-subscribe subscribe-callbacks
                                                  :on-publish   publish-callbacks
                                                  :on-auth      {:allow-anon? true
                                                                 :timeout     10000
                                                                 :secret      auth-secret-fn
                                                                 :permissions auth-permissions-fn}})))