(ns dagga-bay.views.order
  "Secure order submission form."
  (:require [re-frame.core :as rf]
            [dagga-bay.events :as events]
            [dagga-bay.subs :as subs]))

(defn order-page []
  (let [form @(rf/subscribe [::subs/order-form])
        submitting? @(rf/subscribe [::subs/order-submitting?])
        result @(rf/subscribe [::subs/order-result])
        cart-count @(rf/subscribe [::subs/cart-count])
        cart-total @(rf/subscribe [::subs/cart-total])
        cart-items @(rf/subscribe [::subs/cart-items])]
    [:div.page-order
     [:h1.page-title "// PLACE ORDER"]

     (cond
       ;; Success state
       (:success result)
       [:div.order-success
        [:div.success-icon "✅"]
        [:h2 "Order Submitted!"]
        [:p.order-id (str "Reference: " (:order-id result))]
        [:p (:message result)]
        [:button.btn-primary
         {:on-click #(rf/dispatch [::events/navigate :home])}
         "BACK TO HOME"]]

       ;; Empty cart
       (zero? cart-count)
       [:div.order-empty
        [:p "Your cart is empty. Add some products first!"]
        [:button.btn-primary
         {:on-click #(rf/dispatch [::events/navigate :shop])}
         "BROWSE SHOP →"]]

       ;; Order form
       :else
       [:div.order-content
        ;; Order summary
        [:div.order-summary
         [:h3 "Order Summary"]
         [:div.order-items
          (for [{:keys [product weight-option quantity]} cart-items]
            ^{:key (str (:id product) "-" (:g weight-option))}
            [:div.order-item-row
             [:span (str (:name product) " (" (:g weight-option) (or (:unit product) "g") ")")]
             [:span (str "×" quantity)]
             [:span (str "R" (* (:price weight-option) quantity))]])]
         [:div.order-total
          [:span "Total:"] [:span (str "R" cart-total)]]]

        ;; Form
        [:div.order-form
         [:h3 "Delivery Details"]
         [:p.form-note "🔒 Your information is transmitted securely and not stored permanently."]

         ;; Errors
         (when (:errors result)
           [:div.form-errors
            (for [[i err] (map-indexed vector (:errors result))]
              ^{:key i}
              [:p.form-error (str "⚠ " err)])])

         [:div.form-group
          [:label {:for "order-name"} "Full Name *"]
          [:input#order-name.form-input
           {:type "text"
            :value (:name form)
            :placeholder "Your full name"
            :max-length 100
            :on-change #(rf/dispatch [::events/update-order-field :name (.. % -target -value)])}]]

         [:div.form-group
          [:label {:for "order-phone"} "Phone Number * (SA format)"]
          [:input#order-phone.form-input
           {:type "tel"
            :value (:phone form)
            :placeholder "+27 XX XXX XXXX or 0XX XXX XXXX"
            :max-length 15
            :on-change #(rf/dispatch [::events/update-order-field :phone (.. % -target -value)])}]]

         [:div.form-group
          [:label {:for "order-address"} "Delivery Address * (Cape Town area)"]
          [:textarea#order-address.form-input.form-textarea
           {:value (:address form)
            :placeholder "Full street address, suburb, postal code"
            :max-length 500
            :rows 3
            :on-change #(rf/dispatch [::events/update-order-field :address (.. % -target -value)])}]]

         [:div.form-group
          [:label {:for "order-notes"} "Notes (optional)"]
          [:textarea#order-notes.form-input.form-textarea
           {:value (:notes form)
            :placeholder "Delivery instructions, preferred time, etc."
            :max-length 500
            :rows 2
            :on-change #(rf/dispatch [::events/update-order-field :notes (.. % -target -value)])}]]

         [:button.btn-submit
          {:on-click #(do
                        (rf/dispatch [::events/fetch-csrf-token])
                        ;; Small delay to ensure CSRF token is fetched before submit
                        (js/setTimeout
                          (fn [] (rf/dispatch [::events/submit-order]))
                          500))
           :disabled submitting?}
          (if submitting?
            "SUBMITTING..."
            "🔒 SUBMIT ORDER")]

         [:p.form-disclaimer
          "By submitting, you confirm you are 18+ and that this order is for personal use only. "
          "We will contact you via WhatsApp to confirm your order and arrange payment & delivery."]]])]))
