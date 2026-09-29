# QA Test Cases: User Management & Authentication REST API

This document contains detailed formal Quality Assurance test cases mapped directly to the automated test suite and Postman collection.

---

## Summary Matrix

| Module | Test Case ID | Description | Type | Priority | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Auth** | `TC-AUTH-001` | Register user with valid attributes | Positive | Blocker | **PASSED** |
| **Auth** | `TC-AUTH-002` | Reject registration with invalid email syntax | Negative | Normal | **PASSED** |
| **Auth** | `TC-AUTH-003` | Reject registration with empty username | Negative | Normal | **PASSED** |
| **Auth** | `TC-AUTH-004` | Reject registration with empty password | Negative | Normal | **PASSED** |
| **Auth** | `TC-AUTH-005` | Reject registration with password shorter than 8 characters | Boundary | Normal | **PASSED** |
| **Auth** | `TC-AUTH-006` | Reject registration with duplicate email address | Business Rule | Critical | **PASSED** |
| **Auth** | `TC-AUTH-007` | Reject duplicate email case-insensitively (`BUG-001`) | Regression | Critical | **PASSED** |
| **Auth** | `TC-AUTH-008` | Reject registration with whitespace-only username (`BUG-004`) | Regression | Normal | **PASSED** |
| **Auth** | `TC-AUTH-009` | Authenticate user with valid credentials | Positive | Blocker | **PASSED** |
| **Auth** | `TC-AUTH-010` | Reject login with non-existent email | Negative | Critical | **PASSED** |
| **Auth** | `TC-AUTH-011` | Reject login with incorrect password | Negative | Critical | **PASSED** |
| **Users** | `TC-USER-001` | Create user when authenticated with Bearer token | Positive | Blocker | **PASSED** |
| **Users** | `TC-USER-002` | Reject user creation with duplicate email address | Negative | Critical | **PASSED** |
| **Users** | `TC-USER-003` | Reject user creation without authentication header | Security | Blocker | **PASSED** |
| **Users** | `TC-USER-004` | Retrieve all users list when authenticated | Positive | Blocker | **PASSED** |
| **Users** | `TC-USER-005` | Retrieve existing user by ID | Positive | Critical | **PASSED** |
| **Users** | `TC-USER-006` | Filter users list using `search` query parameter | Positive | Normal | **PASSED** |
| **Users** | `TC-USER-007` | Retrieve non-existent user ID returns 404 | Negative | Normal | **PASSED** |
| **Users** | `TC-USER-008` | Retrieve user with alphanumeric ID returns 400 | Negative | Normal | **PASSED** |
| **Users** | `TC-USER-009` | Verify password field is never exposed in responses | Security | Critical | **PASSED** |
| **Users** | `TC-USER-010` | Update user details (username, email, role) | Positive | Blocker | **PASSED** |
| **Users** | `TC-USER-011` | Allow user to update username while retaining own email (`BUG-002`) | Regression | Critical | **PASSED** |
| **Users** | `TC-USER-012` | Reject user update with email already assigned to another user | Negative | Critical | **PASSED** |
| **Users** | `TC-USER-013` | Delete existing user by ID returns 204 No Content | Positive | Blocker | **PASSED** |
| **Users** | `TC-USER-014` | Delete non-existent user returns 404 Not Found (`BUG-003`) | Regression | Normal | **PASSED** |
| **Users** | `TC-USER-015` | Verify deleted user cannot be retrieved via GET | End-to-End | Critical | **PASSED** |
| **Security** | `TC-SEC-001` | Reject protected endpoint request without token with standard JSON (`BUG-005`) | Security | Blocker | **PASSED** |
| **Security** | `TC-SEC-002` | Reject protected endpoint request with malformed JWT | Security | Critical | **PASSED** |
| **Security** | `TC-SEC-003` | Reject protected endpoint request with tampered JWT signature | Security | Critical | **PASSED** |
| **Edge** | `TC-EDGE-001` | Reject username exceeding maximum length (50 chars) | Boundary | Normal | **PASSED** |
| **Edge** | `TC-EDGE-002` | Reject malformed JSON request body with 400 Bad Request | Negative | Critical | **PASSED** |

---

