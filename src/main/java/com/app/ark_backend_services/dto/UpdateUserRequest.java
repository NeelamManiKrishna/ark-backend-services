package com.app.ark_backend_services.dto;

import com.app.ark_backend_services.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    private String fullName;

    @Email(message = "Invalid email format")
    private String email;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private User.Role role;

    private String branchId;

    private String department;

    private User.UserStatus status;
}
