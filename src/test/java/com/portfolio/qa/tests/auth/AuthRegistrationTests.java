package com.portfolio.qa.tests.auth;

import com.portfolio.qa.config.BaseTest;
import com.portfolio.qa.dto.ErrorResponse;
import com.portfolio.qa.dto.RegisterRequest;
import com.portfolio.qa.dto.UserResponse;
import com.portfolio.qa.testdata.TestDataGenerator;
import com.portfolio.qa.testdata.UserTestData;
import com.portfolio.qa.utils.RequestSpecs;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@Epic("Authentication Management")
@Feature("User Registration")
@Tag("auth")
@Tag("regression")
public class AuthRegistrationTests extends BaseTest {

    @Test
    @DisplayName("TC-AUTH-001: Register user with valid data should return 201 Created")
    @Story("Positive Registration Flow")
    @Severity(SeverityLevel.BLOCKER)
    public void shouldRegisterUserWithValidData() {
        RegisterRequest request = UserTestData.validRegisterRequest();

        UserResponse response = given()
                .spec(RequestSpecs.unauthenticatedSpec())
                .body(request)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(201)
                .contentType("application/json")
                .body("id", notNullValue())
                .body("username", equalTo(request.getUsername()))
                .body("email", equalTo(request.getEmail().toLowerCase()))
                .body("role", equalTo("USER"))
                .body("createdAt", notNullValue())
                .body("updatedAt", notNullValue())
                .extract()
                .as(UserResponse.class);

        assertThat(response.getId()).isPositive();
        assertThat(response.getUsername()).isEqualTo(request.getUsername());
        assertThat(response.getEmail()).isEqualTo(request.getEmail().toLowerCase());
    }

    @Test
    @DisplayName("TC-AUTH-002: Registration should reject invalid email format with 400 Bad Request")
    @Story("Negative Registration Flow")
    @Severity(SeverityLevel.NORMAL)
    public void shouldRejectRegistrationWithInvalidEmailFormat() {
        RegisterRequest request = new RegisterRequest(
                TestDataGenerator.randomUsername(),
                "invalid-email-format",
                TestDataGenerator.randomPassword()
        );

        ErrorResponse error = given()
                .spec(RequestSpecs.unauthenticatedSpec())
                .body(request)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(400)
                .contentType("application/json")
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"))
                .body("validationErrors.email", notNullValue())
                .extract()
                .as(ErrorResponse.class);

        assertThat(error.getStatus()).isEqualTo(400);
        assertThat(error.getValidationErrors()).containsKey("email");
    }

    @Test
    @DisplayName("TC-AUTH-003: Registration should reject empty username with 400 Bad Request")
    @Story("Negative Registration Flow")
    @Severity(SeverityLevel.NORMAL)
    public void shouldRejectRegistrationWithEmptyUsername() {
        RegisterRequest request = new RegisterRequest(
                "",
                TestDataGenerator.randomEmail(),
                TestDataGenerator.randomPassword()
        );

        given()
                .spec(RequestSpecs.unauthenticatedSpec())
                .body(request)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("validationErrors.username", notNullValue());
    }

    @Test
    @DisplayName("TC-AUTH-004: Registration should reject empty password with 400 Bad Request")
    @Story("Negative Registration Flow")
    @Severity(SeverityLevel.NORMAL)
    public void shouldRejectRegistrationWithEmptyPassword() {
        RegisterRequest request = new RegisterRequest(
                TestDataGenerator.randomUsername(),
                TestDataGenerator.randomEmail(),
                ""
        );

        given()
                .spec(RequestSpecs.unauthenticatedSpec())
                .body(request)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("validationErrors.password", notNullValue());
    }

    @Test
    @DisplayName("TC-AUTH-005: Registration should reject password shorter than 8 characters")
    @Story("Boundary Registration Flow")
    @Severity(SeverityLevel.NORMAL)
    public void shouldRejectRegistrationWithShortPassword() {
        RegisterRequest request = new RegisterRequest(
                TestDataGenerator.randomUsername(),
                TestDataGenerator.randomEmail(),
                TestDataGenerator.veryShortPassword()
        );

        given()
                .spec(RequestSpecs.unauthenticatedSpec())
                .body(request)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("validationErrors.password", containsString("at least 8 characters"));
    }

    @Test
    @DisplayName("TC-AUTH-006: Registration should reject duplicate email with 409 Conflict")
    @Story("Business Rules Validation")
    @Severity(SeverityLevel.CRITICAL)
    public void shouldRejectRegistrationWithDuplicateEmail() {
        RegisterRequest request = UserTestData.validRegisterRequest();

        // First registration succeeds
        given()
                .spec(RequestSpecs.unauthenticatedSpec())
                .body(request)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(201);

        // Second registration with the same email fails
        ErrorResponse error = given()
                .spec(RequestSpecs.unauthenticatedSpec())
                .body(request)
        .when()
                .post("/api/auth/register")
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
    @DisplayName("TC-AUTH-007: Registration should reject duplicate email case-insensitively [Regression BUG-001]")
    @Story("Regression Verification")
    @Severity(SeverityLevel.CRITICAL)
    public void shouldRejectRegistrationWithDuplicateEmailCaseInsensitive() {
        String baseEmail = "unique_" + System.currentTimeMillis() + "@example.com";
        RegisterRequest request1 = new RegisterRequest(
                TestDataGenerator.randomUsername(),
                baseEmail.toLowerCase(),
                TestDataGenerator.randomPassword()
        );

        RegisterRequest request2 = new RegisterRequest(
                TestDataGenerator.randomUsername(),
                baseEmail.toUpperCase(),
                TestDataGenerator.randomPassword()
        );

        // Register lowercase
        given()
                .spec(RequestSpecs.unauthenticatedSpec())
                .body(request1)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(201);

        // Attempt uppercase duplicate
        given()
                .spec(RequestSpecs.unauthenticatedSpec())
                .body(request2)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(409)
                .body("status", equalTo(409))
                .body("error", equalTo("Conflict"))
                .body("message", containsString("Email is already registered"));
    }

    @Test
    @DisplayName("TC-AUTH-008: Registration should reject whitespace-only username [Regression BUG-004]")
    @Story("Regression Verification")
    @Severity(SeverityLevel.NORMAL)
    public void shouldRejectRegistrationWithWhitespaceOnlyUsername() {
        RegisterRequest request = new RegisterRequest(
                "    ",
                TestDataGenerator.randomEmail(),
                TestDataGenerator.randomPassword()
        );

        given()
                .spec(RequestSpecs.unauthenticatedSpec())
                .body(request)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("validationErrors.username", notNullValue());
    }
}
