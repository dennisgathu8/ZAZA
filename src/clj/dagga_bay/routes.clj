(ns dagga-bay.routes
  "API routes and static file serving."
  (:require [dagga-bay.security :as sec]
            [dagga-bay.orders :as orders]
            [clojure.data.json :as json]
            [ring.util.response :as resp]))

;; ──────────────────────────────────────────────
;; API Handlers
;; ──────────────────────────────────────────────

(defn csrf-token-handler
  "Generate and return a fresh CSRF token."
  [_request]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/write-str {:csrf-token (sec/generate-csrf-token)})})

(defn submit-order-handler
  "Handle order submission with CSRF validation (Rate limiting is global middleware)."
  [request]
  (cond
    ;; CSRF validation
    (not (sec/valid-csrf-token? (get-in request [:headers "x-csrf-token"])))
    {:status 403
     :headers {"Content-Type" "application/json"}
     :body (json/write-str {:error "Invalid or expired security token. Please refresh and try again."})}

    ;; Process order
    :else
    (let [body (try
                 (json/read-str (slurp (:body request)) :key-fn keyword)
                 (catch Exception _ nil))]
      (if (nil? body)
        {:status 400
         :headers {"Content-Type" "application/json"}
         :body (json/write-str {:error "Invalid request body."})}
        (let [[ok? result] (orders/submit-order! body)]
          {:status (if ok? 200 400)
           :headers {"Content-Type" "application/json"}
           :body (json/write-str result)})))))

;; ──────────────────────────────────────────────
;; Route Definitions (for Reitit)
;; ──────────────────────────────────────────────

(def api-routes
  ["/api"
   ["/csrf-token" {:get csrf-token-handler}]
   ["/orders"     {:post submit-order-handler}]])
