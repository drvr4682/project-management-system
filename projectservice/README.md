# Project Service (`projectservice`)

The Project Service manages collaborative project workspaces, metadata configurations (statuses: `ACTIVE`, `ON_HOLD`, `COMPLETED`), and memberships (adding/removing collaborators).

---

## 🛠️ Technology Stack

- **Framework**: Spring Boot 3.4.5, Spring Security, Java 21
- **Database**: PostgreSQL (relational persistence)
- **Shared Common Library**: Token validation filter & request correlation tracing
- **Internal Communication**: Spring Cloud OpenFeign

---

## 🚀 Port & Service Endpoints

The service runs internally on **Port `8082`** (accessible through API Gateway `/api/v1/projects/**`).

### Endpoint Mappings (`/api/v1/projects`)

| Method | Endpoint | Request Body / Query Params | Purpose | Access Control |
| :---: | :--- | :--- | :--- | :---: |
| `GET` | `/` | `?page=0&size=10&status=ACTIVE` | Retrieve paginated list of workspaces | **Authenticated** |
| `POST` | `/` | `{ name, description, status }` | Create project workspace (requesting user assigned as OWNER) | **Authenticated** |
| `GET` | `/{id}` | None | Retrieve project workspace details | **Authenticated** |
| `PUT` | `/{id}` | `{ name, description, status }` | Update project metadata (requires Owner/Admin check) | **Authenticated** |
| `DELETE`| `/{id}` | None | Delete project workspace (requires Owner/Admin check) | **Authenticated** |
| `GET` | `/{id}/members` | None | List collaborators enrolled in the project | **Authenticated** |
| `POST` | `/{id}/members` | `{ userId, role }` | Enroll a collaborator in the project using user UUID | **Authenticated** |
| `DELETE`| `/{id}/members/{userId}`| None | Remove a collaborator from the project | **Authenticated** |

---

## 💾 Database & Persistence

- **PostgreSQL**: Stores project definitions and project membership links. 

---

## 👥 Internal Service Communication

- **Feign / OpenFeign client**: The Project Service uses declarative REST clients via OpenFeign to communicate with the `userservice`.
  - It resolves collaborator profile details (names, designation) using the collaborator's UUID during member list queries.

---

## 💻 Environment Variables

Configure these settings inside the runtime environment or in `application.properties`:

- `PROJECT_SERVICE_PORT`: Port for the service to bind to (Default: `8082`).
- `DB_URL`: JDBC url for connection (e.g. `jdbc:postgresql://localhost:5432/pms_project`).
- `DB_USERNAME`: Database username.
- `DB_PASSWORD`: Database password.
- `JWT_SECRET`: Base64 encoded signing key.

---

## 🐳 Docker Image

To pull the compiled image directly from DockerHub:
```bash
docker pull drvr4682/pms-projectservice:latest
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
