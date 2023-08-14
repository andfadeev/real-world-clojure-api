(ns unit.real-world-clojure-api.simple-test
  (:require [clojure.test :refer :all]
            [real-world-clojure-api.components.pedestal-component :refer [url-for]]))


(deftest a-simple-passing-test
  (is (= 1 1)))

(deftest url-for-test
  (testing "greet endpoint url"
    (is (= "/greet" (url-for :greet))))

  (testing "get todo by id endpoint url"
    (let [todo-id (random-uuid)]
      (is (= (str "/todo/" todo-id)
             (url-for :get-todo {:path-params {:todo-id todo-id}}))))))
