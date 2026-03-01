(ns dagga-bay.server
  "Ring HTTP server with security-hardened middleware stack."
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.util.response :as resp]
            [reitit.ring :as reitit]
            [hiccup.page :as page]
            [dagga-bay.routes :as routes]
            [dagga-bay.security :as sec]
            [dagga-bay.views :as views]
            [clojure.string :as str])
  (:gen-class))

;; ──────────────────────────────────────────────
;; Cookie Parsing
;; ──────────────────────────────────────────────

(defn- parse-cookies
  "Parse the Cookie header into a map of {name value}."
  [request]
  (when-let [cookie-header (get-in request [:headers "cookie"])]
    (into {}
          (for [pair (str/split cookie-header #";\s*")
                :let [kv (str/split pair #"=" 2)]
                :when (= 2 (count kv))]
            [(str/trim (first kv)) (str/trim (second kv))]))))

(defn- age-verified?
  "Check if the request contains a valid age-verified cookie."
  [request]
  (let [cookies (parse-cookies request)]
    (sec/valid-age-cookie? (get cookies "dagga-bay-verified"))))

;; ──────────────────────────────────────────────
;; Security Headers Middleware
;; ──────────────────────────────────────────────

(defn wrap-security-headers
  "Add hardened security headers to every response."
  [handler]
  (fn [request]
    (let [response (handler request)]
      (when response
        (-> response
            ;; Prevent MIME-type sniffing
            (assoc-in [:headers "X-Content-Type-Options"] "nosniff")
            ;; Prevent clickjacking
            (assoc-in [:headers "X-Frame-Options"] "DENY")
            ;; Referrer policy
            (assoc-in [:headers "Referrer-Policy"] "strict-origin-when-cross-origin")
            ;; Feature restrictions
            (assoc-in [:headers "Permissions-Policy"]
                      "camera=(), microphone=(), geolocation=(), payment=()")
            ;; HSTS — enforce HTTPS (1 year, include subdomains)
            (assoc-in [:headers "Strict-Transport-Security"]
                      "max-age=31536000; includeSubDomains")
            ;; CSP in REPORT-ONLY mode first (safe rollout)
            (assoc-in [:headers "Content-Security-Policy-Report-Only"]
                      (str "default-src 'self'; "
                           "script-src 'self' 'unsafe-inline' 'unsafe-eval'; "
                           "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                           "font-src 'self' https://fonts.gstatic.com; "
                           "img-src 'self' data:; "
                           "connect-src 'self'; "
                           "frame-ancestors 'none';")))))))

(defn wrap-request-size-limit
  "Reject requests with bodies larger than max-bytes."
  [handler max-bytes]
  (fn [request]
    (if-let [content-length (some-> (get-in request [:headers "content-length"])
                                     parse-long)]
      (if (> content-length max-bytes)
        {:status 413
         :headers {"Content-Type" "application/json"}
         :body "{\"error\":\"Request body too large\"}"}
        (handler request))
      (handler request))))

(defn wrap-rate-limit
  "Rate limit middleware using IP-based token bucket."
  [handler]
  (fn [request]
    (let [ip (or (get-in request [:headers "x-forwarded-for"])
                 (:remote-addr request)
                 "unknown")]
      (if (sec/allowed? ip)
        (handler request)
        {:status 429
         :headers {"Content-Type" "application/json" "Retry-After" "60"}
         :body "{\"error\":\"Too many requests. Please wait a moment.\"}"}))))

;; ──────────────────────────────────────────────
;; Server-Rendered Age Gate Page (Hiccup)
;; ──────────────────────────────────────────────

(defn- render-age-gate-page
  "Server-side rendered age gate — works without JavaScript."
  []
  (page/html5
    {:lang "en"}
    [:head
     [:meta {:charset "UTF-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
     [:meta {:name "robots" :content "noindex, nofollow"}]
     [:title "Dagga Bay — Age Verification"]
     [:link {:rel "preconnect" :href "https://fonts.googleapis.com"}]
     [:link {:rel "preconnect" :href "https://fonts.gstatic.com" :crossorigin "anonymous"}]
     [:link {:href "https://fonts.googleapis.com/css2?family=VT323&family=JetBrains+Mono:wght@400;700&display=swap"
             :rel "stylesheet"}]
     [:link {:rel "stylesheet" :href "/css/style.css"}]
     [:link {:rel "icon"
             :href "data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text y=%22.9em%22 font-size=%2290%22>🌿</text></svg>"}]
     [:style
      ".ssr-gate{display:flex;align-items:center;justify-content:center;min-height:100vh;background:#0a0a0a;color:#00ff9f;font-family:'VT323',monospace;}"
      ".ssr-gate-box{background:#111;border:2px solid #00ff9f;border-radius:8px;padding:40px;max-width:420px;width:90%;text-align:center;box-shadow:0 0 30px rgba(0,255,159,0.15);}"
      ".ssr-gate-box h1{font-size:2.5rem;margin:0;letter-spacing:4px;}"
      ".ssr-gate-box h1 .bay{color:#ff6b35;}"
      ".ssr-gate-box .icon{font-size:3rem;margin:16px 0;}"
      ".ssr-gate-box h2{font-size:1.3rem;margin:12px 0;color:#e0e0e0;font-family:'JetBrains Mono',monospace;}"
      ".ssr-gate-box p{font-size:0.9rem;color:#888;line-height:1.5;font-family:'JetBrains Mono',monospace;}"
      ".ssr-gate-box label{display:block;margin:20px 0 8px;font-size:1.1rem;color:#ccc;}"
      ".ssr-gate-box input[type=date]{width:100%;padding:10px;background:#1a1a1a;border:1px solid #333;color:#00ff9f;font-size:1rem;border-radius:4px;font-family:'JetBrains Mono',monospace;}"
      ".ssr-gate-box .btn-enter{display:block;width:100%;padding:12px;margin-top:16px;background:#00ff9f;color:#0a0a0a;border:none;font-size:1.2rem;font-weight:bold;cursor:pointer;border-radius:4px;font-family:'VT323',monospace;letter-spacing:2px;}"
      ".ssr-gate-box .btn-enter:hover{background:#39ff14;}"
      ".ssr-gate-box .footer-text{margin-top:20px;font-size:0.75rem;color:#555;}"
      ".ssr-error{background:#2a0000;border:1px solid #ff4444;color:#ff4444;padding:10px;border-radius:4px;margin-top:12px;font-family:'JetBrains Mono',monospace;font-size:0.85rem;}"]]
    [:body
     [:div.ssr-gate
      [:div.ssr-gate-box
       [:h1 "DAGGA" [:span.bay "BAY"]]
       [:div.icon "🌿"]
       [:h2 "AGE VERIFICATION REQUIRED"]
       [:p "This website contains cannabis products. You must be 18 years or older to enter."]
       [:p "In accordance with South African law and the Constitutional Court ruling "
        "on personal cannabis use (2018), access is restricted to adults only."]
       [:form {:id "age-gate-form"}
        [:label {:for "dob"} "Enter your date of birth:"]
        [:input {:type "date" :id "dob" :name "dob" :required true}]
        [:div {:id "age-error" :style "display:none"}]
        [:button.btn-enter {:type "submit"} "ENTER SITE"]]
       [:p.footer-text "By entering, you confirm you are 18+ and agree to our terms of use."]]]

     ;; Minimal inline JS for the form submission (no SPA bundle needed)
     [:script
      (str
        "document.getElementById('age-gate-form').addEventListener('submit',function(e){"
        "e.preventDefault();"
        "var dob=document.getElementById('dob').value;"
        "if(!dob)return;"
        "var errEl=document.getElementById('age-error');"
        "fetch('/api/verify-age',{"
        "method:'POST',"
        "headers:{'Content-Type':'application/json'},"
        "body:JSON.stringify({dob:dob})"
        "}).then(function(r){return r.json().then(function(d){return{ok:r.ok,data:d}});})"
        ".then(function(res){"
        "if(res.ok){"
        "try{sessionStorage.setItem('dagga-bay-age-verified','true');}catch(e){}"
        "window.location.reload();"
        "}else{"
        "errEl.className='ssr-error';errEl.style.display='block';"
        "errEl.textContent=res.data.error||'You must be 18 or older.';"
        "}"
        "}).catch(function(){"
        "errEl.className='ssr-error';errEl.style.display='block';"
        "errEl.textContent='Connection error. Please try again.';"
        "});"
        "});")]]))

;; ──────────────────────────────────────────────
;; SPA Handler (served only to age-verified users)
;; ──────────────────────────────────────────────

(defn- spa-handler
  "Serve SSR shell for verified users, age gate for unverified."
  [request]
  (if (age-verified? request)
    (-> (resp/response (views/render-spa-shell))
        (resp/content-type "text/html; charset=utf-8"))
    (-> (resp/response (render-age-gate-page))
        (resp/content-type "text/html; charset=utf-8"))))

;; ──────────────────────────────────────────────
;; Age Gate Middleware (blocks non-static, non-API content)
;; ──────────────────────────────────────────────

(defn- static-asset?
  "Check if the request is for a static asset that should be served regardless of age gate."
  [uri]
  (or (str/starts-with? uri "/css/")
      (str/starts-with? uri "/js/")
      (str/starts-with? uri "/img/")
      (str/starts-with? uri "/fonts/")
      (str/ends-with? uri ".ico")
      (str/ends-with? uri ".woff2")
      (str/ends-with? uri ".woff")))

(defn wrap-age-gate
  "Middleware that blocks SPA content for users who haven't verified their age.
   Static assets and API routes are allowed through."
  [handler]
  (fn [request]
    (let [uri (:uri request)]
      (cond
        ;; Always allow API routes (age verification, CSRF, orders)
        (str/starts-with? uri "/api/")
        (handler request)

        ;; Always allow static assets (CSS, fonts, images)
        (static-asset? uri)
        (handler request)

        ;; For all other routes: check age verification
        (age-verified? request)
        (handler request)

        ;; Not verified — serve the age gate page
        :else
        (-> (resp/response (render-age-gate-page))
            (resp/content-type "text/html; charset=utf-8"))))))

;; ──────────────────────────────────────────────
;; Cleanup Scheduler
;; ──────────────────────────────────────────────

(defonce ^:private cleanup-task (atom nil))

(defn start-cleanup-scheduler!
  "Run periodic cleanup of expired tokens, orders, rate limits."
  []
  (reset! cleanup-task
          (future
            (loop []
              (Thread/sleep 300000) ;; every 5 minutes
              (try
                (sec/cleanup-expired-tokens!)
                (sec/cleanup-rate-limits!)
                ((resolve 'dagga-bay.orders/cleanup-expired-orders!))
                (catch Exception e
                  (println "Cleanup error:" (.getMessage e))))
              (recur)))))

;; ──────────────────────────────────────────────
;; App Ring Handler
;; ──────────────────────────────────────────────

(def app
  (-> (reitit/ring-handler
        (reitit/router
          [routes/api-routes
           ["/" {:get spa-handler}]])
        (reitit/routes
          (reitit/create-resource-handler {:path "/"})
          (reitit/create-default-handler
            {:not-found spa-handler})))
      (wrap-age-gate)
      (wrap-security-headers)
      (wrap-rate-limit)
      (wrap-request-size-limit 65536)
      (wrap-content-type)
      (wrap-not-modified)))

;; ──────────────────────────────────────────────
;; Server Lifecycle
;; ──────────────────────────────────────────────

(defonce ^:private server (atom nil))

(defn start-server!
  "Start the HTTP server."
  [& {:keys [port host] :or {port (parse-long (or (System/getenv "PORT") "3001"))
                             host (or (System/getenv "HOST") "127.0.0.1")}}]
  (println "\n🔒 Security Directive: ACTIVE — defense-in-depth, OWASP Top 10, CSRF, rate limiting")
  (println (str "🌿 Dagga Bay server starting on http://" host ":" port))
  (start-cleanup-scheduler!)
  (reset! server (jetty/run-jetty app {:port port :host host :join? false}))
  (println (str "✅ Server running on port " port ". Press Ctrl+C to stop.\n")))

(defn stop-server!
  "Stop the HTTP server."
  []
  (when-let [s @server]
    (.stop s)
    (reset! server nil)
    (println "🛑 Server stopped.")))

(defn -main
  "Entry point."
  [& _args]
  (start-server!))
