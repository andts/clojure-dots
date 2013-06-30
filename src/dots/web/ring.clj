(ns dots.web.ring
  (:require [compojure.core :as c]
            [clojure.core.match :as m]
            [org.httpkit.server :as hk]
            [ring.middleware.params :as p]
            [clojure.data.json :as json]
            [dots.core.player :as player]
            [dots.core.game :as game]))

(defmacro define-routes
  "Creates a function that will define routes"
  [fn-name & actions]
  `(defn ~fn-name
     [~'request ~'channel]
     (clojure.core.match/match [(:action ~'request)]
       ~@actions)))

(define-routes dots-ws-routes
  "Define routes to dispatch actions based on :action key in request"
  ["get-players"] (player/load-all-players) ;get list of all players - why?
  ["get-player"] (player/load-player (:player-id request)) ;get player by id - get all info about a specific player
  ["create-player"] (player/create-player (:player-name request)) ;create new player
  ["update-player"] (player/save-player (:player-id request)) ;update player
  ["start-game"] () ;start a game from invite, delete invite
  ["put-dot"] (game/put-dot (:game-id request) (:x request) (:y request) (:player-id request)) ;put a new dot
  ["save-game"] (game/save-game ((:game-id request) @game/games)) ;pause and save a game
  ["leave-game"] () ;leave a game, can't continue
  ["create-invite"] () ;create new invite for  game with current player
  ["get-invites"] () ;get a list of all invites with one player
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