package com.portfolio.qa.tests.users;

import com.portfolio.qa.config.BaseTest;
import com.portfolio.qa.dto.ErrorResponse;
import com.portfolio.qa.dto.UserCreateRequest;
import com.portfolio.qa.dto.UserResponse;
import com.portfolio.qa.dto.UserUpdateRequest;
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
@Feature("Update User")
@Tag("users")
public class UserUpdateTests extends BaseTest {

    @Test
    @DisplayName("TC-USER-013: Update existing user with valid data returns 200 OK")
    @Story("Positive User Update")
    @Severity(SeverityLevel.BLOCKER)
    public void shouldUpdateExistingUserSuccessfully() {
        String token = AuthTokenManager.getAdminToken();
        UserCreateRequest createRequest = UserTestData.validUserCreateRequest();

        // 1. Create initial user
        UserResponse createdUser = given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .body(createRequest)
                .post("/api/users")
                .then()
                .statusCode(201)
                .extract()
                .as(UserResponse.class);

        // 2. Prepare update payload
        String updatedUsername = "updated_" + TestDataGenerator.randomUsername();
        String updatedEmail = "updated_" + TestDataGenerator.randomEmail();
        UserUpdateRequest updateRequest = new UserUpdateRequest(updatedUsername, updatedEmail, "ADMIN");

        // 3. Perform update
        UserResponse updatedUser = given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .pathParam("id", createdUser.getId())
                .body(updateRequest)
        .when()
                .put("/api/users/{id}")
        .then()
                .statusCode(200)
                .contentType("application/json")
                .body("id", equalTo(createdUser.getId().intValue()))
                .body("username", equalTo(updatedUsername))
                .body("email", equalTo(updatedEmail.toLowerCase()))
                .body("role", equalTo("ADMIN"))
                .extract()
                .as(UserResponse.class);

        assertThat(updatedUser.getUsername()).isEqualTo(updatedUsername);
        assertThat(updatedUser.getEmail()).isEqualTo(updatedEmail.toLowerCase());
        assertThat(updatedUser.getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("TC-USER-014: Update user keeping own email should succeed without 409 Conflict [Regression BUG-002]")
    @Story("Regression Verification")
    @Severity(SeverityLevel.CRITICAL)
    public void shouldAllowUserToKeepOwnEmailOnUpdate() {
        String token = AuthTokenManager.getAdminToken();
        UserCreateRequest createRequest = UserTestData.validUserCreateRequest();

        UserResponse createdUser = given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .body(createRequest)
                .post("/api/users")
                .then()
                .statusCode(201)
                .extract()
                .as(UserResponse.class);

        // Update username only while preserving the same email
        String newUsername = "renamed_" + TestDataGenerator.randomUsername();
        UserUpdateRequest updateRequest = new UserUpdateRequest(
                newUsername,
                createdUser.getEmail(),
                createdUser.getRole()
        );

        UserResponse response = given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .pathParam("id", createdUser.getId())
                .body(updateRequest)
        .when()
                .put("/api/users/{id}")
        .then()
                .statusCode(200)
                .body("username", equalTo(newUsername))
                .body("email", equalTo(createdUser.getEmail()))
                .extract()
                .as(UserResponse.class);

        assertThat(response.getUsername()).isEqualTo(newUsername);
    }

    @Test
    @DisplayName("TC-USER-015: Update user with an email already taken by another user returns 409 Conflict")
    @Story("Negative User Update")
    @Severity(SeverityLevel.CRITICAL)
    public void shouldRejectUserUpdateWithDuplicateEmail() {
        String token = AuthTokenManager.getAdminToken();

        // Create User A
        UserCreateRequest userA = UserTestData.validUserCreateRequest();
        UserResponse responseA = given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .body(userA)
                .post("/api/users")
                .then()
                .statusCode(201)
                .extract()
                .as(UserResponse.class);

        // Create User B
        UserCreateRequest userB = UserTestData.validUserCreateRequest();
        UserResponse responseB = given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .body(userB)
                .post("/api/users")
                .then()
                .statusCode(201)
                .extract()
                .as(UserResponse.class);

        // Attempt to update User B's email to User A's email
        UserUpdateRequest updateRequest = new UserUpdateRequest(
                responseB.getUsername(),
                responseA.getEmail()
        );

        ErrorResponse error = given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .pathParam("id", responseB.getId())
                .body(updateRequest)
        .when()
                .put("/api/users/{id}")
        .then()
                .statusCode(409)
                .body("status", equalTo(409))
                .body("error", equalTo("Conflict"))
                .body("message", containsString("Email is already in use by another user"))
                .extract()
                .as(ErrorResponse.class);

        assertThat(error.getStatus()).isEqualTo(409);
    }

    @Test
    @DisplayName("TC-USER-016: Update non-existent user returns 404 Not Found")
    @Story("Negative User Update")
    @Severity(SeverityLevel.NORMAL)
    public void shouldReturn404WhenUpdatingNonexistentUser() {
        String token = AuthTokenManager.getAdminToken();
        long nonExistentId = 888888L;
        UserUpdateRequest updateRequest = UserTestData.validUserUpdateRequest();

        given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .pathParam("id", nonExistentId)
                .body(updateRequest)
        .when()
                .put("/api/users/{id}")
        .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("message", containsString("User not found with id: " + nonExistentId));
    }

    @Test
    @DisplayName("TC-USER-017: Update user without authentication returns 401 Unauthorized")
    @Story("Security Validation")
    @Severity(SeverityLevel.BLOCKER)
    public void shouldRejectUserUpdateWithoutAuthentication() {
        UserUpdateRequest updateRequest = UserTestData.validUserUpdateRequest();

        given()
                .spec(RequestSpecs.unauthenticatedSpec())
                .pathParam("id", 1L)
                .body(updateRequest)
        .when()
                .put("/api/users/{id}")
        .then()
                .statusCode(401)
                .body("status", equalTo(401));
    }
}
