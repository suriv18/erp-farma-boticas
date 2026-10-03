# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Fuente única de verdad

Este es un monorepo para un ERP de cadena de farmacias en Perú. No existe un documento único de gobierno (no hay `docs/00-gobierno/` ni `GOV-FAR-001` en el repo pese a referencias previas a ese nombre); la fuente de verdad de negocio/arquitectura está distribuida entre estos documentos de `docs/cadena-farmacias-docs/`:
- `README.md` — punto de entrada: estado de fase actual, secuencia de trabajo de 15 pasos y la regla de evidencia (`NORM/MKT/FUNC/DOM/STD/TEC/POR_VALIDAR`) que clasifica el origen de cualquier capacidad/regla/dato — nunca presentar una decisión `TEC` o `MKT` como obligación `NORM`.
- `docs/02-procesos/13-matriz-proceso-regla-fuente.md` — precedencia de reglas de negocio por proceso.
- `docs/06-datos/08-consolidacion-v1-v2-campos.md` y `database/README.md` — precedencia entre el DDL legado (V1/V2) y el modelo físico actual (`database/cadena_farmacias_postgresql18.sql` + `database/migrations/V001-V017`, validado solo estáticamente, aún no ejecutado contra PostgreSQL real).
- `docs/05-arquitectura/adr/` — los ADR (11 a la fecha).

`docs/arquitectura/`, `docs/sprint/`, `docs/decisiones-adr/` y `docs/db/` (fuera de `cadena-farmacias-docs/`) son antecedentes/borradores legados, conservados solo para trazabilidad — no confiar en ellos como estado actual sin contrastarlos contra `docs/cadena-farmacias-docs/`.

**Estado real del proyecto (no asumir más de lo implementado):**
- `service-botica/`: monolito modular Java/Spring que compila y verifica 17 módulos explícitos, pero la mayoría son scaffolds. El módulo `security` (IAM: login, JWT, RBAC, sesiones, dispositivos) es el más maduro y funcional del backend. `catalogo` es el segundo módulo más completo: expone CRUD REST real (CQRS + `Result` + `@PreAuthorize`) para 10 de los ~11 catálogos maestros de `sch_catalogo` bajo `/api/v1/catalogo/*` (categorías, marcas, rubros comerciales, condiciones de venta, formas farmacéuticas, vías de administración, unidades de medida, clasificaciones controladas, principios activos, productos regulados/SKU); falta solo `tipo_documento_identidad` (V018, existe en el diseño de BD pero sin controller/handler/DTO). El módulo no tiene tests de integración HTTP (`*ApiIntegrationTest`), solo cobertura unitaria de application/domain. `organizacion` expone CRUD REST real (CQRS + `Result` + `@PreAuthorize`) para Empresa Operadora, Establecimiento, Almacén y Terminal POS bajo `/api/v1/organizacion/*` (Empresa y Establecimiento con `PATCH .../estado`; Almacén y Terminal cambian de estado dentro de su `PUT`), más `GET /api/v1/estructura-corporativa` (el tenant se resuelve del claim `tid` del JWT, no de un parámetro); los permisos `organizacion.*` los siembra `V025` y se conceden al rol `ADMIN` de FARMALAB. Aplica reglas de negocio propias: RUC con dígito verificador (módulo 11), series de comprobantes únicas por empresa (índices `V026`), altas de hijos bloqueadas bajo un padre `SUSPENDIDO`/`BLOQUEADO` (empresa) o `SUSPENDIDO`/`CLAUSURADO` (establecimiento) y almacenes con temperaturas coherentes; las reglas de series, estado del padre y refrigerado están marcadas POR_VALIDAR. Tiene cobertura unitaria al 100% y `OrganizacionApiIntegrationTest` (login real, permisos `organizacion.*` de `V025`, PostgreSQL vía Testcontainers), y su alta/edición se verificó end-to-end en navegador desde el frontend contra el backend real. `inventario` (lotes, posiciones y movimientos con idempotencia) y `compras` (proveedores, órdenes de compra y recepciones con ingreso a inventario) ya tienen controladores REST, adapters JDBC/JPA y migraciones propias (`V027`–`V032`), con `InventarioApiIntegrationTest`, `ComprasApiIntegrationTest` y tests de concurrencia (`InventarioConcurrencyIntegrationTest`, `ComprasConcurrencyIntegrationTest`). Los demás módulos de negocio (ventas, etc.) aún no tienen controladores, adapters de persistencia ni migraciones propias.
- `frontend/`: scaffold React navegable, no un ERP funcional todavía en conjunto, pero con slices reales ya integrados contra `packages/api-client` (fetch wrapper + TanStack Query, sin mocks). `features/seguridad/` (permisos, roles, usuarios) es la feature más madura, con listado+detalle, formularios react-hook-form/zod y diálogos de confirmación. `features/catalogo/` sigue el mismo patrón para 2 de los 10 catálogos maestros ya expuestos por el backend (categorías y marcas); los otros 8 (rubros comerciales, condiciones de venta, formas farmacéuticas, vías de administración, unidades de medida, clasificaciones controladas, principios activos, productos regulados/SKU) aún no tienen `api/`/páginas/rutas propias. No hay handlers de MSW para ningún endpoint de catálogo. Dashboard usa MSW/mocks, `features/inventario/` tiene UI real (posiciones paginadas con filtros por establecimiento/almacén/SKU en la URL, ingreso con lote nuevo y ajustes de stock que envían un `Idempotency-Key` estable por payload, detalle de lote con bloqueo/desbloqueo) integrada contra `packages/api-client`, sin handlers de MSW, con e2e de Playwright simulado y verificada en navegador contra el backend real; limitación vigente: `PosicionResponse` no trae el nombre del SKU, `features/organizacion/` tiene UI real de gestión (empresas → detalle de empresa → detalle de establecimiento con almacenes y terminales POS, formularios completos y cambio de estado) integrada contra `packages/api-client`, sin handlers de MSW para sus endpoints, verificada en navegador contra el backend real (alta y edición de empresa, establecimiento, almacén y terminal); el e2e de Playwright (`frontend/e2e/`, desktop/tablet/móvil) corre con Vite en modo `http` y simula la API con `page.route`, y compras/ventas/POS/caja/clientes siguen siendo placeholders sin lógica.

