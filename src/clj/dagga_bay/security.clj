(ns dagga-bay.security
  "Security utilities — CSRF tokens, input sanitization, rate limiting."
  (:require [crypto.random :as random]
            [clojure.string :as str]))

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
        (str/replace #"[<>\"';&|`$\\]" "")  ;; Strip injection chars
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
  {:max-tokens 100      ;; increased from 10
   :refill-ms  60000    ;; refill window: 1 minute
   :refill-amt 50})     ;; increased from 5

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
