(ns dots.web.ring
  (:require [ring.adapter.jetty :as rj]
            [compojure.core :as c]
            [ring.middleware.params :as p]
            [ring.middleware.json :as json]))

(c/defroutes rest-routes
  (c/GET "/" []
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body {"hello" "world"}})
  (c/POST "/" [name]
    {:status 200
     :headers {"Content-Type" "application/json"}
     :body {"hello" (or name "sssss")}}))

(def dots-rest
  (-> rest-routes
    p/wrap-params
    json/wrap-json-params
    json/wrap-json-response
    ))

(rj/run-jetty dots-rest {:port 8080})