## Detailed Test Case Specifications

### TC-AUTH-001: Register user with valid attributes
- **Module**: Authentication
- **Preconditions**: Target email does not exist in the database.
- **Steps**:
  1. Construct `POST /api/auth/register` payload with valid `username`, `email`, and `password` (>= 8 chars).
  2. Send request to endpoint with `Content-Type: application/json`.
- **Expected Result**:
  - HTTP Status: `201 Created`
  - Response body contains numeric `id`, matching `username`, normalized `email`, default `role: "USER"`, and timestamps.
  - `password` field is not present in response.
- **Actual Result**: `201 Created` with valid `UserResponse` JSON payload.
- **Status**: **PASSED**

---

### TC-AUTH-002: Reject registration with invalid email syntax
- **Module**: Authentication
- **Preconditions**: None.
- **Steps**:
  1. Construct `POST /api/auth/register` with `email: "invalid-email-format"`.
  2. Send request with valid `username` and `password`.
- **Expected Result**:
  - HTTP Status: `400 Bad Request`
  - Response contains `validationErrors.email: "Email format is invalid"`.
- **Actual Result**: `400 Bad Request` with field error in `validationErrors`.
- **Status**: **PASSED**

---

### TC-AUTH-005: Reject registration with password shorter than 8 characters
- **Module**: Authentication
- **Preconditions**: None.
- **Steps**:
  1. Construct `POST /api/auth/register` with `password: "abc12"` (5 characters).
  2. Send request with valid `username` and `email`.
- **Expected Result**:
  - HTTP Status: `400 Bad Request`
  - Error message specifies minimum length constraint of 8 characters.
- **Actual Result**: `400 Bad Request` with message containing `"Password must be at least 8 characters long"`.
- **Status**: **PASSED**

---

### TC-AUTH-006: Reject registration with duplicate email address
- **Module**: Authentication
- **Preconditions**: A user is already registered with email `duplicate_test@example.com`.
- **Steps**:
  1. Send `POST /api/auth/register` with the same email `duplicate_test@example.com`.
- **Expected Result**:
  - HTTP Status: `409 Conflict`
  - Response JSON includes `status: 409`, `error: "Conflict"`, and message `"Email is already registered: duplicate_test@example.com"`.
- **Actual Result**: `409 Conflict` with standard error payload.
- **Status**: **PASSED**

---

### TC-AUTH-007: Reject duplicate email case-insensitively [Regression BUG-001]
- **Module**: Authentication
- **Preconditions**: User registered with lowercase email `user@example.com`.
- **Steps**:
  1. Send `POST /api/auth/register` with uppercase email `USER@EXAMPLE.COM`.
- **Expected Result**:
  - HTTP Status: `409 Conflict` (case-insensitive check prevents duplicate account).
- **Actual Result**: `409 Conflict` returned successfully.
- **Status**: **PASSED**

---

### TC-AUTH-008: Reject registration with whitespace-only username [Regression BUG-004]
- **Module**: Authentication
- **Preconditions**: None.
- **Steps**:
  1. Send `POST /api/auth/register` with `username: "    "` (spaces only).
- **Expected Result**:
  - HTTP Status: `400 Bad Request`
  - Field error for `username` indicating blank string violation.
- **Actual Result**: `400 Bad Request` with validation error on `username`.
- **Status**: **PASSED**

---

### TC-AUTH-009: Authenticate user with valid credentials
- **Module**: Authentication
- **Preconditions**: User exists with registered email and password.
- **Steps**:
  1. Send `POST /api/auth/login` with correct `email` and `password`.
- **Expected Result**:
  - HTTP Status: `200 OK`
  - Response contains non-empty `token` string, `tokenType: "Bearer"`, and `expiresIn > 0`.
- **Actual Result**: `200 OK` with valid JWT token.
- **Status**: **PASSED**

---

### TC-AUTH-010: Reject login with non-existent email
- **Module**: Authentication
- **Preconditions**: Email `nonexistent@test.com` is not in database.
- **Steps**:
  1. Send `POST /api/auth/login` with unmapped email.
- **Expected Result**:
  - HTTP Status: `401 Unauthorized`
  - Response contains message `"Invalid email or password"`.
