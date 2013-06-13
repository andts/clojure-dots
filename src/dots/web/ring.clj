(ns dots.web.ring
  (:require [compojure.core :as c]
            [org.httpkit.server :as hk]
            [ring.middleware.params :as p]
            [ring.middleware.json :as json]
            [dots.core.player :as player]
            [dots.core.game :as game])
  )

(defn websocket-handler [request]
  (hk/with-channel request channel
    (hk/on-close channel (fn [status] (println "channel closed: " status)))
    (hk/on-receive channel (fn [data]
                          (do
                            (println "received: " data)
                            (hk/send! channel data))))
    ))

(c/defroutes dots-routes
  ;PLAYERS
  ;GET ALL PLAYERS
  (c/GET "/players" []
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (player/load-all-players)})
  ;GET SPECIFIC PLAYER
  (c/GET "/players/:id" [id]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (player/load-player id)})
  ;CREATE NEW PLAYER
  (c/POST "/players" [name]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (player/create-player name)})
  ;GAMES
  ;GET GAME BY ID
  (c/GET "/games/:id" [game-id]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (game/load-game game-id)})
  ;GET GAME BY PLAYER ID
  (c/GET "/games/by-player/:player" [player-id]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body (game/get-all-games-for-player player-id)})
  ;STATIC TEST PAGE
  (c/GET "/dots" [] websocket-handler)
  )

(def dots-web
  (-> dots-routes
    p/wrap-params
    json/wrap-json-params
    json/wrap-json-response
    ))

(hk/run-server dots-web {:port 8080})