Una pantalla visible, un módulo detectado en el código o una tabla en el DDL legado **no** equivalen a una capacidad terminada. El flujo de entrega es `RF/RN → CU/CA → dominio → ADR → contrato/modelo → slice → pruebas`.

## Comandos

### Backend (`service-botica/`) — Java 25, Spring Boot 4.1, Spring Modulith 2.1, Gradle 9.5.1

```powershell
cd service-botica
.\gradlew.bat check --warning-mode all      # build + tests + ArchUnit + Spring Modulith verify
.\gradlew.bat architectureTest              # solo pruebas de arquitectura
.\gradlew.bat test                          # solo tests unitarios/integración
.\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.security.api.IamApiIntegrationTest"   # un test/clase específico
.\gradlew.bat :bootstrap-app:bootJar        # genera bootstrap-app/build/libs/service-botica.jar
.\gradlew.bat :bootstrap-app:bootRun        # levanta la app (requiere PostgreSQL)
```

Requiere JDK 25 (Gradle usa toolchains, no una ruta fija) y PostgreSQL para el perfil normal. El perfil `test` usa H2 en memoria con Flyway deshabilitado (`ddl-auto: create`) — ver `bootstrap-app/src/test/resources/application-test.yaml`.

Configuración mínima local (tiene defaults de dev, nunca poner credenciales productivas en el repo):
```
DB_URL=jdbc:postgresql://localhost:5432/erp_botica
DB_USERNAME=postgres
DB_PASSWORD=postgres
SERVER_PORT=8080
```

### Frontend (`frontend/`) — React 19.2.8, React Router 8.3.0, Vite 8.2.1, Tailwind CSS 4.3.3, pnpm 11.9.0, Node 24.14.1

```bash
corepack enable
pnpm install --frozen-lockfile
pnpm dev            # servidor de desarrollo en http://localhost:3000
pnpm check           # lint + typecheck + test + build (usar antes de dar por terminado un cambio)
pnpm test            # vitest run (todas)
pnpm test:watch      # vitest en watch
pnpm --filter @boticas/erp-web test -- ruta/al/archivo.test.tsx   # un archivo específico
pnpm lint
pnpm typecheck
pnpm format          # prettier --write
pnpm format:check
pnpm build
pnpm preview
```

