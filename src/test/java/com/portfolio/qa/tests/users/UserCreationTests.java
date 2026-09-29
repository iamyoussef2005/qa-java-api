package com.portfolio.qa.tests.users;

import com.portfolio.qa.config.BaseTest;
import com.portfolio.qa.dto.ErrorResponse;
import com.portfolio.qa.dto.UserCreateRequest;
import com.portfolio.qa.dto.UserResponse;
import com.portfolio.qa.testdata.TestDataGenerator;
import com.portfolio.qa.testdata.UserTestData;
import com.portfolio.qa.utils.AuthTokenManager;
import com.portfolio.qa.utils.RequestSpecs;
import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@Epic("User Management")
@Feature("Create User")
@Tag("users")
public class UserCreationTests extends BaseTest {

    @Test
    @DisplayName("TC-USER-001: Create user with valid data when authenticated returns 201 Created")
    @Story("Positive User Creation")
    @Severity(SeverityLevel.BLOCKER)
    public void shouldCreateUserWithValidDataWhenAuthenticated() {
        String token = AuthTokenManager.getAdminToken();
        UserCreateRequest request = UserTestData.validUserCreateRequest();

        UserResponse response = given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .body(request)
        .when()
                .post("/api/users")
        .then()
                .statusCode(201)
                .contentType("application/json")
                .body("id", notNullValue())
                .body("username", equalTo(request.getUsername()))
                .body("email", equalTo(request.getEmail().toLowerCase()))
                .body("role", equalTo("USER"))
                .body("password", nullValue())
                .extract()
                .as(UserResponse.class);

        assertThat(response.getId()).isPositive();
        assertThat(response.getUsername()).isEqualTo(request.getUsername());
        assertThat(response.getEmail()).isEqualTo(request.getEmail().toLowerCase());
    }

    @Test
    @DisplayName("TC-USER-002: Create user should reject duplicate email with 409 Conflict")
    @Story("Negative User Creation")
    @Severity(SeverityLevel.CRITICAL)
    public void shouldRejectUserCreationWithDuplicateEmail() {
        String token = AuthTokenManager.getAdminToken();
        UserCreateRequest request = UserTestData.validUserCreateRequest();

        // First creation succeeds
        given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .body(request)
                .post("/api/users")
                .then()
                .statusCode(201);

        // Second creation with the same email fails
        ErrorResponse error = given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .body(request)
        .when()
                .post("/api/users")
        .then()
                .statusCode(409)
                .contentType("application/json")
                .body("status", equalTo(409))
                .body("error", equalTo("Conflict"))
                .body("message", containsString("Email is already registered"))
                .extract()
                .as(ErrorResponse.class);

        assertThat(error.getStatus()).isEqualTo(409);
        assertThat(error.getMessage()).contains(request.getEmail().toLowerCase());
    }

    @Test
    @DisplayName("TC-USER-003: Create user should reject missing required fields with 400 Bad Request")
    @Story("Negative User Creation")
    @Severity(SeverityLevel.NORMAL)
    public void shouldRejectUserCreationWithMissingRequiredFields() {
        String token = AuthTokenManager.getAdminToken();
        UserCreateRequest emptyRequest = new UserCreateRequest();

        given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .body(emptyRequest)
        .when()
                .post("/api/users")
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("validationErrors.username", notNullValue())
                .body("validationErrors.email", notNullValue())
                .body("validationErrors.password", notNullValue());
    }

    @Test
    @DisplayName("TC-USER-004: Create user should reject invalid email format with 400 Bad Request")
    @Story("Negative User Creation")
    @Severity(SeverityLevel.NORMAL)
    public void shouldRejectUserCreationWithInvalidEmailFormat() {
        String token = AuthTokenManager.getAdminToken();
        UserCreateRequest request = new UserCreateRequest(
                TestDataGenerator.randomUsername(),
                "bad-email@",
                TestDataGenerator.randomPassword()
        );

        given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .body(request)
        .when()
                .post("/api/users")
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("validationErrors.email", notNullValue());
    }

    @Test
    @DisplayName("TC-USER-005: Create user should reject unauthenticated request with 401 Unauthorized")
    @Story("Security Validation")
    @Severity(SeverityLevel.BLOCKER)
    public void shouldRejectUserCreationWithoutAuthentication() {
        UserCreateRequest request = UserTestData.validUserCreateRequest();

        given()
                .spec(RequestSpecs.unauthenticatedSpec())
                .body(request)
        .when()
                .post("/api/users")
        .then()
                .statusCode(401)
                .body("status", equalTo(401))
                .body("error", equalTo("Unauthorized"));
    }
}
