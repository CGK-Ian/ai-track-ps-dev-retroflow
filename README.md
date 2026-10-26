# RetroFlow

RetroFlow is a Spring Boot backend for StackCraft's retrospective workflow:
teams, retrospectives, feedback, and action items.

**Current status:** the JPA entities, repositories, and business-rule services
are implemented and tested. The [REST controller](src/main/java/com/stackcraft/retroflow/controller/RetroflowController.java)
is still empty: there are **no business HTTP endpoints yet**. You can run the
server and inspect its database, but you cannot create teams or retrospectives
over HTTP. There is no frontend, Swagger UI, or Actuator health endpoint.

## Getting Started

### Prerequisites

- **JDK 21** (not just a JRE).
- **Maven 3.6.3+** on your `PATH`. There is no Maven Wrapper in this repository.
- Git and internet access for the initial Maven dependency download.

No PostgreSQL, Docker, database provisioning, secrets, or `.env` file is needed
for the local workflow. The application uses an embedded, in-memory H2 database.

### Fresh clone to running server

With the prerequisites installed, these steps should take under five minutes on
a typical connection. The first dependency download can take longer on a slow
network.

1. Clone and enter the repository:

   ```text
   git clone https://github.com/CGK-Ian/ai-track-ps-dev-retroflow.git
   cd ai-track-ps-dev-retroflow
   ```

2. Check the toolchain:

   ```text
   java -version
   mvn -version
   ```

   Both must report Java **21**. Maven can use a different JDK from your terminal
   or IDE; check the Java version and Java home in its output.

3. Start the server from the repository root:

   ```text
   mvn spring-boot:run
   ```

   Wait for `Started RetroflowApplication` in the logs. The default address is
   `http://localhost:8080`. Leave this terminal running.

4. In a second terminal, check the H2 console:

   ```text
   curl -i http://localhost:8080/h2-console/
   ```

   **Expected: HTTP 200** with an HTML login page. In Windows PowerShell use
   `curl.exe` rather than `curl`, which may resolve to a PowerShell alias.
   Alternatively, open the URL in a browser.

Stop the server with **Ctrl+C**. All data is lost when the application process
stops. A `404` at `http://localhost:8080/` is expected, not a startup failure.

## Local configuration and database

The defaults live in [application.properties](src/main/resources/application.properties).
Spring Boot creates the schema from the JPA entities at startup.

| Setting | Local default |
| --- | --- |
| HTTP port | `8080` |
| Database | Embedded H2, in memory |
| JDBC URL | `jdbc:h2:mem:retroflow;DB_CLOSE_DELAY=-1` |
| Database user | `sa` |
| Database password | Empty |
| Schema management | `spring.jpa.hibernate.ddl-auto=update` |
| H2 console | `http://localhost:8080/h2-console/` |

To inspect data, open the H2 console while the server is running, select the H2
driver, enter the JDBC URL and credentials above, and connect. The database
starts without seed data; tables are populated only when application code writes
to them. Tests run in their own application process, so their records will not
appear in your running server.

To use another port without changing the shared configuration:

