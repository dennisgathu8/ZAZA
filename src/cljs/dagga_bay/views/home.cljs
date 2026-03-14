(ns dagga-bay.views.home
  "Home/landing page with hero and featured products."
  (:require [re-frame.core :as rf]
            [dagga-bay.events :as events]
            [dagga-bay.subs :as subs]))

(defn product-card [product]
  (let [first-weight (first (:weights product))]
    [:div.product-card
     [:div.product-card-header
      [:span.product-category (name (:category product))]
      (when (:strain-type product)
        [:span.strain-badge
         {:class (name (:strain-type product))}
         (name (:strain-type product))])]
     [:h3.product-card-name (:name product)]
     [:p.product-card-desc (subs (:description product) 0 (min 80 (count (:description product)))) "..."]
     [:div.product-card-footer
      [:span.product-price (str "R" (:price first-weight))]
      [:span.product-weight (str "from " (:g first-weight) "g")]
      [:button.btn-add-cart
       {:on-click #(rf/dispatch [::events/add-to-cart product first-weight])}
       "+ Cart"]]]))

(defn home-page []
  (let [featured @(rf/subscribe [::subs/featured-products])]
    [:div.page-home
     ;; Hero Section
     [:section.hero
      [:div.hero-content
       [:div.hero-ascii
        [:pre.ascii-art
"     _                          _
   __| | __ _  __ _  __ _  __ _ | |__   __ _ _   _
  / _` |/ _` |/ _` |/ _` |/ _` || '_ \\ / _` | | | |
 | (_| | (_| | (_| | (_| | (_| || |_) | (_| | |_| |
  \\__,_|\\__,_|\\__, |\\__, |\\__,_||_.__/ \\__,_|\\__, |
               |___/ |___/                    |___/ "]]
       [:h1.hero-title "DAGGA " [:span.hero-highlight "BAY"]]
       [:p.hero-tagline "Cape Town's Premier Cannabis Dispensary"]
       [:p.hero-sub "Quality flower • Edibles • Vapes • Concentrates • Accessories"]
       [:div.hero-actions
        [:button.btn-primary
         {:on-click #(rf/dispatch [::events/navigate :shop])}
         "BROWSE SHOP →"]
        [:button.btn-secondary
         {:on-click #(rf/dispatch [::events/navigate :about])}
         "ABOUT US"]]]]

     ;; Stats bar
     [:section.stats-bar
      [:div.stat [:span.stat-num "22+"] [:span.stat-label "Products"]]
      [:div.stat [:span.stat-num "5"] [:span.stat-label "Categories"]]
      [:div.stat [:span.stat-num "🇿🇦"] [:span.stat-label "Cape Town"]]
      [:div.stat [:span.stat-num "18+"] [:span.stat-label "Adults Only"]]]

     ;; Featured Products
     [:section.featured-section
      [:h2.section-title "// FEATURED_PRODUCTS"]
      [:div.product-grid
       (for [product featured]
         ^{:key (:id product)}
         [product-card product])]
      [:div.section-cta
       [:button.btn-primary
        {:on-click #(rf/dispatch [::events/navigate :shop])}
        "VIEW ALL PRODUCTS →"]]]

     ;; How it works
     [:section.how-section
      [:h2.section-title "// HOW_IT_WORKS"]
      [:div.steps-grid
       [:div.step
        [:span.step-num "01"]
        [:h3 "BROWSE"]
        [:p "Explore our curated selection of SA's finest cannabis products."]]
       [:div.step
        [:span.step-num "02"]
        [:h3 "ADD TO CART"]
        [:p "Select your products, choose weight options, add to cart."]]
       [:div.step
        [:span.step-num "03"]
        [:h3 "SUBMIT ORDER"]
        [:p "Complete the secure order form with your delivery details."]]
       [:div.step
        [:span.step-num "04"]
        [:h3 "CONFIRM & DELIVER"]
        [:p "Send your order summary via Matrix or WhatsApp — we'll confirm and arrange delivery."]]]]]))
