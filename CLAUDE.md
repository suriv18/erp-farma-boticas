# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Fuente única de verdad

Este es un monorepo para un ERP de cadena de farmacias en Perú. El punto de entrada canónico de negocio/arquitectura es `docs/cadena-farmacias-docs/docs/00-gobierno/01-fuente-unica-verdad.md` (referenciado como `GOV-FAR-001`) — ese documento define la precedencia entre requisitos, ADR, código existente y artefactos legados. `docs/arquitectura/`, `docs/sprint/`, `docs/decisiones-adr/` y `docs/db/` son antecedentes/borradores legados, conservados solo para trazabilidad — no confiar en ellos como estado actual sin contrastarlos contra `docs/cadena-farmacias-docs/`.

**Estado real del proyecto (no asumir más de lo implementado):**
- `service-botica/`: monolito modular Java/Spring que compila y verifica 17 módulos explícitos, pero la mayoría son scaffolds. El módulo `security` (IAM: login, JWT, RBAC, sesiones, dispositivos) es el más maduro y funcional del backend. `organizacion` tiene un primer slice parcial (agregado, command/handler, puerto). Los demás módulos de negocio (ventas, inventario, compras, etc.) aún no tienen controladores, adapters de persistencia ni migraciones propias.
- `frontend/`: scaffold React navegable, no un ERP funcional. Login es simulado (no llama al backend, sesión solo en memoria), dashboard usa MSW/mocks, inventario usa fixtures, organización consulta un contrato mock, y catálogo/compras/ventas/POS/caja/clientes/seguridad son placeholders sin lógica.

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
pnpm dev            # servidor de desarrollo en http://localhost:5173
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
