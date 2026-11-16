# Workout Tracker: API plan

## Shared conventions

- JSON request bodies use `Content-Type: application/json`, except PATCH requests, which use `application/merge-patch+json`.
- Successful responses with a body use `Content-Type: application/json`.
- Error responses use `Content-Type: application/problem+json` and the shared schema below.
- GET and DELETE requests have no body. Starting a session also requires no body.
- Each resource's own identifier is returned as `id`. Path parameters use descriptive names such as `sessionId`, `entryId`, and `setId`. An exercise entry also stores the reference `catalogueExerciseId`.
- Timestamps use RFC 3339 format in UTC, for example `2026-11-15T10:30:00Z`. Date filters use `YYYY-MM-DD`.
- Endpoints accepting a request body require a JSON object. Valid JSON of the wrong structure, such as an array, string, number, boolean, or null, returns `422 VALIDATION_FAILED`.
- Request bodies may contain only the fields defined for the operation. Unknown fields return `422 VALIDATION_FAILED`; they are not silently ignored.
- Required body fields cannot be `null`. For PATCH, omitted fields remain unchanged; the editable fields in this API cannot be set to `null`.
- Exercise and set ordering is assigned and stored by the backend. The frontend does not send an ordering parameter; it displays the returned arrays in order.



## Session lifecycle

- An ACTIVE session allows adding exercises and sets, editing existing sets, and deleting sets or exercise entries.
- A COMPLETED session allows editing existing sets and deleting sets or exercise entries. Adding new exercises or sets and reopening the session are intentionally unsupported in this MVP.
- ACTIVE and COMPLETED sessions can be viewed or deleted.
- Completing a session requires at least one logged set at that moment. Later deletions may leave a completed session empty; it remains in history until the user deletes it.
- A user can start another session after completing or deleting their active session.
- Session-state rules must hold even when requests run concurrently, including across service instances. Checking ACTIVE and adding an exercise or set must be coordinated with completing the same session.
- If an addition takes effect before completion, it is included when checking completion. If completion takes effect first, the addition returns `409 SESSION_ALREADY_COMPLETED`.
- The completion check must also be coordinated with deleting sets or exercise entries. If deletion removes the last set before completion takes effect, completion returns `409 SESSION_HAS_NO_SETS`. If completion takes effect first, the later deletion is allowed and the session remains COMPLETED.



## Shared error response

Both services use Problem Details with `Content-Type: application/problem+json`. The table defines this project's common schema; `code` and `errors` are application-specific extensions.

| Field | Type | Required | Meaning |
| --- | --- | --- | --- |
| `type` | string | Yes | Always `about:blank` for this MVP. |
| `title` | string | Yes | HTTP status title, such as `Conflict` or `Unprocessable Content`. |
| `status` | integer | Yes | Must match the HTTP response status. |
| `detail` | string | Yes | Human-readable explanation of this error. |
| `code` | string | Yes | Stable application error code listed for the endpoint. |
| `errors` | array of objects | No | Field-level details for validation or query-parameter errors. Each object has required string fields `field` and `message`. |

- Omit optional fields when they do not apply; do not send them as null.
- The frontend uses `code` to select behaviour, rather than parsing `detail` or `message`.
- In `errors`, `field` is the request field or query parameter name, such as `reps` or `limit`.
- Malformed JSON returns `400 MALFORMED_JSON`.
- Valid JSON with the wrong structure, unknown fields, missing required fields, or invalid field values returns `422 VALIDATION_FAILED`. This applies to all endpoints accepting a JSON body, including PATCH.
- Invalid query parameters return `400 INVALID_QUERY_PARAMETERS`. Unsupported request media types return `415 UNSUPPORTED_MEDIA_TYPE`.
- For an unknown field, the optional `errors` array can identify that field with a message such as "Unknown field." For a wrong top-level structure, explain the problem in `detail`; field-level errors may be omitted.
- Endpoints accepting ordinary JSON bodies support `application/json`. PATCH endpoints support `application/merge-patch+json`. The bodyless start-session request does not require a Content-Type header.
- Error details must not expose another user's resources or internal database errors. Missing, inaccessible, or incorrectly nested resources use the same `RESOURCE_NOT_FOUND` code.

Example validation error (`422 Unprocessable Content`):

```json
{
  "type": "about:blank",
  "title": "Unprocessable Content",
  "status": 422,
  "detail": "One or more fields are invalid.",
  "code": "VALIDATION_FAILED",
  "errors": [
    {
      "field": "reps",
      "message": "Must be an integer greater than or equal to 1."
    }
  ]
}
```

