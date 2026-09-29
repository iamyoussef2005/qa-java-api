package com.portfolio.qa.service;

import com.portfolio.qa.dto.AuthResponse;
import com.portfolio.qa.dto.LoginRequest;
import com.portfolio.qa.dto.RegisterRequest;
import com.portfolio.qa.dto.UserResponse;
import com.portfolio.qa.entity.Role;
import com.portfolio.qa.entity.User;
import com.portfolio.qa.exception.DuplicateResourceException;
import com.portfolio.qa.repository.UserRepository;
import com.portfolio.qa.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("Email is already registered: " + normalizedEmail);
        }

        Role role = Role.fromString(request.getRole());
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User(
                request.getUsername().trim(),
                normalizedEmail,
                encodedPassword,
                role
        );

        User savedUser = userRepository.save(user);
        return UserResponse.fromEntity(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
        );

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + normalizedEmail));

        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        long expiresInSeconds = jwtTokenProvider.getExpirationMs() / 1000;

        return new AuthResponse(token, expiresInSeconds, UserResponse.fromEntity(user));
    }
}
