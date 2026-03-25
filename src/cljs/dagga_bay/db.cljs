(ns dagga-bay.db
  "Default app-db state."
  (:require [dagga-bay.products :as products]))

(def default-db
  {:age-verified?  false
   :current-route  :home
   :products       products/products
   :categories     products/categories
   :strain-types   products/strain-types
   :cart           {}           ;; {product-key {:product ... :weight-option ... :quantity n}}
   :search-query   ""
   :filters        {:category   nil
                    :strain-type nil
                    :thc-min     0
                    :thc-max     100
                    :price-sort  nil}  ;; :asc or :desc
   :order-form     {:name    ""
                     :address ""
                     :notes   ""}
   :order-submitting? false
   :order-result      nil
   :csrf-token        nil
   :show-mobile-menu? false})
