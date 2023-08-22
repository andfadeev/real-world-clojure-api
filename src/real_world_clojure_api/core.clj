(ns real-world-clojure-api.core
  (:require [com.stuartsierra.component :as component]
            [next.jdbc.connection :as connection]
            [real-world-clojure-api.config :as config]
            [real-world-clojure-api.components.example-component
             :as example-component]
            [real-world-clojure-api.components.pedestal-component
             :as pedestal-component]
            [real-world-clojure-api.components.in-memory-state-component
             :as in-memory-state-component]
            [real-world-clojure-api.components.htmx-pedestal-component
             :as htmx-pedestal-component])
  (:import (com.zaxxer.hikari HikariDataSource)))

(defn real-world-clojure-api-system
  [config]
  (clojure.pprint/pprint config)
  (component/system-map
    :example-component (example-component/new-example-component config)
    :in-memory-state-component (in-memory-state-component/new-in-memory-state-component config)

    :ds (connection/component HikariDataSource (:db-spec config))

    :pedestal-component
    (component/using
      (pedestal-component/new-pedestal-component config)
      [:example-component
       :ds
       :in-memory-state-component])

    :htmx-pedestal-component
    (component/using
      (htmx-pedestal-component/new-htmx-pedestal-component config)
      [:in-memory-state-component])))

(defn -main
  []
  (let [system (-> (config/read-config)
                   (real-world-clojure-api-system)
                   (component/start-system))]
    (println "Starting Real-World Clojure API Service with config")
    (.addShutdownHook
      (Runtime/getRuntime)
      (new Thread #(component/stop-system system)))))