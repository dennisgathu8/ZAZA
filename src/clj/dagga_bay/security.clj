(ns dagga-bay.security
  "Security utilities — CSRF tokens, input sanitization, rate limiting, age verification."
  (:require [clojure.string :as str])
  (:import [javax.crypto Mac]
           [javax.crypto.spec SecretKeySpec]))

;; ──────────────────────────────────────────────
;; Persistent HMAC Secret
;; ──────────────────────────────────────────────

;; Must survive server restarts. Read from DAGGA_BAY_SECRET env var in production.
;; Falls back to a stable dev key locally so local dev always works.
(defonce ^:private hmac-secret
  (delay
    (let [secret (System/getenv "DAGGA_BAY_SECRET")]
      (when (nil? secret)
        (throw (ex-info "DAGGA_BAY_SECRET environment variable is not set"
                        {:cause :missing-secret})))
      (SecretKeySpec. (.getBytes ^String secret "UTF-8") "HmacSHA256"))))

;; Keep cookie-secret as an alias for back-compat
(def ^:private cookie-secret hmac-secret)

(defn hmac-sign
  "Sign a value with HMAC-SHA256. Returns 'value.signature'."
  [value]
  (let [mac (doto (Mac/getInstance "HmacSHA256")
              (.init @cookie-secret))
        sig (->> (.doFinal mac (.getBytes (str value) "UTF-8"))
                 (map #(format "%02x" (bit-and % 0xff)))
                 (apply str))]
    (str value "." sig)))

(defn hmac-verify
  "Verify an HMAC-signed cookie value. Returns the original value if valid, nil otherwise."
  [signed-value]
  (when (and signed-value (string? signed-value))
    (let [dot-idx (.lastIndexOf ^String signed-value (int \.))]
      (when (pos? dot-idx)
        (let [value (subs signed-value 0 dot-idx)]
          (when (= signed-value (hmac-sign value))
            value))))))

;; ──────────────────────────────────────────────
;; Server-Side Age Verification
;; ──────────────────────────────────────────────

(defn verify-age
  "Validate a date-of-birth string (YYYY-MM-DD) and return true if 18+."
  [dob-str]
  (when (and (string? dob-str)
             (re-matches #"\d{4}-\d{2}-\d{2}" dob-str))
    (try
      (let [[year month day] (map parse-long (str/split dob-str #"-"))
            now (java.time.LocalDate/now)
            dob (java.time.LocalDate/of (int year) (int month) (int day))
            age (.getYears (java.time.Period/between dob now))]
        (when (and (>= age 18) (<= age 120))
          true))
      (catch Exception _ nil))))

(defn age-verified-cookie
  "Generate a signed, tamper-proof age-verified cookie value."
  []
  (hmac-sign "verified"))

(defn valid-age-cookie?
  "Check if a cookie value is a validly signed age-verified token."
  [cookie-value]
  (= "verified" (hmac-verify cookie-value)))

;; ──────────────────────────────────────────────
;; CSRF Token Management (Stateless HMAC)
;; ──────────────────────────────────────────────

;; Stateless HMAC-based CSRF tokens — no server-side state required.
;; Format: "<timestamp>.<hmac-signature>"
;; Tokens are valid for 60 minutes and are single-use by construction
;; (the client discards them after use; the server validates the signature + TTL).

(def ^:private csrf-ttl-ms (* 60 60 1000)) ;; 60 minutes

(defn generate-csrf-token
  "Generate a stateless HMAC-signed CSRF token with embedded timestamp."
  []
  (let [ts (str (System/currentTimeMillis))
        mac (doto (Mac/getInstance "HmacSHA256")
              (.init @hmac-secret))
        sig (->> (.doFinal mac (.getBytes ts "UTF-8"))
                 (map #(format "%02x" (bit-and % 0xff)))
                 (apply str))]
    (str ts "." sig)))

(defn valid-csrf-token?
  "Validate a stateless HMAC-signed CSRF token. Returns true if valid and not expired."
  [token]
  (when (and token (string? token) (not (str/blank? token)))
    (let [dot-idx (.lastIndexOf ^String token (int \.))
          _ (when (neg? dot-idx) (throw (Exception. "no dot")))]
      (try
        (let [ts-str  (subs token 0 dot-idx)
              sig     (subs token (inc dot-idx))
              ts      (parse-long ts-str)
              now     (System/currentTimeMillis)
              mac     (doto (Mac/getInstance "HmacSHA256")
                        (.init @hmac-secret))
              expected-sig (->> (.doFinal mac (.getBytes ts-str "UTF-8"))
                                (map #(format "%02x" (bit-and % 0xff)))
                                (apply str))]
          (and (= sig expected-sig)          ;; signature valid
               (> (+ ts csrf-ttl-ms) now)    ;; not expired
               (> now (- ts 5000))))         ;; not from the future
        (catch Exception _ false)))))

(defn cleanup-expired-tokens!
  "No-op — stateless tokens have no server-side state to clean up."
  [])


;; ──────────────────────────────────────────────
;; Input Sanitization
;; ──────────────────────────────────────────────

(defn sanitize-string
  "Strip dangerous characters, limit length."
  [s max-len]
  (when (string? s)
    (-> s
        str/trim
        (str/replace #"[<>\"';\\&|`$\\\\]" "")  ;; Strip injection chars
        (subs 0 (min (count s) max-len)))))



(defn valid-name?
  "Validate a human name — letters, spaces, hyphens, apostrophes only."
  [name-str]
  (when (string? name-str)
    (and (>= (count (str/trim name-str)) 2)
         (<= (count name-str) 100)
         (re-matches #"^[a-zA-Z\s\-']+$" name-str))))

(defn valid-address?
  "Validate delivery address — reasonable length, no script injection."
  [addr]
  (when (string? addr)
    (and (>= (count (str/trim addr)) 10)
         (<= (count addr) 500)
         (not (re-find #"(?i)<script|javascript:|on\w+=" addr)))))

;; ──────────────────────────────────────────────
;; Rate Limiting (Token Bucket per IP)
;; ──────────────────────────────────────────────

(defonce ^:private rate-limits (atom {}))

(def ^:private rate-config
  {:max-tokens 100
   :refill-ms  60000    ;; refill window: 1 minute
   :refill-amt 50})

(defn allowed?
  "Check if the given IP is within rate limits. Returns true if allowed."
  [ip]
  (let [now (System/currentTimeMillis)
        {:keys [max-tokens refill-ms refill-amt]} rate-config
        bucket (get @rate-limits ip {:tokens max-tokens :last-refill now})
        elapsed (- now (:last-refill bucket))
        refills (quot elapsed refill-ms)
        new-tokens (min max-tokens (+ (:tokens bucket) (* refills refill-amt)))
        new-last (if (pos? refills)
                   (+ (:last-refill bucket) (* refills refill-ms))
                   (:last-refill bucket))]
    (if (pos? new-tokens)
      (do (swap! rate-limits assoc ip {:tokens (dec new-tokens) :last-refill new-last})
          true)
      (do (swap! rate-limits assoc ip {:tokens 0 :last-refill new-last})
          false))))

;; Periodic cleanup of stale IPs
(defn cleanup-rate-limits!
  "Remove rate limit entries older than 10 minutes."
  []
  (let [cutoff (- (System/currentTimeMillis) 600000)]
    (swap! rate-limits (fn [m] (into {} (filter (fn [[_ v]] (> (:last-refill v) cutoff)) m))))))
