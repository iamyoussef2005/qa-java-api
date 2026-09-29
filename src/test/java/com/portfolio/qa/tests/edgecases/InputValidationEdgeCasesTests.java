package com.portfolio.qa.tests.edgecases;

import com.portfolio.qa.config.BaseTest;
import com.portfolio.qa.dto.RegisterRequest;
import com.portfolio.qa.testdata.TestDataGenerator;
import com.portfolio.qa.utils.RequestSpecs;
import io.qameta.allure.*;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Epic("Input Validation & Edge Cases")
@Feature("Boundary & Format Verification")
@Tag("edgecases")
public class InputValidationEdgeCasesTests extends BaseTest {

    @Test
    @DisplayName("TC-EDGE-001: Reject username exceeding max boundary (50 characters)")
    @Story("Boundary Value Analysis")
    @Severity(SeverityLevel.NORMAL)
    public void shouldRejectUsernameExceedingMaxLength() {
        RegisterRequest request = new RegisterRequest(
                TestDataGenerator.veryLongUsername(),
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
                .body("validationErrors.username", containsString("between 3 and 50 characters"));
    }

    @Test
    @DisplayName("TC-EDGE-002: Reject username below min boundary (3 characters)")
    @Story("Boundary Value Analysis")
    @Severity(SeverityLevel.NORMAL)
    public void shouldRejectUsernameBelowMinLength() {
        RegisterRequest request = new RegisterRequest(
                TestDataGenerator.veryShortUsername(),
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
                .body("validationErrors.username", containsString("between 3 and 50 characters"));
    }

    @Test
    @DisplayName("TC-EDGE-003: Reject email exceeding max boundary (100 characters)")
    @Story("Boundary Value Analysis")
    @Severity(SeverityLevel.NORMAL)
    public void shouldRejectEmailExceedingMaxLength() {
        RegisterRequest request = new RegisterRequest(
                TestDataGenerator.randomUsername(),
                TestDataGenerator.veryLongEmail(),
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
                .body("validationErrors.email", notNullValue());
    }

    @Test
    @DisplayName("TC-EDGE-004: Reject malformed JSON request body with 400 Bad Request")
    @Story("Malformed Payloads")
    @Severity(SeverityLevel.CRITICAL)
    public void shouldRejectMalformedJsonBody() {
        String malformedJson = "{\"username\": \"test_user\", \"email\": \"incomplete...";

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(malformedJson)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"))
                .body("message", containsString("Malformed JSON request body"));
    }

    @Test
    @DisplayName("TC-EDGE-005: Reject empty JSON object request body with 400 Bad Request")
    @Story("Empty Payloads")
    @Severity(SeverityLevel.NORMAL)
    public void shouldRejectEmptyJsonPayload() {
        String emptyJson = "{}";

        given()
                .spec(RequestSpecs.unauthenticatedSpec())
                .body(emptyJson)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("validationErrors.username", notNullValue())
                .body("validationErrors.email", notNullValue())
                .body("validationErrors.password", notNullValue());
    }

    @Test
    @DisplayName("TC-EDGE-006: Reject username containing forbidden special characters or XSS script tags")
    @Story("Format & Security Sanitization")
    @Severity(SeverityLevel.CRITICAL)
    public void shouldRejectUsernameWithSpecialCharacters() {
        RegisterRequest request = new RegisterRequest(
                "user<script>",
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
                .body("validationErrors.username", containsString("can only contain alphanumeric"));
    }

    @Test
    @DisplayName("TC-EDGE-007: Reject whitespace-only email string with 400 Bad Request")
    @Story("Format Sanitization")
    @Severity(SeverityLevel.NORMAL)
    public void shouldRejectWhitespaceOnlyEmail() {
        RegisterRequest request = new RegisterRequest(
                TestDataGenerator.randomUsername(),
                "    ",
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
                .body("validationErrors.email", notNullValue());
    }
}
