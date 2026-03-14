(ns dagga-bay.events
  "Re-frame event handlers."
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [dagga-bay.db :as db]
            [ajax.core :as ajax]))

;; ──────────────────────────────────────────────
;; Initialization
;; ──────────────────────────────────────────────

(rf/reg-event-db
  ::initialize-db
  (fn [_ _]
    ;; Check sessionStorage for age verification
    (let [verified? (= "true" (.getItem js/sessionStorage "dagga-bay-age-verified"))]
      (assoc db/default-db :age-verified? verified?))))

;; ──────────────────────────────────────────────
;; Age Gate
;; ──────────────────────────────────────────────

(rf/reg-event-db
  ::verify-age
  (fn [db [_ dob-str]]
    (let [dob (js/Date. dob-str)
          now (js/Date.)
          age (- (.getFullYear now) (.getFullYear dob))
          age-adjusted (if (or (< (.getMonth now) (.getMonth dob))
                               (and (= (.getMonth now) (.getMonth dob))
                                    (< (.getDate now) (.getDate dob))))
                         (dec age)
                         age)
          verified? (>= age-adjusted 18)]
      (when verified?
        (.setItem js/sessionStorage "dagga-bay-age-verified" "true"))
      (assoc db :age-verified? verified?
                :age-error (when-not verified? "You must be 18 or older to enter.")))))

(rf/reg-event-db
  ::dismiss-age-error
  (fn [db _]
    (dissoc db :age-error)))

;; ──────────────────────────────────────────────
;; Navigation
;; ──────────────────────────────────────────────

(rf/reg-event-db
  ::navigate
  (fn [db [_ route]]
    (assoc db :current-route route
              :show-mobile-menu? false
              :cart-open? false)))

(rf/reg-event-db
  ::toggle-mobile-menu
  (fn [db _]
    (update db :show-mobile-menu? not)))

;; ──────────────────────────────────────────────
;; Search & Filters
;; ──────────────────────────────────────────────

(rf/reg-event-db
  ::set-search
  (fn [db [_ query]]
    (assoc db :search-query (or query ""))))

(rf/reg-event-db
  ::set-filter
  (fn [db [_ filter-key value]]
    (assoc-in db [:filters filter-key]
              (if (= value (get-in db [:filters filter-key])) nil value))))

(rf/reg-event-db
  ::clear-filters
  (fn [db _]
    (assoc db :filters {:category nil :strain-type nil
                        :thc-min 0 :thc-max 100 :price-sort nil}
              :search-query "")))

;; ──────────────────────────────────────────────
;; Cart
;; ──────────────────────────────────────────────

(rf/reg-event-db
  ::add-to-cart
  (fn [db [_ product weight-option]]
    (let [cart-key (str (:id product) "-" (:g weight-option))]
      (update-in db [:cart cart-key]
                 (fn [existing]
                   (if existing
                     (update existing :quantity inc)
                     {:product product
                      :weight-option weight-option
                      :quantity 1}))))))

(rf/reg-event-db
  ::remove-from-cart
  (fn [db [_ cart-key]]
    (update db :cart dissoc cart-key)))

(rf/reg-event-db
  ::update-quantity
  (fn [db [_ cart-key qty]]
    (let [q (max 1 (min 99 qty))]
      (assoc-in db [:cart cart-key :quantity] q))))

(rf/reg-event-db
  ::clear-cart
  (fn [db _]
    (assoc db :cart {})))

(rf/reg-event-db
  ::toggle-cart
  (fn [db _]
    (update db :cart-open? not)))

;; ──────────────────────────────────────────────
;; Order Form
;; ──────────────────────────────────────────────

(rf/reg-event-db
  ::update-order-field
  (fn [db [_ field value]]
    ;; Client-side length limits
    (let [max-len (case field :name 100 :phone 15 :address 500 :notes 500 100)
          v (str value)
          sanitized (subs v 0 (min (count v) max-len))]
      (assoc-in db [:order-form field] sanitized))))

;; ── CSRF Token Fetch ──

(rf/reg-event-fx
  ::fetch-csrf-token
  (fn [{:keys [db]} _]
    {:http-xhrio {:method          :get
                  :uri             "/api/csrf-token"
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::csrf-token-success]
                  :on-failure      [::csrf-token-failure]}}))

(rf/reg-event-db
  ::csrf-token-success
  (fn [db [_ response]]
    (assoc db :csrf-token (:csrf-token response))))

(rf/reg-event-db
  ::csrf-token-failure
  (fn [db _]
    (assoc db :csrf-error "Failed to load security token. Please refresh.")))

;; ── Submit Order ──

(rf/reg-event-fx
  ::submit-order
  (fn [{:keys [db]} _]
    (let [{:keys [name phone address notes]} (:order-form db)
          items (mapv (fn [[_ {:keys [product weight-option quantity]}]]
                        {:product-id (:id product)
                         :name (:name product)
                         :quantity quantity
                         :price (:price weight-option)})
                      (:cart db))
          ;; Client-side validation
          errors (cond-> []
                   (< (count (str/trim name)) 2)
                   (conj "Name must be at least 2 characters")
                   (not (re-matches #"^(\+27|0)[0-9]{9,10}$"
                                    (str/replace phone #"[\s\-]" "")))
                   (conj "Enter a valid SA phone number (+27... or 0...)")
                   (< (count (str/trim address)) 10)
                   (conj "Address must be at least 10 characters")
                   (empty? items)
                   (conj "Your cart is empty"))]
      (if (seq errors)
        {:db (assoc db :order-result {:errors errors})}
        {:db (assoc db :order-submitting? true :order-result nil)
         :http-xhrio {:method          :post
                      :uri             "/api/orders"
                      :params          {:name name :phone phone :address address
                                        :notes notes :items items}
                      :headers         {"X-CSRF-Token" (:csrf-token db)}
                      :format          (ajax/json-request-format)
                      :response-format (ajax/json-response-format {:keywords? true})
                      :on-success      [::order-success]
                      :on-failure      [::order-failure]}}))))

(rf/reg-event-db
  ::order-success
  (fn [db [_ response]]
    ;; Keep cart + form in db so the success screen can build the order message
    (assoc db :order-submitting? false
              :order-result {:success true :message (:message response) :order-id (:order-id response)})))

(rf/reg-event-db
  ::clear-order-state
  (fn [db _]
    (assoc db :cart {}
              :order-form {:name "" :phone "" :address "" :notes ""}
              :order-result nil)))

(rf/reg-event-db
  ::order-failure
  (fn [db [_ {:keys [response]}]]
    (assoc db :order-submitting? false
              :order-result {:errors [(or (:error response) "Order failed. Please try again.")]})))
