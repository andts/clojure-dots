(ns dots.web.ring
  (:require [compojure.core :as c]
            [clojure.core.match :as m]
            [org.httpkit.server :as hk]
            [ring.middleware.params :as p]
            [clojure.data.json :as json]
            [dots.core.player :as player]
            [dots.core.game :as game])
  )

(defn websocket-handler [request]
  (hk/with-channel request channel
    (hk/on-close channel (fn [status] (println "channel closed: " status)))
    (hk/on-receive channel (fn [data]
                             (do
                               (println data)
                               (let [request-data (json/read-str data :key-fn keyword)
                                     response (m/match [(:request request-data)]
                                                ["get-players"] (player/load-all-players)
                                                ["get-player"] (player/load-player (:id request-data))
                                                )]
                                 (hk/send! channel (json/write-str response))))))))

(c/defroutes dots-routes
  ;websocket entry point
  (c/GET "/d" [] websocket-handler))

(def dots-web
  (-> dots-routes
    p/wrap-params))

(hk/run-server dots-web {:port 9090})