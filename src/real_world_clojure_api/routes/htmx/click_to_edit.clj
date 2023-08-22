(ns real-world-clojure-api.routes.htmx.click-to-edit
  (:require [clojure.string :as str]
            [hiccup.page :as hp]
            [hiccup2.core :as h]
            [io.pedestal.http.body-params :as body-params]))

;; Helpers
(defn ok
  [body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (-> body
             (h/html)
             (str))})

(defn tw
  [classes]
  (->> (flatten classes)
       (remove nil?)
       (map name)
       (sort)
       (str/join " ")))

(defn- dependencies->state
  [dependencies]
  (get-in dependencies [:in-memory-state-component
                        :htmx-click-to-edit-state]))

;; Tailwind classes
(def tw-primary-button
  [:bg-blue-500 :hover:bg-blue-400 :text-white :font-bold :py-2 :px-4 :border-b-4 :border-blue-700 :hover:border-blue-500 :rounded])

(def tw-cancel-button
  [:bg-red-500 :hover:bg-red-400 :text-white :font-bold :py-2 :px-4 :border-b-4 :border-red-700 :hover:border-red-500 :rounded])

(def tw-input
  [:bg-gray-200 :appearance-none :border-2 :border-gray-200 :rounded :py-2 :px-4 :text-gray-700 :leading-tight :focus:outline-none :focus:bg-white :focus:border-blue-500])

;; Hiccup components
(defn- layout
  [body]
  [:head
   [:title "HTMX: Click to edit"]
   (hp/include-js
     "https://cdn.tailwindcss.com"
     "https://unpkg.com/htmx.org@1.9.4?plugins=forms")
   [:body
    [:div {:class "container mx-auto mt-10"}
     body]]])

(defn user-details-component
  [{:keys [id
           first-name
           last-name
           email]}]
  [:div {:class (tw [:p-5 :bg-slate-100])
         :hx-target "this"
         :hx-swap "outerHTML"}
   [:div {:class "mt-2 flex gap-2 items-center"}
    [:label "First Name:"]
    [:span {:class (tw [:font-bold])} first-name]]
   [:div {:class "mt-2 flex gap-2 items-center"}
    [:label "Last Name:"]
    [:span {:class (tw [:font-bold])} last-name]]
   [:div {:class "mt-2 flex gap-2 items-center"}
    [:label "Email:"]
    [:span {:class (tw [:font-bold])} email]]
   [:button {:class (tw [tw-primary-button :mt-5])
             :hx-get (format "/htmx/click-to-edit/user/%s/edit" id)}
    "Click To Edit"]])

(defn click-to-edit-form
  [{:keys [id
           first-name
           last-name
           email]}]
  [:form
   {:class (tw [:bg-slate-100
                :p-5])
    :hx-put (format "/htmx/click-to-edit/user/%s/edit" id)
    :hx-target "this"
    :hx-swap "outerHTML"}
   [:div {:class "mt-2 flex gap-2 items-center"}
    [:label "First Name"]
    [:input {:class (tw [tw-input])
             :type "text"
             :name "first-name"
             :value first-name}]]
   [:div {:class "mt-2 flex gap-2 items-center"}
    [:label "Last Name"]
    [:input {:class (tw [tw-input])
             :type "text"
             :name "last-name"
             :value last-name}]]
   [:div {:class "mt-2 flex gap-2 items-center"}
    [:label "Email"]
    [:input {:class (tw [tw-input])
             :type "email"
             :name "email"
             :value email}]]
   [:div {:class (tw [:flex :flex-row :gap-2 :mt-5])}
    [:button {:class (tw [tw-primary-button])} "Submit"]
    [:button {:hx-get "/htmx/click-to-edit"
              :hx-target "body"
              :class (tw [tw-cancel-button])} "Cancel"]]])

;; Pedestal handlers
(def root-handler
  {:name ::root
   :enter
   (fn [{:keys [dependencies] :as context}]
     (let [users @(dependencies->state dependencies)
           response
           (-> [:div
                [:h1 {:class "text-2xl font-bold leading-7 text-gray-900 mb-5"} "Click to edit"]
                (map (fn [[id user]]
                       [:div {:class (tw [:mt-2])}
                        (user-details-component (assoc user :id id))]) users)]
               (layout)
               (ok))]
       (assoc context :response response)))})

(def get-form-handler
  {:name ::get
   :enter
   (fn [{:keys [dependencies] :as context}]
     (let [user-id (-> context :request :path-params :user-id)
           users @(dependencies->state dependencies)
           user (get users user-id)
           response
           (-> (assoc user :id user-id)
               (click-to-edit-form)
               (ok))]
       (assoc context :response response)))})

(def put-form-handler
  {:name ::put
   :enter
   (fn [{:keys [dependencies request] :as context}]
     (let [user-id (-> context :request :path-params :user-id)
           _ (swap! (dependencies->state dependencies)
                    assoc user-id (:form-params request))
           users @(dependencies->state dependencies)
           user (get users user-id)
           response
           (-> (assoc user :id user-id)
               (user-details-component)
               (ok))]
       (assoc context :response response)))})

(def routes
  #{["/htmx/click-to-edit"
     :get root-handler
     :route-name ::root]
    ["/htmx/click-to-edit/user/:user-id/edit"
     :get get-form-handler
     :route-name ::get]
    ["/htmx/click-to-edit/user/:user-id/edit"
     :put [(body-params/body-params)
           put-form-handler]
     :route-name ::put]})

(comment
  (defn ->tw
    [s]
    (map keyword (str/split s #" "))))

