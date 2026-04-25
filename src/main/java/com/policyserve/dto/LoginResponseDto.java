package com.policyserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response body for POST /api/v1/auth/login — matches Angular LoginResponse shape */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    private String token;
    private String username;
    private String role;
    private long   expiresIn;   // seconds
}
