package com.orderflow.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderflow.auth.dto.AuthResponse;
import com.orderflow.auth.dto.LoginRequest;
import com.orderflow.auth.dto.RegisterRequest;
import com.orderflow.auth.dto.UserResponse;
import com.orderflow.auth.entity.User;
import com.orderflow.auth.repository.UserRepository;
import com.orderflow.auth.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    @Transactional
    public UserResponse register(RegisterRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        if (userRepository.existsByEmail(email)) {

            throw new IllegalArgumentException(
                    "User already exists with email: " + email
            );
        }

        String encodedPassword =
                passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .email(email)
                .password(encodedPassword)
                .role(User.Role.CUSTOMER)
                .build();

        User savedUser = userRepository.save(user);

        return UserResponse.from(savedUser);
    }

    @Transactional(readOnly = true)
    public LoginResult login(LoginRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid email or password"
                        ));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new IllegalArgumentException(
                    "Invalid email or password"
            );
        }

        String accessToken =
                jwtService.generateAccessToken(user);

        String refreshToken =
                jwtService.generateRefreshToken(user);

        AuthResponse response = AuthResponse.builder()
                .message("Login successful")
                .user(UserResponse.from(user))
                .build();

        return new LoginResult(
                response,
                accessToken,
                refreshToken
        );
    }

    public record LoginResult(
            AuthResponse response,
            String accessToken,
            String refreshToken
    ) {
    }
    
    // gives new access token by validating the refresh token
    public String refreshAccessToken(String refreshToken) {

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        if (!"REFRESH".equals(jwtService.extractTokenType(refreshToken))) {
            throw new IllegalArgumentException("Invalid token type");
        }

        Long userId = jwtService.extractUserId(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return jwtService.generateAccessToken(user);
    }
    
    
    // for getting the current user details
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return UserResponse.from(user);
    }
}