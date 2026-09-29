package com.portfolio.qa.testdata;

import java.util.UUID;

public class TestDataGenerator {

    public static String randomEmail() {
        return "qa_user_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6) + "@test.com";
    }

    public static String randomUsername() {
        return "qa_user_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String randomPassword() {
        return "Pass_" + UUID.randomUUID().toString().substring(0, 8) + "!";
    }

    public static String veryLongUsername() {
        // Max allowed is 50 characters; this generates 55 characters
        return "user_with_an_excessively_long_username_that_exceeds_max";
    }

    public static String veryLongEmail() {
        // Max allowed is 100 characters; this generates 110+ characters
        return "an_extremely_long_email_address_designed_to_test_maximum_boundary_limits_in_the_system_" +
                System.currentTimeMillis() + "@longdomainexample.org";
    }

    public static String veryShortUsername() {
        // Min allowed is 3 characters; this is 2 characters
        return "ab";
    }

    public static String veryShortPassword() {
        // Min allowed is 8 characters; this is 5 characters
        return "abc12";
    }

    public static String[] invalidEmails() {
        return new String[]{
                "plainaddress",
                "#@%^%#$@#$@#.com",
                "@missingusername.com",
                "username@.com.my",
                "user name@example.com"
        };
    }

    public static String[] disallowedUsernames() {
        return new String[]{
                "user with spaces",
                "user<script>",
                "user#hash",
                "user!exclamation",
                "user$dollar"
        };
    }
}
