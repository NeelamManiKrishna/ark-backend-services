# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Project ARK (Academic Record Keeper)** — backend services for a multi-tenant academic records management platform. Built with Spring Boot 4.0.3, Java 17, MongoDB, and Lombok.

Full requirements are in `../ark_brd_doc.txt` (BRD with requirement IDs: BR-ORG-xxx, BR-ROLE-xxx, BR-AUTH-xxx, BR-REC-xxx, etc.).

## Build & Run Commands

```bash
# Build (skip tests)
./mvnw clean package -DskipTests

# Build with tests
./mvnw clean package

# Run the application
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=ArkBackendServicesApplicationTests

# Run a single test method
./mvnw test -Dtest=ArkBackendServicesApplicationTests#contextLoads
```

Uses the Maven wrapper (`mvnw`) — no global Maven installation required.

## Tech Stack

- **Framework:** Spring Boot 4.0.3 (spring-boot-starter-webmvc)
- **Language:** Java 17
- **Database:** MongoDB (spring-boot-starter-mongodb)
- **Monitoring:** Spring Boot Actuator
- **Boilerplate reduction:** Lombok (annotation processor configured in pom.xml)
- **Testing:** JUnit 5 via Spring Boot test starters
- **Build:** Maven with Maven Wrapper

## Project Structure

```
src/main/java/com/app/ark_backend_services/   — application code
src/main/resources/application.yaml            — Spring configuration
src/test/java/com/app/ark_backend_services/    — tests
```

Base package: `com.app.ark_backend_services`
Entry point: `ArkBackendServicesApplication.java`

**Note:** The package uses underscores (`ark_backend_services`) because the artifact name `ark-backend-services` contains hyphens which are invalid in Java package names.

## Architecture Requirements

### Multi-Tenancy
- Each organization gets isolated data — no cross-org data access
- Must scale to 100+ orgs, each with up to 100K student records

### Role-Based Access Control (4 tiers)
1. **Super Admin** — platform-wide; manages orgs and Org Admins
2. **Org Admin** — full admin within their org; manages Admins/Users and org settings
3. **Admin** — manages academic records and Users within their org
4. **User** — read-only or limited edit, scoped to assigned department

### Key Domains
- Organization Management, User & Role Management, Authentication & Authorization
- Academic Record Management (students, faculty, courses, enrollment, grades, attendance)
- Search & Reporting (PDF/Excel/CSV export)

### Non-Functional
- 1,000 concurrent users, <3s page loads, <2s search over 10K records
- TLS 1.3+, encryption at rest, FERPA/GDPR compliance
- 99.5% uptime, daily backups, 24-hour RPO
