(ns dots.web.ring
  (:require [compojure.core :as c]
            [clojure.core.match :as m]
            [org.httpkit.server :as hk]
            [ring.middleware.params :as p]
            [clojure.data.json :as json]
            [dots.core.player :as player]
            [dots.core.game :as game]))

;create a map to store all rooms with channels
;this is basically a pubsub, after this works, move to wamp protocol
; rooms structure-
; room-id (equals game-id, but a keyword) to list of connected channels
; {
;   :1(room-id) (ch1(some channel) ch2 ch3)
;   :2 (ch100 ch145)
;   :3 (ch27 ch35)
;   :4 (ch12 ch67 ch20 ch47)
; }

; TODO:
; use rooms to broadcast updates and commands to all clients
; simple flow:
;   1. player1 sends update to server
;   2. server updates the game
;   3. server sends new version of game field to all clients
;   4. all clients except player1 update their game fields on canvas
;   5. server broadcasts a command like "player2's turn"
;   6. all clients except player2 discard the message
;   7. game field for player2 is enabled, he can put a dot
;   8. goto 1
;
; ways to improve:
;   do not just broadcast a message, but send it to specific channel
;   need some structure of a room then, to now which role each channel has (e.g. {:player1 channel, :player2 channel, :spectators (ch3 ch4 ch5)})
;
; maybe extract this rooms "management" to a separate namespace

;managed synchronous storage for rooms
(def rooms (atom {}))

; my own multimap...
(defn add-to-multimap [map key item]
  (if (contains? map key)
    (assoc map key (cons item (get map key)))
    (assoc map key (list item))))

;add a channel to a room by id
(defn add-channel-to-room
  [room-id channel]
  (swap! rooms add-to-multimap (keyword (str room-id)) channel))
;end of map to store rooms


(defmacro define-routes
  "Creates a function that will define routes"
  [fn-name & actions]
  `(defn ~fn-name
     [~'request ~'channel]
     (clojure.core.match/match [(:action ~'request)]
       ~@actions)))

;Define routes to dispatch actions based on :action key in request
(define-routes dots-ws-routes
  ["get-players"] (player/load-all-players) ;get list of all players - why?
  ["get-player"] (player/load-player (:player-id request)) ;get player by id - get all info about a specific player
  ["create-player"] (player/create-player (:player-name request)) ;create new player
  ["update-player"] (player/save-player (:player-id request)) ;update player
  ["start-game"] (game/create-game (:invite-id request)) ;start a game from invite, delete invite
  ["put-dot"] (game/put-dot (:game-id request) (:x request) (:y request) (:player-id request)) ;put a new dot
  ["save-game"] (game/save-game (get @game/games (:game-id request))) ;pause and save a game
  ["leave-game"] () ;leave a game, can't continue
  ["create-invite"] (game/create-invite (:player-id request) (:width request) (:height request)) ;create new invite for  game with current player
  ["get-invites"] (game/get-all-invites) ;get a list of all invites with one player
  )

(defn process-request
  "Returns a fn that will process requests"
  [request channel]
  (fn [data]
    (let [request-data (json/read-str data :key-fn keyword)
          response (dots-ws-routes request-data channel)]
      (hk/send! channel (json/write-str response)))))

(defn websocket-handler [request]
  (hk/with-channel request channel
    (hk/on-close channel (fn [status] (println "channel closed: " status)))
    (hk/on-receive channel (process-request request channel))))

(c/defroutes dots-routes
  ;websocket entry point
  (c/GET "/d" [] websocket-handler))

(def dots-web
  (-> dots-routes
    p/wrap-params))

(hk/run-server dots-web {:port 9090})