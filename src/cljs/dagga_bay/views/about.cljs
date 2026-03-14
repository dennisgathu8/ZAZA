(ns dagga-bay.views.about
  "About, Contact, Locations, and Legal page.")

(defn about-page []
  [:div.page-about
   [:h1.page-title "// ABOUT"]

   ;; About Section
   [:section.about-section
    [:h2.section-heading "WHO WE ARE"]
    [:div.about-content
     [:p "Dagga Bay is Cape Town's community-driven cannabis dispensary, "
      "born from a love for South Africa's legendary landrace strains "
      "and the belief that quality cannabis should be accessible to every "
      "responsible adult."]
     [:p "We source directly from local growers across the Western Cape, "
      "KwaZulu-Natal, and partner farms in Eswatini (Swaziland). Every "
      "product in our catalog is quality-checked and honestly described."]
     [:p "Named after the Afrikaans/South African word for cannabis — "
      [:strong "dagga"] " — and inspired by the raw, functional beauty "
      "of the early internet, Dagga Bay is built on transparency, "
      "simplicity, and respect for the plant."]]]

   ;; Locations
   [:section.about-section
    [:h2.section-heading "📍 OUR LOCATIONS"]
    [:div.locations-grid
     [:div.location-card
      [:h3 "Woodstock"]
      [:p "📍 Albert Road, Woodstock"]
      [:p "🕐 Mon–Sat: 9am–7pm"]
      [:p "Our flagship spot in the creative heart of Cape Town."]]
     [:div.location-card
      [:h3 "Observatory"]
      [:p "📍 Lower Main Road, Observatory"]
      [:p "🕐 Mon–Sat: 10am–8pm"]
      [:p "Chill vibes near the student quarter."]]
     [:div.location-card
      [:h3 "Gardens"]
      [:p "📍 Kloof Street, Gardens"]
      [:p "🕐 Mon–Fri: 10am–6pm"]
      [:p "Boutique experience on the iconic Kloof strip."]]]]

   ;; Contact
   [:section.about-section
    [:h2.section-heading "📞 CONTACT US"]
    [:div.contact-info
     [:div.contact-card
      [:h3 "WhatsApp (Fastest)"]
      [:a.whatsapp-btn
       {:href "https://wa.me/254780693707" :target "_blank" :rel "noopener noreferrer"}
       "💬 Chat on WhatsApp"]
      [:p "We typically respond within 15 minutes during business hours."]]
     [:div.contact-card
      [:h3 "Matrix (Private / Encrypted)"]
      [:a.matrix-btn
       {:href "https://matrix.to/#/@raekwonzila:matrix.org" :target "_blank" :rel "noopener noreferrer"}
       "👾 Chat via Matrix/FluffyChat"]
      [:p "Privacy-focused, decentralized end-to-end encrypted chat."]]
     [:div.contact-card
      [:h3 "Email"]
      [:p [:a {:href "mailto:orders@daggabay.co.za"} "orders@daggabay.co.za"]]]
     [:div.contact-card
      [:h3 "Instagram"]
      [:p [:a {:href "#" :target "_blank" :rel "noopener noreferrer"} "@daggabay_ct"]]]]]

   ;; Legal
   [:section.about-section.legal-section
    [:h2.section-heading "⚖️ LEGAL INFORMATION"]
    [:div.legal-content
     [:h3 "South African Cannabis Law"]
     [:p "On 18 September 2018, the Constitutional Court of South Africa ruled "
      "that the personal cultivation, possession, and use of cannabis by adults "
      "in private is not a criminal offence (Minister of Justice v Prince)."]
     [:p "The Cannabis for Private Purposes Act (2024) further defines the legal "
      "framework for personal cannabis use in South Africa."]

     [:h3 "Our Compliance"]
     [:ul
      [:li "All products are for personal use only — not for resale"]
      [:li "Access is restricted to persons 18 years and older"]
      [:li "We do not ship across international borders"]
      [:li "Delivery is available within the Cape Town metropolitan area only"]
      [:li "We reserve the right to refuse service"]]

     [:h3 "Responsible Use"]
     [:ul
      [:li "Do not operate vehicles or heavy machinery under the influence"]
      [:li "Keep all cannabis products out of reach of children and pets"]
      [:li "Start with low doses, especially with edibles — effects may take 30-90 minutes"]
      [:li "If you experience adverse effects, stop use and seek medical attention"]
      [:li "Cannabis use during pregnancy or breastfeeding is not recommended"]]]]])
