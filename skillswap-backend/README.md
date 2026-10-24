# SkillSwap Backend

SkillSwap's backend is a Spring Boot REST API using Spring Data JPA, MySQL,
Spring Security, and JWT authentication.

## Run locally

Use Java 17 and the Maven wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

The API defaults to `http://localhost:8080/api`. Database and JWT settings
belong in local configuration and must not be committed.

## Key API areas

- `/api/users/**` — registration, login, profile, and password recovery.
- `/api/skills/**` — skill management, skill discovery, and reciprocal matches.
- `/api/skill-requests/**` — skill exchange requests and private chat.
- `/api/class-sessions/**` — class sessions and session reviews.
- `/api/notifications/**` — recipient-scoped notifications and read status.
- `/api/admin/**` — administrator-only member management and messaging.

The learner course-rating check is available at
`GET /api/class-sessions/reviews/required`. Once an accepted exchange reaches
10 days and its latest confirmed session has ended, the learner must submit a
rating through the existing session-review endpoint. The review is stored in
`session_reviews` against that session.

Admin warning and instruction notifications use the same authenticated
notification APIs as other user notifications. At startup, an idempotent
schema check converts a legacy MySQL ENUM notification type column to
`VARCHAR(50)` when required.

## Test

```powershell
.\mvnw.cmd test
```
