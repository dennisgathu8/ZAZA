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

(defn verify-age-handler
  "Server-side age verification. Sets a signed HttpOnly cookie on success."
  [request]
  (let [body (try
               (json/read-str (slurp (:body request)) :key-fn keyword)
               (catch Exception _ nil))
        dob (:dob body)]
    (if (sec/verify-age dob)
      ;; Age verified — set signed cookie
      (-> (resp/response (json/write-str {:verified true}))
          (resp/content-type "application/json")
          (assoc-in [:headers "Set-Cookie"]
                    (str "dagga-bay-verified=" (sec/age-verified-cookie)
                         "; Path=/; HttpOnly; SameSite=Strict"
                         (when (= "production" (System/getenv "DAGGA_BAY_MODE"))
                           "; Secure"))))
      ;; Rejected
      {:status 403
       :headers {"Content-Type" "application/json"}
       :body (json/write-str {:verified false
                              :error "You must be 18 or older to access this site."})})))

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
   ["/csrf-token"  {:get csrf-token-handler}]
   ["/verify-age"  {:post verify-age-handler}]
   ["/orders"      {:post submit-order-handler}]])
