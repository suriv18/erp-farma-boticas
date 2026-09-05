package com.softprimesolutions.security.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestablecerPasswordRequest(
        @NotBlank @Size(max = 512) String resetToken,
        @NotBlank @Size(max = 128) String newPassword) {
}