```text
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

To log generated SQL for a debugging session:

```text
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.jpa.show-sql=true"
```

These database credentials and the enabled H2 console are for local development,
not a production deployment.

## IDE setup

### VS Code

1. Open the repository root, not just the source directory.
2. Install **Extension Pack for Java** (`vscjava.vscode-java-pack`), which includes
   Java language support, debugging, Maven support, and Test Runner for Java.
3. Use **Java: Configure Java Runtime** to select JDK 21 for this project.
4. Run or debug
   [RetroflowApplication](src/main/java/com/stackcraft/retroflow/RetroflowApplication.java)
   using the `Run` / `Debug` links above `main`. Set breakpoints in the services
   and run tests from the Testing view.

Optional extensions:

- **Spring Boot Extension Pack** (`vmware.vscode-boot-dev-pack`) for Spring
  navigation and application dashboard support.
- **REST Client** (`humao.rest-client`) to send requests from `.http` files once
  endpoints are implemented. There is no request collection checked in yet.

The repository ignores `.vscode`, so local launch settings are not shared.

### IntelliJ IDEA

Open [pom.xml](pom.xml) as a Maven project. Set the Project SDK and Maven
runner/importer JDK to **21**, then run `RetroflowApplication` or the JUnit tests.
H2 is embedded, so no database connection setup is required to launch the app.

### Dev container

An existing [dev container configuration](.devcontainer/devcontainer.json)
provides Java 21, Maven, and Java extensions, compiles the project after creation,
and forwards port 8080.

With Docker running and the VS Code **Dev Containers**
(`ms-vscode-remote.remote-containers`) extension installed, choose
**Dev Containers: Reopen in Container**, then run `mvn spring-boot:run` in the
container terminal. This is an alternative to installing Java and Maven locally;
the initial image download may exceed five minutes.

## Running tests and coverage

Run commands from the repository root. Tests use JUnit 5, AssertJ, and
`@SpringBootTest` with H2; no external services or running API are required.

| Task | Command |
| --- | --- |
| Run all tests | `mvn test` |
| Run the business-rule test class | `mvn "-Dtest=RetroflowBusinessRulesTest" test` |
| Run one test method | `mvn "-Dtest=RetroflowBusinessRulesTest#closingARetrospective_preventsAddingFeedback" test` |
| Run the same verification as CI | `mvn -B verify` |
| Remove old build output and verify | `mvn clean verify` |

The eight tests in
[RetroflowBusinessRulesTest](src/test/java/com/stackcraft/retroflow/RetroflowBusinessRulesTest.java)
currently cover:

- Closing a retrospective prevents adding feedback.
- A team cannot have two open retrospectives.
- A completed action item cannot be marked incomplete.
- Created teams, retrospectives, feedback, and action items retain their details.
- Incomplete action items remain incomplete when uncompleted.
- Missing teams, retrospectives, and action items produce domain errors.

`mvn verify` generates a JaCoCo report at `target/site/jacoco/index.html` and
enforces **at least 80% overall line coverage**. `mvn test` runs the tests but
does not generate that report or enforce the coverage threshold. Test results
are written to `target/surefire-reports`.

PIT mutation testing is configured for the service package and runs the passing
`RetroflowBusinessRulesTest` suite. Run it with JDK 21, like the other project
commands:

```text
mvn -q org.pitest:pitest-maven:1.30.1:mutationCoverage
```

PIT writes its HTML/XML output under `target/pit-reports`; the checked-in
[mutation report](PIT-MUTATION-REPORT.md) records the measured baseline and
final scores and the remaining mutations.

The [CI workflow](.github/workflows/ci.yml) uses Java 21 and `mvn -B verify`.
Run that command before opening a pull request. The existing tests exercise the
service layer, not HTTP contracts.

## Testing HTTP manually

For now, manual HTTP testing can confirm only that the server and H2 console
are reachable:

```text
curl -i http://localhost:8080/h2-console/
curl -i http://localhost:8080/
curl -i http://localhost:8080/actuator/health
```

Use `curl.exe` in Windows PowerShell. Expected responses:

| Request | Expected status | Meaning |
| --- | --- | --- |
| `GET /h2-console/` | `200` | H2 console login page is available |
| `GET /` | `404` | No root endpoint is mapped |
| `GET /actuator/health` | `404` | Actuator is not installed |

These are reachability checks, **not business API tests or a health contract**.
Postman, Insomnia, or VS Code REST Client can send the same requests.

## REST API

Controllers: `TeamController`, `RetrospectiveController`, `FeedbackItemController`
(contract tests in `src/test/java/.../controller`, mocked services, no database).

| # | Endpoint | Success |
| --- | --- | --- |
| 1 | `POST /api/teams` | 201 |
| 2 | `GET /api/teams` (paged) | 200 |
| 3 | `GET /api/teams/{id}` | 200 |
| 4 | `POST /api/teams/{teamId}/retrospectives` | 201 |
| 5 | `GET /api/teams/{teamId}/retrospectives?email=` (paged) | 200 |
| 6 | `GET /api/retrospectives?id=&title=&status=` (paged) | 200 |
| 7 | `GET /api/retrospectives/{id}` | 200 |
| 8 | `PUT /api/retrospectives/{id}/close?email=` | 204 |
| 9 | `POST /api/retrospectives/{retrospectiveId}/feedbackItems` | 201 |
| 10 | `GET /api/retrospectives/{retrospectiveId}/feedbackItems?priority=&completed=` (paged) | 200 |
| 11 | `GET .../feedbackItems/{feedbackItemId}` | 200 |
| 12 | `PUT .../feedbackItems/{feedbackItemId}` | 200 |
| 13 | `DELETE .../feedbackItems/{feedbackItemId}?email=` | 204 |
| 14 | `PUT .../feedbackItems/{feedbackItemId}/complete?email=` | 200 |

- Actor `email` is in the body for POST and PUT with a body, otherwise a required `email` query parameter.
- Paging: `pageNumber` (default 0), `pageSize` (default 20, max 100); response `{ "items": [...], "paging": { "totalItems": n } }`.
- Enum values (status, type, priority) are case-insensitive.
- Errors: `{ "code", "description" }` with 400 (`VALIDATION_ERROR`, `BUSINESS_RULE_VIOLATION`, `DATA_CONFLICT`) and 404 (`NOT_FOUND`).
- Controllers call the real `TeamService`, `RetrospectiveService` and `FeedbackItemService`;
  `RetroflowApiIntegrationTest` exercises the whole flow against the in-memory database.

## Faster development loop

- Keep Maven's dependency cache between runs. Use `mvn test` or a focused test
  command during development; reserve `clean` for stale output or a fresh build.
- Debug a focused test in your IDE to investigate service behavior without
  starting a separate server.
- Restart `mvn spring-boot:run` after code changes, or use your IDE's supported
  debugger HotSwap for simple method-body edits. Spring Boot DevTools is **not**
  included, so automatic restart is not currently configured.
- Inspect state through the H2 console instead of adding temporary log output.

To build and run the packaged application:

```text
mvn package
java -jar target/retroflow-0.1.0-SNAPSHOT.jar
```

Run only one server on a given port at a time. `package` runs tests but does not
run the later JaCoCo verification phase.

## Project layout

```text
src/main/java/com/stackcraft/retroflow/
  RetroflowApplication.java    Spring Boot entry point
  controller/                 Empty REST controller; HTTP work remains
  entity/                     Team, Retrospective, FeedbackItem, ActionItem
  repository/                 Spring Data JPA repositories
  service/                    Business rules and persistence operations
  exception/                  Domain exception
