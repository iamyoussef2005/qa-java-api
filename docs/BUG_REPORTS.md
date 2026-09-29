# QA Defect Log & Bug Reports

This document tracks realistic defects identified during the Quality Assurance testing lifecycle of the **User Management & Authentication REST API**. 

It demonstrates the full QA lifecycle: **Test &rarr; Identify Defect &rarr; Document &rarr; Root Cause Analysis &rarr; Fix &rarr; Regression Retest &rarr; Verify Closure**.

---

## Defect Summary Dashboard

| Bug ID | Title | Severity | Priority | Status | Regression Test |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **BUG-001** | Duplicate email accepted during registration when case differs | High | High | **CLOSED (VERIFIED)** | `TC-AUTH-007` |
| **BUG-002** | User update endpoint throws false 409 Conflict when retaining current email | Medium | High | **CLOSED (VERIFIED)** | `TC-USER-011` |
| **BUG-003** | Deletion of non-existent user returns success instead of 404 Not Found | Medium | Medium | **CLOSED (VERIFIED)** | `TC-USER-014` |
| **BUG-004** | Whitespace-only username strings accepted during user registration | Low | Medium | **CLOSED (VERIFIED)** | `TC-AUTH-008` |
| **BUG-005** | Missing authentication header triggers empty 403 instead of standard 401 JSON | High | Critical | **CLOSED (VERIFIED)** | `TC-SEC-001` |

---

## Detailed Bug Reports

### BUG-001: Duplicate email accepted during registration when case differs

- **Bug ID**: `BUG-001`
- **Title**: Duplicate email registration permitted when letter case differs
- **Environment**: Local Test Environment / Spring Boot 3.3.4 / In-Memory H2 DB
- **Severity**: High
- **Priority**: High
- **Status**: **CLOSED (VERIFIED)**
- **Reported By**: QA Automation Suite
- **Date Logged**: 2026-09-29

#### Description
The registration endpoint `/api/auth/register` accepted duplicate account registrations if the user submitted an email address differing only by character case (e.g., `user@example.com` followed by `USER@EXAMPLE.COM`). This violates RFC 5321 email uniqueness conventions and allows account spoofing / duplicate collisions.

#### Steps to Reproduce
1. Send `POST /api/auth/register` with payload:
   ```json
   {
     "username": "john_doe",
     "email": "john.doe@example.com",
     "password": "Password123!"
   }
   ```
   *(Response: 201 Created)*
2. Send a second `POST /api/auth/register` with payload:
   ```json
   {
     "username": "john_duplicate",
     "email": "JOHN.DOE@EXAMPLE.COM",
     "password": "Password123!"
   }
   ```

#### Expected Result
- HTTP Status: `409 Conflict`
- Body: Standard `ErrorResponse` stating `"Email is already registered: john.doe@example.com"`.

#### Actual Result (Prior to Fix)
- HTTP Status: `201 Created`
- Duplicate account was persisted in the database.

#### Root Cause Analysis
`UserRepository` performed exact string matching using default JPA `existsByEmail(String email)` instead of case-insensitive comparison, and incoming emails were not normalized to lowercase prior to persistence.

#### Resolution & Verification
1. Replaced query with `existsByEmailIgnoreCase(String email)`.
2. Added normalization in `AuthService` and entity `@PrePersist` / `@PreUpdate` hooks via `.trim().toLowerCase()`.
3. Added automated regression test: `AuthRegistrationTests.shouldRejectRegistrationWithDuplicateEmailCaseInsensitive()`.
4. **Verification**: Automated regression test runs green on every build.

---

### BUG-002: User update endpoint throws false 409 Conflict when retaining current email

- **Bug ID**: `BUG-002`
- **Title**: User update endpoint throws false 409 Conflict when updating username while retaining existing email
- **Environment**: Local Test Environment / Spring Boot 3.3.4
- **Severity**: Medium
- **Priority**: High
- **Status**: **CLOSED (VERIFIED)**
- **Reported By**: QA Automation Suite
- **Date Logged**: 2026-09-29

#### Description
When an existing user submits a `PUT /api/users/{id}` to update their username or profile attributes while keeping their current email in the request payload, the API mistakenly flagged their own email as a duplicate collision and returned `409 Conflict`.

#### Steps to Reproduce
1. Register user with ID `14` and email `user14@example.com`.
2. Send `PUT /api/users/14` with payload:
   ```json
   {
     "username": "new_username",
     "email": "user14@example.com"
   }
   ```

#### Expected Result
- HTTP Status: `200 OK`
- User's username is updated to `"new_username"`, retaining `"user14@example.com"`.

#### Actual Result (Prior to Fix)
- HTTP Status: `409 Conflict`
- Error: `"Email is already in use by another user: user14@example.com"`.

#### Root Cause Analysis
The uniqueness validation in `UserService.updateUser()` checked `userRepository.existsByEmailIgnoreCase(email)` globally across all users without excluding the ID of the user currently being updated.

#### Resolution & Verification
1. Introduced custom repository query:
   `boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);`
2. Updated `UserService` to verify email uniqueness only against *other* records (`id != currentId`).
3. Added automated regression test: `UserUpdateTests.shouldAllowUserToKeepOwnEmailOnUpdate()`.
4. **Verification**: Tested and passed in continuous integration.

