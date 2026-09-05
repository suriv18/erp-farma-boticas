# Plataforma Integral de Gestión para Cadena de Farmacias

Estado: **Fase 9 — Modelo físico PostgreSQL 18 consolidado y enriquecido**  
Fecha base: **02-09-2026**

## Propósito

Este repositorio documenta el análisis y diseño de una plataforma integral para una cadena de farmacias en Perú. El enfoque sigue el mismo rigor aplicado a otros proyectos: primero negocio y normativa; después procesos, requisitos, reglas de negocio, dominio, arquitectura, datos, API, seguridad y pruebas.

## Regla de evidencia

Cada capacidad, regla o dato debe identificarse según su origen:

- `NORM`: norma, autoridad o fuente oficial.
- `MKT`: capacidad observada en productos/sistemas actuales del mercado.
- `FUNC`: necesidad funcional del proyecto validada por stakeholders.
- `DOM`: decisión del modelo de dominio.
- `STD`: estándar técnico.
- `TEC`: decisión tecnológica/arquitectónica.
- `POR_VALIDAR`: hipótesis aún no confirmada.

No se presentará una decisión `TEC` o una práctica `MKT` como si fuera una obligación `NORM`.

## Arquitectura funcional

La plataforma se divide inicialmente en cinco grandes dominios de capacidad:

1. **ERP Corporativo** — finanzas, contabilidad, tesorería, compras, proveedores, cuentas por pagar/cobrar y consolidación.
2. **Retail / POS** — tienda, caja, ventas, precios, promociones, devoluciones, clientes y operación offline.
3. **Supply Chain / WMS** — inventario, almacenes, lotes, vencimientos, FEFO, transferencias, reposición, compras y distribución.
4. **Farmacéutico / Regulatorio** — registro sanitario, condición de venta, recetas, dispensación, director técnico, productos controlados, alertas/retiros y farmacovigilancia.
5. **Omnicanal / CRM** — web/app, pedidos, retiro en tienda, delivery, fidelización, disponibilidad por local y atención al cliente.

> Esta separación es funcional. **No implica todavía microservicios ni bases de datos independientes.** La arquitectura técnica se decidirá después de validar procesos y requisitos.

## Documentos de la fase actual

- [Visión y alcance](docs/01-negocio/01-vision-alcance.md)
- [Benchmarking de mercado](docs/01-negocio/02-benchmarking.md)
- [Mapa de capacidades](docs/01-negocio/03-mapa-capacidades.md)
- [Fronteras ERP / Retail / WMS / Farmacéutico](docs/01-negocio/04-fronteras-funcionales.md)
- [Mapa de procesos macro](docs/02-procesos/01-mapa-procesos.md)
- [Matriz fuente → capacidad](docs/01-negocio/05-matriz-fuente-capacidad.md)
- [Stakeholders y actores](docs/01-negocio/06-stakeholders-actores.md)
- [TO-BE abastecimiento y recepción](docs/02-procesos/02-to-be-abastecimiento-recepcion.md)
- [TO-BE transferencias e inventario](docs/02-procesos/03-to-be-transferencias-inventario.md)
- [TO-BE venta, dispensación y POS](docs/02-procesos/04-to-be-venta-dispensacion-pos.md)
- [TO-BE precios/promociones/Observatorio](docs/02-procesos/05-to-be-precios-promociones-observatorio.md)
- [Modelo de dominio DDD](docs/04-dominio/01-modelo-dominio.md)
- [Bounded Contexts / Context Map](docs/04-dominio/02-bounded-contexts-context-map.md)
- [Agregados / Entidades / Value Objects](docs/04-dominio/03-agregados-entidades-value-objects.md)
- [Referencias](docs/99-referencias.md)

## Secuencia de trabajo

```text
01. Visión y alcance
        ↓
02. Benchmarking
        ↓
03. Mapa de capacidades
        ↓
04. Procesos AS-IS / TO-BE
        ↓
05. Stakeholders y actores
        ↓
06. Requerimientos funcionales
        ↓
07. Requerimientos no funcionales
        ↓
08. Reglas de negocio
        ↓
09. Casos de uso y criterios de aceptación
        ↓
10. Modelo de dominio
        ↓
11. Arquitectura / ADR
        ↓
12. Modelo lógico y físico
        ↓
13. API / OpenAPI
        ↓
14. Seguridad / auditoría
        ↓
15. Pruebas / CI-CD
```

## Fase 2 — Procesos profundizados

Ya se encuentran documentados:

- compras/proveedores/ERP (Procure-to-Pay);
- caja/POS/cierre de turno;
- devoluciones y notas de crédito;
- productos fiscalizados;
- alertas e inmovilizaciones/retiros;
- farmacovigilancia y tecnovigilancia;
- cierre financiero e integración Retail → ERP;
- matriz proceso → regla → fuente.

Ver [Mapa de procesos](docs/02-procesos/01-mapa-procesos.md).
## Fase 3 — Requerimientos

La fase de requerimientos ya contiene:

- SRS funcional;
- catálogo de RF por dominio;
- RNF sin inventar SLO/RPO/RTO;
- reglas de negocio clasificadas por fuente;
- trazabilidad inicial proceso → RF → RN → prueba candidata.

Ver [docs/03-requerimientos/README.md](docs/03-requerimientos/README.md).

## Fase 4 — Casos de uso y criterios de aceptación

La especificación funcional ya fue refinada a comportamiento verificable:

- [Casos de uso detallados](docs/03-requerimientos/05-casos-uso.md)
- [Criterios de aceptación](docs/03-requerimientos/08-criterios-aceptacion.md)
- [Matriz de trazabilidad expandida](docs/03-requerimientos/07-matriz-trazabilidad.md)
- [Estrategia de pruebas](docs/10-pruebas/01-estrategia-pruebas.md)

Cobertura funcional: 22 CU prioritarios, 206 RF vinculados y 626 criterios de aceptación.

## Fase 5 — Modelo de Dominio DDD

El modelo de dominio ya incorpora:

- subdominios Core / Supporting / Generic;
- 16 Bounded Contexts candidatos;
- Context Map;
- agregados, entidades y Value Objects;
- estados/transiciones;
- comandos, Domain Events y servicios/políticas;
- invariantes sanitarias, fiscales y de dominio;
- lenguaje ubicuo;
- trazabilidad RF/CU/CA → dominio;
- matriz de origen de conceptos;
- decisiones abiertas que no deben hardcodearse.

Ver [docs/04-dominio/README.md](docs/04-dominio/README.md).

Esta fase quedó superada por la arquitectura y el modelo físico PostgreSQL 18 ya documentados.


## Fase 6 — Arquitectura y ADR

La arquitectura base ya fue definida como:

```text
Core Central
  = Monolito Modular multi-módulo
  + DDD
  + Clean Architecture / Ports & Adapters
  + CQRS selectivo
  + Result Pattern
  + Domain Events

Store Edge
  = perfil opcional por tienda para continuidad offline
  + base/servicio local
  + Outbox/Inbox
  + sincronización idempotente
```

Decisiones principales:

- POS `online-first` con perfil `STORE_EDGE` offline-capable;
- SUNAT/CPE detrás de Fiscal Port/Adapter;
- ERP detrás de un boundary/ACL independiente de si será propio o externo;
- no microservicios por Bounded Context;
- no Event Sourcing ni broker obligatorio por ahora;
- política detallada de stock offline continúa propuesta/por validar.

Ver [Arquitectura y ADR](docs/05-arquitectura/README.md).

Esta fase quedó superada por el modelo lógico/físico y la validación estática del DDL.

## Fase 7 — Datos, API, Seguridad y UX

Ya se incorporó la línea base documental que faltaba:

- [06 — Datos](docs/06-datos/README.md): modelo conceptual/lógico y ownership Central vs Store Edge.
- [07 — API](docs/07-api/README.md): REST, Problem Details, idempotencia, OpenAPI e integraciones.
- [08 — Seguridad](docs/08-seguridad/README.md): seguridad por capas, scopes, Store Edge, privacidad, pagos/PCI y auditoría.
- [09 — UX](docs/09-ux/README.md): arquitectura de información, POS, dispensación, inventario, offline y accesibilidad.

El Core Central ya cuenta con baseline físico PostgreSQL 18; la persistencia específica del Store Edge continúa pendiente de ADR.

## Fase 8 — DevOps, Roadmap y Estándares de Desarrollo

Ya se incorporó la capa documental complementaria:

- [11 — DevOps/DevSecOps](docs/11-devops/README.md): entornos, CI/CD, observabilidad, Store Edge, DR, releases, supply-chain security y runbooks.
- [12 — Roadmap](docs/12-roadmap/README.md): fases por capacidades, MVP candidato, gates, riesgos y decisiones pendientes.
- [13 — Estándares de Desarrollo](docs/13-estandares-desarrollo/README.md): SOLID/GRASP/KISS/YAGNI/DRY, patrones, Java candidato, frontend/POS, CQRS/Result/Events, Git, pruebas de arquitectura, persistencia y Definition of Done.

Estas secciones no fijan proveedor cloud, Kubernetes, herramienta CI/CD ni stack Java como decisiones definitivas. Las elecciones dependientes de tecnología siguen sujetas a ADR.



## Fase 9 — Consolidación del modelo físico

Se consolidaron los scripts de referencia aportados posteriormente con el modelo DDD/arquitectónico vigente. La fuente de verdad queda en `database/migrations/V001..V017`; el SQL consolidado es una representación derivada.

El enriquecimiento incluye campos adicionales de organización, catálogo regulatorio/SKU, proveedores/compras, lotes/stock/kardex, clientes/POS/pagos, prescripción/dispensación, controlados, CPE, recall/farmacovigilancia, ERP, IAM/RBAC, navegación, auditoría, integración, notificaciones y feature flags.

No se ejecutan en paralelo los antiguos scripts `V1/V2`; sus capacidades útiles fueron absorbidas y alineadas. RRHH completo, CRM/CMR avanzado y e-commerce/delivery completo permanecen diferidos hasta contar con RF/CU/ADR específicos.

Ver [consolidación V1/V2 y campos](docs/06-datos/08-consolidacion-v1-v2-campos.md).

## Base de datos PostgreSQL 18

La línea base física del Core Central se encuentra en [`database/`](database/README.md).

- [DDL consolidado PostgreSQL 18](database/cadena_farmacias_postgresql18.sql)
- [Migraciones V001–V017](database/migrations/)
- [Modelo físico documentado](docs/06-datos/07-modelo-fisico-postgresql18.md)
- [Consolidación V1/V2 y enriquecimiento de campos](docs/06-datos/08-consolidacion-v1-v2-campos.md)
- [Validación estática del DDL](docs/10-pruebas/02-validacion-ddl-postgresql18.md)
