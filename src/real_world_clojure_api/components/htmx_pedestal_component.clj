(ns real-world-clojure-api.components.htmx-pedestal-component
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]
            [real-world-clojure-api.routes.htmx.click-to-edit
             :as click-to-edit]
            [real-world-clojure-api.routes.htmx.infinite-scroll
             :as infinite-scroll]
            [real-world-clojure-api.routes.htmx.active-search
             :as active-search]
            [real-world-clojure-api.routes.htmx.delete-with-confirmation
             :as delete-with-confirmation]
            [real-world-clojure-api.routes.htmx.comments-section
             :as comments-section]))

(def routes
  (route/expand-routes
    (into #{}
          (concat click-to-edit/routes
                  infinite-scroll/routes
                  active-search/routes
                  delete-with-confirmation/routes
                  comments-section/routes))))

(defn inject-dependencies
  [dependencies]
  (interceptor/interceptor
    {:name ::inject-dependencies
     :enter (fn [context]
              (assoc context :dependencies dependencies))}))

(defrecord HTMXPedestalComponent
  [config
   in-memory-state-component]
  component/Lifecycle

  (start [component]
    (println "Starting HTMXPedestalComponent")
    (let [server (-> {::http/routes routes
                      ::http/type :jetty
                      ::http/join? false
                      ::http/port (-> config :htmx :server :port)
                      ::http/secure-headers {:content-security-policy-settings {:object-src "none"}}}
                     (http/default-interceptors)
                     (update ::http/interceptors concat
                             [(inject-dependencies component)])
                     (http/create-server)
                     (http/start))]
      (assoc component :server server)))

  (stop [component]
    (println "Stopping HTMXPedestalComponent")
    (when-let [server (:server component)]
      (http/stop server))
    (assoc component :server nil)))

(defn new-htmx-pedestal-component
  [config]
  (map->HTMXPedestalComponent {:config config}))