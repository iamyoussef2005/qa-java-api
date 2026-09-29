package com.portfolio.qa.service;

import com.portfolio.qa.dto.UserCreateRequest;
import com.portfolio.qa.dto.UserResponse;
import com.portfolio.qa.dto.UserUpdateRequest;
import com.portfolio.qa.entity.Role;
import com.portfolio.qa.entity.User;
import com.portfolio.qa.exception.DuplicateResourceException;
import com.portfolio.qa.exception.ResourceNotFoundException;
import com.portfolio.qa.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
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

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers(String search) {
        List<User> users;
        if (search != null && !search.trim().isEmpty()) {
            String query = search.trim().toLowerCase();
            users = userRepository.findAll().stream()
                    .filter(u -> u.getUsername().toLowerCase().contains(query) ||
                                 u.getEmail().toLowerCase().contains(query))
                    .collect(Collectors.toList());
        } else {
            users = userRepository.findAll();
        }

        return users.stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // Check if new email is taken by another user
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, id)) {
            throw new DuplicateResourceException("Email is already in use by another user: " + normalizedEmail);
        }

        user.setUsername(request.getUsername().trim());
        user.setEmail(normalizedEmail);

        if (request.getRole() != null && !request.getRole().trim().isEmpty()) {
            user.setRole(Role.fromString(request.getRole()));
        }

        User updatedUser = userRepository.save(user);
        return UserResponse.fromEntity(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}
