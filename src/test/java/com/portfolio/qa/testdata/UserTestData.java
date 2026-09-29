package com.portfolio.qa.testdata;

import com.portfolio.qa.dto.LoginRequest;
import com.portfolio.qa.dto.RegisterRequest;
import com.portfolio.qa.dto.UserCreateRequest;
import com.portfolio.qa.dto.UserUpdateRequest;

public class UserTestData {

    public static RegisterRequest validRegisterRequest() {
        return new RegisterRequest(
                TestDataGenerator.randomUsername(),
                TestDataGenerator.randomEmail(),
                TestDataGenerator.randomPassword()
        );
    }

    public static RegisterRequest registerRequestWithRole(String role) {
        return new RegisterRequest(
                TestDataGenerator.randomUsername(),
                TestDataGenerator.randomEmail(),
                TestDataGenerator.randomPassword(),
                role
        );
    }

    public static UserCreateRequest validUserCreateRequest() {
        return new UserCreateRequest(
                TestDataGenerator.randomUsername(),
                TestDataGenerator.randomEmail(),
                TestDataGenerator.randomPassword(),
                "USER"
        );
    }

    public static UserUpdateRequest validUserUpdateRequest() {
        return new UserUpdateRequest(
                TestDataGenerator.randomUsername(),
                TestDataGenerator.randomEmail(),
                "USER"
        );
    }

    public static LoginRequest loginRequest(String email, String password) {
        return new LoginRequest(email, password);
    }
}
