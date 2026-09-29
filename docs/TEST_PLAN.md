# QA Test Plan: User Management & Authentication REST API

## 1. Overview & Objectives
This document defines the comprehensive Quality Assurance Test Plan for the **User Management & Authentication REST API**. The objective is to validate that the backend services adhere to functional, performance, security, and data-integrity requirements, while ensuring high reliability and defect-free execution across all endpoints.

---

## 2. Scope

### 2.1 In-Scope
- **Authentication & Authorization (`/api/auth`)**:
  - User registration (`POST /api/auth/register`) with field validations, case-insensitive email uniqueness, and password strength checks.
  - User login (`POST /api/auth/login`) with credential verification, JWT token issuance, and expiration handling.
- **User Resource Management (`/api/users`)**:
  - User creation (`POST /api/users`) by authorized clients.
  - User retrieval (`GET /api/users`, `GET /api/users/{id}`) including list fetching, single entity lookups, and query parameter search/filtering.
  - User update (`PUT /api/users/{id}`) ensuring partial/full attribute updates and collision prevention.
  - User deletion (`DELETE /api/users/{id}`) and post-deletion integrity verification.
- **Security & Token Enforcement**:
  - JWT Bearer token authentication filter.
  - Rejection of expired, tampered, malformed, or missing authentication tokens.
  - Role-based attributes verification.
- **API Error Handling & Schema Compliance**:
  - Consistent HTTP status code mapping (`200`, `201`, `204`, `400`, `401`, `404`, `409`).
  - Standardized JSON `ErrorResponse` schema across all fault scenarios.

### 2.2 Out-of-Scope
- Third-party OAuth2 single-sign-on (Google/GitHub SSO).
- Live email delivery verification (SMTP/SES server integration).
- Distributed rate limiting and volumetric DDoS resilience.
- Web frontend UI testing (this repository focuses strictly on Backend REST API & QA Automation).

---

## 3. Testing Approach & Methodologies

### 3.1 Functional Testing
Validates that each API endpoint meets the specified business rules and requirements.
- Positive happy-path test cases ensure users can be registered, logged in, listed, modified, and deleted.
- Data persistence and field mapping checks ensure database entities match API response DTOs.

### 3.2 Automated API Testing
All test cases are codified in Java using **REST Assured** and **JUnit 5**, integrated into the Maven build lifecycle.
- **Isolated Execution**: Tests run against a dynamic random port (`@SpringBootTest(webEnvironment = RANDOM_PORT)`), eliminating environment collisions.
- **Data Isolation**: Dynamic test data generation ensures each test is fully independent and idempotent.
- **Fluent Assertions**: Combining REST Assured Hamcrest matchers and **AssertJ** assertions for rich validation.

### 3.3 Negative & Boundary Testing
Exhaustive verification of failure handling:
- Null, empty, and whitespace-only field submissions.
- Field length boundary analysis (e.g., minimum 3 characters, maximum 50 characters for usernames).
- Malformed syntax (invalid JSON, truncated payloads, malformed email addresses).
- Invalid types (alphanumeric strings passed into numerical ID parameters).

### 3.4 Security Testing
- Verification of token requirement across all `/api/users/**` endpoints.
- Rejection of invalid, tampered, or malformed JWT signatures.
- Verification that sensitive data (passwords, password hashes) are **never** leaked in API responses.

### 3.5 Regression Testing
A dedicated regression suite verifies that previously resolved defects (documented in `docs/BUG_REPORTS.md`) do not re-emerge in future releases.

---

## 4. Test Environment & Tools

| Component | Tool / Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Language** | Java | 21 LTS | Test automation & backend runtime |
| **Framework** | Spring Boot | 3.3.4 | REST API backend service |
| **Test Runner** | JUnit 5 (Jupiter) | 5.10.x | Test execution and lifecycle management |
| **HTTP Client** | REST Assured | 5.4.0 | Fluent API request construction & validation |
| **Assertions** | AssertJ & Hamcrest | 3.25.x | Detailed, readable assertion statements |
| **Reporting** | Allure Report | 2.27.0 | Visual test execution dashboards & attachments |
| **Manual Testing** | Postman | v10+ | Manual exploration & standalone collection runs |
| **CI/CD** | GitHub Actions | Ubuntu Latest | Continuous integration and test gate |

---

## 5. Risks & Mitigation Strategies

| Risk | Impact | Mitigation Strategy |
| :--- | :--- | :--- |
| **Test Data Collisions** | High (flaky tests due to duplicate emails/usernames) | Use `TestDataGenerator` to generate unique timestamps and UUIDs for every test execution. |
| **Port Conflicts** | Medium (build failures on shared CI runners) | Use Spring Boot random port allocation (`RANDOM_PORT`) dynamically injected into REST Assured. |
| **Database State Pollution** | High (dependent test failures) | Use isolated in-memory H2 database with automatic schema recreation per run. |
| **Flaky Token Expirations** | Low | Configure test JWT token expiration to 24 hours to prevent mid-test expiry. |

---

## 6. Entry & Exit Criteria

### 6.1 Entry Criteria
1. Backend application source code compiles cleanly without warnings.
2. Maven dependencies and test plugins are successfully resolved.
3. Database migration / DDL auto-creation operates without runtime errors.
4. Test configuration parameters (base URL, default credentials) are loaded.

### 6.2 Exit Criteria
1. **100% Pass Rate**: All automated JUnit 5 tests must execute and pass without failures or errors.
2. **Zero Unhandled Defects**: No open critical or high-severity defects.
3. **Regression Integrity**: All documented bug reports verified and covered by passing regression tests.
4. **Report Generation**: Allure test results generated and verified.
5. **CI/CD Pipeline**: GitHub Actions build runs cleanly and turns green.
