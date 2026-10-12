# PlanMyTrip Platform

A scalable, cloud-native travel planning and booking platform built using a microservices architecture.

## Overview

PlanMyTrip is designed to provide end-to-end travel itinerary planning and reservation capabilities across multiple modules (Hotel, Flight, Transport, and Activity). The system uses an API-first approach with centralized API Gateway routing, microservices-based business logic, and standardized data contracts.

## Tech Stack and Infrastructure

### Backend
- Java 17
- Spring Boot
- Spring Cloud (API Gateway, Service Discovery)
- Spring Data JPA / Hibernate
- OpenAPI 3.0 / Swagger (API specifications)

### Frontend
- React
- HTML5 / CSS3 / JavaScript

### Database and Cloud Infrastructure
- Database: MySQL (Cloud-hosted via Aiven)
- Backend Hosting: Amazon Web Services (AWS EC2, administered via MobaXterm)
- Frontend Hosting: Render (automated build via render.yaml)

### DevOps and CI/CD
- Containerization: Docker, Docker Compose
- CI/CD: GitHub Actions (automated compilation, testing, and delivery)

## Microservices Status

| Service | Description | Status |
|---|---|---|
| Gateway Service | Central API Gateway routing and request filtering | Completed |
| User Service | User authentication, registration, and profile management | Completed |
| Trip Service | Trip lifecycle, destination tracking, and itinerary storage | Completed |
| AI Itinerary Service | AI itinerary generation and personalized recommendations | Completed |
| Booking Service | Universal booking lifecycle, provider integration, confirmations, and cancellations | In Progress (Hotel module completed) |

## Repository Structure

```text
planmytrip-platform/
│
├── .github/
│   └── workflows/                # GitHub Actions CI/CD workflows
├── docs/
│   ├── architecture/             # Architecture specifications
│   ├── ui/                       # UI references and screenshots
│   └── booking-service-openapi.yaml # OpenAPI 3.0 contract definition
├── services/
│   ├── gateway-service/          # API Gateway
│   ├── user-service/             # User management service
│   ├── trip-service/             # Trip management service
│   ├── ai-itinerary-service/     # AI planning service
│   └── booking-service/          # Universal booking service
│       ├── src/
│       ├── pom.xml
│       └── src/main/resources/openapi.yaml
├── docker/                       # Docker deployment configs
├── docker-compose.yml            # Multi-service local composition
├── render.yaml                   # Frontend deployment specification
├── start.sh                      # Automation script
├── pom.xml                       # Root parent POM
└── README.md                     # Project documentation


Branching Strategy
The repository follows a standard Git Flow workflow:

main: Production-ready releases.
develop: Integration branch for testing and QA verification.
feature/<service-name>: Core service development branch.
feature/<service-name>-<module-name>: Scoped module development branch (e.g., feature/booking-service-hotel-module).

Local Development Setup
Prerequisites
JDK 17
Apache Maven 3.8+
Docker and Docker Compose
MySQL instance (local or remote Aiven connection)
Running a Service Locally
Navigate to the target service directory:
cd services/booking-service
mvn clean compile
mvn spring-boot:run


Running Tests
Execute unit and integration tests across services:
mvn test

API Documentation
API definitions are maintained in OpenAPI 3.0 format. Import docs/booking-service-openapi.yaml into Postman or any OpenAPI viewer to inspect endpoints, schemas, and test responses.
