# Authentication Service (`authservice`)

The authentication service handles user registration, registration confirmation (email verification with cooldown limits), authentication, JWT session issuance, token rotation, and password management.

---

## 🛠️ Technology Stack

- **Framework**: Spring Boot 3.4.5, Spring Security, Java 21
- **Database**: PostgreSQL (relational persistence)
- **Caching & Rate Limiting**: Redis
- **Mailing**: Java Mail Sender

---

## 🚀 Port & Service Endpoints

The service runs internally on **Port `8081`** (accessible through API Gateway `/api/v1/auth/**`).

### Endpoint Mappings (`/api/v1/auth`)

| Method | Endpoint | Request Body / Query Params | Purpose | Access Control |
| :---: | :--- | :--- | :--- | :---: |
| `POST` | `/register` | `{ userName, email, password, role }` | Register inactive account & send verification email | Public |
| `GET` | `/verify` | `?token=UUID` | Validate token to enable account | Public |
| `POST` | `/resend-verification`| `{ email }` | Resend verification link (60s cooldown limit) | Public |
| `POST` | `/login` | `{ email, password }` | Authenticate credentials; return access & refresh tokens | Public |
| `POST` | `/refresh` | `{ refreshToken }` | Generate new access token using refresh token UUID | Public |
| `POST` | `/forgot-password`| `{ email }` | Generate forgot password link | Public |
| `POST` | `/reset-password` | `{ token, newPassword }` | Set new password using token | Public |
| `POST` | `/change-password`| `{ currentPassword, newPassword }` | Update user password | **Authenticated** |

---

## 🔒 Security Implementation

- **Token Invalidation**: During user logout, the gateway or identity provider logs out the user by writing the token UUID directly to the Redis blocklist cache.
- **Account Verification**: New accounts are initialized with `enabled = false`. A link containing a random UUID token is emailed, and the account status is updated upon validation.
- **Complexity Requirements**: Passwords must satisfy regex validation (length >= 8, containing at least one uppercase, lowercase, number, and special character).

---

## 💾 Database & Persistence

- **PostgreSQL**: Stores persistent accounts and registration data.
- **Redis**: Handles token blocklist values, rate limits, and verification link request cooldown timers.

---

## 💻 Environment Variables

Configure these settings inside the runtime environment or in `application.properties`:

- `AUTH_SERVICE_PORT`: Port for the service to bind to (Default: `8081`).
- `DB_URL`: JDBC url for connection (e.g. `jdbc:postgresql://localhost:5432/pms_auth`).
- `DB_USERNAME`: Database username.
- `DB_PASSWORD`: Database password.
- `REDIS_HOST`: Hostname of Redis instance (Default: `localhost`).
- `REDIS_PORT`: Port of Redis instance (Default: `6379`).
- `MAIL_HOST`: SMTP server host.
- `MAIL_PORT`: SMTP server port.
- `MAIL_USERNAME`: SMTP server account username.
- `MAIL_PASSWORD`: SMTP server account password.
- `JWT_SECRET`: Base64 encoded signing key.

---

## 🐳 Docker Image

To pull the compiled image directly from DockerHub:
```bash
docker pull drvr4682/pms-authservice:latest
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
