package com.softprimesolutions.security.domain.valueobject;

/** Referencia agnóstica a una identidad administrada por un proveedor externo. */
public record ReferenciaIdentidad(String provider, String subject, String issuer, String emailClaim) {
}
