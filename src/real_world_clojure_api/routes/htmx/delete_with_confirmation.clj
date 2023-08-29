(ns real-world-clojure-api.routes.htmx.delete-with-confirmation
  (:require [clojure.string :as str]
            [hiccup.page :as hp]
            [hiccup2.core :as h]
            [faker.lorem :as fl]))

(defn tw
  [classes]
  (->> (flatten classes)
       (remove nil?)
       (map name)
       (sort)
       (str/join " ")))

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

(def title "HTMX: Delete with confirmation")

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

(def root-handler
  {:name ::root
   :enter
   (fn [context]
     (let [response (-> [:div
                         (for [item @items-atom]
                           [:div.p-2.odd:bg-red-50.even:bg-green-50
                            {:id (item-div-id item)}
                            [:div (:title item)]
                            [:div
                             [:button.text-red-600.hover:underline
                              {:hx-delete (str "/htmx/delete-with-confirmation/items/" (:id item))
                               :hx-target (item-div-id-ref item)
                               :hx-swap "outerHTML"
                               :hx-confirm "Are you sure you wish to delete this item?"}
                              "delete"]

                             ]
                            [:div
                             [:button.text-red-600.hover:underline
                              {:hx-delete (str "/htmx/delete-with-confirmation/items/" (:id item))
                               :hx-target (item-div-id-ref item)
                               :hx-swap "outerHTML"
                               :_ "on htmx:confirm(issueRequest)
             halt the event
             call Swal.fire({title: 'Confirm', text:'Do you want to continue?', showDenyButton: true,\n  showCancelButton: true,\n  confirmButtonText: 'Save',})
             if result.isConfirmed issueRequest()"} "delete 2"]
                             ]
                            ]
                           )
                         ]
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
                          (= (str (:id item)) item-id)) items)))
       )

     (assoc context :response (ok nil)))})

(def routes
  #{["/htmx/delete-with-confirmation"
     :get root-handler
     :route-name ::root]
    ["/htmx/delete-with-confirmation/items/:item-id"
     :delete delete-handler
     :route-name ::delete]})