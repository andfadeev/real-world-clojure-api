(ns real-world-clojure-api.routes.htmx.comments-section
  (:require [clojure.string :as str]
            [faker.lorem :as fl]
            [faker.name :as fn]
            [hiccup.page :as hp]
            [hiccup2.core :as h]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.params :as params]
            [real-world-clojure-api.routes.htmx.shared :as shared]))

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

(def tw-input
  [:bg-gray-200 :appearance-none :border-2 :border-gray-200 :rounded :py-2 :px-4 :text-gray-700 :leading-tight :focus:outline-none :focus:bg-white :focus:border-blue-500])

(def title "HTMX: Comments section")

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

(defn random-comment
  []
  {:name (first (fn/names))
   :comment (first (fl/paragraphs))
   :picture (shared/random-picture)})

(def comments-atom (atom (repeatedly 3 random-comment)))

(def author-picture (shared/random-picture))
(def author-name "Andrey Fadeev")

(defn comment-component
  ([comment]
   (comment-component comment {}))
  ([comment opts]
   [:div.odd:bg-white.even:bg-slate-100.px-4.py-4.sm:px-6.lg:px-8
    opts
    [:p.text-gray-700.text-sm.linkify.break-all
     (:comment comment)]
    [:div.mt-2.flex.items-center.gap-1.text-xs.text-gray-500
     [:a.flex.items-center.hover:underline.gap-1
      {:href "#"}
      [:img.rounded-full.h-5.w-5
       {:src (:picture comment)}]
      [:span (:name comment)]]]]))

(defn comment-form-component
  []
  [:form.px-4.py-4.sm:px-6.lg:px-8
   {:hx-post "/htmx/comments-section/comments"
    :hx-target "this"
    :hx-swap "outerHTML"
    :hx-boost "true"
    }
   [:div
    [:label.sr-only {:for "comment"} "Comment"]
    [:textarea#comment.w-full.p-3.text-sm.rounded-sm
     {:autocomplete "off"
      :name "comment"
      :class (tw [tw-input])
      :placeholder "Your comment" :rows "5"}]]
   [:div.mt-2
    [:button.px-5.py-3.text-white.text-sm.bg-indigo-800.rounded-sm
     {:type "submit"}
     [:span.font-medium "Publish comment"]]]])

(defn comments-component
  [comments]
  [:div#comments
   {:hx-get "/htmx/comments-section/comments"
    :hx-trigger "newComment from:body"}
   [:h2.px-4.sm:px-6.lg:px-8.bg-gray-100.py-5.text-lg.font-medium.sm:text-xl
    (format "Comments (%s)" (count comments))]
   [:div.mx-auto
    (for [comment comments]
      (comment-component comment))]])

(defn comments-section-component
  [comments]
  [:div#comments-section.shadow-xl.mt-4
   (comments-component comments)
   (comment-form-component)])

(def root-handler
  {:name ::root
   :enter
   (fn [context]
     (let [response
           (-> (comments-section-component @comments-atom)
               (layout)
               (ok))]
       (assoc context :response response)))})

(def post-handler
  {:name ::post
   :enter
   (fn [context]
     (let [comment {:comment (-> context :request :params :comment)
                    :name author-name
                    :picture author-picture}
           _ (swap! comments-atom conj comment)
           response
           (-> (comment-form-component)
               (ok))]
       (assoc context :response
              (update response :headers merge {"HX-Trigger" "newComment"}))))})

(def comments-handler
  {:name ::comments
   :enter
   (fn [context]
     (let [response
           (-> (comments-component @comments-atom)
               (ok))]
       (assoc context :response response)))})

(def routes
  #{["/htmx/comments-section"
     :get root-handler
     :route-name ::root]
    ["/htmx/comments-section/comments"
     :get comments-handler
     :route-name ::comments]
    ["/htmx/comments-section/comments"
     :post [(body-params/body-params)
            params/keyword-params
            post-handler]
     :route-name ::post]})


(comment
  {:hx-post "/htmx/comments-section/comments"
   :hx-target "#comments-section"
   :hx-swap "outerHTML"
   :hx-boost "true"
   }


  {:hx-swap-oob "afterbegin:#comments"}

  (update {} :headers merge {"HX-Trigger" "newComment"})

  ["/htmx/comments-section/comments"
   :get comments-handler
   :route-name ::comments]

  {:hx-get "/contacts/table"
   :hx-trigger "newComment from:body, every 10s"})