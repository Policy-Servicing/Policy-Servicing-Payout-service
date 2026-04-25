package com.policyserve.controller;

import com.policyserve.dto.LoginRequestDto;
import com.policyserve.dto.LoginResponseDto;
import com.policyserve.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication controller — issues JWT tokens on successful login.
 *
 * POST /api/v1/auth/login  (publicly accessible — see SecurityConfig)
 *
 * Users are validated against the InMemoryUserDetailsManager defined
 * in SecurityConfig. Replace with DB-backed UserDetailsService when ready.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    /**
     * POST /api/v1/auth/login
     * Body: { "username": "admin", "password": "password" }
     * Returns: { "token": "...", "username": "...", "role": "...", "expiresIn": 3600 }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto req) {
        log.info("Login attempt for user: {}", req.getUsername());
        try {
            Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
            );

            String token = tokenProvider.generateToken(auth);
            String role  = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("ROLE_VIEWER");

            log.info("Login successful for user: {}", req.getUsername());
            return ResponseEntity.ok(LoginResponseDto.builder()
                    .token(token)
                    .username(req.getUsername())
                    .role(role.replace("ROLE_", ""))
                    .expiresIn(3600L)
                    .build());

        } catch (BadCredentialsException ex) {
            log.warn("Login failed for user: {}", req.getUsername());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        }
    }
}
