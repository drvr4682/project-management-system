# ─────────────────────────────────────────────
# Stage 1 — Build
# ─────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /build

COPY common/pom.xml             common/pom.xml
COPY common/src                 common/src
RUN mvn -f common/pom.xml install -DskipTests -q

COPY apigateway/pom.xml         apigateway/pom.xml
RUN mvn -f apigateway/pom.xml dependency:go-offline -q

COPY apigateway/src             apigateway/src
RUN mvn -f apigateway/pom.xml package -DskipTests -q

# ─────────────────────────────────────────────
# Stage 2 — Runtime
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S pms && adduser -S pms -G pms
USER pms

COPY --from=build /build/apigateway/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]