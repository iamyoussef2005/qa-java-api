package com.portfolio.qa.tests.users;

import com.portfolio.qa.config.BaseTest;
import com.portfolio.qa.dto.ErrorResponse;
import com.portfolio.qa.dto.UserCreateRequest;
import com.portfolio.qa.dto.UserResponse;
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
@Feature("Delete User")
@Tag("users")
public class UserDeletionTests extends BaseTest {

    @Test
    @DisplayName("TC-USER-018: Delete existing user returns 204 No Content")
    @Story("Positive User Deletion")
    @Severity(SeverityLevel.BLOCKER)
    public void shouldDeleteExistingUserSuccessfully() {
        String token = AuthTokenManager.getAdminToken();
        UserCreateRequest createRequest = UserTestData.validUserCreateRequest();

        // 1. Create a user to delete
        UserResponse createdUser = given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .body(createRequest)
                .post("/api/users")
                .then()
                .statusCode(201)
                .extract()
                .as(UserResponse.class);

        // 2. Delete the created user
        given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .pathParam("id", createdUser.getId())
        .when()
                .delete("/api/users/{id}")
        .then()
                .statusCode(204);
    }

    @Test
    @DisplayName("TC-USER-019: Deleting non-existent user returns 404 Not Found [Regression BUG-003]")
    @Story("Regression Verification")
    @Severity(SeverityLevel.NORMAL)
    public void shouldReturn404WhenDeletingNonexistentUser() {
        String token = AuthTokenManager.getAdminToken();
        long nonExistentId = 777777L;

        ErrorResponse error = given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .pathParam("id", nonExistentId)
        .when()
                .delete("/api/users/{id}")
        .then()
                .statusCode(404)
                .contentType("application/json")
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("message", containsString("User not found with id: " + nonExistentId))
                .extract()
                .as(ErrorResponse.class);

        assertThat(error.getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("TC-USER-020: Verify deleted user is no longer retrievable via GET endpoint")
    @Story("End-to-End Deletion Verification")
    @Severity(SeverityLevel.CRITICAL)
    public void shouldVerifyUserIsNoLongerRetrievableAfterDeletion() {
        String token = AuthTokenManager.getAdminToken();
        UserCreateRequest createRequest = UserTestData.validUserCreateRequest();

        // 1. Create user
        UserResponse createdUser = given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .body(createRequest)
                .post("/api/users")
                .then()
                .statusCode(201)
                .extract()
                .as(UserResponse.class);

        // 2. Delete user
        given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .pathParam("id", createdUser.getId())
                .delete("/api/users/{id}")
                .then()
                .statusCode(204);

        // 3. Verify user retrieval returns 404
        given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .pathParam("id", createdUser.getId())
        .when()
                .get("/api/users/{id}")
        .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"));
    }

    @Test
    @DisplayName("TC-USER-021: Delete user without authentication returns 401 Unauthorized")
    @Story("Security Validation")
    @Severity(SeverityLevel.BLOCKER)
    public void shouldRejectUserDeletionWithoutAuthentication() {
        given()
                .spec(RequestSpecs.unauthenticatedSpec())
                .pathParam("id", 1L)
        .when()
                .delete("/api/users/{id}")
        .then()
                .statusCode(401)
                .body("status", equalTo(401));
    }
}
