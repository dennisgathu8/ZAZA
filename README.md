# 🌿 Dagga Bay — Cannabis Dispensary Website

> **AUTHENTIC SOUTH AFRICAN CANNABIS ORDERING | CAPE TOWN, SA**  
> *Where The Pirate Bay’s raw aesthetic meets 2026 security engineering.*

---

## // VISION & AESTHETIC
**Dagga Bay** is a digital homage to the raw, functional beauty of the early-2000s web, specifically optimized for the modern South African cannabis landscape. Built with a "Brutalist Elegance" philosophy, it combines a deep black canvas (`#0a0a0a`) with neon/forest green accents and monospace typography. It is designed to be lightning-fast, mobile-first, and uncompromisingly secure, providing a premium ordering experience for the discerning Cape Town connoisseur.

## 🔒 SECURITY FEATURES
This project is built with a **Defense-in-Depth** architecture, prioritizing the OWASP Top 10 and South African regulatory compliance.
- **Enforced Age Gate**: A non-bypassable 18+ verification system using session-persistent logic.
- **Global Rate Limiting**: IP-based token bucket middleware protection for all API endpoints.
- **CSRF Protection**: Comprehensive protection using cryptographically secure, single-use tokens.
- **Input Sanitization**: Rigorous XSS and injection prevention on all user-controlled data.
- **Secure Headers**: Hardened configuration including CSP, X-Frame-Options (DENY), X-Content-Type-Options (nosniff), and Permissions-Policy.
- **Data Minimization**: In-memory Atom-based storage with 24-hour auto-expiry for all order data. No persistent database footprint.

## 🌿 CORE FEATURES
- **Retro Product Grid**: Interactive shop with global search and advanced filters (Strain Type, Category, THC%, Price).
- **Floating Cart**: A responsive, real-time cart system with weight-based pricing logic.
- **Secure Order Form**: Validated submission flow that logs securely to the backend for WhatsApp fulfillment.
- **SA Legal Readiness**: Pre-configured with Constitutional Court (2018) and Cannabis for Private Purposes Act (2024) disclaimers.
- **Nairobi Call Center Integration**: Direct WhatsApp linkage to the Nairobi-based centralized dispatch (+254 780 693 707).

## TECH STACK
- **Language**: Clojure & ClojureScript
- **Backend**: Ring + Reitit + Jetty
- **Frontend**: Reagent + Re-frame + shadow-cljs
- **Styling**: Pure CSS3 (Retro-Modern Hybrid)
- **State**: In-memory Atom + Transit for secure data handling

## HOW TO RUN

### Prerequisites
- Java 17+
- Clojure CLI
- Node.js & npm

### Development Mode
1. **Install JS Dependencies**
   ```bash
   npm install
   ```
2. **Launch Frontend Compiler**
   ```bash
   npx shadow-cljs watch app
   ```
3. **Start Ring Server**
   ```bash
   clojure -M:dev
   ```
   *The application will be available at http://127.0.0.1:3001*

### Production Build
```bash
npx shadow-cljs release app
clojure -M:prod
```

## DEPLOYMENT
- **GitHub Repository**: [dennisgathu8/ZAZA](https://github.com/dennisgathu8/ZAZA)
- **Live Production URL**: [https://dagga-bay-dispensary.fly.dev](https://dagga-bay-dispensary.fly.dev)
- **Primary Region**: `jnb` (Johannesburg, South Africa)

## LEGAL & RESPONSIBLE USE
This software is intended for use within South African legal boundaries regarding personal cannabis use. All users must be 18+. Dagga Bay is a simulation of a delivery-orchestration platform and does not facilitate peer-to-peer payments on-site. Consume responsibly. Do not operate machinery under the influence.
