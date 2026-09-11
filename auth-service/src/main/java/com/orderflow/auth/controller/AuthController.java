package com.orderflow.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orderflow.auth.dto.AuthResponse;
import com.orderflow.auth.dto.LoginRequest;
import com.orderflow.auth.dto.RegisterRequest;
import com.orderflow.auth.dto.UserResponse;
import com.orderflow.auth.security.JwtService;
import com.orderflow.auth.service.AuthService;
import com.orderflow.auth.service.AuthService.LoginResult;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse response =
                authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResult result =
                authService.login(request);

        ResponseCookie accessCookie =
                createCookie(
                        "access_token",
                        result.accessToken(),
                        15 * 60
                );

        ResponseCookie refreshCookie =
                createCookie(
                        "refresh_token",
                        result.refreshToken(),
                        7 * 24 * 60 * 60
                );

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        accessCookie.toString()
                )
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie.toString()
                )
                .body(result.response());
    }

    private ResponseCookie createCookie(
            String name,
            String value,
            long maxAge) {

        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(false) // true in HTTPS production
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
    
    private ResponseCookie deleteCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }
    
    // for access token by validating the refresh token
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(value = "refresh_token", required = false) String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is missing");
        }

        String accessToken = authService.refreshAccessToken(refreshToken);

        ResponseCookie accessCookie = createCookie(
                "access_token",
                accessToken,
                15 * 60
        );

        AuthResponse response = AuthResponse.builder()
                .message("Access token refreshed successfully")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .body(response);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout() {

        ResponseCookie accessCookie = deleteCookie("access_token");
        ResponseCookie refreshCookie = deleteCookie("refresh_token");

        AuthResponse response = AuthResponse.builder()
                .message("Logout successful")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(response);
    }
    
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @CookieValue(value = "access_token", required = false) String accessToken) {

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token is missing");
        }

        if (!jwtService.isTokenValid(accessToken)) {
            throw new IllegalArgumentException("Invalid or expired access token");
        }

        Long userId = jwtService.extractUserId(accessToken);

        UserResponse user = authService.getCurrentUser(userId);

        return ResponseEntity.ok(user);
    }
}