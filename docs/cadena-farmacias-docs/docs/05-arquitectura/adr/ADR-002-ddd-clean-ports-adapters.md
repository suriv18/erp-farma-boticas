# ADR-002 — DDD + Clean Architecture + Ports & Adapters

**Estado:** Aceptado.

## Decisión
El diseño del negocio se basará en DDD. Cada módulo mantendrá dependencias hacia el dominio mediante Clean Architecture y Ports & Adapters. El dominio no dependerá de HTTP, ORM, PostgreSQL, SUNAT, ERP ni proveedores externos.

## Consecuencias
- puertos de entrada/salida explícitos;
- adapters para persistencia e integraciones;
- Bounded Context no equivale a microservicio;
- reglas de negocio permanecen probables sin infraestructura.
