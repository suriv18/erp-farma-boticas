/**
 * API de entrada y contratos públicos estables de Identidad y Acceso.
 *
 * <p>Los contratos entre módulos residen directamente en este paquete. Los controllers, DTOs y
 * mapeadores HTTP viven en subpaquetes internos y no exponen entidades de dominio, detalles del
 * proveedor de identidad ni modelos de persistencia.
 */
@org.springframework.modulith.NamedInterface("api")
package com.softprimesolutions.security.api;
