# Task Service (`taskservice`)

The Task Service manages tasks inside project workspaces, including parameter tracking (priorities: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), status transition mapping (columns: `TODO`, `IN_PROGRESS`, `DONE`, `BLOCKED`), due dates, and assignee relationships.

---

## 🛠️ Technology Stack

- **Framework**: Spring Boot 3.4.5, Spring Security, Java 21
- **Database**: PostgreSQL (relational persistence)
- **Shared Common Library**: Token validation filter & request correlation tracing
- **Internal Communication**: Spring Cloud OpenFeign

---

## 🚀 Port & Service Endpoints

The service runs internally on **Port `8083`** (accessible through API Gateway `/api/v1/tasks/**`).

### Endpoint Mappings (`/api/v1/tasks`)

| Method | Endpoint | Request Body / Query Params | Purpose | Access Control |
| :---: | :--- | :--- | :--- | :---: |
| `GET` | `/` | `?projectId=1&page=0&size=20` | Retrieve paginated tasks for a project with status filters | **Authenticated** |
| `POST` | `/` | `{ title, description, status, priority, dueDate, projectId }` | Create task within a project | **Authenticated** |
| `GET` | `/{id}` | None | Retrieve task details | **Authenticated** |
| `PUT` | `/{id}` | `{ title, description, status, priority, dueDate, projectId }` | Update task details or transition column status | **Authenticated** |
| `DELETE`| `/{id}` | None | Delete task | **Authenticated** |
| `PUT` | `/{id}/assign`| `{ assigneeId }` | Assign task to a collaborator using user UUID | **Authenticated** |
| `DELETE`| `/{id}/assign`| None | Remove assignee from task | **Authenticated** |

---

## 💾 Database & Persistence

- **PostgreSQL**: Stores detailed task entries (titles, descriptions, columns, assignees) bound to specific project and user IDs.

---

## 🔒 Internal Service Communication

- **Feign / OpenFeign Client Validation**: Before assigning a task, the Task Service uses an OpenFeign declarative client to query the `projectservice` internally. It checks if the `assigneeId` is registered as a member of the project workspace. If they are not in the membership collection, the assignment is blocked.

---

## 💻 Environment Variables

Configure these settings inside the runtime environment or in `application.properties`:

- `TASK_SERVICE_PORT`: Port for the service to bind to (Default: `8083`).
- `DB_URL`: JDBC url for connection (e.g. `jdbc:postgresql://localhost:5432/pms_task`).
- `DB_USERNAME`: Database username.
- `DB_PASSWORD`: Database password.
- `JWT_SECRET`: Base64 encoded signing key.

---

## 🐳 Docker Image

To pull the compiled image directly from DockerHub:
```bash
docker pull drvr4682/pms-taskservice:latest
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
