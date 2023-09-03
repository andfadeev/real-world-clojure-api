(ns real-world-clojure-api.components.pedestal-component
  (:require [com.stuartsierra.component :as component]
            [honey.sql :as sql]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.http.content-negotiation :as content-negotiation]
            [io.pedestal.http.body-params :as body-params]
            [cheshire.core :as json]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [schema.core :as s]))

(defn response
  ([status]
   (response status nil))
  ([status body]
   (merge
     {:status status
      :headers {"Content-Type" "application/json"}}
     (when body {:body (json/encode body)}))))

(def ok (partial response 200))
(def created (partial response 201))
(def not-found (partial response 404))

(defn get-todo-by-id
  [{:keys [in-memory-state-component]} todo-id]
  (->> @(:state-atom in-memory-state-component)
       (filter (fn [todo]
                 (= todo-id (:id todo))))
       (first)))

(def get-todo-handler
  {:name :get-todo-handler
   :enter
   (fn [{:keys [dependencies] :as context}]
     (let [request (:request context)
           todo (get-todo-by-id dependencies
                                (-> request
                                    :path-params
                                    :todo-id))
           response (if todo
                      (ok todo)
                      (not-found))]
       (assoc context :response response)))})


(def db-get-todo-handler
  {:name :db-get-todo-handler
   :enter
   (fn [{:keys [dependencies] :as context}]
     (let [{:keys [datasource]} dependencies
           todo-id (-> context
                       :request
                       :path-params
                       :todo-id
                       (parse-uuid))
           todo (jdbc/execute-one!
                  (datasource)
                  (-> {:select :*
                       :from :todo
                       :where [:= :todo-id todo-id]}
                      (sql/format))
                  {:builder-fn rs/as-unqualified-kebab-maps})
           response (if todo
                      (ok todo)
                      (not-found))]
       (assoc context :response response)))})


(def info-handler
  {:name :info-handler
   :enter
   (fn [{:keys [dependencies] :as context}]
     (let [{:keys [datasource]} dependencies
           db-response (first (jdbc/execute!
                                (datasource)
                                ["SHOW SERVER_VERSION"]))]
       (assoc context :response
              {:status 200
               :body (str "Database server version: " (:server_version db-response))})))})

(comment
  [{:id (random-uuid)
    :name "My todo list"
    :items [{:id (random-uuid)
             :name "Make a new youtube video"
             :status :created}]}
   {:id (random-uuid)
    :name "Empty todo list"
    :items []}])

(defn respond-hello
  [request]
  {:status 200
   :body "Hello world"})

(defn save-todo!
  [{:keys [in-memory-state-component]} todo]
  (swap! (:state-atom in-memory-state-component) conj todo))



(s/defschema
  TodoItem
  {:id s/Str
   :name s/Str
   :status s/Str})

(s/defschema
  Todo
  {:id s/Str
   :name s/Str
   :items [TodoItem]})

(def post-todo-handler
  {:name :post-todo-handler
   :enter
   (fn [{:keys [dependencies] :as context}]
     (let [request (:request context)
           todo (s/validate Todo (:json-params request))]
       (save-todo! dependencies todo)
       (assoc context :response (created todo))))})

(def routes
  (route/expand-routes
    #{["/greet" :get respond-hello :route-name :greet]
      ["/info" :get info-handler :route-name :info]
      ["/todo/:todo-id" :get get-todo-handler :route-name :get-todo]
      ["/todo" :post [(body-params/body-params) post-todo-handler] :route-name :post-todo]

      ["/db/todo/:todo-id" :get db-get-todo-handler :route-name :db-get-todo]
      }))

(def url-for (route/url-for-routes routes))

(defn inject-dependencies
  [dependencies]
  (interceptor/interceptor
    {:name ::inject-dependencies
     :enter (fn [context]
              (assoc context :dependencies dependencies))}))

(def content-negotiation-interceptor
  (content-negotiation/negotiate-content ["application/json"]))

(defrecord PedestalComponent
  [config
   example-component
   datasource
   in-memory-state-component]
  component/Lifecycle

  (start [component]
    (println "Starting PedestalComponent")
    (let [server (-> {::http/routes routes
                      ::http/type :jetty
                      ::http/join? false
                      ::http/port (-> config :server :port)}
                     (http/default-interceptors)
                     (update ::http/interceptors concat
                             [(inject-dependencies component)
                              content-negotiation-interceptor])
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