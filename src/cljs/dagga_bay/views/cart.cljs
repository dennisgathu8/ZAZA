(ns dagga-bay.views.cart
  "Cart page with quantity controls."
  (:require [re-frame.core :as rf]
            [dagga-bay.events :as events]
            [dagga-bay.subs :as subs]))

(defn cart-item-row [[cart-key {:keys [product weight-option quantity]}]]
  [:div.cart-item
   [:div.cart-item-info
    [:h4.cart-item-name (:name product)]
    [:span.cart-item-weight (str (:g weight-option) (or (:unit product) "g"))]
    [:span.cart-item-unit-price (str "R" (:price weight-option) " each")]]
   [:div.cart-item-controls
    [:button.qty-btn
     {:on-click #(when (> quantity 1)
                   (rf/dispatch [::events/update-quantity cart-key (dec quantity)]))}
     "−"]
    [:span.qty-display quantity]
    [:button.qty-btn
     {:on-click #(rf/dispatch [::events/update-quantity cart-key (inc quantity)])}
     "+"]
    [:span.cart-item-subtotal (str "R" (* (:price weight-option) quantity))]
    [:button.cart-item-remove
     {:on-click #(rf/dispatch [::events/remove-from-cart cart-key])}
     "✕"]]])

(defn cart-page []
  (let [items @(rf/subscribe [::subs/cart-items])
        cart @(rf/subscribe [::subs/cart])
        total @(rf/subscribe [::subs/cart-total])
        count @(rf/subscribe [::subs/cart-count])]
    [:div.page-cart
     [:h1.page-title "// CART"]

     (if (empty? items)
       [:div.cart-empty
        [:p.cart-empty-icon "🛒"]
        [:p "Your cart is empty."]
        [:button.btn-primary
         {:on-click #(rf/dispatch [::events/navigate :shop])}
         "BROWSE SHOP →"]]

       [:div.cart-content
        [:div.cart-items
         (for [[k v] cart]
           ^{:key k}
           [cart-item-row [k v]])]

        [:div.cart-summary
         [:div.cart-summary-row
          [:span "Items:"] [:span count]]
         [:div.cart-summary-row.cart-total
          [:span "Total:"] [:span (str "R" total)]]

         [:div.cart-actions
          [:button.btn-primary
           {:on-click #(rf/dispatch [::events/navigate :order])}
           "PROCEED TO ORDER →"]
          [:button.btn-secondary
           {:on-click #(rf/dispatch [::events/navigate :shop])}
           "← CONTINUE SHOPPING"]
          [:button.btn-danger
           {:on-click #(rf/dispatch [::events/clear-cart])}
           "CLEAR CART"]]]])]))