---

### BUG-003: Deletion of non-existent user returns success instead of 404 Not Found

- **Bug ID**: `BUG-003`
- **Title**: Deleting a non-existent user ID returns success instead of 404 Not Found
- **Environment**: Local Test Environment / Spring Boot 3.3.4
- **Severity**: Medium
- **Priority**: Medium
- **Status**: **CLOSED (VERIFIED)**
- **Reported By**: QA Automation Suite
- **Date Logged**: 2026-09-29

#### Description
When client sends `DELETE /api/users/999999` for an ID that does not exist in the database, the endpoint returned HTTP `204 No Content` or `200 OK` silently rather than informing the client that the targeted resource does not exist.

#### Steps to Reproduce
1. Send authenticated `DELETE /api/users/777777` where `777777` does not exist.

#### Expected Result
- HTTP Status: `404 Not Found`
- Error payload:
  ```json
  {
    "status": 404,
    "error": "Not Found",
    "message": "User not found with id: 777777"
  }
  ```

#### Actual Result (Prior to Fix)
- HTTP Status: `204 No Content` (silent no-op).

#### Root Cause Analysis
`userRepository.deleteById(id)` was invoked directly without checking entity existence via `userRepository.existsById(id)`.

#### Resolution & Verification
1. Added existence validation prior to deletion:
   ```java
   if (!userRepository.existsById(id)) {
       throw new ResourceNotFoundException("User not found with id: " + id);
   }
   ```
2. Added regression test: `UserDeletionTests.shouldReturn404WhenDeletingNonexistentUser()`.
3. **Verification**: Automated test verifies `404 Not Found` status and descriptive error message.

---

### BUG-004: Whitespace-only username strings accepted during user registration

- **Bug ID**: `BUG-004`
- **Title**: Registration endpoint accepts whitespace-only usernames
- **Environment**: Local Test Environment / Spring Boot 3.3.4
- **Severity**: Low
- **Priority**: Medium
- **Status**: **CLOSED (VERIFIED)**
- **Reported By**: QA Automation Suite
- **Date Logged**: 2026-09-29

#### Description
Submitting a username containing only blank whitespace characters (e.g. `"    "`) bypassed basic length checks and allowed creating invisible / ghost usernames.

#### Steps to Reproduce
1. Send `POST /api/auth/register` with:
   ```json
   {
     "username": "    ",
     "email": "ghost@example.com",
     "password": "Password123!"
   }
   ```

#### Expected Result
- HTTP Status: `400 Bad Request`
- Validation error indicating `username` cannot be blank and must match allowed alphanumeric characters.

#### Actual Result (Prior to Fix)
- HTTP Status: `201 Created` with blank username.

#### Root Cause Analysis
Validation annotations lacked strict `@NotBlank` and regex pattern enforcement against whitespace characters.

#### Resolution & Verification
1. Updated `RegisterRequest` and `UserCreateRequest` with `@NotBlank` and regex `@Pattern(regexp = "^[a-zA-Z0-9_.-]+$")`.
2. Added regression test: `AuthRegistrationTests.shouldRejectRegistrationWithWhitespaceOnlyUsername()`.
3. **Verification**: Automated test passes consistently.

---

### BUG-005: Missing authentication header triggers empty 403 instead of standard 401 JSON

- **Bug ID**: `BUG-005`
- **Title**: Unauthenticated access to protected endpoints yields default 403 instead of standardized 401 JSON
- **Environment**: Local Test Environment / Spring Boot 3.3.4 / Spring Security 6
- **Severity**: High
- **Priority**: Critical
- **Status**: **CLOSED (VERIFIED)**
- **Reported By**: QA Automation Suite
- **Date Logged**: 2026-09-29

#### Description
Requests to protected endpoints (e.g. `GET /api/users`) without an `Authorization` header returned default Spring Security response code `403 Forbidden` with an empty body rather than `401 Unauthorized` with our standardized `ErrorResponse` JSON. This violates REST API standards and broke client-side error parsing.

#### Steps to Reproduce
1. Send `GET /api/users` with no `Authorization` header.

#### Expected Result
- HTTP Status: `401 Unauthorized`
- `Content-Type: application/json`
- Body matches standardized schema:
  ```json
  {
    "timestamp": "2026-09-29T21:00:00",
    "status": 401,
    "error": "Unauthorized",
    "message": "Full authentication is required to access this resource. Provide a valid Bearer token.",
    "path": "/api/users"
  }
  ```

#### Actual Result (Prior to Fix)
- HTTP Status: `403 Forbidden` with empty body.

#### Root Cause Analysis
Spring Security defaulted unauthenticated filter failures to `403 Forbidden` because no custom `AuthenticationEntryPoint` was registered in `SecurityConfig`.

#### Resolution & Verification
1. Implemented `JwtAuthenticationEntryPoint` implementing Spring's `AuthenticationEntryPoint`.
2. Configured `http.exceptionHandling(e -> e.authenticationEntryPoint(jwtAuthenticationEntryPoint))` in `SecurityConfig`.
3. Added automated regression test: `AuthenticationSecurityTests.shouldReturn401WhenAccessingProtectedEndpointWithoutToken()`.
4. **Verification**: Test validates status `401`, JSON content-type, and all required JSON payload keys.
