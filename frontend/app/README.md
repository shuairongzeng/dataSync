# Frontend Application (Vue 3 + Vite + Element Plus)

This directory contains the frontend application for the project, built using Vue 3, Vite, and the Element Plus UI library.

## Project Structure

- **`public/`**: Static assets that are copied directly to the root of the `dist` directory on build.
- **`src/`**: Main application source code.
  - **`assets/`**: Static assets like images, fonts, and global styles.
  - **`components/`**: Reusable Vue components (if any were created, otherwise this might be empty or not present).
  - **`router/`**: Vue Router configuration (`router.js` or `router/index.js`).
  - **`services/`**: Modules for interacting with backend APIs (e.g., `authService.js`, `taskService.js`). Currently uses mock data.
  - **`views/`**: Vue components that represent application pages/views (e.g., `Login.vue`, `TaskConfiguration.vue`, `TaskProgress.vue`).
  - **`App.vue`**: The root Vue component.
  - **`main.js`**: The entry point of the application, where Vue is initialized, plugins are registered, and the router is attached.
- **`vite.config.js`**: Vite configuration file, including Vitest setup.
- **`package.json`**: Project metadata, dependencies, and scripts.
- **`.gitignore`**: Specifies intentionally untracked files that Git should ignore.

## Development Setup

1.  **Prerequisites:**
    *   Node.js (version specified in `pom.xml` by `frontend-maven-plugin`, e.g., v20.11.0 or higher recommended).
    *   npm (version specified in `pom.xml`, e.g., 10.4.0 or higher recommended).
    *   Ensure your Java and Maven setup is correct for the main project.

2.  **Install Dependencies:**
    Navigate to this directory (`frontend/app`) in your terminal and run:
    ```bash
    npm install
    ```
    This will download all necessary frontend dependencies defined in `package.json`.

## Running the Development Server

To run the frontend application in development mode with hot-reloading:

```bash
npm run dev
```

This will typically start a development server on `http://localhost:5173` (or the next available port, check the terminal output). You can access the application through your browser.

## Building for Production

To compile and minify the application for production:

```bash
npm run build
```

The build artifacts will be placed in the `frontend/app/dist/` directory.
When building the main Java project with Maven (e.g., `mvn clean package`), this frontend build process is automatically triggered by the `frontend-maven-plugin`, and the contents of `dist/` are copied to `target/classes/static` to be served by the Spring Boot application.

## Running Unit Tests

To run the unit tests using Vitest:

```bash
npm run test:unit
```

This will execute test files (e.g., `*.spec.js`) and report the results in the console.

## Important TODOs / Next Steps

This frontend application provides the basic structure and UI for the required features. However, several key integrations and refinements are pending:

1.  **Backend API Integration:**
    *   **`src/services/authService.js`**: Currently uses mock login logic. Update to make HTTP requests to the actual Spring Boot backend login API endpoint.
    *   **`src/services/taskService.js`**: Currently uses mock data for task configurations and progress. Update all methods (CRUD for tasks, progress fetching) to make HTTP requests to the corresponding backend API endpoints.
    *   The specific API URLs, request payloads, and response structures will be needed from the backend team/documentation.

2.  **Element Plus Component Usage:**
    *   While Element Plus is installed, the views (`Login.vue`, `TaskConfiguration.vue`, `TaskProgress.vue`) primarily use standard HTML elements for forms, tables, etc.
    *   Refactor these views to utilize Element Plus components (e.g., `el-form`, `el-input`, `el-button`, `el-table`, `el-progress`, `el-alert`) for an improved and consistent UI/UX.

3.  **Enhanced Error Handling & User Feedback:**
    *   Implement more user-friendly error messages and notifications throughout the application. Instead of just `console.error`, use UI elements (e.g., Element Plus `ElMessage` or `ElNotification`) to inform the user about API errors, validation issues, or other problems.

4.  **Comprehensive Unit Tests:**
    *   The current setup includes an example test suite for `Login.vue`. Add more unit tests for other components (`TaskConfiguration.vue`, `TaskProgress.vue`) and services to ensure adequate code coverage and reliability.

5.  **SPA Fallback Route (Backend Configuration):**
    *   Ensure the Spring Boot backend has a "catch-all" or "fallback" route configured. For Single Page Applications, any route not handled by the API or static file serving should forward to `index.html`. This allows Vue Router to manage client-side navigation correctly when users directly access a frontend route or refresh the page.