En Windows PowerShell, si la política de ejecución bloquea `pnpm.ps1`, usar `pnpm.cmd` en los mismos comandos.

## Arquitectura de alto nivel

### Backend: Clean Architecture + DDD + Ports & Adapters + CQRS + Result Pattern

Cada bounded context vive bajo `service-botica/modules/<contexto>/` como subproyecto Gradle independiente, con paquete raíz `com.softprimesolutions.<contexto>` y capas:
- `domain/` (model, valueobject, event, exception, service) — no depende de Spring, JPA ni adaptadores.
- `application/` (port/in = casos de uso, port/out = puertos de infraestructura, usecase/{command,query} = handlers, dto) — los casos de uso devuelven `Result<T>` para errores esperables del negocio, nunca `null` ni excepciones para casos previstos.
- `infrastructure/` (persistence/{read,write}, configuration, client) — CQRS pragmático: escritura vía JPA en `persistence/write`, lectura vía JDBC/projections en `persistence/read`, sin bases de datos separadas.
- `api/` — adaptador REST (controllers, DTOs de request/response, mapper); es la única superficie pública del módulo hacia otros módulos y hacia el exterior.

Reglas de dependencia entre módulos (verificadas por ArchUnit + Spring Modulith `ApplicationModules.verify()`, ambas corren dentro de `check`):
- Un módulo solo expone tipos vía su paquete `api`; nunca se importan paquetes internos de otro módulo.
- Una dependencia permitida por `@ApplicationModule(allowedDependencies = ...)` de Spring Modulith también debe declararse explícitamente en `build.gradle` antes de poder importar la API de otro subproyecto — son dos capas de verificación independientes, hay que actualizar ambas.
- `shared-kernel` no depende de Spring (tipos base: `Result`, `ErrorDetail`, `AggregateRoot`, `DomainEvent`, identificadores). `shared-application` aporta contratos CQRS transversales (`Command`, `Query`, `CommandHandler`, `ApplicationError`). `shared-web` centraliza `GlobalExceptionHandler`/mapeo a `ProblemDetail` (RFC 9457). `shared-persistence` solo config técnica de transacciones — ninguno de los `shared-*` contiene lógica de negocio ni repositorios concretos.
- `bootstrap-app` es el único módulo ejecutable (`@SpringBootApplication`) y punto de ensamblaje; no contiene controladores ni reglas de negocio propias.
- Las carpetas `domain`, `application`, `adapter` se crean por vertical slice cuando existe comportamiento real — no generar capas vacías por adelantado.

**No existe un contexto de "actor autenticado" compartido**: cada módulo que necesita saber quién hace la operación debe resolverlo por su cuenta desde `Jwt`/`Authentication` de Spring Security (no hay `ActorId` reutilizable en `shared-*` todavía).

**Módulo `security` (IAM)** es el más completo: login local + JWT (access + refresh opaco con rotación y detección de reuso) vive en `infrastructure.configuration` (`LocalAuthSecurityConfiguration`, `SessionAwareJwtDecoder`, `LocalJwtAuthenticationConverter`). Las autorizaciones HTTP (`@PreAuthorize("hasAuthority(...)")`) se resuelven **en cada request** consultando permisos efectivos desde BD (no se confía en claims de rol/permiso embebidos en el JWT). El modelo RBAC soporta ámbito organizacional (`GLOBAL/EMPRESA/ESTABLECIMIENTO/ALMACEN/TERMINAL`) vía `usuario_rol_ambito` — al modificar la resolución de permisos efectivos, verificar que se cubran todos los niveles de ámbito, no solo `GLOBAL` (hubo una regresión real de esto en `LocalAuthJdbcAdapter`, corregida — ver el test `grantsEffectiveAuthoritiesFromEstablishmentScopedRoleAssignment` en `IamApiIntegrationTest` como caso de referencia para probar autorización real vía login, no solo inyectando authorities con `SecurityMockMvcRequestPostProcessors.user(...)`).

