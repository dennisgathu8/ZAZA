(ns dagga-bay.views.order
  "Secure order submission form."
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [dagga-bay.events :as events]
            [dagga-bay.subs :as subs]))

;; ──────────────────────────────────────────────
;; Order Summary Message Builder
;; ──────────────────────────────────────────────

(defn- build-order-message
  "Build a plain-text order summary for sending via Matrix."
  [{:keys [order-id cart-items cart-total form]}]
  (let [{:keys [name phone address notes]} form
        items-text (str/join "\n"
                    (map (fn [{:keys [product weight-option quantity]}]
                           (str "• " (:name product)
                                " (" (:g weight-option) (or (:unit product) "g") ")"
                                " ×" quantity
                                " — R" (* (:price weight-option) quantity)))
                         cart-items))]
    (str "🌿 DAGGA BAY — Order " order-id "\n"
         "━━━━━━━━━━━━━━━━━━━━━━\n"
         items-text "\n"
         "━━━━━━━━━━━━━━━━━━━━━━\n"
         "Total: R" cart-total "\n\n"
         "📦 Delivery Details:\n"
         "Name: " name "\n"
         "Phone: " phone "\n"
         "Address: " address
         (when (and notes (seq notes))
           (str "\nNotes: " notes))
         "\n\n✅ Please confirm this order. Thank you!")))

(defn- matrix-url []
  ;; matrix: URI opens Matrix clients directly (Element, FluffyChat, etc.)
  ;; Falls back to matrix.to in web browsers that don't have a Matrix client
  "https://matrix.to/#/@markandmark1:matrix.org")

;; ──────────────────────────────────────────────
;; Success Screen
;; ──────────────────────────────────────────────

(defn- order-success-view [result cart-items cart-total form]
  (let [order-msg (build-order-message
                    {:order-id   (:order-id result)
                     :cart-items cart-items
                     :cart-total cart-total
                     :form       form})]
    [:div.order-success
     [:div.success-icon "✅"]
     [:h2 "Order Submitted!"]
     [:p.order-id (str "Reference: " (:order-id result))]
     [:div.order-summary-msg
      [:h3 "📋 Your Order Summary"]
      [:pre.order-msg-text order-msg]
      [:p.order-msg-hint "Copy the summary above and send it via Matrix:"]]
     ;; Action button
     [:div.order-send-actions
      [:a.matrix-btn
       {:href (matrix-url)}
       "👾 Send via Matrix"]]
     [:button.btn-primary.order-done-btn
      {:on-click #(do (rf/dispatch [::events/clear-order-state])
                      (rf/dispatch [::events/navigate :home]))}
      "✓ DONE — BACK TO HOME"]]))

;; ──────────────────────────────────────────────
;; Empty Cart View
;; ──────────────────────────────────────────────

(defn- order-empty-view []
  [:div.order-empty
   [:p "Your cart is empty. Add some products first!"]
   [:button.btn-primary
    {:on-click #(rf/dispatch [::events/navigate :shop])}
    "BROWSE SHOP →"]])

;; ──────────────────────────────────────────────
;; Order Form View
;; ──────────────────────────────────────────────

(defn- order-form-view [form submitting? result cart-items cart-total]
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
     {:on-click #(rf/dispatch [::events/fetch-csrf-token-then-submit])
      :disabled submitting?}
     (if submitting? "SUBMITTING..." "🔒 SUBMIT ORDER")]

    [:p.form-disclaimer
     "By submitting, you confirm you are 18+ and that this order is for personal use only. "
     "After submission, you'll be prompted to send your order details via Matrix."]]])

;; ──────────────────────────────────────────────
;; Order Page (main component)
;; ──────────────────────────────────────────────

(defn order-page []
  (let [form       @(rf/subscribe [::subs/order-form])
        submitting? @(rf/subscribe [::subs/order-submitting?])
        result     @(rf/subscribe [::subs/order-result])
        cart-count @(rf/subscribe [::subs/cart-count])
        cart-total @(rf/subscribe [::subs/cart-total])
        cart-items @(rf/subscribe [::subs/cart-items])]
    [:div.page-order
     [:h1.page-title "// PLACE ORDER"]
     (cond
       (:success result)
       [order-success-view result cart-items cart-total form]

       (zero? cart-count)
       [order-empty-view]

       :else
       [order-form-view form submitting? result cart-items cart-total])]))
