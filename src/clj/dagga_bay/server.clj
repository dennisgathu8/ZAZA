(ns dagga-bay.server
  "Ring HTTP server with security-hardened middleware stack."
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.util.response :as resp]
            [reitit.ring :as reitit]
            [dagga-bay.routes :as routes]
            [dagga-bay.security :as sec])
  (:gen-class))

;; ──────────────────────────────────────────────
;; Security Headers Middleware
;; ──────────────────────────────────────────────

(defn wrap-security-headers
  "Add security headers to every response."
  [handler]
  (fn [request]
    (let [response (handler request)]
      (when response
        (-> response
            (assoc-in [:headers "X-Content-Type-Options"] "nosniff")
            (assoc-in [:headers "X-Frame-Options"] "DENY")
            (assoc-in [:headers "X-XSS-Protection"] "1; mode=block")
            (assoc-in [:headers "Referrer-Policy"] "strict-origin-when-cross-origin")
            (assoc-in [:headers "Permissions-Policy"] "camera=(), microphone=(), geolocation=()")
            (assoc-in [:headers "Content-Security-Policy"]
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
;; SPA Fallback — serve index.html for client routes
;; ──────────────────────────────────────────────

(defn spa-handler
  "Serve index.html for all non-API, non-static routes (SPA fallback)."
  [_request]
  (-> (resp/resource-response "public/index.html")
      (resp/content-type "text/html; charset=utf-8")))

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
      (wrap-security-headers)
      (wrap-rate-limit)
      (wrap-request-size-limit 65536)
      (wrap-content-type)
      (wrap-not-modified)
      ((fn [handler]
         (fn [request]
           (println "DEBUG:" (:request-method request) (:uri request) "from" (:remote-addr request))
           (handler request))))))

;; ──────────────────────────────────────────────
;; Server Lifecycle
;; ──────────────────────────────────────────────

(defonce ^:private server (atom nil))

(defn start-server!
  "Start the HTTP server."
  [& {:keys [port] :or {port 3000}}]
  (println "\n🔒 Security Directive: ACTIVE — defense-in-depth, OWASP Top 10, CSRF, rate limiting")
  (println (str "🌿 Dagga Bay server starting on http://127.0.0.1:" port))
  (start-cleanup-scheduler!)
  (reset! server (jetty/run-jetty app {:port port :host "127.0.0.1" :join? false}))
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
  (start-server! :port 3001))
