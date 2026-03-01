(ns dagga-bay.security
  "Security utilities — CSRF tokens, input sanitization, rate limiting, age verification."
  (:require [crypto.random :as random]
            [clojure.string :as str])
  (:import [javax.crypto Mac]
           [javax.crypto.spec SecretKeySpec]))

;; ──────────────────────────────────────────────
;; HMAC Cookie Signing (age gate enforcement)
;; ──────────────────────────────────────────────

(defonce ^:private cookie-secret
  (SecretKeySpec. (.getBytes ^String (random/url-part 32) "UTF-8") "HmacSHA256"))

(defn hmac-sign
  "Sign a value with HMAC-SHA256. Returns 'value.signature'."
  [value]
  (let [mac (doto (Mac/getInstance "HmacSHA256")
              (.init cookie-secret))
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
;; CSRF Token Management
;; ──────────────────────────────────────────────

(defonce ^:private csrf-tokens (atom {}))

(defn generate-csrf-token
  "Generate a cryptographically secure CSRF token."
  []
  (let [token (random/url-part 32)
        expiry (+ (System/currentTimeMillis) (* 30 60 1000))] ;; 30 min TTL
    (swap! csrf-tokens assoc token expiry)
    token))

(defn valid-csrf-token?
  "Validate a CSRF token and consume it (single-use)."
  [token]
  (when (and token (string? token) (not (str/blank? token)))
    (let [expiry (get @csrf-tokens token)]
      (when (and expiry (> expiry (System/currentTimeMillis)))
        (swap! csrf-tokens dissoc token)
        true))))

(defn cleanup-expired-tokens!
  "Remove expired CSRF tokens."
  []
  (let [now (System/currentTimeMillis)]
    (swap! csrf-tokens (fn [m] (into {} (filter (fn [[_ exp]] (> exp now)) m))))))

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

(defn valid-phone?
  "Validate South African phone format."
  [phone]
  (when (string? phone)
    (re-matches #"^(\+27|0)[0-9]{9,10}$" (str/replace phone #"[\s\-]" ""))))

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
