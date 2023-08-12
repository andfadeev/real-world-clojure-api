(ns real-world-clojure-api.core
  (:require [com.stuartsierra.component :as component]
            [real-world-clojure-api.config :as config]
            [real-world-clojure-api.components.example-component
             :as example-component]
            [real-world-clojure-api.components.pedestal-component
             :as pedestal-component]))

(defn real-world-clojure-api-system
  [config]
  (component/system-map
    :example-component (example-component/new-example-component config)
    :pedestal-component
    (component/using
      (pedestal-component/new-pedestal-component config)
      [:example-component])))

(defn -main
  []
  (let [system (-> (config/read-config)
                   (real-world-clojure-api-system)
                   (component/start-system))]
    (println "Starting Real-World Clojure API Service with config")
    (.addShutdownHook
      (Runtime/getRuntime)
      (new Thread #(component/stop-system system)))))