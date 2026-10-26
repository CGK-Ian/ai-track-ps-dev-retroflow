# AI Track Standards Audit

**Initial audit status:** BLOCKED  
**Revision originally audited:** `bcf5a85`  
**Standards reviewed:** AI Track MCP StackCraft Coding Standards and QA & Test Standards

At the time of the initial audit, `mvn verify` failed during compilation with eight errors. The follow-up below records remediation made after that audit.

## Findings

| Severity | Finding | Evidence |
|---|---|---|
| **Critical** | **The latest code does not compile.** The feedback controller imports a missing `FeedbackItemService`; the controllers also call service methods that do not exist or have incompatible signatures. Maven reported 8 compilation errors. | [FeedbackItemController.java:8](../src/main/java/com/stackcraft/retroflow/controller/FeedbackItemController.java#L8), [TeamController.java:41](../src/main/java/com/stackcraft/retroflow/controller/TeamController.java#L41), [RetrospectiveController.java:50](../src/main/java/com/stackcraft/retroflow/controller/RetrospectiveController.java#L50), [TeamService.java:35](../src/main/java/com/stackcraft/retroflow/service/TeamService.java#L35), [RetrospectiveService.java:99](../src/main/java/com/stackcraft/retroflow/service/RetrospectiveService.java#L99) |
| **Critical** | **Core service operations remain unimplemented.** Request-based team creation/retrieval and retrospective creation/retrieval throw `UnsupportedOperationException`; when reached, these endpoints return 501 rather than serving their advertised flows. | [TeamService.java:35](../src/main/java/com/stackcraft/retroflow/service/TeamService.java#L35), [TeamService.java:39](../src/main/java/com/stackcraft/retroflow/service/TeamService.java#L39), [RetrospectiveService.java:99](../src/main/java/com/stackcraft/retroflow/service/RetrospectiveService.java#L99), [RetrospectiveService.java:103](../src/main/java/com/stackcraft/retroflow/service/RetrospectiveService.java#L103) |
| **Critical** | **Business-rule and missing-resource statuses do not match the standard.** All `RetroflowException`s become HTTP 400, but business-rule conflicts should be 409 and missing resources should be 404. Existing service lookups throw `RetroflowException` for missing records, so those also take the 400 path. | [ApiExceptionHandler.java:46](../src/main/java/com/stackcraft/retroflow/exception/ApiExceptionHandler.java#L46), [TeamService.java:23](../src/main/java/com/stackcraft/retroflow/service/TeamService.java#L23), [RetrospectiveService.java:40](../src/main/java/com/stackcraft/retroflow/service/RetrospectiveService.java#L40) |
| **Critical** | **Error responses do not use the required standard shape.** The current response contains `code` and `description`; the MCP standard requires `error`, `status`, and `timestamp`. | [ErrorMessage.java:3](../src/main/java/com/stackcraft/retroflow/dto/ErrorMessage.java#L3), [ApiExceptionHandler.java:71](../src/main/java/com/stackcraft/retroflow/exception/ApiExceptionHandler.java#L71) |
| **Critical** | **Service exception messages do not follow the required format** `[EntityType] [identifier]: [reason]`. Current messages such as “Team name is required” and “A completed action item cannot be uncompleted” lack that structure. | [TeamService.java:23](../src/main/java/com/stackcraft/retroflow/service/TeamService.java#L23), [RetrospectiveService.java:93](../src/main/java/com/stackcraft/retroflow/service/RetrospectiveService.java#L93) |
| **Major** | **The build does not enforce the required branch-coverage minimum.** JaCoCo is configured with an 80% line threshold only; the MCP standard also requires at least 70% branch coverage. Coverage could not be measured in this run because compilation failed. | [pom.xml:85](../pom.xml#L85) |
| **Major** | **Feedback content has no boundary validation for the specified 5,000-character limit** on either create or update requests. The request DTOs only require nonblank content. | [CreateFeedbackItemRequest.java:12](../src/main/java/com/stackcraft/retroflow/dto/CreateFeedbackItemRequest.java#L12), [UpdateFeedbackItemRequest.java:8](../src/main/java/com/stackcraft/retroflow/dto/UpdateFeedbackItemRequest.java#L8) |
| **Major** | **Public service methods lack the required JavaDoc**, including methods implementing business rules. | [TeamService.java](../src/main/java/com/stackcraft/retroflow/service/TeamService.java), [RetrospectiveService.java](../src/main/java/com/stackcraft/retroflow/service/RetrospectiveService.java) |
| **Minor** | **The README describes an obsolete state:** it says the controller is empty and links to `RetroflowController`, while the current source has separate team, retrospective, and feedback controllers. | [README.md:6](../README.md#L6) |

## Scope limitation

The original audit did not verify tests, coverage, or live endpoint behavior because compilation failed.

## Remediation follow-up

The critical findings above have been addressed:

- Implemented the team, retrospective, and feedback service operations required by the controllers, resolving the compilation failures and removing the corresponding unimplemented service stubs.
- Mapped business-rule violations to HTTP 409 and missing resources to HTTP 404.
- Changed error responses to the required `error`, `status`, and `timestamp` structure.
- Updated service-layer error messages to include the entity and identifier.
- Added API-boundary validation for the 5,000-character feedback limit on create and update.

`mvn test` and `mvn verify` now pass: **190 tests, 0 failures, 0 errors**. The configured JaCoCo line-coverage gate also passes. The added integration test exercises team creation with member persistence, retrospective creation, action-item feedback persistence, filtering, and completion.

Branch coverage is not configured, so the standards' 70% branch-coverage threshold remains unenforced. The remaining JavaDoc and README findings above are also outside the critical-finding fixes.
