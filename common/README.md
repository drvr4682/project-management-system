# Shared Common Library (`common`)

The Shared Common Library provides cross-cutting components used by all microservices in the Project Management System. It standardizes authentication filters, correlation tracking, and exception handling across services.

---

## 🛠️ Technology Stack

- **Framework**: Spring Boot 3.4.5, Spring Security, Java 21
- **JSON Web Tokens (JWT)**: `jjwt-api` 0.11.5
- **Utilities**: Lombok 1.18.32, Jackson

---

## 📦 Core Component Configurations

### 1. JWT Authentication
- **`JwtAuthenticationFilter`**: Intercepts HTTP requests, reads the authorization header (`Authorization: Bearer <token>`), and sets the security context.
- **`JwtTokenProvider`**: Manages JWT parsing, signature checks, and validation logic.
- **Optional Redis Verification**: Handles cases where a service does not bind to Redis (e.g. `projectservice`). The filter checks if a `RedisTemplate` is present; if null, it skips blocklist validation and processes standard JWT claims.

### 2. Request Correlation Tracing
- **`CorrelationIdFilter`**: Extracts or generates an `X-Correlation-ID` header.
- **Log Propagation**: Binds the correlation ID to the SLF4J MDC, outputting the request ID in all logs generated during execution.

### 3. Shared Exception Mapping
- **`GlobalExceptionHandler`**: Intercepts standard application exceptions (e.g. `ResourceNotFoundException`, validation failures) and maps them to uniform error structures:
  ```json
  {
    "status": 404,
    "message": "Project not found",
    "timestamp": "2026-06-02T17:00:00Z"
  }
  ```

---

## 💾 Deployment & Run Details
- This module is a dependency library (jar packaging). It does not bind to any network port and has no standalone Docker image.
- Build and install it locally so microservice dependencies compile successfully:
  ```bash
  mvn clean install
  ```
