(ns real-world-clojure-api.components.pedestal-component
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.http.content-negotiation :as content-negotiation]
            [io.pedestal.http.body-params :as body-params]
            [cheshire.core :as json]
            [next.jdbc :as jdbc]
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


(def response-from-database
  {:name :response-from-database
   :enter
   (fn [{:keys [dependencies] :as context}]
     (let [{:keys [ds]} dependencies
           db-response (first (jdbc/execute! (ds) ["SHOW SERVER_VERSION"]))]
       (clojure.pprint/pprint db-response)
       (assoc context :response {:status 200
                                 :body (str "From database: " (:server_version db-response))})))})

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
      ["/response-from-database" :get response-from-database :route-name :response-from-database]
      ["/todo/:todo-id" :get get-todo-handler :route-name :get-todo]
      ["/todo" :post [(body-params/body-params) post-todo-handler] :route-name :post-todo]
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
   in-memory-state-component
   ds]
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