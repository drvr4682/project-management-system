# API Gateway Service (`apigateway`)

The API Gateway is the public entry point for all traffic. It manages routing proxy configurations, JWT authentication verification, and queries Redis to block requests using blacklisted access tokens.

---

## 🛠️ Technology Stack

- **Framework**: Spring Boot 3.4.5, Spring Cloud Gateway MVC, Spring Security, Java 21
- **Caching Database**: Redis (token blocklist validation)
- **Shared Common Library**: Centralized filters and authentication verification

---

## 🚀 Port & Route Configuration

The Gateway binds to **Port `8080`** and proxies endpoints based on path prefixes:

| Request Prefix Route | Target Downstream Service | Downstream Port | Purpose |
| :--- | :--- | :---: | :--- |
| `/api/v1/auth/**` | `authservice` | `8081` | Registration, login, verification |
| `/api/v1/projects/**` | `projectservice` | `8082` | Workspaces and memberships |
| `/api/v1/tasks/**` | `taskservice` | `8083` | Project tasks and board transitions |
| `/api/v1/users/**` | `userservice` | `8084` | User directory and profiles |
| `/api/v1/social-links/**` | `userservice` | `8084` | Connected developer profile handles |

---

## 🔒 Security Gatekeeping

### 1. Token Blacklist Check
The gateway executes `JwtAuthenticationFilter` on incoming requests. It extracts the authorization header and queries the Redis cache to check if the session is blacklisted. If found, the connection is closed with a `401 Unauthorized` status.

### 2. Request Correlation Tracing
The gateway injects or propagates an `X-Correlation-ID` header. This correlation ID maps to backend SLF4J MDC, linking logs across services for debugging.

---

## 💻 Environment Variables

Configure these settings inside the runtime environment or in `application.properties`:

- `API_GATEWAY_PORT`: Port for the gateway to bind to (Default: `8080`).
- `REDIS_HOST`: Hostname of Redis instance (Default: `localhost`).
- `REDIS_PORT`: Port of Redis instance (Default: `6379`).
- `JWT_SECRET`: Base64 encoded signing key.

---

## 🐳 Docker Image

To pull the compiled image directly from DockerHub:
```bash
docker pull drvr4682/pms-apigateway:latest
```

---

## 🚀 Running the Service

### Compile and Start Locally:
1. Ensure the shared `common` library has been built and installed locally:
   ```bash
   mvn -f ../common/pom.xml clean install
   ```
2. Start the service:
   ```bash
   mvn spring-boot:run
   ```
