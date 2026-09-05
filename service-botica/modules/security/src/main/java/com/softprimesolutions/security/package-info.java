/**
 * Bounded Context de Identidad y Acceso (BC-IAM).
 *
 * <p>El módulo administra identidad, sesión y autorizaciones. La autenticación delegada y las
 * integraciones con proveedores de identidad pertenecen a adaptadores, no al dominio.
 */
@org.springframework.modulith.ApplicationModule(
        id = "security",
        displayName = "Seguridad",
        allowedDependencies = {"organizacion::api"})
package com.softprimesolutions.security;