Example active-session conflict (`409 Conflict`):

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "You already have an active session.",
  "code": "ACTIVE_SESSION_EXISTS"
}
```



## Recovering an active session

1. When the user clicks the start-or-continue workout button, the frontend calls `GET /v1/sessions?status=ACTIVE` without date filters and with the default offset of 0.
2. If an active session is found, the frontend uses its `id` to call `GET /v1/sessions/{sessionId}` and opens the existing workout with its exercises and sets.
3. If the list is empty, the frontend calls `POST /v1/sessions`. On `201 Created`, it opens the newly created workout.
4. If POST returns `409` with code `ACTIVE_SESSION_EXISTS`, another request may have created a session after the initial lookup. The frontend repeats the active-session GET, then fetches and opens the session it finds.

Notes:
- The initial GET does not prevent concurrent creation; the one-active-session rule and POST conflict handling remain necessary.
- If the repeated lookup is empty, or the discovered session disappears before its details are fetched, refresh the screen and let the user try again rather than retrying indefinitely.
- The same active-session lookup can be used when the app opens.




## `POST /v1/sessions`

**Request:** No request body.

**Access:** authenticated user.

**Rules:**

- A user can have at most one ACTIVE session.
- This rule also applies to concurrent requests, including requests handled by different service instances. A creation that conflicts with an existing ACTIVE session returns 409.

**Success:**

- 201 Created + created session.
- Location: `/v1/sessions/{sessionId}`.
- Response contains id, startedAt, finishedAt, and status.
- The server generates a globally unique session ID and startedAt; the ID is returned as id.
- `status = ACTIVE`, `finishedAt = null`.

**Errors:**

- 401 Unauthorized (`UNAUTHENTICATED`): authentication is missing or invalid.
- 409 Conflict (`ACTIVE_SESSION_EXISTS`): the user already has an active session.




## `GET /v1/exercises`

**Request:**

- Query parameters (all optional):
    - name: case-insensitive substring search
    - limit: integer, default 20, range 1–100
    - offset: integer, default 0, minimum 0
- No request body.

**Access:** shared catalogue, publicly accessible.

**Rules:**

- Sort by name ascending, then id ascending.
- `total` counts all matches before pagination.
- No matches or an offset beyond the results returns an empty items array.

**Success:**

- 200 OK + items, limit, offset, total.
- Each item contains id and name.

**Errors:**

- 400 Bad Request (`INVALID_QUERY_PARAMETERS`): invalid query parameters, such as a non-integer limit or offset, limit outside 1–100, or a negative offset.




## `POST /v1/sessions/{sessionId}/exercises`

**Request:**

- Path parameter: sessionId.
- JSON body: catalogueExerciseId (required string).

**Access:** authenticated user who owns the session.

**Resources:**

- The session must exist and belong to the user.
- The catalogue exercise must exist.

**Rules:**

- Exercises can only be added to an ACTIVE session.
- The same catalogue exercise cannot be added to the same session more than once.
- Save the catalogueExerciseId and name in a new exercise entry belonging to the session.
- Keep the saved name if the catalogue entry is later renamed. Retrieving sessions and logging sets for existing exercise entries do not require Catalogue to be available.
- Generate a globally unique exercise entry ID, returned as id.

**Dependencies:**

- GET /v1/exercises/{catalogueExerciseId} on Exercise Catalogue.

**Success:**

- 201 Created + created exercise entry.
- Location: `/v1/sessions/{sessionId}/exercises/{entryId}`.
- Entry contains id, catalogueExerciseId, and name.

**Errors:**

- 400 Bad Request (`MALFORMED_JSON`): malformed JSON.
- 401 Unauthorized (`UNAUTHENTICATED`): authentication is missing or invalid.
- 404 Not Found (`RESOURCE_NOT_FOUND`): session does not exist or belongs to another user.
- 409 Conflict (`SESSION_ALREADY_COMPLETED`): session is already completed.
- 409 Conflict (`EXERCISE_ALREADY_ADDED`): the same catalogue exercise is already in the session.
- 415 Unsupported Media Type (`UNSUPPORTED_MEDIA_TYPE`): the request Content-Type is not supported by this endpoint.
- 422 Unprocessable Content (`VALIDATION_FAILED`): wrong JSON structure, unknown fields, missing or invalid catalogueExerciseId, or referenced catalogue exercise does not exist.
- 503 Service Unavailable (`CATALOGUE_UNAVAILABLE`): catalogue is temporarily unavailable.




## `GET /v1/exercises/{catalogueExerciseId}`

**Request:**

- Path parameter: catalogueExerciseId.

**Access:** shared catalogue, publicly accessible.

**Resources:**

- The catalogue exercise must exist.

**Success:**

- 200 OK + an object containing id and name.

**Errors:**

- 404 Not Found (`RESOURCE_NOT_FOUND`): exercise does not exist.




## `POST /v1/session-exercises/{entryId}/sets`

**Request:**

- Path parameter: entryId.
- JSON body:
    - weightKg: required number, minimum 0
    - reps: required integer, minimum 1

**Access:** authenticated user who owns the session.

**Resources:**

- The session must exist and belong to the user.
- The exercise entry must exist and belong to the session.

**Rules:**

- Sets can only be added to an exercise in an ACTIVE session.
- Save the weight and reps in a new set entry.
- Generate a globally unique set ID, returned as id.

**Success:**

- 201 Created + created set entry.
- Location: `/v1/session-exercises/{entryId}/sets/{setId}`.
- Set contains id, weightKg, and reps.

**Errors:**

- 400 Bad Request (`MALFORMED_JSON`): malformed JSON.
- 401 Unauthorized (`UNAUTHENTICATED`): authentication is missing or invalid.
- 404 Not Found (`RESOURCE_NOT_FOUND`): session or exercise entry does not exist or belongs to another user.
- 409 Conflict (`SESSION_ALREADY_COMPLETED`): session is already completed.
- 415 Unsupported Media Type (`UNSUPPORTED_MEDIA_TYPE`): the request Content-Type is not supported by this endpoint.
- 422 Unprocessable Content (`VALIDATION_FAILED`): wrong JSON structure, unknown fields, or missing or invalid weightKg or reps.




## `PATCH /v1/sessions/{sessionId}`

**Request:**

- Path parameter: sessionId.
- Content-Type: `application/merge-patch+json`.
- JSON body: status, required string with value "COMPLETED".

**Access:** authenticated user who owns the session.

**Resources:**

- The session must exist and belong to the user.

**Rules:**

- The session must be ACTIVE.
- The session must contain at least one logged set at the moment of completion. This is a completion check, not a permanent requirement for completed sessions.
- Individual exercise entries may have no sets.
- The server sets finishedAt to its current timestamp.

**Success:**

- 200 OK + updated session entry.
- Response contains id, startedAt, finishedAt, and status.
- `status = COMPLETED`.

**Errors:**

- 400 Bad Request (`MALFORMED_JSON`): malformed JSON.
- 401 Unauthorized (`UNAUTHENTICATED`): authentication is missing or invalid.
- 404 Not Found (`RESOURCE_NOT_FOUND`): session does not exist or belongs to another user.
- 409 Conflict (`SESSION_ALREADY_COMPLETED`): session is already completed.
- 409 Conflict (`SESSION_HAS_NO_SETS`): session contains no logged sets.
- 415 Unsupported Media Type (`UNSUPPORTED_MEDIA_TYPE`): the request Content-Type is not supported by this endpoint.
- 422 Unprocessable Content (`VALIDATION_FAILED`): wrong JSON structure, unknown fields, or missing or invalid status.




## `PATCH /v1/session-exercises/{entryId}/sets/{setId}`

**Request:**

- Path parameters:
    - entryId
    - setId
- Content-Type: `application/merge-patch+json`.
- JSON body:
    - weightKg: number, minimum 0
    - reps: integer, minimum 1

**Access:** authenticated user who owns the session.

**Resources:**

- The session must exist and belong to the user.
- The exercise entry must exist and belong to the session.
- The set entry must exist and belong to the exercise entry.

**Rules:**

- Sets can be edited in ACTIVE and COMPLETED sessions.
- At least one of weightKg or reps must be present.
- Omitted fields keep their current values.
- Supplied fields must satisfy their constraints.
- Neither field may be null.

**Success:**

- 200 OK + updated set entry.
- Set entry contains id, weightKg, and reps.

**Errors:**

- 400 Bad Request (`MALFORMED_JSON`): malformed JSON.
- 401 Unauthorized (`UNAUTHENTICATED`): authentication is missing or invalid.
- 404 Not Found (`RESOURCE_NOT_FOUND`): a referenced resource is missing, inaccessible, or does not belong to the parent specified in the URL.
- 415 Unsupported Media Type (`UNSUPPORTED_MEDIA_TYPE`): the request Content-Type is not supported by this endpoint.
- 422 Unprocessable Content (`VALIDATION_FAILED`): wrong JSON structure, unknown fields, neither editable field supplied, or an invalid supplied value.




## `DELETE /v1/session-exercises/{entryId}/sets/{setId}`

**Request:**

- Path parameters:
    - entryId
    - setId

**Access:** authenticated user who owns the session.

**Resources:**

- The session must exist and belong to the user.
- The exercise entry must exist and belong to the session.
- The set entry must exist and belong to the exercise entry.

**Rules:**

- Sets can be deleted in ACTIVE and COMPLETED sessions.
- Deleting the last logged set is allowed. A completed session remains COMPLETED even if it becomes empty; it is not automatically deleted or reopened.

**Success:**

- 204 No Content.

**Errors:**

- 401 Unauthorized (`UNAUTHENTICATED`): authentication is missing or invalid.
- 404 Not Found (`RESOURCE_NOT_FOUND`): a referenced resource is missing, inaccessible, or does not belong to the parent specified in the URL.




## `DELETE /v1/sessions/{sessionId}/exercises/{entryId}`

**Request:**

- Path parameters:
    - sessionId
    - entryId

**Access:** authenticated user who owns the session.

**Resources:**

- The session must exist and belong to the user.
- The exercise entry must exist and belong to the session.

**Rules:**

- Exercise entries can be deleted in ACTIVE and COMPLETED sessions.
- Deleting an exercise entry also deletes all sets belonging to it.
- Removing the last exercise entry or the last logged sets is allowed. A completed session remains COMPLETED even if it becomes empty; it is not automatically deleted or reopened.

**Success:**

- 204 No Content.

**Errors:**

- 401 Unauthorized (`UNAUTHENTICATED`): authentication is missing or invalid.
- 404 Not Found (`RESOURCE_NOT_FOUND`): a referenced resource is missing, inaccessible, or does not belong to the parent specified in the URL.




## `GET /v1/sessions`

**Request:**

- Query parameters:
    - status: optional string, ACTIVE or COMPLETED; default COMPLETED
    - limit: optional integer, default 20, range 1–100
    - offset: optional integer, default 0, minimum 0
    - from: optional date in YYYY-MM-DD format
    - to: optional date in YYYY-MM-DD format

**Access:** authenticated user.

**Rules:**

- Return only the authenticated user's sessions matching the selected status.
- If status is omitted, return COMPLETED sessions.
- With status=ACTIVE, there is at most one matching session, so the items array contains zero or one element. Pagination and date filters still apply.
- `from` and `to` are inclusive UTC dates applied only to startedAt. The whole session does not have to fit within the range; finishedAt does not affect filtering.
- When supplied, from includes sessions starting at or after 00:00:00 UTC on that date; to includes sessions starting before 00:00:00 UTC on the following date.
- Either date filter may be supplied on its own.
- When both are supplied, from must be earlier than or equal to to.
- Sort by startedAt descending, then id ascending.
- `total` counts all matches before pagination.
- No matches or offset beyond results returns empty items.

**Success:**

- 200 OK + items, limit, offset, total.
- Each item contains id, startedAt, finishedAt, status.
- finishedAt is null for ACTIVE sessions.

**Errors:**

- 400 Bad Request (`INVALID_QUERY_PARAMETERS`): invalid status, invalid pagination, invalid date format, or from later than to.
- 401 Unauthorized (`UNAUTHENTICATED`): authentication is missing or invalid.




## `GET /v1/sessions/{sessionId}`

**Request:**

- Path parameters:
    - sessionId

**Access:** authenticated user who owns the session.

**Resources:**

- The session must exist and belong to the user.

**Rules:**

- ACTIVE and COMPLETED sessions can be retrieved.
- Return all exercise entries with all their sets; neither nested array is paginated.
- Return exercise entries in the order they were added to the session.
- Return sets in the order they were added to their exercise entry.
- Editing or deleting an entry does not change the relative order of the remaining entries.

**Success:**

- 200 OK + id, startedAt, finishedAt, status, exercises.
- Each exercise entry contains id, catalogueExerciseId, name, sets.
- Each set entry contains id, weightKg, reps.
- `finishedAt` is `null` for an ACTIVE session.

**Errors:**

- 401 Unauthorized (`UNAUTHENTICATED`): authentication is missing or invalid.
- 404 Not Found (`RESOURCE_NOT_FOUND`): session does not exist or belongs to another user.




## `DELETE /v1/sessions/{sessionId}`

**Request:**

- Path parameters:
    - sessionId

**Access:** authenticated user who owns the session.

**Resources:**

- The session must exist and belong to the user.

**Rules:**

- ACTIVE and COMPLETED sessions can be deleted.
- Deleting a session also deletes all its exercise entries and their sets.

**Success:**

- 204 No Content.

**Errors:**

- 401 Unauthorized (`UNAUTHENTICATED`): authentication is missing or invalid.
- 404 Not Found (`RESOURCE_NOT_FOUND`): session does not exist or belongs to another user.
