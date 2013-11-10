(ns dots.web.websocket
  (:use [clojure.string :only [split]])
  (:require [clojure.tools.logging :as log]
            [org.httpkit.server :as hk]
            [org.httpkit.timer :as timer]
            [clj-wamp.server :as w]))

;based on clj-wamp-example (https://github.com/cgmartin/clj-wamp-example)

;topic urls
(def ws-base-url "http://dots")
(def call-base-url (str ws-base-url "/c#"))
(def topic-base-url (str ws-base-url "/t#"))

(defn call-url [path] (str call-base-url path))
(defn topic-url [path] (str topic-base-url path))

;; Main http-kit/WAMP WebSocket handler

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

;define callbacks for default topics (only game-list for now)
(def publish-callbacks (ref {(topic-url "game-list") pass-through}))
(def subscribe-callbacks (ref {(topic-url "game-list") true}))

(defn create-topic [name]
  (dosync
    (log/trace "Create topic for new game: " name)
    (ref-set subscribe-callbacks (assoc @subscribe-callbacks name true))
    (ref-set publish-callbacks (assoc @publish-callbacks name pass-through))
    true))

(defn create-game [game-settings]
  ;TODO create new game (dots.game...) and create a topic for it
  ;TODO return topic id as a response
  ;consider topic name == game-id
  (log/info "Create game for settings: " game-settings)
  )

(defn wamp-handler
  "Returns a http-kit websocket handler with wamp subprotocol"
  [req]
  (hk/with-channel req channel
    (w/http-kit-handler channel
      {
        :on-open ws-on-open
        :on-close ws-on-close
        :on-call {(call-url "create-game") create-game}
        :on-subscribe subscribe-callbacks
        :on-publish publish-callbacks
        })))