# Plan por fases: módulo Clientes

**Fecha:** 2026-10-30
**Estado:** propuesto. La fase 0 está en curso; las demás esperan sus decisiones.
**Flujo del repo:** RF/RN → CU/CA → dominio → ADR → contrato/modelo → slice → pruebas.

## Punto de partida (verificado por exploración el 2026-10-30)

| Capa | Estado |
|---|---|
| Modelo de datos | `sch_venta.cliente` y `sch_venta.cliente_consentimiento` existen en `V007__retail_pos_ventas.sql`, que Flyway ejecuta. `sch_catalogo.tipo_documento_identidad` está en V018 y V024. |
| Backend | `service-botica/modules/clientes` contiene solo dos `package-info.java`. `crm` y `cmr` están igual de vacíos y permiten `clientes::api`. |
| Frontend | `features/clientes` es un `ModulePlaceholderPage`. La sección se abre con cualquier permiso `ventas.ventas.*`. |
| Ventas | Anónimas. `venta.cliente_id` existe en la tabla (admite NULL) y ningún código ni migración posterior lo usa. |
| Permisos | No hay permisos `clientes.*` sembrados. |

Requisito existente: `RF-POS-006` (Gestionar cliente de venta). Reglas aplicables: `RN-SEC-001` (Ley 29733), `RN-SEC-003` (marketing separado), `RN-FVG-002`.

## Fase 0: Decisiones y requisitos (sin código)

Entregable: [especificación de decisiones](../specs/2026-10-30-clientes-fase0-decisiones-design.md). Cierre: el usuario confirma las decisiones y se escribe `ADR-012`.

## Fase 1: Modelo de datos (migración V041 en adelante)

Revisar antes la numeración en las dos ubicaciones de migraciones.

- Alinear `cliente.tipo_documento` con el catálogo (hoy `VARCHAR(2)` frente a `VARCHAR(1)` tras V024), con FK y CHECK de longitud por tipo.
- Catálogo de finalidades de consentimiento, versión del texto, base jurídica y canal. Cambiar el `ON DELETE CASCADE` por una política que conserve la evidencia.
- Clasificar `fecha_nacimiento` y `email`. Decidir cifrado y RLS por ADR aparte.
- Sembrar permisos `clientes.*` y concederlos al rol ADMIN de FARMALAB, como hizo `V025`.
- Verificar contra PostgreSQL real (Testcontainers) y confirmar que V007 corre como Flyway real.

Cierre: migraciones aplican limpias y hay un test de esquema.

## Fase 2: Backend `clientes`

Un slice por caso de uso (CQRS + `Result`), siguiendo `organizacion`.

- Declarar dependencias en `allowedDependencies` y en `build.gradle` (`catalogo::api`, probablemente `security::api`).
- Casos de uso: registrar, actualizar, cambiar estado, consultar por id o documento, listar con filtros y paginación, otorgar y revocar consentimiento.
- API `/api/v1/clientes/*` con `@PreAuthorize`; tenant desde el claim `tid`; validación de documento (reutilizar el módulo 11 de RUC de `organizacion`).
- Publicar en `clientes::api` una consulta mínima para `ventas`.
- Pruebas: cobertura unitaria al 100 %, `ClientesApiIntegrationTest` con login real y aislamiento entre tenants.

Cierre: `gradlew check` verde (ArchUnit y Modulith incluidos).

## Fase 3: Frontend `features/clientes`

Patrón de `organizacion` e `inventario`.

- `api/` con tipos, `invalidate.ts` y tests; reutilizar paginación y query-string.
- Esquemas zod con validación DNI/RUC (reutilizar `organizacion/lib/ruc.ts`).
- Listado con filtros en la URL, detalle, alta y edición, gestión de consentimientos.
- Permiso propio en `acceso-secciones.ts` y botones por permiso.
- Vitest al 100 % en archivos nuevos, `routes.test.ts` y `e2e/clientes.spec.ts`; verificación en navegador contra el backend real.

Cierre: `pnpm check` en verde, descontando el fallo global de cobertura ya existente.

## Fase 4: Integración con Ventas y POS

- `cliente_id` opcional en la venta, con `clientes::api` en `allowedDependencies` y la dependencia en el `build.gradle` de ventas. Snapshot del adquirente para el comprobante.
- Selector de cliente en el POS con búsqueda por documento y opción de venta sin identificar. El comprobante muestra el cliente.
- Ampliar `VentaApiIntegrationTest` y el e2e del POS.

Cierre: venta con cliente de punta a punta, y las ventas anónimas siguen funcionando.

## Fase 5: Documentación y cierre

- Actualizar `CLAUDE.md` (`tipo_documento_identidad` ya está implementado) y el estado real del módulo.
- Corregir la contradicción entre `db/migration/README.md` y el `sourceSet` de `bootstrap-app/build.gradle`.
- Dejar crédito y fidelización como POR_VALIDAR con su propia fase.

## Riesgos

- La fase 1 es la menos reversible (esquema y datos personales); por eso la fase 0 va antes.
- Las dependencias entre módulos se verifican en dos capas (Modulith y Gradle); olvidar una rompe el build.
- `cuenta_por_cobrar` ya referencia `cliente`; cambios en su PK o estado la afectan.
- Un PR por fase.
