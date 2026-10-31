# Mi puesto de trabajo y acceso por permisos — diseño

Fecha: 2026-10-19. Estado: diseño aprobado por el dueño del producto (enfoque A).
Insumos: `docs/observaciones/2026-10-15-analisis-dueno-y-administrador-de-organizacion.md` (pasos 4 y 5), `docs/superpowers/specs/2026-10-15-organizacion-superadmin-delegacion-design.md` §8 (subproyectos D y E).

## 1. Objetivo y alcance

Que un cajero pueda recorrer en el navegador **login → Caja (abrir turno) → POS (registrar venta) → cierre de turno** con permisos mínimos, y que la interfaz solo le muestre lo que puede usar.

Verificado en el navegador el 2026-10-19 con `crit001.manana1`: el login funciona con el correo, pero Caja no ofrece establecimiento ni terminal (403 en `/estructura-corporativa` y `/organizacion/terminales-pos`), y el menú muestra Seguridad y Organización.

Incluye:

1. `GET /api/v1/organizacion/mi-puesto` y consulta de alcance en `security.api`.
2. `permissions` en `GET /api/v1/auth/me`.
3. Caja y POS leen de `mi-puesto`.
4. Menú, rutas y botones principales condicionados por permiso; página 403.
5. Login que acepta usuario o correo.
6. Corrección de datos de prueba (hecha: `CRIT-ALM` con `permite_venta = true`).

Fuera de alcance: asistente de alta de usuario y plantilla de turnos mañana/tarde (subproyectos C y E), `ActorScope` y delegación (B1), migrar compras y ventas a `mi-puesto`, aislamiento por organización (B2).

## 2. Backend

### 2.1 Alcance del usuario (`security.api`)

- Interfaz nueva `AlcanceUsuario` en `security.api` con `AlcanceResultado alcanceDe(UUID tenantId, UUID usuarioId)`.
- `AlcanceResultado`: `boolean global`, `Set<UUID> empresas`, `Set<UUID> establecimientos`, `Set<UUID> almacenes`, `Set<UUID> terminales` (uuid públicos), calculado desde `usuario_rol_ambito` con las mismas reglas de `AutorizacionAmbitoJdbcAdapter`: `estado = 'ACTIVO'`, `es_activo = '1'`, `vigente_desde <= now()` y `vigente_hasta` nulo o futuro.
- Adapter JDBC en `security/infrastructure/persistence/read/adapter`, junto a `AutorizacionAmbitoJdbcAdapter`; la condición de vigencia se extrae a un fragmento compartido para no duplicarla.
- No filtra por permiso: informa el alcance, no autoriza operaciones.

### 2.2 Endpoint `mi-puesto` (`organizacion`)

- `GET /api/v1/organizacion/mi-puesto`, `@PreAuthorize("isAuthenticated()")`; tenant y usuario salen del JWT (`tid`, `sub`).
- Caso de uso de consulta `ObtenerMiPuestoUseCase` con `Result<MiPuestoResult>`; puerto `AlcancePort` en `application/port/out` implementado por un adapter en `infrastructure/client` que llama a `AlcanceUsuario` (patrón de `SecurityAmbitoAdapter` en `ventas`).
- Reglas:
  - `global` o empresa en alcance: todos los establecimientos activos de esas empresas (`global`: de todo el tenant).
  - Ámbito `ESTABLECIMIENTO`: ese establecimiento con todas sus terminales y almacenes activos.
  - Ámbito `TERMINAL` o `ALMACEN`: solo esos recursos, dentro de su establecimiento.
  - Solo establecimientos con `estado_operativo = 'ACTIVO'` y empresa `ACTIVO`.
  - Sin asignaciones vigentes: lista vacía (200).
- Respuesta: `{ establecimientos: [{ id, codigo, nombre, almacenes: [{ id, codigo, nombre, tipo, permiteVenta }], terminales: [{ id, codigo, nombre, estado, operable }] }] }`; `operable = estado == 'ACTIVO'`. Las terminales no operables se incluyen para que la UI las muestre deshabilitadas.
- Lectura en `OrganizacionReadPort` (método nuevo con filtro de alcance) e implementación JDBC en `OrganizacionJdbcReadAdapter`.

### 2.3 Dependencias entre módulos

- `organizacion/package-info.java`: `allowedDependencies = {"security::api"}`; `organizacion/build.gradle`: `implementation project(':modules:security')`.
- `security/package-info.java`: se elimina `organizacion::api` (declarado y no usado) para evitar el ciclo; se verifica con `ModulithArchitectureTest` y `CleanArchitectureTest`.

