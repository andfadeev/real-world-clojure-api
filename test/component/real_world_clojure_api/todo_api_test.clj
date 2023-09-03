(ns component.real-world-clojure-api.todo-api-test
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [honey.sql :as sql]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [real-world-clojure-api.components.pedestal-component :refer [url-for]]
            [real-world-clojure-api.core :as core]
            [clj-http.client :as client])
  (:import (java.net ServerSocket)
           (org.testcontainers.containers PostgreSQLContainer)))

(defmacro with-system
  [[bound-var binding-expr] & body]
  `(let [~bound-var (component/start ~binding-expr)]
     (try
       ~@body
       (finally
         (component/stop ~bound-var)))))

(defn sut->url
  [sut path]
  (str/join ["http://localhost:"
             (-> sut :pedestal-component :config :server :port)
             path]))

(defn get-free-port
  []
  (with-open [socket (ServerSocket. 0)]
    (.getLocalPort socket)))
(deftest get-todo-test
  (let [database-container (PostgreSQLContainer. "postgres:15.4")]
    (try
      (.start database-container)
      (with-system
        [sut (core/real-world-clojure-api-system
               {:server {:port (get-free-port)}
                :htmx {:server {:port (get-free-port)}}
                :db-spec {:jdbcUrl (.getJdbcUrl database-container)
                          :username (.getUsername database-container)
                          :password (.getPassword database-container)}})]
        (let [{:keys [datasource]} sut
              {:keys [todo-id
                      title]} (jdbc/execute-one!
                                (datasource)
                                (-> {:insert-into [:todo]
                                     :columns [:title]
                                     :values [["My todo for test"]]
                                     :returning :*}
                                    (sql/format))
                                {:builder-fn rs/as-unqualified-kebab-maps})
              {:keys [status body]} (-> (sut->url sut
                                                  (url-for :db-get-todo
                                                           {:path-params {:todo-id todo-id}}))
                                        (client/get {:accept :json
                                                     :as :json
                                                     :throw-exceptions false})
                                        (select-keys [:body :status]))]
          (is (= 200 status))
          (is (some? (:created-at body)))
          (is (= {:todo-id (str todo-id)
                  :title title}
                 (select-keys body [:todo-id :title])
                 )))
        (testing "Empty body is return for random todo id"
          (is (= {:body ""
                  :status 404}
                 (-> (sut->url sut
                               (url-for :db-get-todo
                                        {:path-params {:todo-id (random-uuid)}}))
                     (client/get {:throw-exceptions false})
                     (select-keys [:body :status]))
                 ))))
      (finally
        (.stop database-container)))))