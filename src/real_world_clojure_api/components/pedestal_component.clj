(ns real-world-clojure-api.components.pedestal-component
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]))



(defn respond-hello
  [request]
  {:status 200
   :body "Hello world"})

(def routes
  (route/expand-routes
    #{["/greet" :get respond-hello :route-name :greet]}))

(defrecord PedestalComponent
  [config example-component]
  component/Lifecycle

  (start [component]
    (println "Starting PedestalComponent")
    (let [server (-> {::http/routes routes
                      ::http/type :jetty
                      ::http/join? false
                      ::http/port (-> config :server :port)}
                     (http/create-server)
                     (http/start))]
      (assoc component :server server)))

  (stop [component]
    (println "Stopping PedestalComponent")
    (when-let [server (:server component)]
      (http/stop server))
    (assoc component :server nil)))

(defn new-pedestal-component
  [config]
  (map->PedestalComponent {:config config}))