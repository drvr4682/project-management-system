# ─────────────────────────────────────────────
# Stage 1 — Build
# ─────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /build

# 1. Install the common library into the local Maven repo
COPY common/pom.xml             common/pom.xml
COPY common/src                 common/src
RUN mvn -f common/pom.xml install -DskipTests -q

# 2. Resolve authservice dependencies (layer-cached separately)
COPY authservice/pom.xml        authservice/pom.xml
RUN mvn -f authservice/pom.xml dependency:go-offline -q

# 3. Build authservice
COPY authservice/src            authservice/src
RUN mvn -f authservice/pom.xml package -DskipTests -q

# ─────────────────────────────────────────────
# Stage 2 — Runtime
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Non-root user for security
RUN addgroup -S pms && adduser -S pms -G pms
USER pms

COPY --from=build /build/authservice/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]