(ns real-world-clojure-api.routes.htmx.form-validation
  (:require [clojure.string :as str]
            [faker.name :as fn]
            [hiccup.page :as hp]
            [hiccup2.core :as h]
            [faker.lorem :as fl]
            [faker.internet :as fi]
            [real-world-clojure-api.routes.htmx.shared.picture :as picture]))

(defn tw
  [classes]
  (->> (flatten classes)
       (remove nil?)
       (map name)
       (sort)
       (str/join " ")))

(def tw-input
  [:bg-gray-200 :appearance-none :border-2 :border-gray-200 :rounded :py-2 :px-4 :text-gray-700 :leading-tight :focus:outline-none :focus:bg-white :focus:border-blue-500])

(def tw-primary-button
  [:bg-blue-500 :hover:bg-blue-400 :text-white :font-bold :py-2 :px-4 :border-b-4 :border-blue-700 :hover:border-blue-500 :rounded])

(def tw-cancel-button
  [:bg-red-500 :hover:bg-red-400 :text-white :font-bold :py-2 :px-4 :border-b-4 :border-red-700 :hover:border-red-500 :rounded])



(defn random-item
  []
  {:id (random-uuid)
   :title (first (fl/sentences))})

(def items-atom (atom (repeatedly 50 random-item)))

(defn ok
  [body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (-> body
             (h/html)
             (str))})

(def title "HTMX: Form Validation")

(defn- layout
  [body]
  [:head
   [:title title]
   (hp/include-js
     "https://cdn.tailwindcss.com?plugins=forms"
     "https://unpkg.com/htmx.org@1.9.4"
     "https://unpkg.com/hyperscript.org@0.9.11"
     "https://cdn.jsdelivr.net/npm/sweetalert2@11")
   [:body
    [:div {:class "container mx-auto mt-10"}
     [:h1 {:class "text-2xl font-bold leading-7 text-gray-900 mb-5 sm:p-0 p-6"}
      title]
     body]]])

(defn item-div-id
  [item]
  (str "item-div-" (:id item)))

(defn item-div-id-ref
  [item]
  (str "#" (item-div-id item)))

(def users-atom
  (atom [{:id (random-uuid)
          :email (fi/email)
          :picture (picture/random-picture)
          :name (first (fn/names))}
         {:id (random-uuid)
          :email (fi/email)
          :picture (picture/random-picture)
          :name (first (fn/names))}]))

(def root-handler
  {:name ::root
   :enter
   (fn [context]
     (let [response
           (-> [:div
                [:h1 "Users"]
                (for [user @users-atom]
                  [:div.rounded.shadow.p-5.mb-5.bg-slate-50
                   [:div.flex.items-center.gap-2.mb-5
                    [:img.h-10.w-10.bg-red-100.rounded
                     {:src (:picture user)}]
                    [:div (:name user)]

                    [:div.flex.items-center.gap-2
                     [:span "Email:"]
                     [:span (:email user)]]
                    [:div.flex.items-center.gap-2
                     [:span "Name:"]
                     [:span (:name user)]]]
                   [:a.text-blue-400 {:href ""} "View details"]])

                [:a.text-white.bg-gradient-to-br.from-purple-600.to-blue-500.hover:bg-gradient-to-bl.focus:ring-4.focus:outline-none.focus:ring-blue-300.dark:focus:ring-blue-800.font-medium.rounded-lg.text-sm.px-5.py-2.5.text-center.mr-2.mb-2
                 {:href "/htmx/form-validation/create"} "Create user"]

                ]
               (layout)
               (ok))]
       (assoc context :response response)))})


(defn create-user-form
  [{:keys [id
           first-name
           last-name
           email]}]
  [:form
   {:class (tw [:bg-slate-100
                :p-5])
    :hx-post (format "/htmx/form-validation/user/%s/edit" id)
    :hx-target "this"
    :hx-swap "outerHTML"}
   [:div
    [:div {:class "mt-2 flex gap-2 items-center"}
     [:label "Name"]
     [:input {:class (tw [tw-input])
              :type "text"
              :name "last-name"
              :value last-name}]]
    [:p.text-red-600.text-sm "Should be a valid email"]]
   [:div
    [:div {:class "mt-2 flex gap-2 items-center"}
     [:label "Email"]
     [:input {:class (tw [tw-input])
              :type "email"
              :name "email"
              :value email}]]
    [:p.text-red-600.text-sm "Should be a valid email"]]
   [:div {:class (tw [:flex :flex-row :gap-2 :mt-5])}
    [:button {:class (tw [tw-primary-button])} "Submit"]
    [:a {:href "/htmx/form-validation"
              :class (tw [tw-cancel-button])} "Cancel"]]])


(def get-form-handler
  {:name ::get-form
   :enter
   (fn [context]
     (let [response
           (-> (create-user-form {})
               (layout)
               (ok))]
       (assoc context :response response)))})


(def delete-handler
  {:name ::delete
   :enter
   (fn [context]
     (let [item-id (-> context :request :path-params :item-id)]
       (swap! items-atom
              (fn [items]
                (remove (fn [item]
                          (= (str (:id item)) item-id)) items))))

     (assoc context :response (ok nil)))})

(def routes
  #{["/htmx/form-validation"
     :get root-handler
     :route-name ::root]
    ["/htmx/form-validation/create"
     :get get-form-handler
     :route-name ::get-form]
    ["/htmx/delete-with-confirmation/items/:item-id"
     :delete delete-handler
     :route-name ::delete]})