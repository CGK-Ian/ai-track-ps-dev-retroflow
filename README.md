# RetroFlow

RetroFlow is a Spring Boot backend for StackCraft's retrospective workflow. It is intended to replace ad-hoc retrospective tooling with a structured API for managing teams, retrospectives, feedback, and action items.

## Tech stack

- Java 21
- Spring Boot 3.3.4
- Maven
- H2 in-memory database for local development

## Current project status

This repository is still in the scaffolded / implementation-in-progress stage. The core domain entities are present, but the business logic and API endpoints are not fully implemented yet. The project includes a test suite that defines the expected business rules for the final application.

## Prerequisites

- Java 21+
- Maven

## Getting started

1. Clone the repository and open it in your IDE or terminal.
2. Run the application:

   ```bash
   mvn spring-boot:run
   ```

3. The application starts on:

   ```text
   http://localhost:8080
   ```

4. The in-memory H2 database console is available at:

   ```text
   http://localhost:8080/h2-console
   ```

   Use these credentials for the console:

   - JDBC URL: `jdbc:h2:mem:retroflow`
   - Username: `sa`
   - Password: empty

## Running tests

```bash
mvn test
```

## Project structure

```text
src/
  main/
    java/
      com/stackcraft/retroflow/
        controller/
        entity/
        exception/
        repository/
        RetroflowApplication.java
    resources/
      application.properties
  test/
    java/
      com/stackcraft/retroflow/
        RetroflowBusinessRulesTest.java
```

## Business rules captured in tests

The test suite establishes the required behavior for the application:

- A retrospective cannot accept new feedback after it has been closed.
- A team cannot have more than one open retrospective at a time.
- A completed action item cannot be marked as incomplete.

## Notes

- The application currently uses an H2 in-memory database, not PostgreSQL.
- The controller is still a stub and the service layer has not been implemented yet.
- The repository is intended as a guided exercise to implement the missing business logic and API behavior.
