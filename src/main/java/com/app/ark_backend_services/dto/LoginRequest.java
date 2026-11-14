package com.app.ark_backend_services.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    /** Optional. When sent, the refresh token lives longer (device session, e.g. mobile). */
    @Size(max = 100, message = "deviceId is too long")
    private String deviceId;

    @Size(max = 100, message = "deviceName is too long")
    private String deviceName;
}
