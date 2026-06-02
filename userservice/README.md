# User Service (`userservice`)

The User Service handles user profile details (such as names, bio, designation, and timezone), social link configurations, and provides autocomplete query support to search for users during team collaboration invites.

---

## 🛠️ Technology Stack

- **Framework**: Spring Boot 3.4.5, Spring Security, Java 21
- **Database**: PostgreSQL (relational persistence)
- **Shared Common Library**: Token validation filter & request correlation tracing

---

## 🚀 Port & Service Endpoints

The service runs internally on **Port `8084`** (accessible through API Gateway `/api/v1/users/**` and `/api/v1/social-links/**`).

### Profile Endpoints (`/api/v1/users`)

| Method | Endpoint | Request Body / Query Params | Purpose | Access Control |
| :---: | :--- | :--- | :--- | :---: |
| `GET` | `/me` | None | Retrieve authenticated user profile | **Authenticated** |
| `POST` | `/me` | `{ firstName, surname }` | Initialize name details on initial signup | **Authenticated** |
| `PUT` | `/me` | `{ firstName, surname, designation, bio, timezone, statusMessage }` | Update user profile data | **Authenticated** |
| `GET` | `/search` | `?q=searchQuery` | Autocomplete user lookup by name or email | **Authenticated** |

### Social Link Endpoints (`/api/v1/social-links`)

| Method | Endpoint | Request Body | Purpose | Access Control |
| :---: | :--- | :--- | :--- | :---: |
| `POST` | `/` | `{ platform, url }` | Connect a social handle (GitHub, LinkedIn, etc.) | **Authenticated** |
| `DELETE`| `/{id}` | None | Delete a social link by UUID | **Authenticated** |

---

## 💾 Database & Persistence

- **PostgreSQL**: Stores detailed user profile mappings and platform URLs linked to the user account's logical UUID.

---

## 💻 Environment Variables

Configure these settings inside the runtime environment or in `application.properties`:

- `USER_SERVICE_PORT`: Port for the service to bind to (Default: `8084`).
- `DB_URL`: JDBC url for connection (e.g. `jdbc:postgresql://localhost:5432/pms_user`).
- `DB_USERNAME`: Database username.
- `DB_PASSWORD`: Database password.
- `JWT_SECRET`: Base64 encoded signing key (for JWT payload verification).

---

## 🐳 Docker Image

To pull the compiled image directly from DockerHub:
```bash
docker pull drvr4682/pms-userservice:latest
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
