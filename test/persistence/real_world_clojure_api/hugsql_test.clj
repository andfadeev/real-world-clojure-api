(ns persistence.real-world-clojure-api.hugsql-test
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [real-world-clojure-api.core :as core]
            [real-world-clojure-api.sql.queries :as queries])
  (:import (org.testcontainers.containers PostgreSQLContainer)))


(defmacro with-system
  [[bound-var binding-expr] & body]
  `(let [~bound-var (component/start ~binding-expr)]
     (try
       ~@body
       (finally
         (component/stop ~bound-var)))))

(defn datasource-only-system
  [config]
  (component/system-map
    :datasource (core/datasource-component config)))

(defn create-database-container
  []
  (PostgreSQLContainer. "postgres:15.4"))

(deftest todo-table-test
  (let [database-container (create-database-container)]
    (try
      (.start database-container)
      (with-system
        [sut (datasource-only-system
               {:db-spec {:jdbcUrl (.getJdbcUrl database-container)
                          :username (.getUsername database-container)
                          :password (.getPassword database-container)}})]
        (let [{:keys [datasource]} sut
              insert-results [(queries/insert-todo! sut "my todo list")
                              (queries/insert-todo! sut "other todo list")]
              select-results [] #_(jdbc/execute!
                               (datasource)
                               (-> {:select :*
                                    :from :todo}
                                   (sql/format))
                               {:builder-fn rs/as-unqualified-lower-maps})]
          #_(is (= ["INSERT INTO todo (title) VALUES (?), (?) RETURNING *"
                  "my todo list"
                  "other todo list"] insert-query ))
          (is ( = [] insert-results))
          (is (= [] sut))
          (is (= 2 (:c (queries/find-todos-count sut))))
          (is (= 2
                 (count insert-results)
                 (count select-results)))
          (is (= #{"my todo list"
                   "other todo list"}
                 (->> insert-results (map :title) (into #{}))
                 (->> select-results (map :title) (into #{}))))))
      (finally
        (.stop database-container)))))