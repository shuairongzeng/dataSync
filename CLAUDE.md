# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

DBSync is a database synchronization tool with a Java Spring Boot backend and Vue.js frontend. It supports data synchronization between multiple database types including Oracle, PostgreSQL, MySQL, SQLServer, Dameng, and Vastbase.

## Architecture

### Backend (Java/Spring Boot)
- **Main Application**: `src/main/java/com/dbsync/dbsync/DbsyncApplication.java`
- **Database Synchronization**: Core service in `service/DatabaseSyncService.java`
- **Type Mapping**: Extensible system in `typemapping/` package with 36 mappers for all database combination pairs
- **Progress Tracking**: `progress/` package with `ProgressManager`, `TaskProgress`, and `TableSyncProgress`
- **Security**: JWT-based authentication with Spring Security, SQLite for user storage
- **Controllers**: REST APIs in `controller/` package for sync tasks, connections, and custom queries
- **Configuration**: Multiple data sources - SQLite for auth, dynamic connections for sync operations
- **Caching**: Caffeine cache for metadata and connection optimization

### Frontend (Vue.js)
- **Framework**: Vue 3 + TypeScript + Element Plus + vue-pure-admin template
- **Location**: `frontend/` directory
- **Build Tool**: Vite
- **Package Manager**: pnpm (version >=9 required)
- **Key Features**: SQL Editor with CodeMirror, database connection management, sync task monitoring
- **Styling**: TailwindCSS + SCSS

## Common Development Commands

### Backend
```bash
# Run Spring Boot application
mvn spring-boot:run

# Build backend
mvn clean package

# Copy dependencies for standalone execution (required for start-backend.bat)
mvn dependency:copy-dependencies -DoutputDirectory=target/lib

# Run tests
mvn test

# Run specific test class
mvn test -Dtest=TypeMappingRegistryTest

# Use batch file (Windows) - requires dependencies in target/lib
start-backend.bat
```

### Frontend
```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
pnpm install

# Start development server
pnpm dev

# Build for production
pnpm build

# Build for staging
pnpm build:staging

# Type checking
pnpm typecheck

# Run ESLint
pnpm lint:eslint

# Run Prettier
pnpm lint:prettier

# Run Stylelint
pnpm lint:stylelint

# Run all linting and quality checks
pnpm lint && pnpm typecheck

# Clean cache and reinstall dependencies
pnpm clean:cache
```

## Configuration

### Database Connections
Configure database connections in `src/main/resources/application.properties`:
```properties
db.connections.<connectionId>.url=jdbc:...
db.connections.<connectionId>.username=...
db.connections.<connectionId>.password=...
db.connections.<connectionId>.driverClassName=...
db.connections.<connectionId>.dbType=oracle|postgresql|mysql|sqlserver|dameng|vastbase
```

### Synchronization Tasks
Define sync tasks in application.properties:
```properties
sync.tasks[<index>].name=TaskName
sync.tasks[<index>].sourceConnectionId=sourceId
sync.tasks[<index>].targetConnectionId=targetId
sync.tasks[<index>].sourceSchemaName=schema
sync.tasks[<index>].targetSchemaName=schema
sync.tasks[<index>].tables=table1,table2
sync.tasks[<index>].truncateBeforeSync=true|false
```

## Key Components

### Backend Services
- **DatabaseSyncService**: Main synchronization logic with batch processing and error handling
- **TypeMappingRegistry**: Centralized registry with 36 type mappers for all database pair combinations
- **ProgressManager**: Tracks sync progress with task and table-level status reporting
- **UserDetailsServiceImpl**: JWT-based authentication with password encryption
- **SyncTaskService**: Manages sync task CRUD operations and execution
- **DbConnectionService**: Database connection management with testing capabilities
- **QueryHistoryService**: Tracks custom query execution history

### Frontend Features
- **Database Connection Management**: Connection CRUD with real-time testing (`DbConnectionController`)
- **Sync Task Operations**: Task creation, monitoring, and execution (`SyncTaskController`)
- **SQL Editor**: CodeMirror-based editor with syntax highlighting and auto-completion
- **Query History**: Track and replay previous queries with export functionality
- **Dashboard**: Real-time monitoring of sync operations and system status
- **User Management**: Registration, login, and profile management

## Database Support

The system supports synchronization between these database types:
- Oracle ↔ PostgreSQL, MySQL, SQLServer, Dameng, Vastbase
- PostgreSQL ↔ Oracle, MySQL, SQLServer, Dameng, Vastbase  
- MySQL ↔ Oracle, PostgreSQL, SQLServer, Dameng, Vastbase
- SQLServer ↔ Oracle, PostgreSQL, MySQL, Dameng, Vastbase
- Dameng ↔ Oracle, PostgreSQL, MySQL, SQLServer, Vastbase
- Vastbase ↔ Oracle, PostgreSQL, MySQL, SQLServer, Dameng

## Testing

### Backend Tests
- Unit tests for type mapping: `src/test/java/com/dbsync/dbsync/typemapping/`
- Integration tests: `src/test/java/com/dbsync/dbsync/controller/`, `src/test/java/com/dbsync/dbsync/service/`

### Frontend Tests
- API tests: `frontend/src/tests/api.test.ts`
- Component tests are configured but may need additional setup

## Development Notes

- The backend uses Spring Boot 2.6.13 with Java 8
- Frontend requires Node.js >= 18.0.0 and pnpm >= 9.0.0
- Database drivers are included in pom.xml for all supported databases (Oracle, PostgreSQL, MySQL, SQLServer, Dameng, Vastbase)
- JWT secret is configured in application.properties (`jwt.secret`)
- SQLite is used for user authentication database (`auth.db` in project root)
- Progress tracking uses `ProgressManager` service with detailed logging
- The start-backend.bat script requires dependencies copied to target/lib directory
- Frontend is based on vue-pure-admin template with extensive customization
- Caching implemented with Caffeine for database metadata optimization
- MyBatis-Plus used for SQLite operations, native JDBC for sync operations

## Project Structure Patterns

### Backend Package Organization
```
com.dbsync.dbsync/
├── controller/          # REST API endpoints
├── service/            # Business logic layer
├── mapper/             # MyBatis mappers for SQLite operations
├── model/              # Data models and DTOs
├── entity/             # Database entities
├── config/             # Spring configuration classes
├── security/           # JWT and authentication components
├── typemapping/        # Database type conversion system
├── progress/           # Sync progress tracking
├── utils/              # Utility classes
└── exception/          # Global exception handling
```

### Key Design Patterns
- **Type Mapping System**: Extensible registry pattern with dedicated mappers for each database pair
- **Multi-DataSource**: Separate configurations for auth (SQLite) and sync operations (dynamic)
- **Progress Tracking**: Observer pattern for real-time sync monitoring
- **Caching Strategy**: Metadata caching with Caffeine to optimize repeated database queries
- **Security**: JWT with stateless authentication, BCrypt password hashing

### Frontend Architecture
- **Component-based**: Vue 3 Composition API with TypeScript
- **State Management**: Pinia for global state
- **Routing**: Vue Router with role-based access control
- **API Layer**: Axios with interceptors for authentication
- **UI Framework**: Element Plus with custom styling via TailwindCSS