src/main/resources/
  application.properties      Local H2 and JPA configuration
src/test/java/com/stackcraft/retroflow/
  RetroflowBusinessRulesTest.java
.devcontainer/                Optional containerized development environment
.github/workflows/ci.yml       Build, tests, and coverage verification
pom.xml                       Dependencies and Maven plugins
Dockerfile                    Multi-stage application image
```

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| `release version 21 not supported` or a class-version error | Point `JAVA_HOME`, Maven, and the IDE at JDK 21; confirm with `mvn -version`. |
| `mvn` is not recognized | Install Maven and add its `bin` directory to `PATH`, then reopen the terminal. No `mvnw` script is provided. |
| Port 8080 is already in use | Stop your own previous server or use the port override above. |
| Connection refused | Wait for the startup message, check the logs and port, and keep the server terminal running. |
| HTTP 404 at `/` | Expected: no root route is implemented. Check `/h2-console/` instead. |
| H2 login fails or shows a different database | Use the exact configured JDBC URL, user `sa`, and a blank password while the app is running. |
| Records disappeared | H2 is in memory; every server restart resets the data. |
| `verify` fails but `test` passes | Inspect the JaCoCo report; verification also enforces the 80% line-coverage gate. |
| Maven cannot download dependencies | Check internet/proxy access and your local Maven `settings.xml`; do not commit credentials. |

## Useful next additions

These are follow-up improvements, **not features currently available**:

- A Maven Wrapper to pin the Maven version and remove the separate installation
  prerequisite.
- Shared VS Code extension recommendations and run/test tasks, with a narrow
  exception to the current `.vscode` ignore rule.
- Business HTTP endpoints, validation/error responses, controller tests, and an
  executable `.http` request collection; OpenAPI documentation once the
  contracts are defined.
- An opt-in development profile with DevTools for automatic restarts.
- A real health endpoint and matching Docker health check. The existing
  [Dockerfile](Dockerfile) checks `/actuator/health` and `/`, but both currently
  return 404, so its container health check cannot pass even when the server is
  running. It also skips tests during the image build; run `mvn verify`
  separately. Use the local or dev-container workflow above for onboarding.
