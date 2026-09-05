# Fase 6 — Arquitectura y ADR

**Estado:** Arquitectura base definida; decisiones de despliegue offline y ERP parcialmente condicionales.  
**Fecha base:** 30-08-2026.

## Objetivo

Transformar el modelo de dominio de la cadena de farmacias en una arquitectura implementable sin confundir Bounded Contexts con microservicios y sin asumir que una farmacia siempre tendrá conectividad estable.

## Línea arquitectónica

La solución se define como una **plataforma híbrida de retail distribuido**:

1. **Core central** implementado inicialmente como **Monolito Modular multi-módulo**, orientado al dominio con DDD.
2. **Clean Architecture mediante Ports & Adapters** dentro de cada módulo.
3. **CQRS selectivo** en la capa de aplicación; no Event Sourcing por defecto.
4. **Result Pattern** para resultados/errores esperables del negocio.
5. **Domain Events** para desacoplamiento interno y **Transactional Outbox** cuando un evento deba salir del límite transaccional.
6. **POS online-first** con un **perfil Store Edge offline-capable** activable por tienda cuando la continuidad del negocio lo requiera.
7. Integraciones SUNAT, DIGEMID, adquirentes, ERP externo u otros proveedores detrás de **Ports/Adapters y Anti-Corruption Layers**.

> `Bounded Context != Microservicio`. Los 16 contextos del dominio no se desplegarán como 16 servicios desde el inicio.

## Documentos

- [ARC-FAR-001 — arc42](01-arc42.md)
- [ARC-FAR-002 — C4](02-c4.md)
- [ARC-FAR-003 — Estrategia de solución](03-estrategia-solucion.md)
- [ARC-FAR-004 — Arquitectura de tienda/POS](04-arquitectura-tienda-pos.md)
- [ARC-FAR-005 — Sincronización tienda-central](05-sincronizacion-tienda-central.md)
- [ARC-FAR-006 — Integraciones ERP, SUNAT y DIGEMID](06-integraciones-erp-sunat-digemid.md)
- [ARC-FAR-007 — Matriz decisión → evidencia](07-matriz-arquitectura-evidencia.md)
- [Índice de ADR](adr/README.md)

## Fuentes técnicas verificadas

- LS Central documenta POS online y offline, incluyendo base local por POS o servidor/base offline por tienda y replicación con Head Office.
- Dynamics 365 Commerce documenta cambio automático a base offline y sincronización posterior de transacciones.
- SUNAT documenta que el Facturador SUNAT puede emitir sin conexión y transmitir posteriormente dentro del plazo aplicable; también existe procedimiento oficial de contingencia para ciertos casos.
- Spring Modulith documenta soporte para aplicaciones Spring Boot modulares orientadas al dominio y verificación de módulos. Se considera una herramienta candidata si el stack definitivo es Java/Spring, no una decisión de negocio.
- Azure/AWS documentan CQRS, Transactional Outbox e idempotencia para evitar inconsistencias de dual-write y tolerar entregas duplicadas.

Ver [Referencias](../99-referencias.md).
