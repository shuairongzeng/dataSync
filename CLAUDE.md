# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

DBSync is a database synchronization tool with a Java Spring Boot backend and Vue.js frontend. It supports data synchronization between multiple database types including Oracle, PostgreSQL, MySQL, SQLServer, Dameng, and Vastbase.

## Architecture

### Backend (Java/Spring Boot)
- **Main Application**: `src/main/java/com/dbsync/dbsync/DbsyncApplication.java`
- **Database Synchronization**: Core service in `service/DatabaseSyncService.java`
- **Type Mapping**: Extensible system in `typemapping/` package with mappers for different database combinations
- **Progress Tracking**: `progress/` package for monitoring sync operations
- **Security**: JWT-based authentication with Spring Security
- **Configuration**: `src/main/resources/application.properties`

### Frontend (Vue.js)
- **Framework**: Vue 3 + TypeScript + Element Plus
- **Location**: `frontend/` directory
- **Build Tool**: Vite
- **Package Manager**: pnpm

## Common Development Commands

### Backend
```bash
# Run Spring Boot application
mvn spring-boot:run

# Build backend
mvn clean package

# Copy dependencies for standalone execution
mvn dependency:copy-dependencies -DoutputDirectory=target/lib

# Run tests
mvn test

# Use batch file (Windows)
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

# Type checking
pnpm typecheck

# Linting
pnpm lint

# Run all quality checks
pnpm lint && pnpm typecheck
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
- **DatabaseSyncService**: Main synchronization logic
- **TypeMappingRegistry**: Handles data type conversions between databases
- **ProgressManager**: Tracks sync progress and status
- **UserDetailsService**: Authentication and user management

### Frontend Features
- **Database Management**: Connection configuration and testing
- **Sync Operations**: Create and monitor synchronization tasks
- **Custom Queries**: SQL editor with result export
- **User Authentication**: JWT-based login system

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
- Frontend requires Node.js >= 18.0.0 and pnpm >= 8.0.0
- Database drivers are included in pom.xml for all supported databases
- JWT secret is configured in application.properties
- SQLite is used for user authentication database
- Progress tracking is currently log-based but could be extended to API endpoints