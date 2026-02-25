(ns dagga-bay.views.age-gate
  "Enforced 18+ age verification gate."
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [dagga-bay.events :as events]
            [dagga-bay.subs :as subs]))

(defn- today-str []
  (let [d (js/Date.)]
    (str (.getFullYear d) "-"
         (.padStart (str (inc (.getMonth d))) 2 "0") "-"
         (.padStart (str (.getDate d)) 2 "0"))))

(defn age-gate []
  (let [dob (r/atom "")
        declined? (r/atom false)]
    (fn []
      (let [error @(rf/subscribe [::subs/age-error])]
        [:div.age-gate-overlay
         [:div.age-gate-modal
          [:div.age-gate-logo
           [:span.logo-dagga "DAGGA"]
           [:span.logo-bay "BAY"]]
          [:div.age-gate-skull "🌿"]
          [:h2.age-gate-title "AGE VERIFICATION REQUIRED"]
          [:p.age-gate-subtitle
           "This website contains cannabis products. You must be 18 years or older to enter."]
          [:p.age-gate-legal
           "In accordance with South African law and the Constitutional Court ruling "
           "on personal cannabis use (2018), access is restricted to adults only."]

          (if @declined?
            [:div.age-gate-declined
             [:p "❌ Access Denied"]
             [:p "You must be 18 or older to access this website."]
             [:button.btn-retry
              {:on-click #(reset! declined? false)}
              "Try Again"]]

            [:div.age-gate-form
             [:label.age-gate-label {:for "dob-input"} "Enter your date of birth:"]
             [:input#dob-input.age-gate-input
              {:type "date"
               :max (today-str)
               :value @dob
               :on-change #(reset! dob (.. % -target -value))}]

             (when error
               [:div.age-gate-error error])

             [:div.age-gate-actions
              [:button.btn-enter
               {:on-click #(when (seq @dob)
                             (rf/dispatch [::events/verify-age @dob]))
                :disabled (empty? @dob)}
               "ENTER SITE"]
              [:button.btn-decline
               {:on-click #(reset! declined? true)}
               "I AM UNDER 18"]]])

          [:p.age-gate-footer
           "By entering, you confirm you are 18+ and agree to our terms of use."]]]))))
