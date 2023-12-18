(ns persistence.real-world-clojure-api.event-sourcing-test
  (:require [cheshire.core :as json]
            [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [real-world-clojure-api.core :as core]
            [honey.sql :as sql])
  (:import (org.postgresql.util PGobject)
           (org.testcontainers.containers PostgreSQLContainer)))

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

(defn ->jsonb
  [value]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (json/encode value))))

(defn <-jsonb
  [v]
  (json/decode (.getValue v) true))

(defn insert-event!
  [{:keys [datasource]} event]
  (jdbc/execute!
    (datasource)
    (-> {:insert-into [:events]
         :values [(update event :payload ->jsonb)]
         :returning :*}
        (sql/format))
    {:builder-fn rs/as-unqualified-kebab-maps}))

(defmulti apply-event
          (fn [_ event]
            (keyword
              (str (:aggregate-type event)
                   "/"
                   (:type event)))))

(defmethod apply-event :order/order-created
  [_ event]
  (merge
    {:resource-type (:aggregate-type event)
     :order-id (:aggregate-id event)
     :created-at (:created-at event)}
    (:payload event)))

(defmethod apply-event :order/order-paid
  [state event]
  (merge state
         (:payload event)
         {:updated-at (:created-at event)}))

(defmethod apply-event :order/order-dispatched
  [state event]
  (merge state
         (:payload event)
         {:updated-at (:created-at event)}))

(defn project
  ([events]
   (project {} events))
  ([state events]
   (reduce apply-event state events)))

(defn get-by-aggregate-id
  [{:keys [datasource]} aggregate-id]
  (let [select-query (-> {:select :*
                          :from :events
                          :where [:= :aggregate-id aggregate-id]
                          :order-by [:created-at]}
                         (sql/format))
        events (jdbc/execute!
                 (datasource)
                 select-query
                 {:builder-fn rs/as-unqualified-kebab-maps})]
    (->> events
         (map (fn [event]
                (update event :payload <-jsonb)))
         (project))))

(defn get-all-by-customer-id
  "Just an example when you need to search by some field that exits only inside the payload, in this case by the customer id"
  [{:keys [datasource]} customer-id]
  (let [select-query ["SELECT DISTINCT e1.* FROM events e1
  INNER JOIN events e2 using (aggregate_id)
  WHERE e2.payload ->> 'customer-id' = ?" customer-id]
        events (jdbc/execute!
                 (datasource)
                 select-query
                 {:builder-fn rs/as-unqualified-kebab-maps})]

    (->> events
         (map (fn [event]
                (update event :payload <-jsonb)))
         (group-by :aggregate-id)
         (vals)
         (sort-by :created-at)
         (reverse)
         (map project))))

(deftest event-sourcing-test
  (let [database-container (create-database-container)]
    (try
      (.start database-container)
      (with-system
        [sut (datasource-only-system
               {:db-spec {:jdbcUrl (.getJdbcUrl database-container)
                          :username (.getUsername database-container)
                          :password (.getPassword database-container)}})]
        (let [customer-id (str "customer:" (random-uuid))
              order-id (random-uuid)
              order-created-event {:aggregate-id order-id
                                   :aggregate-type "order"
                                   :type "order-created"
                                   :payload {:items ["x" "y" "z"]
                                             :customer-id customer-id
                                             :price "100.45"
                                             :status "pending"}}
              order-paid-event {:aggregate-id order-id
                                :aggregate-type "order"
                                :type "order-paid"
                                :payload {:status "paid"
                                          :payment-method "CARD"}}
              tracking-number (str "TX-" (random-uuid))
              order-dispatched-event {:aggregate-id order-id
                                      :aggregate-type "order"
                                      :type "order-dispatched"
                                      :payload {:status "dispatched"
                                                :tracking-number tracking-number}}
              other-order-id (random-uuid)
              other-order-created-event {:aggregate-id other-order-id
                                         :aggregate-type "order"
                                         :type "order-created"
                                         :payload {:items ["something"]
                                                   :customer-id customer-id
                                                   :price "99.99"
                                                   :status "pending"}}]
          (insert-event! sut order-created-event)
          (insert-event! sut other-order-created-event)
          (insert-event! sut order-paid-event)
          (insert-event! sut order-dispatched-event)

          (testing "can get order by id and project events to a resource"
            (let [order (get-by-aggregate-id sut order-id)]
              #_(is (= [] order))
              (is (= {:items ["x"
                              "y"
                              "z"]
                      :order-id order-id
                      :payment-method "CARD"
                      :price "100.45"
                      :customer-id customer-id
                      :resource-type "order"
                      :status "dispatched"}
                     (dissoc order :updated-at :created-at :tracking-number)))
              (is (some? (:tracking-number order))))
            (let [other-order (get-by-aggregate-id sut other-order-id)]
              (is (= {:items ["something"]
                      :order-id other-order-id
                      :customer-id customer-id
                      :price "99.99"
                      :resource-type "order"
                      :status "pending"}
                     (dissoc other-order :created-at)))
              (is (some? (:created-at other-order)))))
          (testing "example of more complex query to search by payload content"
            (let [orders (get-all-by-customer-id sut customer-id)]
              (is (= 2 (count orders)))
              #_(is (= [] orders))))))
      (finally
        (.stop database-container)))))