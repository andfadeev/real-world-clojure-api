(ns real-world-clojure-api.routes.htmx.infinite-scroll
  (:require [clojure.string :as str]
            [hiccup.page :as hp]
            [hiccup2.core :as h]
            [faker.lorem :as fl]
            [faker.name :as fn]))

;; Helpers
(defn ok
  [body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (-> body
             (h/html)
             (str))})

(def page-size 10)



(defn random-picture
  []
  (let [skin-color ["Tanned"
                    "Yellow"
                    "Pale"
                    "Light"
                    "Brown"
                    "DarkBrown"
                    "Black"]
        top-type #{"Hat"
                   "LongHairNotTooLong"
                   "ShortHairDreads01"
                   "ShortHairShortFlat"
                   "ShortHairDreads02"
                   "LongHairFrida"
                   "WinterHat4"
                   "Turban"
                   "LongHairShavedSides"
                   "ShortHairTheCaesarSidePart"
                   "ShortHairShaggyMullet"
                   "ShortHairShortWaved"
                   "ShortHairFrizzle"
                   "LongHairMiaWallace"
                   "WinterHat2"
                   "LongHairBigHair"
                   "Hijab"
                   "LongHairStraightStrand"
                   "LongHairFroBand"
                   "ShortHairSides"
                   "NoHair"
                   "ShortHairShortRound"
                   "WinterHat1"
                   "LongHairDreads"
                   "ShortHairTheCaesar"
                   "LongHairFro"
                   "LongHairBun"
                   "WinterHat3"
                   "LongHairCurvy"
                   "Eyepatch"
                   "LongHairStraight"
                   "LongHairStraight2"
                   "ShortHairShortCurly"
                   "LongHairBob"
                   "LongHairCurly"}
        mouth-type #{"Tongue" "Default" "Smile" "Grimace" "Twinkle" "Disbelief" "Eating" "Sad" "Serious" "Concerned" "ScreamOpen" "Vomit"}
        hair-color #{"Platinum" "Black" "BlondeGolden" "BrownDark" "SilverGray" "Blue" "Brown" "Blonde" "Red" "PastelPink" "Auburn"}]
    (format "https://avataaars.io/?skinColor=%s&topType=%s&hairColor=%s&mouthType=%s"
            (first (shuffle skin-color))
            (first (shuffle top-type))
            (first (shuffle hair-color))
            (first (shuffle mouth-type)))))

(defn random-infinite-scroll-item
  []
  {:title (first (fl/sentences))
   :description (first (fl/paragraphs))
   :author {:name (first (fn/names))
            :picture (random-picture)}})

(def items
  (repeatedly 50 random-infinite-scroll-item))

(defn items->page
  [page-number]
  (nth (partition-all page-size items) page-number nil))

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

(defn item-component
  [{:keys [id
           title
           description
           author]}]
  [:article.p-6.even:bg-white.odd:bg-slate-100.sm:p-8
   [:h2.break-all.text-lg.font-medium.sm:text-xl
    [:a.hover:underline
     {:href (str "htmx/infinite-scroll/item/" id)}
     title]]
   [:p.mt-1.break-all.text-sm.text-gray-700
    description]
   [:div.mt-4.text-xs.font-medium.text-gray-500
    [:div.flex.items-center.gap-2
     [:span.relative.flex.h-10.w-10.shrink-0.overflow-hidden.rounded-full
      [:img.aspect-square.h-full.w-full
       {:src (:picture author "https://avataaars.io/?hairColor=BrownDark")}]]
     [:span (:name author)]]]])

(defn loader
  [next-page-number]
  [:div.bg-red-100.p-10
   {:hx-get (str "/htmx/infinite-scroll/items?page=" next-page-number)
    :hx-trigger "revealed"
    :hx-target "this"
    :hx-swap "outerHTML"}
   [:span (format "...loading page %s ..." next-page-number)]])

(def root-handler
  {:name ::root
   :enter
   (fn [{:keys [dependencies] :as context}]
     (let [initial-page-number 0

           items-page (items->page initial-page-number)

           response
           (-> [:div
                [:h1 {:class "text-2xl font-bold leading-7 text-gray-900 mb-5 sm:p-0 p-6"}
                 "Infinite scroll"]
                [:div.shadow-xl
                 (map item-component items-page)
                 (loader (inc initial-page-number))]]
               (layout)
               (ok))]
       (assoc context :response response)))})


(def get-items-page-handler
  {:name ::get
   :enter
   (fn [{:keys [dependencies] :as context}]
     #_(Thread/sleep 1000)
     (let [page-number (-> context :request :query-params :page parse-long)
           items-page (items->page page-number)

           response (if (seq items-page)
                      (-> (mapv item-component items-page)
                          (conj (loader (inc page-number)))
                          (seq)
                          (ok))
                      (-> [:div.bg-green-100.p-10
                           [:span "nothing to load"]]
                          (ok)))]
       (assoc context :response response)))})

(def routes
  #{["/htmx/infinite-scroll"
     :get root-handler
     :route-name ::root]
    ["/htmx/infinite-scroll/items"
     :get get-items-page-handler
     :route-name ::get]})