package com.app.ark_backend_services.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 128, message = "Password must be 8 to 128 characters")
    private String newPassword;

    /** Optional; used for the fresh session issued to the device that changed the password. */
    @Size(max = 100, message = "deviceId is too long")
    private String deviceId;

    @Size(max = 100, message = "deviceName is too long")
    private String deviceName;
}
