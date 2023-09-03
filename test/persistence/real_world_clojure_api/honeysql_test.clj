(ns persistence.real-world-clojure-api.honeysql-test
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [real-world-clojure-api.core :as core]
            [honey.sql :as sql])
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

(deftest migrations-test
  (let [database-container (create-database-container)]
    (try
      (.start database-container)
      (with-system
        [sut (datasource-only-system
               {:db-spec {:jdbcUrl (.getJdbcUrl database-container)
                          :username (.getUsername database-container)
                          :password (.getPassword database-container)}})]
        (let [{:keys [datasource]} sut
              select-query (sql/format {:select :*
                                        :from :schema-version})
              [schema-version :as schema-versions]
              (jdbc/execute!
                (datasource)
                select-query
                {:builder-fn rs/as-unqualified-lower-maps})]
          (is (= ["SELECT * FROM schema_version"]
                 select-query))
          (is (= 1 (count schema-versions)))
          (is (= {:description "add todo tables"
                  :script "V1__add_todo_tables.sql"
                  :success true}
                 (select-keys schema-version [:description :script :success])))))
      (finally
        (.stop database-container)))))

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
              insert-query (-> {:insert-into [:todo]
                                :columns [:title]
                                :values [["my todo list"]
                                         ["other todo list"]]
                                :returning :*}
                               (sql/format))
              insert-results (jdbc/execute!
                               (datasource)
                               insert-query
                               {:builder-fn rs/as-unqualified-lower-maps})
              select-results (jdbc/execute!
                               (datasource)
                               (-> {:select :*
                                    :from :todo}
                                   (sql/format))
                               {:builder-fn rs/as-unqualified-lower-maps})]
          (is (= ["INSERT INTO todo (title) VALUES (?), (?) RETURNING *"
                  "my todo list"
                  "other todo list"] insert-query ))
          (is (= 2
                 (count insert-results)
                 (count select-results)))
          (is (= #{"my todo list"
                   "other todo list"}
                 (->> insert-results (map :title) (into #{}))
                 (->> select-results (map :title) (into #{}))))))
      (finally
        (.stop database-container)))))