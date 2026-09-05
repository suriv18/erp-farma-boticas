package com.softprimesolutions.security.api.dto.request;

import jakarta.validation.constraints.Size;

public record RevocarSesionRequest(@Size(max = 500) String reason) {
}
