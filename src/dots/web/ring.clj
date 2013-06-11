(ns dots.web.ring
  (:require [ring.adapter.jetty :as rj]
            [compojure.core :as c]
            [ring.middleware.params :as p]
            [ring.middleware.json :as json]
            [dots.core.player :as player]
            [dots.core.game :as game]))

(c/defroutes rest-routes
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
  )

(def dots-rest
  (-> rest-routes
    p/wrap-params
    json/wrap-json-params
    json/wrap-json-response
    ))

(rj/run-jetty dots-rest {:port 8080})