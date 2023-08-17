(ns component.real-world-clojure-api.api-test
  (:require [cheshire.core :as json]
            [clojure.string :as str]
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
             (client/get {:accept :json})
             (select-keys [:body :status]))))))

(deftest content-negotiation-test
  (testing "only application/json is accepted"
    (with-system
      [sut (core/real-world-clojure-api-system {:server {:port (get-free-port)}})]
      (is (= {:body "Not Acceptable"
              :status 406}
             (-> (sut->url sut (url-for :greet))
                 (client/get {:accept :edn
                              :throw-exceptions false})
                 (select-keys [:body :status])))))))

(deftest get-todo-test
  (let [todo-id-1 (str (random-uuid))
        todo-1 {:id todo-id-1
                :name "My todo for test"
                :items [{:id (str (random-uuid))
                         :name "finish the test"}]}]
    (with-system
      [sut (core/real-world-clojure-api-system {:server {:port (get-free-port)}})]
      (reset! (-> sut :in-memory-state-component :state-atom)
              [todo-1])
      (is (= {:body todo-1
              :status 200}
             (-> (sut->url sut
                           (url-for :get-todo
                                    {:path-params {:todo-id todo-id-1}}))
                 (client/get {:accept :json
                              :as :json
                              :throw-exceptions false})
                 (select-keys [:body :status]))))
      (testing "Empty body is return for random todo id"
        (is (= {:body ""
                :status 404}
               (-> (sut->url sut
                             (url-for :get-todo
                                      {:path-params {:todo-id (random-uuid)}}))
                   (client/get {:throw-exceptions false})
                   (select-keys [:body :status]))
               ))))))

(deftest post-todo-test
  (let [todo-id-1 (str (random-uuid))
        todo-1 {:id todo-id-1
                :name "My todo for test"
                :items []}]
    (with-system
      [sut (core/real-world-clojure-api-system {:server {:port (get-free-port)}})]
      (testing "store and retrieve todo by id"
        (is (= {:body todo-1
                :status 201}
               (-> (sut->url sut (url-for :post-todo))
                   (client/post {:accept :json
                                 :content-type :json
                                 :as :json
                                 :throw-exceptions false
                                 :body (json/encode todo-1)})
                   (select-keys [:body :status]))))
        (is (= {:body todo-1
                :status 200}
               (-> (sut->url sut
                             (url-for :get-todo
                                      {:path-params {:todo-id todo-id-1}}))
                   (client/get {:accept :json
                                :as :json
                                :throw-exceptions false})
                   (select-keys [:body :status])))))

      (testing "invalid Todo is rejected"
        (is (= {:status 500}
               (-> (sut->url sut (url-for :post-todo))
                   (client/post {:accept :json
                                 :content-type :json
                                 :as :json
                                 :throw-exceptions false
                                 :body (json/encode {:id todo-id-1})})
                   (select-keys [:status]))))))))
