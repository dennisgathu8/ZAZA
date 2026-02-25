(ns dagga-bay.core
  "App entry point — mounts Reagent root, initializes Re-frame."
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [day8.re-frame.http-fx]
            [dagga-bay.events :as events]
            [dagga-bay.subs :as subs]
            [dagga-bay.views.age-gate :as age-gate]
            [dagga-bay.views.layout :as layout]
            [dagga-bay.views.home :as home]
            [dagga-bay.views.shop :as shop]
            [dagga-bay.views.cart :as cart]
            [dagga-bay.views.order :as order]
            [dagga-bay.views.about :as about]))

(defn current-page []
  (let [route @(rf/subscribe [::subs/current-route])]
    (case route
      :home  [home/home-page]
      :shop  [shop/shop-page]
      :cart  [cart/cart-page]
      :order [order/order-page]
      :about [about/about-page]
      [home/home-page])))

(defn app []
  (let [age-verified? @(rf/subscribe [::subs/age-verified?])]
    [:div#dagga-bay-app
     (if-not age-verified?
       [age-gate/age-gate]
       [:div.app-shell
        [layout/navbar]
        [:main.main-content
         [current-page]]
        [layout/footer]
        [layout/floating-cart]])]))

(defn mount-root []
  (rdom/render [app] (.getElementById js/document "app")))

(defn init []
  (rf/dispatch-sync [::events/initialize-db])
  (mount-root))
