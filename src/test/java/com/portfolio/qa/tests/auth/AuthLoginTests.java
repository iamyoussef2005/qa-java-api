package com.portfolio.qa.tests.auth;

import com.portfolio.qa.config.BaseTest;
import com.portfolio.qa.config.TestConfig;
import com.portfolio.qa.dto.AuthResponse;
import com.portfolio.qa.dto.ErrorResponse;
import com.portfolio.qa.dto.LoginRequest;
import com.portfolio.qa.dto.RegisterRequest;
import com.portfolio.qa.testdata.TestDataGenerator;
import com.portfolio.qa.utils.RequestSpecs;
import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@Epic("Authentication Management")
@Feature("User Login")
@Tag("auth")
@Tag("smoke")
public class AuthLoginTests extends BaseTest {

    @Test
    @DisplayName("TC-AUTH-009: Login with valid credentials returns 200 OK and JWT token")
    @Story("Positive Login Flow")
    @Severity(SeverityLevel.BLOCKER)
    public void shouldLoginWithValidCredentialsAndReceiveToken() {
        // Register a dedicated user for login
        String username = TestDataGenerator.randomUsername();
        String email = TestDataGenerator.randomEmail();
        String password = TestDataGenerator.randomPassword();

        given()
                .spec(RequestSpecs.unauthenticatedSpec())
                .body(new RegisterRequest(username, email, password))
                .post("/api/auth/register")
                .then()
                .statusCode(201);

        // Perform login
        LoginRequest loginRequest = new LoginRequest(email, password);

        AuthResponse authResponse = given()
                .spec(RequestSpecs.unauthenticatedSpec())
                .body(loginRequest)
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(200)
                .contentType("application/json")
                .body("token", notNullValue())
                .body("tokenType", equalTo("Bearer"))
                .body("expiresIn", greaterThan(0))
                .body("user.email", equalTo(email.toLowerCase()))
                .body("user.username", equalTo(username))
                .extract()
                .as(AuthResponse.class);

        assertThat(authResponse.getToken()).isNotBlank();
        assertThat(authResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(authResponse.getUser().getEmail()).isEqualTo(email.toLowerCase());
    }

    @Test
    @DisplayName("TC-AUTH-010: Login with default admin credentials returns 200 OK")
    @Story("Positive Login Flow")
    @Severity(SeverityLevel.CRITICAL)
    public void shouldLoginWithDefaultAdminCredentials() {
        LoginRequest loginRequest = new LoginRequest(
                TestConfig.DEFAULT_ADMIN_EMAIL,
                TestConfig.DEFAULT_ADMIN_PASSWORD
        );

        given()
                .spec(RequestSpecs.unauthenticatedSpec())
                .body(loginRequest)
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("user.role", equalTo("ADMIN"));
    }

    @Test
    @DisplayName("TC-AUTH-011: Login should reject non-existent email with 401 Unauthorized")
    @Story("Negative Login Flow")
    @Severity(SeverityLevel.CRITICAL)
    public void shouldRejectLoginWithNonExistentEmail() {
        LoginRequest loginRequest = new LoginRequest(
                "nonexistent_" + System.currentTimeMillis() + "@test.com",
                "AnyPassword123!"
        );

        ErrorResponse error = given()
                .spec(RequestSpecs.unauthenticatedSpec())
                .body(loginRequest)
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(401)
                .contentType("application/json")
                .body("status", equalTo(401))
                .body("error", equalTo("Unauthorized"))
                .body("message", equalTo("Invalid email or password"))
                .extract()
                .as(ErrorResponse.class);

        assertThat(error.getStatus()).isEqualTo(401);
        assertThat(error.getMessage()).isEqualTo("Invalid email or password");
    }

    @Test
    @DisplayName("TC-AUTH-012: Login should reject incorrect password with 401 Unauthorized")
    @Story("Negative Login Flow")
    @Severity(SeverityLevel.CRITICAL)
    public void shouldRejectLoginWithIncorrectPassword() {
        // Register user
        String email = TestDataGenerator.randomEmail();
        String password = "CorrectPassword123!";

        given()
                .spec(RequestSpecs.unauthenticatedSpec())
                .body(new RegisterRequest(TestDataGenerator.randomUsername(), email, password))
                .post("/api/auth/register")
                .then()
                .statusCode(201);

        // Attempt login with wrong password
        LoginRequest loginRequest = new LoginRequest(email, "WrongPassword999!");

        given()
                .spec(RequestSpecs.unauthenticatedSpec())
                .body(loginRequest)
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(401)
                .body("status", equalTo(401))
                .body("error", equalTo("Unauthorized"))
                .body("message", equalTo("Invalid email or password"));
    }

    @Test
    @DisplayName("TC-AUTH-013: Login should reject empty email with 400 Bad Request")
    @Story("Negative Login Flow")
    @Severity(SeverityLevel.NORMAL)
    public void shouldRejectLoginWithEmptyEmail() {
        LoginRequest loginRequest = new LoginRequest("", "SomePassword123!");

        given()
                .spec(RequestSpecs.unauthenticatedSpec())
                .body(loginRequest)
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("validationErrors.email", notNullValue());
    }

    @Test
    @DisplayName("TC-AUTH-014: Login should reject empty password with 400 Bad Request")
    @Story("Negative Login Flow")
    @Severity(SeverityLevel.NORMAL)
    public void shouldRejectLoginWithEmptyPassword() {
        LoginRequest loginRequest = new LoginRequest(TestDataGenerator.randomEmail(), "");

        given()
                .spec(RequestSpecs.unauthenticatedSpec())
                .body(loginRequest)
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("validationErrors.password", notNullValue());
    }

    @Test
    @DisplayName("TC-AUTH-015: Login should reject invalid email format with 400 Bad Request")
    @Story("Negative Login Flow")
    @Severity(SeverityLevel.NORMAL)
    public void shouldRejectLoginWithInvalidEmailFormat() {
        LoginRequest loginRequest = new LoginRequest("not-an-email", "Password123!");

        given()
                .spec(RequestSpecs.unauthenticatedSpec())
                .body(loginRequest)
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("validationErrors.email", notNullValue());
    }
}