### 2.4 `/auth/me` con permisos

- `PerfilResult` agrega `permissions: List<String>` (permisos efectivos del usuario, ordenados), obtenidos con la misma consulta que usa `LocalJwtAuthenticationConverter` en cada request.
- Es informativo para la UI: el backend sigue autorizando con `@PreAuthorize`.

### 2.5 Migraciones

- No requiere migración de esquema. El endpoint no exige permiso propio. Si se decide un permiso explícito más adelante, va en V043 (V041/V042 están reservadas por el diseño del 15-oct).

## 3. Frontend

### 3.1 `mi-puesto`

- `features/organizacion/api/mi-puesto.api.ts`: `miPuestoQuery` (`apiClient.get('/organizacion/mi-puesto')`), exportada por `organizacion/index.ts`.
- `TerminalSelector` y `PuestoVenta` pasan a leer de `miPuestoQuery`; `useEstablecimientos` y `useEstablecimientosEstado` quedan para los consumidores actuales (compras, ventas) hasta migrarlos.
- Terminales no operables: opción deshabilitada con su estado ("En mantenimiento", "Bloqueada"). Almacén de POS: solo los de `permiteVenta`; si no hay ninguno, mensaje claro y POS no permite vender.
- Un solo establecimiento y una sola terminal operable: se preselecciona. `usePuestoTrabajo` conserva el puesto recordado, validado contra la respuesta.

### 3.2 Acceso por permisos

- `features/auth`: `useProfile` expone `permissions`; helper `usePermisos()` con `puede(codigo)` y `puedeAlgun(prefijo)`.
- Menú de `AppShell`: cada ítem declara el permiso (o prefijo) que lo habilita; los ítems sin permiso no se renderizan. Resumen visible para todos los autenticados.
- Rutas: guarda por permiso en `app/feature-routes.ts`; sin permiso, página 403 ("No tienes acceso a esta sección").
- Botones principales de crear/editar en Seguridad y Organización condicionados con `puede(...)`. Otras acciones se tratan en E.

### 3.3 Login con usuario

- `login.schema.ts`: campo `login` con texto no vacío (se mantiene el formato de correo solo si contiene `@`); `LoginForm` pasa a `type="text"` con `autoComplete="username"`; el backend ya acepta usuario o correo.
- Ajustar `LoginPage.test.tsx` y `e2e/support/login.ts`.

## 4. Pruebas

- **Backend unitarias (100 % en clases nuevas):** reglas de alcance por tipo de ámbito, vigencia y estado, caso de uso de `mi-puesto`, mapeo de `permissions`.
- **Integración con login real y PostgreSQL (Testcontainers):** cajero de un establecimiento ve solo el suyo; cajero con ámbito `TERMINAL` ve solo esa terminal; administrador `GLOBAL` ve todos; usuario sin asignación vigente recibe lista vacía; una terminal en mantenimiento llega con `operable = false`; `/auth/me` devuelve los permisos del rol.
- **Arquitectura:** `ModulithArchitectureTest` y `CleanArchitectureTest` sin ciclos.
- **Frontend (Vitest + MSW, 100 % en archivos nuevos):** selector con terminales deshabilitadas, preselección, almacén sin `permiteVenta`, menú y guardas por permiso, página 403, login con usuario.
- **Playwright:** ajustar `caja.spec.ts` y `pos.spec.ts` (mock de `mi-puesto`).
- **Verificación manual en navegador (`localhost:3000`):** `crit001.manana1` inicia sesión con usuario, ve solo Resumen, Ventas, Punto de venta, Caja y los módulos que sus permisos permitan, abre turno en `CRIT-POS`, registra una venta en efectivo y cierra el turno; `crit001.tarde1` repite el ciclo en `CRIT-POS2` sin ver el turno de la mañana; `ui001` muestra su terminal en mantenimiento como deshabilitada.

## 5. Riesgos

- Los consumidores de `useEstablecimientos` en compras y ventas siguen fallando con 403 para un cajero; se tratan como trabajo aparte.
- Los datos de prueba están sucios (`SUSP-001` figura activo, `CLAUS-ALM` dice "clausurado" en un local activo); la verificación manual se limita a `crit001`, `e2e001` y `ui001`.
- Calcular el alcance en cada llamada a `mi-puesto` y `/auth/me` añade consultas; se mide y, si hace falta, se cachea por petición.
- `AlcanceUsuario` debe converger con `ActorScope` de B1 cuando exista, sin duplicar reglas.
