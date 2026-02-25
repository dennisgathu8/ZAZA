# --- 1. BUILDER STAGE ---
FROM clojure:temurin-21-tools-deps-alpine AS builder

WORKDIR /build

# Install Node.js for shadow-cljs
RUN apk add --no-cache nodejs npm

# Copy dependency files first for caching
COPY package.json package-lock.json shadow-cljs.edn deps.edn ./
RUN npm install
RUN clojure -P

# Copy source and build
COPY . .
RUN npx shadow-cljs release app
RUN clojure -T:build uber

# --- 2. RUNTIME STAGE ---
FROM eclipse-temurin:21-jre-alpine

# Security: Create non-root user
RUN addgroup -S daggabay && adduser -S daggabay -G daggabay

WORKDIR /app

# Copy uberjar from builder
COPY --from=builder /build/target/dagga-bay-standalone.jar /app/app.jar

# Production environment
ENV PORT=8080
ENV CLOJURE_HTTP_PORT=8080

# Security: Set ownership and switch to non-root
RUN chown -R daggabay:daggabay /app
USER daggabay

# Security: Basic JVM hardening
ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75", \
    "-XX:InitialRAMPercentage=50", \
    "-XshowSettings:vm", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "/app/app.jar"]

EXPOSE 8080
