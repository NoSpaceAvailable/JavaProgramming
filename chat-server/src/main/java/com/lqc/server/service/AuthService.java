package com.lqc.server.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.lqc.common.model.User;
import com.lqc.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;

    public AuthService() {
        this(new UserRepository());
    }

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public record RegisterResult(boolean success, String message, User user) {}
    public record LoginResult(boolean success, String message, User user) {}

    public RegisterResult register(String username, String password, String displayName) {
        if (username == null || username.trim().length() < 3) {
            return new RegisterResult(false, "Username must be at least 3 characters", null);
        }
        if (password == null || password.length() < 6) {
            return new RegisterResult(false, "Password must be at least 6 characters", null);
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = username;
        }

        if (userRepository.existsByUsername(username.trim())) {
            return new RegisterResult(false, "Username already taken", null);
        }

        String passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        User user = userRepository.create(username.trim(), passwordHash, displayName.trim());
        logger.info("User registered: {}", username);
        return new RegisterResult(true, "Registration successful", user);
    }

    public LoginResult login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return new LoginResult(false, "Username is required", null);
        }
        if (password == null || password.isEmpty()) {
            return new LoginResult(false, "Password is required", null);
        }

        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isEmpty()) {
            return new LoginResult(false, "Invalid username or password", null);
        }

        User user = userOpt.get();
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), user.getPasswordHash());
        if (!result.verified) {
            return new LoginResult(false, "Invalid username or password", null);
        }

        logger.info("User logged in: {}", username);
        return new LoginResult(true, "Login successful", user);
    }
}
