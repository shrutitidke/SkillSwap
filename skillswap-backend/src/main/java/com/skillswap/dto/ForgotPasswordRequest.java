package com.skillswap.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email")
        @Size(max = 254, message = "Email cannot exceed 254 characters")
        String email

) {
}
