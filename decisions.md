# RetroFlow Decisions

This document records the decisions made to resolve ambiguities in the RetroFlow project brief. These decisions supplement the brief and should guide API, validation, persistence, and test implementation.

## Identity and Team Membership

- A full authentication or authorization flow is out of scope. Users are identified by email address in requests.
- When a request supplies a user's email address as the submitter or actor, validate that the email belongs to the relevant team. This is a roster check, not authentication.
- Team member names do not need to be unique within a team. Member email addresses must be unique.
- Team membership can be updated, and teams and members can be deleted.
- Team member names must not be blank and may be at most 255 characters.
- A team name must be unique, non-empty, 5 to 255 characters long, and contain only alphanumeric characters or dashes.

## Retrospectives

- The client supplies the retrospective date. If omitted, it defaults to the current date. All dates are considered valid; there is no past/future date restriction.
- The request may supply the retrospective status. If omitted, it defaults to `OPEN`.
- Enum values are case-insensitive.
- A retrospective may transition from `OPEN` to `CLOSED` only. It cannot be reopened or otherwise edited.
- A team may have at most one `OPEN` retrospective at a time. Creating another while one is open is an error.
- A retrospective may be closed even when it has no feedback or action items.
- Retrospectives cannot be deleted.

## Feedback and Action Items

- Feedback cannot be added to a closed retrospective.
- A feedback item's submitter is stored as the ID/reference of the corresponding team member, not as an email string. If the request supplies the submitter's email, resolve it to a member of the retrospective's team and persist that member as the submitter.
- Feedback items may be deleted only while their retrospective is open.
- An action item must always have a priority.
- An action item can be fully edited while it is incomplete, even if its parent retrospective is closed.
- Once an action item is completed, it cannot be edited or uncompleted. Here, completion is the action item's closed state.
- A completed action item cannot be uncompleted, as required by the project brief.
- Action items are retrieved within the scope of one retrospective.

## Retrieval and API Conventions

- Teams and retrospectives support retrieval by list, query, or ID.
- All list endpoints are paginated.
- If both action-item filters (priority and completed status) are supplied, both filters apply (`AND` semantics).
- Use standard REST naming conventions for controller paths.
- Return `404 Not Found` when a requested record does not exist. Return `400 Bad Request` for other functional errors.
- Errors use a standard `ErrorMessage` response object containing a code and description.

## Persistence and Documentation

- In-memory persistence is acceptable for the current project scope.
- The README should be updated to reflect the actual requirements and setup.

