(ns dagga-bay.views
  "Server-side rendered Hiccup views — SPA shell, meta tags, structured data."
  (:require [hiccup.page :as page]))

;; ──────────────────────────────────────────────
;; Site Metadata (single source of truth)
;; ──────────────────────────────────────────────

(def site-meta
  {:title       "Dagga Bay — Cape Town Cannabis Dispensary"
   :description "Cape Town's community-driven cannabis dispensary. Premium flower, edibles, vapes, concentrates & accessories. Quality South African cannabis for responsible adults 18+."
   :url         "https://dagga-bay-dispensary.fly.dev"
   :locale      "en_ZA"
   :site-name   "Dagga Bay"
   :image       "/img/dagga-bay-og.png"
   :phone       "+254780693707"
   :address     {:street "Albert Road" :locality "Woodstock"
                 :region "Western Cape" :postal "7925" :country "ZA"}})

;; ──────────────────────────────────────────────
;; Common Head Elements
;; ──────────────────────────────────────────────

(defn- common-head
  "Shared <head> elements for all server-rendered pages."
  [{:keys [title description]}]
  (list
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0, maximum-scale=5.0"}]
    [:meta {:name "robots" :content "noindex, nofollow"}]

    ;; Primary meta
    [:title title]
    [:meta {:name "description" :content description}]

    ;; Open Graph
    [:meta {:property "og:type" :content "website"}]
    [:meta {:property "og:title" :content title}]
    [:meta {:property "og:description" :content description}]
    [:meta {:property "og:site_name" :content (:site-name site-meta)}]
    [:meta {:property "og:url" :content (:url site-meta)}]
    [:meta {:property "og:locale" :content (:locale site-meta)}]
    [:meta {:property "og:image" :content (str (:url site-meta) (:image site-meta))}]

    ;; Twitter Card
    [:meta {:name "twitter:card" :content "summary_large_image"}]
    [:meta {:name "twitter:title" :content title}]
    [:meta {:name "twitter:description" :content description}]
    [:meta {:name "twitter:image" :content (str (:url site-meta) (:image site-meta))}]

    ;; Fonts
    [:link {:rel "preconnect" :href "https://fonts.googleapis.com"}]
    [:link {:rel "preconnect" :href "https://fonts.gstatic.com" :crossorigin "anonymous"}]
    [:link {:href "https://fonts.googleapis.com/css2?family=VT323&family=JetBrains+Mono:wght@400;700&display=swap"
            :rel "stylesheet"}]

    ;; Styles
    [:link {:rel "stylesheet" :href "/css/style.css"}]

    ;; Favicon
    [:link {:rel "icon"
            :href "data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text y=%22.9em%22 font-size=%2290%22>🌿</text></svg>"}]))

;; ──────────────────────────────────────────────
;; Schema.org Structured Data (LocalBusiness)
;; ──────────────────────────────────────────────

(defn- schema-org-json-ld
  "Generate LocalBusiness schema.org JSON-LD for search engines."
  []
  (let [{:keys [address]} site-meta]
    (str
      "<script type=\"application/ld+json\">"
      "{"
      "\"@context\":\"https://schema.org\","
      "\"@type\":\"LocalBusiness\","
      "\"name\":\"Dagga Bay\","
      "\"description\":\"" (:description site-meta) "\","
      "\"url\":\"" (:url site-meta) "\","
      "\"telephone\":\"" (:phone site-meta) "\","
      "\"address\":{"
      "\"@type\":\"PostalAddress\","
      "\"streetAddress\":\"" (:street address) "\","
      "\"addressLocality\":\"" (:locality address) "\","
      "\"addressRegion\":\"" (:region address) "\","
      "\"postalCode\":\"" (:postal address) "\","
      "\"addressCountry\":\"" (:country address) "\""
      "},"
      "\"geo\":{"
      "\"@type\":\"GeoCoordinates\","
      "\"latitude\":\"-33.9271\","
      "\"longitude\":\"18.4411\""
      "},"
      "\"openingHours\":\"Mo-Sa 09:00-20:00\","
      "\"priceRange\":\"$$\","
      "\"currenciesAccepted\":\"ZAR\","
      "\"paymentAccepted\":\"Cash, EFT\""
      "}"
      "</script>")))

;; ──────────────────────────────────────────────
;; SPA Shell (served to age-verified users)
;; ──────────────────────────────────────────────

(defn render-spa-shell
  "Server-side rendered SPA shell with full meta tags, OG, and schema.org.
   The ClojureScript app mounts into #app. sessionStorage is pre-set so the
   client-side age gate is skipped (server already verified)."
  []
  (page/html5
    {:lang "en"}
    [:head
     (common-head {:title       (:title site-meta)
                   :description (:description site-meta)})
     ;; Schema.org structured data
     (schema-org-json-ld)]
    [:body
     [:div#app
      ;; Server-rendered loading state (replaced when ClojureScript mounts)
      [:div {:style "display:flex;align-items:center;justify-content:center;height:100vh;background:#0a0a0a;color:#00ff9f;font-family:'VT323',monospace;font-size:24px;"}
       [:div {:style "text-align:center;"}
        [:div {:style "font-size:48px;margin-bottom:20px;"} "🌿"]
        [:div "LOADING DAGGA BAY..."]
        [:div {:style "font-size:14px;color:#666;margin-top:10px;"} "Initializing secure connection"]]]]
     ;; Pre-set sessionStorage so client-side age gate is skipped
     [:script "try{sessionStorage.setItem('dagga-bay-age-verified','true');}catch(e){}"]
     ;; SPA bundle
     [:script {:src "/js/compiled/main.js"}]]))
