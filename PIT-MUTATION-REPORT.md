# PIT Mutation Test Report

PIT was added to the Maven build per the user request to run mutation analysis
on the service classes, report mutation locations and changes, and verify that
mutations were removed after the run. The configured scope is
`com.stackcraft.retroflow.service.*`, exercised by
`RetroflowBusinessRulesTest`.

The separate `TeamServiceTest` and `RetrospectiveServiceTest` classes are not
selected: they contain failing cases for service behavior that is not
implemented in the current production code. The PIT test run therefore uses the
passing service integration suite and does not mask those failures.

Command used for the original score:

```text
mvn -q -Djacoco.skip=true org.pitest:pitest-maven:1.30.1:mutationCoverage
```

The original score was collected using JDK 25, so JaCoCo was skipped to avoid
its unsupported-class-version error. The project targets Java 21; this command
was subsequently re-run successfully on Temurin Java 21 without skipping JaCoCo:

```text
mvn -q org.pitest:pitest-maven:1.30.1:mutationCoverage
```

That Java 21 run produced the same score: 37 killed and 2 survived out of 39
mutations (95%).

## Mutation score

PIT's score is killed mutants divided by generated mutants.

| Run | Tests | Killed | Generated | Score | Uncovered |
|---|---:|---:|---:|---:|---:|
| Before strengthening assertions | 4 test methods | 31 | 39 | 79% | 5 |
| After strengthening assertions | 8 test methods | 37 | 39 | 95% | 0 |

The after run killed six additional mutants. Two mutants remain. Both appear
behaviorally equivalent to the existing code: `Retrospective` already defaults
to `OPEN`, and an incomplete action item's completion flag is already `false`.

## Mutations not killed before the test updates

| File | Line | Mutation change | Before | After |
|---|---:|---|---|---|
| `RetrospectiveService.java` | 40 | Return `null` instead of raising when the team is missing | No coverage | Killed |
| `RetrospectiveService.java` | 51 | Remove the call that sets a new retrospective's status to `OPEN` | Survived | Survived |
| `RetrospectiveService.java` | 61 | Return `null` instead of the closed retrospective | Survived | Killed |
| `RetrospectiveService.java` | 87 | Return `null` instead of the completed action item | Survived | Killed |
| `RetrospectiveService.java` | 95 | Remove the call that sets an action item's completion flag to `false` | No coverage | Survived |
| `RetrospectiveService.java` | 96 | Return `null` instead of the uncompleted action item | No coverage | Killed |
| `RetrospectiveService.java` | 117 | Return `null` instead of raising when the retrospective is missing | No coverage | Killed |
| `RetrospectiveService.java` | 122 | Return `null` instead of raising when the action item is missing | No coverage | Killed |

No production source files were changed. PIT's generated mutants were temporary;
the persistent `target/pit-reports` directory contains only HTML/XML reports and
style assets. See `target/pit-reports/index.html` for the full generated report.
The final PIT XML records 37 `KILLED` and 2 `SURVIVED` mutations.