- **Actual Result**: `401 Unauthorized` returned with generic error message.
- **Status**: **PASSED**

---

### TC-USER-001: Create user when authenticated with Bearer token
- **Module**: User Management
- **Preconditions**: Valid admin Bearer token obtained.
- **Steps**:
  1. Set header `Authorization: Bearer <valid_token>`.
  2. Send `POST /api/users` with valid `UserCreateRequest` body.
- **Expected Result**:
  - HTTP Status: `201 Created`
  - Response contains generated `id`, `username`, `email`, and `role`.
- **Actual Result**: `201 Created` returned.
- **Status**: **PASSED**

---

### TC-USER-003: Reject user creation without authentication header
- **Module**: User Management / Security
- **Preconditions**: No `Authorization` header provided.
- **Steps**:
  1. Send `POST /api/users` with valid payload but without credentials.
- **Expected Result**:
  - HTTP Status: `401 Unauthorized`
- **Actual Result**: `401 Unauthorized` returned.
- **Status**: **PASSED**

---

### TC-USER-004: Retrieve all users list when authenticated
- **Module**: User Management
- **Preconditions**: Valid Bearer token; database contains seeded users.
- **Steps**:
  1. Send `GET /api/users` with `Authorization: Bearer <token>`.
- **Expected Result**:
  - HTTP Status: `200 OK`
  - Response body is a JSON array with user objects.
- **Actual Result**: `200 OK` with array of users.
- **Status**: **PASSED**

---

### TC-USER-005: Retrieve existing user by ID
- **Module**: User Management
- **Preconditions**: Existing user with `id = X`.
- **Steps**:
  1. Send `GET /api/users/{id}` with Bearer token.
- **Expected Result**:
  - HTTP Status: `200 OK`
  - Response `id` matches requested `id`.
- **Actual Result**: `200 OK` with matching user record.
- **Status**: **PASSED**

---

### TC-USER-006: Filter users list using search query parameter
- **Module**: User Management
- **Preconditions**: User with known unique username exists.
- **Steps**:
  1. Send `GET /api/users?search=<keyword>` with Bearer token.
- **Expected Result**:
  - HTTP Status: `200 OK`
  - Response array contains only users matching the keyword in username or email.
- **Actual Result**: `200 OK` with filtered results.
- **Status**: **PASSED**

---

### TC-USER-007: Retrieve non-existent user ID returns 404
- **Module**: User Management
- **Preconditions**: ID `999999` does not exist in database.
- **Steps**:
  1. Send `GET /api/users/999999` with Bearer token.
- **Expected Result**:
  - HTTP Status: `404 Not Found`
  - Message: `"User not found with id: 999999"`
- **Actual Result**: `404 Not Found` with standard error response.
- **Status**: **PASSED**

---

### TC-USER-008: Retrieve user with alphanumeric ID returns 400
- **Module**: User Management
- **Preconditions**: None.
- **Steps**:
  1. Send `GET /api/users/invalid-id-string` with Bearer token.
- **Expected Result**:
  - HTTP Status: `400 Bad Request`
  - Error indicates type mismatch on parameter `id`.
- **Actual Result**: `400 Bad Request` returned.
- **Status**: **PASSED**

---

### TC-USER-009: Verify password field is never exposed in responses
- **Module**: User Management / Security
- **Preconditions**: User records exist.
- **Steps**:
  1. Send `GET /api/users` with Bearer token.
  2. Inspect every element in the JSON array for the `password` field.
- **Expected Result**:
  - Field `password` is absent or null for every item.
- **Actual Result**: Password field is absent from response DTO.
- **Status**: **PASSED**

---

### TC-USER-010: Update user details (username, email, role)
- **Module**: User Management
- **Preconditions**: User exists with `id = X`.
- **Steps**:
  1. Send `PUT /api/users/{id}` with new `username`, new `email`, and `role: "ADMIN"`.
- **Expected Result**:
  - HTTP Status: `200 OK`
  - Response reflects updated attributes.
- **Actual Result**: `200 OK` with updated values.
- **Status**: **PASSED**

---

