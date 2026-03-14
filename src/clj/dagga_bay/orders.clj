(ns dagga-bay.orders
  "In-memory order store with auto-expiry and validation."
  (:require [dagga-bay.security :as sec]
            [clojure.string :as str]))

;; ──────────────────────────────────────────────
;; Order Store (In-Memory Atom)
;; ──────────────────────────────────────────────

(defonce ^:private orders (atom []))

(def ^:private order-ttl-ms (* 24 60 60 1000)) ;; 24 hours

(defn- generate-order-id
  "Generate a short order reference."
  []
  (str "DB-" (subs (str (java.util.UUID/randomUUID)) 0 8) "-" (System/currentTimeMillis)))

;; ──────────────────────────────────────────────
;; Order Validation
;; ──────────────────────────────────────────────

(defn validate-order
  "Validate and sanitize order data. Returns [ok? result]."
  [{:keys [name phone address notes items]}]
  (let [errors (cond-> []
                 (not (sec/valid-name? name))
                 (conj "Invalid name — letters, spaces, hyphens only (2-100 chars)")

                 (not (sec/valid-phone? phone))
                 (conj "Invalid phone — use SA format: +27XXXXXXXXX or 0XXXXXXXXXX")

                 (not (sec/valid-address? address))
                 (conj "Invalid address — must be 10-500 characters, no scripts")

                 (or (nil? items) (empty? items))
                 (conj "Cart is empty")

                 (and notes (string? notes) (> (count notes) 500))
                 (conj "Notes too long — max 500 characters"))]
    (if (seq errors)
      [false {:errors errors}]
      [true {:name    (sec/sanitize-string name 100)
             :phone   (str/replace phone #"[\s\-]" "")
             :address (sec/sanitize-string address 500)
             :notes   (when notes (sec/sanitize-string notes 500))
             :items   (mapv (fn [item]
                              {:product-id (:product-id item)
                               :name       (sec/sanitize-string (str (:name item)) 100)
                               :quantity   (max 1 (min 99 (or (:quantity item) 1)))
                               :price      (:price item)})
                            items)}])))

;; ──────────────────────────────────────────────
;; Order Operations
;; ──────────────────────────────────────────────

(defn submit-order!
  "Validate and store an order. Returns [ok? result]."
  [order-data]
  (let [[valid? result] (validate-order order-data)]
    (if valid?
      (let [order-id (generate-order-id)
            order (assoc result
                         :id order-id
                         :status :pending
                         :created-at (System/currentTimeMillis))]
        (swap! orders conj order)
        ;; Simulated secure notification (console log only — no PII in logs in prod)
        (println (str "\n══════════════════════════════════════════"))
        (println (str "  📦 NEW ORDER: " order-id))
        (println (str "  Items: " (count (:items order))))
        (println (str "  Total: R" (reduce + (map #(* (:price %) (:quantity %)) (:items order)))))
        (println (str "  Status: PENDING"))
        (println (str "══════════════════════════════════════════\n"))
        [true {:order-id order-id
               :message "Order submitted! Please send your order details via Matrix or WhatsApp below."}])
      [false result])))

(defn cleanup-expired-orders!
  "Remove orders older than TTL to minimize data retention."
  []
  (let [cutoff (- (System/currentTimeMillis) order-ttl-ms)]
    (swap! orders (fn [os] (vec (filter #(> (:created-at %) cutoff) os))))))
