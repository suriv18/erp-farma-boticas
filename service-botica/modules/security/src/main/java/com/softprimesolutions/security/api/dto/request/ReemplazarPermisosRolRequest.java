package com.softprimesolutions.security.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ReemplazarPermisosRolRequest(
        @NotNull @Size(max = 500) List<@Valid @NotBlank @Size(max = 100) String> permissionCodes) {

    public ReemplazarPermisosRolRequest {
        permissionCodes = permissionCodes == null ? null : List.copyOf(permissionCodes);
    }
}
