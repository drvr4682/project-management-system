# Frontend Client (`frontend`)

A React Single Page Application (SPA) client for the Project Management System. It communicates with the backend services via the API Gateway to render project dashboards, team collaborator settings, and Kanban task boards.

---

## 🛠️ Technology Stack

- **Framework**: React 19 (TypeScript)
- **State Management**: Redux Toolkit (slice states and memoized selectors)
- **Routing**: React Router DOM 7
- **CSS Framework**: TailwindCSS 4
- **HTTP Client**: Axios (with interceptors and request cancellation support)
- **Build Engine**: Vite 8

---

## 🏗️ Architecture & Features

### 1. Application Shell
- **Navigation Layout**: A collapsible sidebar panel mapping application routes (`/` Dashboard, `/projects` Directory, `/tasks` Global Tasks, `/profile` Settings).
- **Dynamic Breadcrumbs**: Path indicators parsing URL segments dynamically to render functional backward navigation.
- **Health Check Pinger**: Pings the Gateway `/health` endpoint every 15 seconds to update connection status indicators in the user header.

### 2. State Management (Redux Slices)
- **Decoupled Slices**: Stores project data (`projectSlice.ts`) and task data (`taskSlice.ts`) separately.
- **Memoized Selectors**: Uses `createSelector` to compute task groups (status columns: `TODO`, `IN_PROGRESS`, `DONE`, `BLOCKED`) dynamically on task queries.
- **Optimistic Task Updates**: Kanban task board transitions immediately update local Redux state and perform rollbacks if the PUT request returns an error.
- **Route Clearing**: Dispatches state resets during workspace transitions to prevent stale data overlap.

### 3. Autocomplete User Search Picker
- **Debounced Input**: Delays search queries to the backend by 300ms to reduce database lookups.
- **Request Cancellation**: Uses an `AbortController` inside Axios to abort stale user search queries when typing.
- **Query Cache**: Caches search results locally in component state (`Record<string, UserSearchResponse[]>`) to prevent duplicate HTTP calls.
- **Member Filters**: Filters search results to exclude users already enrolled in the active project.

---

## 💻 Environment Variables

Create a `.env` file in the `frontend` directory:

- `VITE_API_BASE_URL`: Base URL pointing to the API Gateway (Default: `http://localhost:8080`).

---

## 🐳 Docker Image

To pull the compiled image directly from DockerHub:
```bash
docker pull drvr4682/pms-frontend:latest
```

---

## 🚀 Development & Build Instructions

### Prerequisites
- Node.js 22

### Dev Server:
```bash
# Install dependencies
npm ci

# Start local dev server (default port: 5173)
npm run dev
```

### Production Build:
```bash
# Lint code and check for errors
npm run lint

# Compile and optimize code to frontend/dist/
npm run build
```