**Base de datos:** `bootstrap-app/src/main/resources/db/migration` es la ubicación de migraciones Flyway propias del proyecto (numeración continúa desde V018 en adelante). Sin embargo, `bootstrap-app/build.gradle` también agrega `docs/cadena-farmacias-docs/database` como `sourceSet` de recursos, por lo que las migraciones "de diseño" en `docs/cadena-farmacias-docs/database/migrations/` (V001-V017, esquema base multi-tenant, IAM, auditoría, navegación RBAC) **también se ejecutan como Flyway real** vía `spring.flyway.locations: classpath:migrations,classpath:db/migration`. Tener esto presente: cambiar un script en `docs/` cambia el esquema de ejecución real, pese a que esa carpeta se documenta como "diseño". Antes de escribir una migración nueva, revisar ambas ubicaciones para no romper la numeración ni duplicar tablas.

### Frontend: pnpm workspace liviano, features por dominio

```
frontend/
├── apps/erp-web/       # única app desplegable (ERP + POS conviven aquí por ahora)
├── packages/api-client/  # cliente HTTP compartido — no depende de React ni de features
├── packages/ui-web/      # componentes visuales compartidos (React/React DOM como peerDependencies)
└── package.json          # scripts y config raíz del workspace
```

Dentro de `apps/erp-web/src/`, el código de negocio vive en `features/<dominio>/` (auth, dashboard, seguridad, organizacion, catalogo, inventario, compras, ventas, pos, caja, clientes), cada uno publicando su superficie pública vía `index.ts`. `app/feature-routes.ts` compone las rutas de todas las features; el router y otras features **no deben importar archivos internos de otra feature**, solo su `index.ts`. `shared/` solo contiene piezas técnicas locales realmente usadas por varias features — no es un cajón general.

Un paquete nuevo en `packages/` solo se justifica con ≥2 consumidores reales y una frontera técnica estable (ver criterios en `docs/arquitectura/estructura_frontend_multimodular.md` §16); no crear `packages/` por cada feature.

**Configuración de API:** en desarrollo, `.env.development` activa modo mock vía MSW para trabajar sin backend. En producción, la base es `/api/v1`. Contra un backend local, Vite redirige `/api` a `http://localhost:8080` cuando el modo mock está desactivado. Toda variable expuesta al navegador debe empezar con `VITE_` (son públicas por definición, nunca secretos).

React Router 8 y componentes con `forwardRef` (patrón antiguo) están bloqueados por reglas de ESLint — no reintroducir esos patrones.

## Cobertura de tests

Todo archivo fuente **nuevo**, backend (`service-botica/`) y frontend (`frontend/`), debe alcanzar 100% de cobertura de líneas y ramas en sí mismo, exigido como gate automático en el build (JaCoCo por clase/archivo en Gradle para los módulos Java; umbrales por archivo de Vitest para el workspace pnpm). No es un umbral retroactivo global: el código ya existente al momento de introducir el gate no está obligado a llegar a 100% salvo que se reescriba o se cree de nuevo; un archivo nuevo con cobertura menor a 100% no se considera terminado aunque compile y los tests existentes pasen.

## Estándares de implementación

Toda implementación, backend y frontend, debe seguir:
- Bajo acoplamiento, alta cohesión.
- Código mantenible; principios SOLID y Clean Code.
- Sin comentarios explicativos en el código (evitarlos salvo lo estrictamente necesario para una decisión no obvia).
- Patrones de diseño aplicados según la necesidad y complejidad real del problema, no por adelantado.
- Preferir funciones lambda donde el lenguaje/framework lo permita idiomáticamente.
- Prohibido el código duplicado: identificar patrones repetidos y extraer abstracciones reutilizables.
- Respetar la arquitectura definida (Clean Architecture + DDD + Ports & Adapters + CQRS en backend; estructura por features en frontend) y la estructura de carpetas del proyecto descrita en este documento — no introducir capas, paquetes o convenciones nuevas sin justificarlo.

## Atributos de calidad arquitectónica

Toda decisión de arquitectura y diseño, backend y frontend, debe estar guiada por: trazabilidad, rendimiento, corrección, mantenibilidad, seguridad y usabilidad.
