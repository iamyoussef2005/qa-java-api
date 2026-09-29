package com.portfolio.qa.tests.users;

import com.portfolio.qa.config.BaseTest;
import com.portfolio.qa.dto.ErrorResponse;
import com.portfolio.qa.dto.UserCreateRequest;
import com.portfolio.qa.dto.UserResponse;
import com.portfolio.qa.testdata.UserTestData;
import com.portfolio.qa.utils.AuthTokenManager;
import com.portfolio.qa.utils.RequestSpecs;
import io.qameta.allure.*;
import io.restassured.common.mapper.TypeRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@Epic("User Management")
@Feature("Retrieve Users")
@Tag("users")
public class UserRetrievalTests extends BaseTest {

    @Test
    @DisplayName("TC-USER-006: Get all users when authenticated returns 200 OK and non-empty list")
    @Story("Positive User Retrieval")
    @Severity(SeverityLevel.BLOCKER)
    public void shouldGetAllUsersWhenAuthenticated() {
        String token = AuthTokenManager.getAdminToken();

        List<UserResponse> users = given()
                .spec(RequestSpecs.authenticatedSpec(token))
        .when()
                .get("/api/users")
        .then()
                .statusCode(200)
                .contentType("application/json")
                .body("$", not(empty()))
                .extract()
                .as(new TypeRef<List<UserResponse>>() {});

        assertThat(users).isNotEmpty();
        assertThat(users.get(0).getId()).isNotNull();
        assertThat(users.get(0).getUsername()).isNotBlank();
        assertThat(users.get(0).getEmail()).isNotBlank();
    }

    @Test
    @DisplayName("TC-USER-007: Get user by ID when user exists returns 200 OK")
    @Story("Positive User Retrieval")
    @Severity(SeverityLevel.CRITICAL)
    public void shouldGetUserByIdWhenUserExists() {
        String token = AuthTokenManager.getAdminToken();
        UserCreateRequest createRequest = UserTestData.validUserCreateRequest();

        // Create a user first
        UserResponse createdUser = given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .body(createRequest)
                .post("/api/users")
                .then()
                .statusCode(201)
                .extract()
                .as(UserResponse.class);

        // Retrieve by ID
        UserResponse retrievedUser = given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .pathParam("id", createdUser.getId())
        .when()
                .get("/api/users/{id}")
        .then()
                .statusCode(200)
                .contentType("application/json")
                .body("id", equalTo(createdUser.getId().intValue()))
                .body("username", equalTo(createdUser.getUsername()))
                .body("email", equalTo(createdUser.getEmail()))
                .extract()
                .as(UserResponse.class);

        assertThat(retrievedUser.getId()).isEqualTo(createdUser.getId());
        assertThat(retrievedUser.getEmail()).isEqualTo(createdUser.getEmail());
    }

    @Test
    @DisplayName("TC-USER-008: Search users by keyword filters matching users successfully")
    @Story("Positive User Retrieval")
    @Severity(SeverityLevel.NORMAL)
    public void shouldFilterUsersBySearchKeyword() {
        String token = AuthTokenManager.getAdminToken();
        UserCreateRequest createRequest = UserTestData.validUserCreateRequest();

        // Create unique user
        given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .body(createRequest)
                .post("/api/users")
                .then()
                .statusCode(201);

        // Search by username
        List<UserResponse> searchResults = given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .queryParam("search", createRequest.getUsername())
        .when()
                .get("/api/users")
        .then()
                .statusCode(200)
                .body("$", not(empty()))
                .extract()
                .as(new TypeRef<List<UserResponse>>() {});

        assertThat(searchResults).anyMatch(u -> u.getUsername().equals(createRequest.getUsername()));
    }

    @Test
    @DisplayName("TC-USER-009: Get non-existent user ID returns 404 Not Found")
    @Story("Negative User Retrieval")
    @Severity(SeverityLevel.NORMAL)
    public void shouldReturn404WhenGettingNonExistentUser() {
        String token = AuthTokenManager.getAdminToken();
        long nonExistentId = 999999L;

        ErrorResponse error = given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .pathParam("id", nonExistentId)
        .when()
                .get("/api/users/{id}")
        .then()
                .statusCode(404)
                .contentType("application/json")
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("message", containsString("User not found with id: " + nonExistentId))
                .extract()
                .as(ErrorResponse.class);

        assertThat(error.getStatus()).isEqualTo(404);
        assertThat(error.getMessage()).contains(String.valueOf(nonExistentId));
    }

    @Test
    @DisplayName("TC-USER-010: Get user with invalid ID format returns 400 Bad Request")
    @Story("Negative User Retrieval")
    @Severity(SeverityLevel.NORMAL)
    public void shouldReturn400WhenGettingUserWithInvalidIdFormat() {
        String token = AuthTokenManager.getAdminToken();

        given()
                .spec(RequestSpecs.authenticatedSpec(token))
                .pathParam("id", "invalid-id-string")
        .when()
                .get("/api/users/{id}")
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"))
                .body("message", containsString("Parameter 'id' should be of type"));
    }

    @Test
    @DisplayName("TC-USER-011: Get user without authentication returns 401 Unauthorized")
    @Story("Security Validation")
    @Severity(SeverityLevel.BLOCKER)
    public void shouldRejectUserRetrievalWithoutAuthentication() {
        given()
                .spec(RequestSpecs.unauthenticatedSpec())
        .when()
                .get("/api/users")
        .then()
                .statusCode(401)
                .body("status", equalTo(401))
                .body("error", equalTo("Unauthorized"));
    }

    @Test
    @DisplayName("TC-USER-012: Ensure password field is never exposed in user retrieval response")
    @Story("Data Privacy & Security Invariant")
    @Severity(SeverityLevel.CRITICAL)
    public void shouldEnsurePasswordIsNotExposedInUserRetrieval() {
        String token = AuthTokenManager.getAdminToken();

        given()
                .spec(RequestSpecs.authenticatedSpec(token))
        .when()
                .get("/api/users")
        .then()
                .statusCode(200)
                .body("password", everyItem(nullValue()));
    }
}
