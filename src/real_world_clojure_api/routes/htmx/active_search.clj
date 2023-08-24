(ns real-world-clojure-api.routes.htmx.active-search
  (:require [hiccup.page :as hp]
            [hiccup2.core :as h]))

(defn ok
  [body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (-> body
             (h/html)
             (str))})

(def title "HTMX: Active Search")

;https://ocw.mit.edu/ans7870/6/6.006/s08/lecturenotes/files/t8.shakespeare.txt

(defn- layout
  [body]
  [:head
   [:title title]
   (hp/include-js
     "https://cdn.tailwindcss.com"
     "https://unpkg.com/htmx.org@1.9.4?plugins=forms")
   [:body
    [:div {:class "container mx-auto mt-10"}
     [:h1 {:class "text-2xl font-bold leading-7 text-gray-900 mb-5 sm:p-0 p-6"}
      title]
     body]]])

(def root-handler
  {:name ::root
   :enter
   (fn [context]
     (assoc context :response
            (-> [:div "root"]
                (layout)
                (ok))))})


(def search-handler
  {:name ::search
   :enter
   (fn [context]
     (let [q (-> context :request :query-params :q)
           response (-> [:div.bg-green-100.p-10
                         [:span q]]
                        (ok))]
       (assoc context :response response)))})

(def routes
  #{["/htmx/active-search"
     :get root-handler
     :route-name ::root]
    ["/htmx/active-search/search"
     :get search-handler
     :route-name ::search]})