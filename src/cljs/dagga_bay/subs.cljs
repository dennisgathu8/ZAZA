(ns dagga-bay.subs
  "Re-frame subscriptions."
  (:require [re-frame.core :as rf]
            [clojure.string :as str]))

;; ── Simple extractors ───────────────────────

(rf/reg-sub ::age-verified?    (fn [db _] (:age-verified? db)))
(rf/reg-sub ::age-error        (fn [db _] (:age-error db)))
(rf/reg-sub ::current-route    (fn [db _] (:current-route db)))
(rf/reg-sub ::products         (fn [db _] (:products db)))
(rf/reg-sub ::categories       (fn [db _] (:categories db)))
(rf/reg-sub ::strain-types     (fn [db _] (:strain-types db)))
(rf/reg-sub ::search-query     (fn [db _] (:search-query db)))
(rf/reg-sub ::filters          (fn [db _] (:filters db)))
(rf/reg-sub ::cart              (fn [db _] (:cart db)))
(rf/reg-sub ::cart-open?        (fn [db _] (:cart-open? db)))
(rf/reg-sub ::order-form       (fn [db _] (:order-form db)))
(rf/reg-sub ::order-submitting? (fn [db _] (:order-submitting? db)))
(rf/reg-sub ::order-result     (fn [db _] (:order-result db)))
(rf/reg-sub ::csrf-token       (fn [db _] (:csrf-token db)))
(rf/reg-sub ::show-mobile-menu? (fn [db _] (:show-mobile-menu? db)))

;; ── Derived: filtered products ──────────────

(rf/reg-sub
  ::filtered-products
  :<- [::products]
  :<- [::search-query]
  :<- [::filters]
  (fn [[products query filters] _]
    (let [{:keys [category strain-type thc-min thc-max price-sort]} filters
          q (str/lower-case (or query ""))]
      (cond->> products
        ;; Text search
        (not (str/blank? q))
        (filter #(or (str/includes? (str/lower-case (:name %)) q)
                     (str/includes? (str/lower-case (:description %)) q)
                     (str/includes? (str/lower-case (name (or (:category %) ""))) q)))

        ;; Category filter
        category
        (filter #(= (:category %) category))

        ;; Strain type filter
        strain-type
        (filter #(= (:strain-type %) strain-type))

        ;; THC range
        (and thc-min (pos? thc-min))
        (filter #(>= (:thc %) thc-min))

        (and thc-max (< thc-max 100))
        (filter #(<= (:thc %) thc-max))

        ;; Price sort (by first weight option)
        price-sort
        (sort-by #(-> % :weights first :price)
                 (if (= price-sort :desc) > <))))))

;; ── Derived: featured products ──────────────

(rf/reg-sub
  ::featured-products
  :<- [::products]
  (fn [products _]
    (filter :featured products)))

;; ── Derived: cart computations ──────────────

(rf/reg-sub
  ::cart-items
  :<- [::cart]
  (fn [cart _]
    (vals cart)))

(rf/reg-sub
  ::cart-count
  :<- [::cart]
  (fn [cart _]
    (reduce + 0 (map :quantity (vals cart)))))

(rf/reg-sub
  ::cart-total
  :<- [::cart]
  (fn [cart _]
    (reduce + 0
            (map (fn [{:keys [weight-option quantity]}]
                   (* (:price weight-option) quantity))
                 (vals cart)))))
