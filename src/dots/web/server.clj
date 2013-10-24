(ns dots.web.ring
  (:require [compojure.core :as c]
            [clojure.core.match]
            [org.httpkit.server :as hk]
            [ring.middleware.params :as p]
            [clojure.data.json :as json]
            [dots.web.rooms :as rooms]
            [dots.core.game :as game]
            [dots.core.player :as player]))

(defn broadcast-to-room
  [room-id message]
  (doseq [channel (get @rooms/room2ch (keyword (str room-id)))]
    (hk/send! channel message)))

;Define routes to dispatch actions based on :action key in request
;TODO: try to replace all this (:xxx request) with destructuring in called functions
;TODO: remove the macro shit and simplify the flow of websocket routing - less functions to get to the handler
;TODO: think of a unified way to send a response from the ws handler
(defn ws-routes [request channel]
  (clojure.core.match/match [(:action request)]
    ["get-players"] (player/load-all-players) ;get list of all players - why?
    ["get-player"] (player/load-player (:player-id request)) ;get player by id - get all info about a specific player
    ["create-player"] (player/create-player (:player-name request)) ;create new player
    ["update-player"] (player/save-player (:player-id request)) ;update player
    ["start-game"] (game/create-game (:invite-id request)) ;start a game from invite, delete invite
    ["put-dot"] (game/put-dot (:game-id request) (:x request) (:y request) (:player-id request)) ;put a new dot
    ["save-game"] (game/save-game (get @game/games (:game-id request))) ;pause and save a game
    ["leave-game"] () ;leave a game, can't continue
    ["create-invite"] () ;(game/create-invite (:player-id request) (:width request) (:height request)) ;create new invite for  game with current player
    ["get-invites"] () ;(game/get-all-invites) ;get a list of all invites with one player
    ["join-room"] (rooms/add-channel-to-room (:room-id request) channel) ;to join echo test room
    ["send-message"] (broadcast-to-room (:room-id request) (:message request)) ;send smth to echo test room
    ["connect"] (rooms/add-channel-to-room "global" channel) ;join global room to get updates
    ))

;get data from channel, deserialize it into an object,
;pass it to routes for processing
(defn process-request
  "Returns a fn that will process requests"
  [request channel]
  (fn [data]
    (let [request-data (json/read-str data :key-fn keyword)
          response (ws-routes request-data channel)]
      ;      (hk/send! channel (json/write-str response))
      )))

;create a http-kit websocket handler that will pass all request to custom ws-routes
(defn websocket-handler [request]
  (hk/with-channel request channel
    (hk/on-close channel (fn [status] (println "channel closed: " status)))
    (hk/on-receive channel (process-request request channel))))

;create ring routes for dots server
(c/defroutes dots-routes
  ;websocket entry point
  (c/GET "/d" [] websocket-handler))

;setup ring routes and middleware
(def dots-web
  (-> dots-routes
    p/wrap-params))

;start http-kit server
(defn start-server
  (hk/run-server dots-web {:port 9090}))