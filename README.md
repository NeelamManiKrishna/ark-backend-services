# ark-backend-services

Backend for Project ARK (Academic Record Keeper): Spring Boot 4, Java 17, MongoDB, JWT auth.

## Run locally
1. Start MongoDB (Docker, user `admin` / password `secret`):
   `docker run -d --name mongodb -p 27017:27017 -e MONGO_INITDB_ROOT_USERNAME=admin -e MONGO_INITDB_ROOT_PASSWORD=secret mongo`
2. Seed test data (password for every seeded user is `Password@123`):
   ```
   docker cp src/main/resources/test-data.js mongodb:/tmp/test-data.js
   docker exec mongodb mongosh -u admin -p secret --authenticationDatabase admin ark_web /tmp/test-data.js
   ```
3. Start the app with the dev profile:
   `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
4. Swagger UI: http://localhost:8080/swagger-ui/index.html

## Configuration (environment variables)
| Variable | Purpose |
|---|---|
| `MONGO_URI` | MongoDB connection string (required outside dev) |
| `JWT_SECRET` | HS256 signing key, 32+ chars (required outside dev) |
| `FIELD_ENCRYPTION_KEY` | Base64 32-byte AES key for government ID numbers (required outside dev) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins |
| `JWT_ACCESS_EXPIRATION_MS`, `JWT_REFRESH_EXPIRATION_MS`, `JWT_DEVICE_REFRESH_EXPIRATION_MS` | Token lifetimes |
| `MAX_FAILED_ATTEMPTS`, `LOCKOUT_MINUTES` | Login lockout |

## Tests
`mvn test` starts a throwaway MongoDB via Testcontainers, so Docker must be running.

See `docs/` for architecture, API guides and the development log.

## Data migrations
Migrations never run on their own. Rehearse on a copy of the database first:
```
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--migrate.dry-run
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--migrate
```
Each migration runs once and is recorded in the `migrations` collection. The process exits when it finishes.

