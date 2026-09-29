package com.portfolio.qa.config;

import com.portfolio.qa.entity.Role;
import com.portfolio.qa.entity.User;
import com.portfolio.qa.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmailIgnoreCase("admin@example.com")) {
            User admin = new User(
                    "admin_user",
                    "admin@example.com",
                    passwordEncoder.encode("AdminPass123!"),
                    Role.ADMIN
            );
            userRepository.save(admin);
            logger.info("Initialized default admin user: admin@example.com");
        }

        if (!userRepository.existsByEmailIgnoreCase("testuser@example.com")) {
            User regularUser = new User(
                    "test_user",
                    "testuser@example.com",
                    passwordEncoder.encode("UserPass123!"),
                    Role.USER
            );
            userRepository.save(regularUser);
            logger.info("Initialized default test user: testuser@example.com");
        }
    }
}
