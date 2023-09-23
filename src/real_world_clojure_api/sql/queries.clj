(ns real-world-clojure-api.sql.queries
  (:require [hugsql.core :as hugsql]))


(declare insert-todo-query
         todo-by-id-query
         find-all-todos-query
         find-todos-count-query)

(hugsql/def-db-fns
  "real_world_clojure_api/sql/queries.sql")


(defn insert-todo!
  [{:keys [datasource]} title]
  (insert-todo-query (datasource) {:title title}))

(defn todo-by-id
  [{:keys [datasource]} todo-id]
  (todo-by-id-query (datasource) {:todo_id todo-id}))

(defn find-all-todos-query
  [{:keys [datasource]}]
  (find-all-todos-query (datasource)))

(defn find-todos-count
  [{:keys [datasource]}]
  (find-todos-count-query (datasource)))