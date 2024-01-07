(ns real-world-clojure-api.config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [malli.core :as m]
            [malli.error :as me]
            [malli.util :as mu]))



(defn read-config
  []
  (-> "config.edn"
      (io/resource)
      (aero/read-config)))

(defmethod aero/reader 'csv-set
  [_opts _tag value]
  (if (str/blank? value)
    #{}
    (->> (str/split value #",")
         (remove str/blank?)
         (map str/trim)
         (into #{}))))

(defmethod aero/reader 'csv-keyword-set
  [_opts _tag value]
  (if (str/blank? value)
    #{}
    (->> (str/split value #",")
         (remove str/blank?)
         (map str/trim)
         (map keyword)
         (into #{}))))


;; Config schema

(def kafka-config-base-schema
  (m/schema
    [:map
     [:bootstrap.servers :string]
     [:application.id :string]
     [:auto.offset.reset [:enum "earliest" "latest"]]
     [:producer.acks [:enum "0" "1" "all"]]]))

(def kafka-config-schema
  (m/schema
    [:multi {:dispatch :security.protocol}
     ["SSL"
      (mu/merge
        kafka-config-base-schema
        [:map
         [:security.protocol [:enum "SSL"]]
         [:ssl.keystore.type [:enum "PKCS12"]]
         [:ssl.truststore.type [:enum "JKS"]]
         [:ssl.keystore.location :string]
         [:ssl.keystore.password :string]
         [:ssl.key.password :string]
         [:ssl.truststore.location :string]
         [:ssl.truststore.password :string]])]
     ["PLAINTEXT"
      (mu/merge
        kafka-config-base-schema
        [:map [:security.protocol [:enum "PLAINTEXT"]]])]]))

(def config-schema
  (m/schema
    [:map
     [:server [:map
               [:port [:int {:min 1
                             :max 10000}]]]]
     [:htmx [:map
             [:server [:map
                       [:port [:int {:min 1
                                     :max 10000}]]]]]]
     [:input-topics [:set :string]]
     [:kafka kafka-config-schema]]))


(def valid-config?
  (m/validator config-schema))

(defn assert-valid-config!
  [config]
  (if (valid-config? config)
    config
    (->> {:error (me/humanize (m/explain config-schema config))}
         (ex-info "Config is not valid!")
         (throw))))



(comment
  (let [config {:server {:port 8080}
                :htmx {:server {:port 3000}}
                :input-topics #{"topic-1"}
                :kafka {:bootstrap.servers "kafka"
                        :application.id "my-app",
                        :auto.offset.reset "none",
                        :producer.acks "all"

                        :ssl.keystore.type "PKCS12"
                        :ssl.truststore.type "JKS"
                        :ssl.keystore.location "location"
                        :ssl.keystore.password "password"
                        :ssl.key.password "password"
                        :ssl.truststore.location "location"
                        :ssl.truststore.password "password"
                        :security.protocol "SSL"}}]
    (assert-valid-config! config)
    )

  )

(comment
  (read-config)

  (aero/reader {} 'csv-set "a, b, c,,d")


  )


;; Config schema


(comment




  (defmethod aero/reader 'csv-set
    [_opts _tag value]

    (if (str/blank? value)
      #{}
      (->> (str/split value #",")
           (remove str/blank?)
           (map str/trim)
           (into #{}))))

  (defmethod aero/reader 'keyword-csv-set
    [_ _ value]

    (if (str/blank? value)
      #{}
      (->> (str/split value #",")
           (remove str/blank?)
           (map str/trim)
           (map keyword)
           (into #{}))))

  (-> [:map {:closed true}
       [:a [:string {:min 1 :max 10}]]
       [:id {:optional true} :keyword]]
      (m/explain
        {:a "1asd"
         :id :nil
         :wrong-key "asdf"})
      (me/humanize))

  (defn config-errors
    [config]
    (me/humanize (m/explain config-schema config)))



  (defn assert-valid-config!
    [config]
    (when-not (valid-config? config)
      (->> {:errors (config-errors config)}
           (ex-info "Input config is not valid against the schema")
           (throw))))

  (def kafka-config-schema
    (m/schema
      [:multi {:dispatch :security.protocol}
       ["SSL"
        (mu/merge
          kafka-config-base-schema
          [:map
           [:security.protocol [:enum "SSL"]]
           [:ssl.keystore.type [:enum "PKCS12"]]
           [:ssl.truststore.type [:enum "JKS"]]
           [:ssl.keystore.location :string]
           [:ssl.keystore.password :string]
           [:ssl.key.password :string]
           [:ssl.truststore.location :string]
           [:ssl.truststore.password :string]])]
       ["PLAINTEXT"
        (mu/merge
          kafka-config-base-schema
          [:map [:security.protocol [:enum "PLAINTEXT"]]])]]))



  )