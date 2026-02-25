(ns dagga-bay.views.shop
  "Shop page with product grid, search, and filters."
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [dagga-bay.events :as events]
            [dagga-bay.subs :as subs]))

(defn search-bar []
  (let [query @(rf/subscribe [::subs/search-query])]
    [:div.search-bar
     [:input.search-input
      {:type "text"
       :placeholder "🔍 Search products... (e.g. Durban Poison, edibles, vape)"
       :value query
       :max-length 100
       :on-change #(rf/dispatch [::events/set-search (.. % -target -value)])}]
     (when (seq query)
       [:button.search-clear
        {:on-click #(rf/dispatch [::events/set-search ""])}
        "✕"])]))

(defn filter-bar []
  (let [categories @(rf/subscribe [::subs/categories])
        strain-types @(rf/subscribe [::subs/strain-types])
        filters @(rf/subscribe [::subs/filters])]
    [:div.filter-bar
     [:div.filter-group
      [:span.filter-label "Category:"]
      (for [cat categories]
        ^{:key (:id cat)}
        [:button.filter-btn
         {:class (when (= (:category filters) (:id cat)) "active")
          :on-click #(rf/dispatch [::events/set-filter :category (:id cat)])}
         (:label cat)])]

     [:div.filter-group
      [:span.filter-label "Strain:"]
      (for [st strain-types]
        ^{:key (:id st)}
        [:button.filter-btn
         {:class (str (name (:id st)) (when (= (:strain-type filters) (:id st)) " active"))
          :on-click #(rf/dispatch [::events/set-filter :strain-type (:id st)])}
         (:label st)])]

     [:div.filter-group
      [:span.filter-label "Price:"]
      [:button.filter-btn
       {:class (when (= (:price-sort filters) :asc) "active")
        :on-click #(rf/dispatch [::events/set-filter :price-sort :asc])}
       "Low → High"]
      [:button.filter-btn
       {:class (when (= (:price-sort filters) :desc) "active")
        :on-click #(rf/dispatch [::events/set-filter :price-sort :desc])}
       "High → Low"]]

     (when (or (:category filters) (:strain-type filters) (:price-sort filters))
       [:button.filter-clear
        {:on-click #(rf/dispatch [::events/clear-filters])}
        "✕ Clear Filters"])]))

(defn weight-selector [product]
  (let [selected (r/atom 0)]
    (fn [product]
      (let [weights (:weights product)
            current (nth weights @selected)]
        [:div.weight-selector
         [:div.weight-options
          (for [[idx w] (map-indexed vector weights)]
            ^{:key idx}
            [:button.weight-btn
             {:class (when (= idx @selected) "active")
              :on-click #(reset! selected idx)}
             (str (:g w) (or (:unit product) "g"))])]
         [:div.weight-price
          [:span.price-tag (str "R" (:price current))]
          [:button.btn-add-cart
           {:on-click #(rf/dispatch [::events/add-to-cart product current])}
           "ADD TO CART"]]]))))

(defn product-row [product]
  [:div.product-row
   [:div.product-info
    [:div.product-header
     [:h3.product-name (:name product)]
     [:div.product-badges
      (when (:strain-type product)
        [:span.strain-badge {:class (name (:strain-type product))}
         (name (:strain-type product))])
      [:span.category-badge (name (:category product))]
      (when (pos? (:thc product))
        [:span.thc-badge (str (:thc product) "% THC")])]]
    [:p.product-desc (:description product)]]
   [weight-selector product]])

(defn shop-page []
  (let [products @(rf/subscribe [::subs/filtered-products])
        query @(rf/subscribe [::subs/search-query])]
    [:div.page-shop
     [:h1.page-title "// SHOP"]
     [:p.page-subtitle "Browse our complete selection of premium SA cannabis products"]

     [search-bar]
     [filter-bar]

     [:div.results-info
      [:span (str (count products) " product" (when (not= 1 (count products)) "s") " found")]
      (when (seq query)
        [:span.search-for (str " for \"" query "\"")])]

     (if (empty? products)
       [:div.no-results
        [:p "No products match your search."]
        [:button.btn-secondary
         {:on-click #(rf/dispatch [::events/clear-filters])}
         "Clear Filters"]]
       [:div.product-list
        (for [product products]
          ^{:key (:id product)}
          [product-row product])])]))
