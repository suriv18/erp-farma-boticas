package com.softprimesolutions.security.infrastructure.persistence.read.projection;

public record PermisoProjection(
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
