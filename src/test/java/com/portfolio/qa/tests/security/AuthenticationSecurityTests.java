package com.portfolio.qa.tests.security;

import com.portfolio.qa.config.BaseTest;
import com.portfolio.qa.dto.ErrorResponse;
import com.portfolio.qa.utils.RequestSpecs;
import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@Epic("Security & Authorization")
@Feature("Token Authentication")
@Tag("security")
public class AuthenticationSecurityTests extends BaseTest {

    @Test
    @DisplayName("TC-SEC-001: Accessing protected endpoint without token returns 401 with standard ErrorResponse [Regression BUG-005]")
    @Story("Security Enforcement")
    @Severity(SeverityLevel.BLOCKER)
    public void shouldReturn401WhenAccessingProtectedEndpointWithoutToken() {
        ErrorResponse error = given()
                .spec(RequestSpecs.unauthenticatedSpec())
        .when()
                .get("/api/users")
        .then()
                .statusCode(401)
                .contentType("application/json")
                .body("status", equalTo(401))
                .body("error", equalTo("Unauthorized"))
                .body("message", containsString("Full authentication is required"))
                .extract()
                .as(ErrorResponse.class);

        assertThat(error.getStatus()).isEqualTo(401);
        assertThat(error.getPath()).isEqualTo("/api/users");
        assertThat(error.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("TC-SEC-002: Accessing protected endpoint with malformed token returns 401 Unauthorized")
    @Story("Token Validation")
    @Severity(SeverityLevel.CRITICAL)
    public void shouldRejectAccessWithMalformedJwtToken() {
        given()
                .spec(RequestSpecs.customHeaderSpec("Authorization", "Bearer invalid.jwt.token.format"))
        .when()
                .get("/api/users")
        .then()
                .statusCode(401)
                .body("status", equalTo(401))
                .body("error", equalTo("Unauthorized"));
    }

    @Test
    @DisplayName("TC-SEC-003: Accessing protected endpoint with tampered token returns 401 Unauthorized")
    @Story("Token Validation")
    @Severity(SeverityLevel.CRITICAL)
    public void shouldRejectAccessWithTamperedTokenSignature() {
        String forgedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                "eyJzdWIiOiJhZG1pbkBleGFtcGxlLmNvbSIsInJvbGUiOiJBRE1JTiIsImV4cCI6MjU1NjExMjgwMH0." +
                "invalid_signature_hash_bytes_value_here";

        given()
                .spec(RequestSpecs.customHeaderSpec("Authorization", "Bearer " + forgedToken))
        .when()
                .get("/api/users")
        .then()
                .statusCode(401)
                .body("status", equalTo(401));
    }

    @Test
    @DisplayName("TC-SEC-004: Accessing protected endpoint with unsupported auth scheme returns 401 Unauthorized")
    @Story("Token Validation")
    @Severity(SeverityLevel.NORMAL)
    public void shouldRejectAccessWithInvalidAuthScheme() {
        given()
                .spec(RequestSpecs.customHeaderSpec("Authorization", "Basic YWRtaW46cGFzc3dvcmQ="))
        .when()
                .get("/api/users")
        .then()
                .statusCode(401)
                .body("status", equalTo(401));
    }

    @Test
    @DisplayName("TC-SEC-005: Accessing protected endpoint with empty Bearer token returns 401 Unauthorized")
    @Story("Token Validation")
    @Severity(SeverityLevel.NORMAL)
    public void shouldRejectAccessWithEmptyBearerToken() {
        given()
                .spec(RequestSpecs.customHeaderSpec("Authorization", "Bearer "))
        .when()
                .get("/api/users")
        .then()
                .statusCode(401)
                .body("status", equalTo(401));
    }
}
