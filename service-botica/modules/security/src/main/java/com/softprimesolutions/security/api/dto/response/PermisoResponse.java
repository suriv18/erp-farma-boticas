package com.softprimesolutions.security.api.dto.response;

public record PermisoResponse(
        String moduleCode,
        String moduleName,
        String code,
        String resource,
        String action,
        String name,
        String description,
        boolean critical,
        String status) {
}
