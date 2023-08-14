(ns component.real-world-clojure-api.api-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [real-world-clojure-api.components.pedestal-component :refer [url-for]]
            [real-world-clojure-api.core :as core]
            [clj-http.client :as client])
  (:import (java.net ServerSocket)))

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

(deftest greeting-test
  (with-system
    [sut (core/real-world-clojure-api-system {:server {:port (get-free-port)}})]
    (is (= {:body "Hello world"
            :status 200}
           (-> (sut->url sut (url-for :greet))
             (client/get)
             (select-keys [:body :status]))))))

(deftest get-todo-test
  (let [todo-id-1 (random-uuid)
        todo-1 {:id todo-id-1
                :name "My todo for test"
                :items [{:id (random-uuid)
                         :name "finish the test"}]}]
    (with-system
      [sut (core/real-world-clojure-api-system {:server {:port (get-free-port)}})]
      (reset! (-> sut :in-memory-state-component :state-atom)
              [todo-1])
      (is (= {:body (pr-str todo-1)
              :status 200}
             (-> (sut->url sut
                           (url-for :get-todo
                                    {:path-params {:todo-id todo-id-1}}))
                 (client/get)
                 (select-keys [:body :status]))))
      (testing "Empty body is return for random todo id"
        (is (= {:body ""
                :status 200}
               (-> (sut->url sut
                             (url-for :get-todo
                                      {:path-params {:todo-id (random-uuid)}}))
                   (client/get)
                   (select-keys [:body :status]))
               ))))))

(deftest a-simple-api-test
  (is (= 1 1)))