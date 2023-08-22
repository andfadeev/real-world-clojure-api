(ns real-world-clojure-api.components.in-memory-state-component
  (:require [com.stuartsierra.component :as component]))

(defrecord InMemoryStateComponent
  [config]
  component/Lifecycle

  (start [component]
    (println "Starting InMemoryStateComponent")
    (assoc component
           :state-atom (atom [])
           :htmx-click-to-edit-state
           (atom {"1" {:first-name "changeit"
                       :last-name "changeit"
                       :email "change@it.com"}
                  "2" {:first-name "user2"
                       :last-name "user2"
                       :email "change@it.com"}
                  "3" {:first-name "user3"
                       :last-name "user3"
                       :email "change@it.com"}})))
  (stop [component]
    (println "Stopping InMemoryStateComponent")
    (assoc component
           :state-atom nil
           :htmx-click-to-edit-state nil)))

(defn new-in-memory-state-component
  [config]
  (map->InMemoryStateComponent {:config config}))