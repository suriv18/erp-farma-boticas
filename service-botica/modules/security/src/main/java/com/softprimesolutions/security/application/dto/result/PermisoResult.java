package com.softprimesolutions.security.application.dto.result;

public record PermisoResult(
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
