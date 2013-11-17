(ns dots.web.routes
  (:use [compojure.core :only [defroutes GET]]
        (ring.middleware [keyword-params :only [wrap-keyword-params]]
          [params :only [wrap-params]]
          [session :only [wrap-session]])
        [dots.web.websocket :only [wamp-handler]])
  (:require [compojure.route :as route]
            [clojure.tools.logging :as log]))

;Application routes
(defroutes server-routes
  (GET "/" [:as req]
    {:status 200 :body "<html><body><center>This is not the page you are looking for.</center></body></html>"})
  (GET "/ws" [:as req] (wamp-handler req)))

;Ring middleware
(defn wrap-failsafe [handler]
  (fn [req]
    (try (handler req)
      (catch Exception e
        (log/error e "Error handling request" req)
        {:status 500 :body "Sorry, an error occured."}))))

(defn app []
  (-> server-routes
    wrap-session
    wrap-keyword-params
    wrap-params
    wrap-failsafe))