### TC-USER-011: Allow user to update username while retaining own email [Regression BUG-002]
- **Module**: User Management
- **Preconditions**: User exists with email `test@example.com`.
- **Steps**:
  1. Send `PUT /api/users/{id}` updating only `username`, passing the existing email `test@example.com`.
- **Expected Result**:
  - HTTP Status: `200 OK` (own email does NOT trigger 409 conflict).
- **Actual Result**: `200 OK` returned; username updated.
- **Status**: **PASSED**

---

### TC-USER-012: Reject user update with email already assigned to another user
- **Module**: User Management
- **Preconditions**: User A (`email_a@test.com`) and User B (`email_b@test.com`) exist.
- **Steps**:
  1. Send `PUT /api/users/{id_B}` with `email: "email_a@test.com"`.
- **Expected Result**:
  - HTTP Status: `409 Conflict`
  - Message: `"Email is already in use by another user: email_a@test.com"`.
- **Actual Result**: `409 Conflict` returned.
- **Status**: **PASSED**

---

### TC-USER-013: Delete existing user by ID returns 204 No Content
- **Module**: User Management
- **Preconditions**: Target user exists in database.
- **Steps**:
  1. Send `DELETE /api/users/{id}` with Bearer token.
- **Expected Result**:
  - HTTP Status: `204 No Content`
  - Empty response body.
- **Actual Result**: `204 No Content` returned.
- **Status**: **PASSED**

---

### TC-USER-014: Delete non-existent user returns 404 Not Found [Regression BUG-003]
- **Module**: User Management
- **Preconditions**: User ID `777777` does not exist.
- **Steps**:
  1. Send `DELETE /api/users/777777` with Bearer token.
- **Expected Result**:
  - HTTP Status: `404 Not Found`
  - Message: `"User not found with id: 777777"`.
- **Actual Result**: `404 Not Found` returned.
- **Status**: **PASSED**

---

### TC-USER-015: Verify deleted user cannot be retrieved via GET
- **Module**: User Management / End-to-End
- **Preconditions**: User created and then deleted via `DELETE /api/users/{id}`.
- **Steps**:
  1. Send `GET /api/users/{deleted_id}` with Bearer token.
- **Expected Result**:
  - HTTP Status: `404 Not Found`.
- **Actual Result**: `404 Not Found` confirmed.
- **Status**: **PASSED**

---

### TC-SEC-001: Reject protected endpoint request without token with standard JSON [Regression BUG-005]
- **Module**: Security
- **Preconditions**: None.
- **Steps**:
  1. Send `GET /api/users` without `Authorization` header.
- **Expected Result**:
  - HTTP Status: `401 Unauthorized`
  - `Content-Type: application/json`
  - Response body conforms to `ErrorResponse` schema with `status: 401`, `error: "Unauthorized"`, and valid `timestamp`.
- **Actual Result**: `401 Unauthorized` with structured JSON error.
- **Status**: **PASSED**

---

### TC-SEC-002: Reject protected endpoint request with malformed JWT
- **Module**: Security
- **Preconditions**: None.
- **Steps**:
  1. Send `GET /api/users` with header `Authorization: Bearer malformed.jwt.token`.
- **Expected Result**:
  - HTTP Status: `401 Unauthorized`.
- **Actual Result**: `401 Unauthorized` returned.
- **Status**: **PASSED**

---

### TC-EDGE-001: Reject username exceeding maximum length (50 chars)
- **Module**: Boundary Value Analysis
- **Preconditions**: None.
- **Steps**:
  1. Send `POST /api/auth/register` with a 55-character username.
- **Expected Result**:
  - HTTP Status: `400 Bad Request`
  - Validation error specifies character range (3 to 50 characters).
- **Actual Result**: `400 Bad Request` returned with length constraint message.
- **Status**: **PASSED**

---

### TC-EDGE-002: Reject malformed JSON request body with 400 Bad Request
- **Module**: Input Validation
- **Preconditions**: None.
- **Steps**:
  1. Send `POST /api/auth/register` with unclosed JSON syntax (`{"username": "test", "email": ...`).
- **Expected Result**:
  - HTTP Status: `400 Bad Request`
  - Response message: `"Malformed JSON request body or unreadable input"`.
- **Actual Result**: `400 Bad Request` returned with descriptive message.
- **Status**: **PASSED**
