(ns persistence.real-world-clojure-api.simple-persistence-test
  (:require [clojure.test :refer :all]
            [next.jdbc :as jdbc])
  (:import (org.testcontainers.containers PostgreSQLContainer)))

(deftest a-simple-persistence-test
  (let [database-container (doto (PostgreSQLContainer. "postgres:15.4")
                             (.withDatabaseName "real-world-clojure-api-db")
                             (.withUsername "test")
                             (.withPassword "test"))]
    (try
      (.start database-container)
      (let [ds (jdbc/get-datasource {:jdbcUrl (.getJdbcUrl database-container)
                                     :user (.getUsername database-container)
                                     :password (.getPassword database-container)})]
        (is (= {:r 1} (first (jdbc/execute! ds ["select 1 as r;"])))))
      (finally
        (.stop database-container)))))

