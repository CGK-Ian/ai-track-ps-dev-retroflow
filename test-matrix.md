# RetroFlow Business-Rule Test Matrix

This matrix combines the RetroFlow brief with the decisions recorded for this project. `FunctionalError` is the response error type for validation and functional/business-rule failures. Missing records use `404`. Successful HTTP status codes are not specified by the brief or decisions and are marked accordingly.

| Rule | Operation | Situation | Expected result | Exception / error type | HTTP status |
|---|---|---|---|---|---|
| Authentication is out of scope; request identity uses email | Any operation identifying an actor | Request is made without an authentication session | Do not require an authentication flow; apply the email rules for that operation | None | Success status not specified |
| Team requires at least one member | Create team | Member list is empty or absent | Reject creation | `FunctionalError` | `400` |
| Team name is required | Create or update team | Name is empty or whitespace-only | Reject request | `FunctionalError` | `400` |
| Team name has 5 to 255 characters | Create or update team | Name is shorter than 5 or longer than 255 characters | Reject request | `FunctionalError` | `400` |
| Team name uses only alphanumeric characters or dash | Create or update team | Name contains another character | Reject request | `FunctionalError` | `400` |
| Team names are unique | Create or update team | Name duplicates another team's name | Reject request | `FunctionalError` | `400` |
| Member names are nonblank and at most 255 characters | Add or update member | Name is blank or too long | Reject request | `FunctionalError` | `400` |
| Member names need not be unique | Add member | Another team member has the same name | Allow the duplicate name | None | Success status not specified |
| Member email must be a valid email address | Add or update member | Email is malformed | Reject request | `FunctionalError` | `400` |
| Member emails must be unique | Add or update member | Email duplicates an existing member email | Reject request; whether uniqueness is global or per team is unspecified | `FunctionalError` | `400` |
| Team membership can be updated | Update member/team membership | Valid update that satisfies field constraints | Apply the update | None | Success status not specified |
| Members can be deleted, but a team must retain at least one | Delete member | Deleting this member would leave the team empty | Reject deletion | `FunctionalError` | `400` |
| Members can be deleted | Delete member | Team will still have at least one member | Delete the member | None | Success status not specified |
| Teams cannot be deleted | Delete team | Any team | Reject deletion | `FunctionalError` | `400` |
| Teams can be retrieved by ID | Get team | Team exists | Return the team | None | Success status not specified |
| Teams can be retrieved by list or query | List/query teams | Matching teams exist | Return matching teams; exact query behavior is unspecified | None | Success status not specified |
| Every list request is paginated | List teams, retrospectives, feedback, or action items | List request is made | Return a page of results | None | Success status and pagination parameter/response details not specified |
| Retrospective belongs to a team | Create retrospective | Team ID does not identify a team | Reject because the referenced team is missing | Not-found error | `404` |
| Retrospective title is required and supports Unicode up to 255 characters | Create or update retrospective | Title is empty/blank or exceeds 255 characters | Reject request; valid Unicode title is accepted | `FunctionalError` | `400` |
| Client supplies retrospective date; default is today | Create retrospective | Date omitted | Use the current date | None | Success status not specified |
| All retrospective dates are valid | Create retrospective | Supplied date is past, present, or future | Accept the date; no date-range restriction applies | None | Success status not specified |
| Retrospective status defaults to `OPEN` | Create retrospective | Status omitted | Create with `OPEN` status | None | Success status not specified |
| Retrospective status may be supplied and enums are case-insensitive | Create retrospective | Status is a recognized value with any letter casing | Accept and interpret the value case-insensitively | None | Success status not specified |
| Invalid retrospective status is rejected | Create retrospective | Status is not `OPEN` or `CLOSED` | Reject request | `FunctionalError` | `400` |
| A newly created retrospective cannot start `CLOSED`; closure is only an `OPEN` to `CLOSED` transition | Create retrospective | Request supplies initial status `CLOSED` | Reject creation; a retrospective must be opened before it can be closed | `FunctionalError` | `400` |
| One open retrospective per team | Create retrospective | Team already has an `OPEN` retrospective | Reject creation | `FunctionalError` | `400` |
| Team member email is required to create a retrospective | Create retrospective | Actor email is absent | Reject request | `FunctionalError` | `400` |
| Actor email must be valid | Create retrospective | Actor email is malformed | Reject request | `FunctionalError` | `400` |
| Actor email must belong to the team | Create retrospective | Valid email is not associated with the specified team | Reject creation | `FunctionalError` | `400` |
| Team member can create retrospective | Create retrospective | Actor email belongs to the specified team and other rules pass | Create the retrospective | None | Success status not specified |
| Retrospectives can be listed by team | List retrospectives for team | Team exists | Return retrospectives for that team | None | Success status not specified |
| Retrospectives can be retrieved by list, query, or ID | List/query/get retrospectives | Matching records exist | Return the matching retrospective(s); query behavior is unspecified | None | Success status not specified |
| Missing retrospective is reported as not found | Get, list by team, or close retrospective | Referenced retrospective or team does not exist | Return not-found error | Not-found error | `404` |
| Retrospective can close without feedback or action items | Close retrospective | Retrospective is open and has no items | Allow closure | None | Success status not specified |
| Team member email is required to close a retrospective | Close retrospective | Actor email is absent | Reject request | `FunctionalError` | `400` |
| Actor email must be valid | Close retrospective | Actor email is malformed | Reject request | `FunctionalError` | `400` |
| Actor email must belong to the retrospective's team | Close retrospective | Valid email is not associated with the retrospective's team | Reject closure | `FunctionalError` | `400` |
| Retrospective may transition from `OPEN` to `CLOSED` | Close retrospective | Retrospective is open and actor is a member of its team | Set status to `CLOSED` | None | Success status not specified |
| Closed retrospective cannot be reopened; only `OPEN` to `CLOSED` is allowed | Change status or repeat close operation | Current state is `CLOSED` and operation attempts any other transition, including reopening | Reject transition | `FunctionalError` | `400` |
| Retrospective may not otherwise be edited | Update retrospective | Request changes title, date, team, or another property instead of closing it | Reject update | `FunctionalError` | `400` |
| Retrospectives cannot be deleted | Delete retrospective | Any retrospective | Reject deletion | `FunctionalError` | `400` |
| Feedback belongs to a retrospective | Add feedback | Parent retrospective does not exist | Reject because the referenced retrospective is missing | Not-found error | `404` |
| Feedback cannot be added to a closed retrospective | Add feedback | Parent retrospective is closed | Reject addition | `FunctionalError` | `400` |
| Feedback content is nonempty, Unicode text, with a 5000-character limit | Add or modify feedback | Content is blank or longer than 5000 characters | Reject request; Unicode text within the limit is accepted | `FunctionalError` | `400` |
| Feedback type is one of the defined values; enums are case-insensitive | Add or modify feedback | Type is `WENT_WELL`, `NEEDS_IMPROVEMENT`, or `ACTION_ITEM`, regardless of casing | Accept recognized type | None | Success status not specified |
| Invalid feedback type is rejected | Add or modify feedback | Type is not a defined value | Reject request | `FunctionalError` | `400` |
| Feedback submitter is persisted as a team-member reference/ID | Add feedback | Request identifies a submitter by email | Resolve email to the corresponding member in the retrospective's team and store that member reference | `FunctionalError` if no matching team member | `400` |
| Feedback submitter email format is validated when supplied | Add feedback | Supplied email is malformed | Reject request | `FunctionalError` | `400` |
| Feedback retrieval is scoped to one retrospective | List feedback | Retrospective exists | Return its feedback items | None | Success status not specified |
| Feedback in a closed retrospective cannot be modified | Modify ordinary feedback | Parent retrospective is closed | Reject modification | `FunctionalError` | `400` |
| Feedback in a closed retrospective cannot be deleted | Delete ordinary feedback | Parent retrospective is closed | Reject deletion | `FunctionalError` | `400` |
| Feedback can be deleted only while its retrospective is open | Delete feedback, including action-item feedback | Parent retrospective is open | Allow deletion | None | Success status not specified |
| Action item is feedback with type `ACTION_ITEM` | Create action item | Request creates an action item | Persist it as an action item with the matching feedback type | `FunctionalError` if type is inconsistent | `400` |
| Action item requires a priority | Create or modify action item | Priority is absent | Reject request | `FunctionalError` | `400` |
| Action item priority must be a defined enum; enums are case-insensitive | Create or modify action item | Priority is `LOW`, `MEDIUM`, or `HIGH`, regardless of casing | Accept the priority | None | Success status not specified |
| Invalid priority is rejected | Create or modify action item | Priority is not a defined value | Reject request | `FunctionalError` | `400` |
| Incomplete action item can be fully edited while its retrospective is open | Modify action item | Item is incomplete and retrospective is open | Allow edit | None | Success status not specified |
| Incomplete action item can be fully edited after its retrospective closes | Modify action item | Item is incomplete and retrospective is closed | Allow edit | None | Success status not specified |
| Action item can be closed after its retrospective closes | Complete action item | Item is incomplete and its retrospective is closed | Allow completion | None | Success status not specified |
| Completed action item cannot be uncompleted | Uncomplete action item | Item is completed | Reject the transition | `FunctionalError` | `400` |
| Completed action item cannot be edited | Modify action item | Item is completed | Reject modification | `FunctionalError` | `400` |
| Action items are retrieved under one retrospective | List action items | Retrospective exists | Return action items belonging to that retrospective | None | Success status not specified |
| Action items can be filtered by priority | List action items | Priority filter supplied | Return matching items within the specified retrospective | None | Success status not specified |
| Action items can be filtered by completion status | List action items | Completed filter supplied | Return matching items within the specified retrospective | None | Success status not specified |
| Action-item filters combine with AND | List action items | Both priority and completed filters supplied | Return items matching both filters | None | Success status not specified |
| Invalid requests return meaningful errors | Any request with invalid input | Validation fails | Return a meaningful error response | `FunctionalError` | `400` |
| Functional errors use the `FunctionalError` response type | Any operation rejected by a business or functional rule | Operation violates a rule | Return `FunctionalError` with an explanatory description | `FunctionalError` | `400` |
| Missing records return not found | Get or mutate a missing record | Requested entity does not exist | Return not-found error | Not-found error | `404` |
| Persistence may remain in memory for current scope | Application restart | Process stops and restarts | Data need not persist across restarts | None | Not applicable |
