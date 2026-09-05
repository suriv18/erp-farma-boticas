/**
 * Modelo de persistencia para consultas de Identidad y Acceso.
 *
 * <p>Optimiza lecturas CQRS y no modifica el estado del negocio ni reconstruye agregados cuando una
 * proyección sea suficiente.
 */
package com.softprimesolutions.security.infrastructure.persistence.read;
