package com.portfolio.qa.config;

public class TestConfig {

    public static final String DEFAULT_ADMIN_EMAIL = "admin@example.com";
    public static final String DEFAULT_ADMIN_PASSWORD = "AdminPass123!";

    public static final String DEFAULT_USER_EMAIL = "testuser@example.com";
    public static final String DEFAULT_USER_PASSWORD = "UserPass123!";

    public static String getBaseUrl() {
        String baseUrl = System.getProperty("api.base.url");
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            baseUrl = System.getenv("API_BASE_URL");
        }
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return "http://localhost";
        }
        return baseUrl.replaceAll("/$", "");
    }
}
