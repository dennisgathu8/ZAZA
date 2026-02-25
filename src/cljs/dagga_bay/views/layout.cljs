(ns dagga-bay.views.layout
  "Global layout: navbar, footer, floating cart badge."
  (:require [re-frame.core :as rf]
            [dagga-bay.events :as events]
            [dagga-bay.subs :as subs]))

(defn nav-link [route label]
  (let [current @(rf/subscribe [::subs/current-route])]
    [:a.nav-link
     {:class (when (= current route) "active")
      :href "#"
      :on-click (fn [e]
                  (.preventDefault e)
                  (rf/dispatch [::events/navigate route]))}
     label]))

(defn navbar []
  (let [mobile-open? @(rf/subscribe [::subs/show-mobile-menu?])
        cart-count @(rf/subscribe [::subs/cart-count])]
    [:nav.navbar
     [:div.navbar-inner
      [:div.navbar-brand
       {:on-click #(rf/dispatch [::events/navigate :home])}
       [:span.brand-dagga "DAGGA"]
       [:span.brand-bay "BAY"]]

      ;; Desktop nav
      [:div.navbar-links {:class (when mobile-open? "mobile-open")}
       [nav-link :home "HOME"]
       [nav-link :shop "SHOP"]
       [nav-link :cart "CART"]
       [nav-link :order "ORDER"]
       [nav-link :about "ABOUT"]]

      [:div.navbar-actions
       ;; Cart badge
       [:button.cart-badge
        {:on-click #(rf/dispatch [::events/navigate :cart])}
        "🛒"
        (when (pos? cart-count)
          [:span.cart-count cart-count])]

       ;; Mobile hamburger
       [:button.hamburger
        {:on-click #(rf/dispatch [::events/toggle-mobile-menu])}
        (if mobile-open? "✕" "☰")]]]]))

(defn footer []
  [:footer.site-footer
   [:div.footer-inner
    [:div.footer-grid
     [:div.footer-col
      [:h4.footer-heading "DAGGA BAY"]
      [:p "Cape Town's finest cannabis dispensary. Quality products, honest service."]]
     [:div.footer-col
      [:h4.footer-heading "QUICK LINKS"]
      [:ul.footer-links
       [:li [:a {:href "#" :on-click #(do (.preventDefault %) (rf/dispatch [::events/navigate :shop]))} "Shop"]]
       [:li [:a {:href "#" :on-click #(do (.preventDefault %) (rf/dispatch [::events/navigate :about]))} "About"]]
       [:li [:a {:href "#" :on-click #(do (.preventDefault %) (rf/dispatch [::events/navigate :order]))} "Place Order"]]]]
     [:div.footer-col
      [:h4.footer-heading "CONTACT"]
      [:p "📍 Cape Town, South Africa"]
      [:p [:a.whatsapp-link {:href "https://wa.me/254780693707" :target "_blank" :rel "noopener noreferrer"}
           "💬 WhatsApp Us"]]]]

    [:div.footer-legal
     [:p "⚠️ " [:strong "Legal Disclaimer:"]
      " Cannabis products are regulated in South Africa. Personal use is permitted under the "
      "Constitutional Court ruling of September 2018. Sale and distribution remain regulated. "
      "Dagga Bay operates within the provisions of the Cannabis for Private Purposes Act. "
      "Products are for personal use only. Not for resale."]
     [:p "🔞 You must be 18 years or older to use this website and purchase products."]
     [:p "🌿 Please consume responsibly. Do not drive under the influence. "
      "Keep out of reach of children and pets."]
     [:p.footer-copy "© 2025 Dagga Bay — Cape Town, South Africa. All rights reserved."]]]])

(defn floating-cart []
  (let [cart-count @(rf/subscribe [::subs/cart-count])
        cart-total @(rf/subscribe [::subs/cart-total])]
    (when (pos? cart-count)
      [:div.floating-cart
       {:on-click #(rf/dispatch [::events/navigate :cart])}
       [:span.fc-icon "🛒"]
       [:span.fc-count cart-count " items"]
       [:span.fc-total (str "R" cart-total)]
       [:span.fc-action "→"]])))
