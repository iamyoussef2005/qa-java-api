package com.portfolio.qa.utils;

import com.portfolio.qa.config.TestConfig;
import com.portfolio.qa.dto.AuthResponse;
import com.portfolio.qa.dto.LoginRequest;
import com.portfolio.qa.dto.RegisterRequest;
import com.portfolio.qa.testdata.TestDataGenerator;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class AuthTokenManager {

    private static String cachedAdminToken;
    private static String cachedUserToken;

    public static synchronized String getAdminToken() {
        if (cachedAdminToken == null) {
            cachedAdminToken = loginAndGetToken(TestConfig.DEFAULT_ADMIN_EMAIL, TestConfig.DEFAULT_ADMIN_PASSWORD);
        }
        return cachedAdminToken;
    }

    public static synchronized String getUserToken() {
        if (cachedUserToken == null) {
            cachedUserToken = loginAndGetToken(TestConfig.DEFAULT_USER_EMAIL, TestConfig.DEFAULT_USER_PASSWORD);
        }
        return cachedUserToken;
    }

    public static String getFreshUserToken() {
        String username = TestDataGenerator.randomUsername();
        String email = TestDataGenerator.randomEmail();
        String password = TestDataGenerator.randomPassword();

        RegisterRequest registerRequest = new RegisterRequest(username, email, password);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(registerRequest)
                .post("/api/auth/register")
                .then()
                .statusCode(201);

        return loginAndGetToken(email, password);
    }

    public static String loginAndGetToken(String email, String password) {
        LoginRequest loginRequest = new LoginRequest(email, password);

        AuthResponse authResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .as(AuthResponse.class);

        return authResponse.getToken();
    }
}
