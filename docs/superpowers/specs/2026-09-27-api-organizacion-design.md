# API de Organización (Empresa, Establecimiento, Almacén, Terminal POS) — Diseño

## Contexto

El módulo `organizacion` del backend (`service-botica/modules/organizacion/`) tiene hoy solo un slice parcial:
agregado `EmpresaOperadora` desalineado con la tabla real (usa `identifierType`/`identifierValue` genéricos en vez
de `ruc`, y le faltan casi todos los campos reales), un comando+handler que solo hace `save` (sin lectura), un
puerto sin `findById`, cero JPA, cero controller REST, cero código de `Establecimiento`/`Almacen`/`TerminalPos`.
El `build.gradle` del módulo ni siquiera depende de `shared-persistence`/`shared-web`.

El frontend (`features/organizacion/`) ya consume `GET /estructura-corporativa` con una llamada HTTP real
(`apiClient.get`, no mock hardcodeado), hoy interceptada por MSW porque no existe backend. Es usado en producción
por `AsignacionRolForm.tsx` (selects de empresa/establecimiento/almacén/terminal al asignar un rol con ámbito).

Las tablas de `sch_organizacion` ya existen y están aplicadas (`V002__organizacion_establecimientos.sql`, 8 tablas).
No se modifica el esquema — solo se agregan permisos RBAC nuevos.

## Evidencia de negocio

- RF-ORG-001 a 012 (`docs/cadena-farmacias-docs/docs/03-requerimientos/02-requerimientos-funcionales.md`, líneas
  36-51) cubren el ciclo completo de organización. Este spec implementa RF-ORG-001 (empresa), RF-ORG-002
  (establecimiento), RF-ORG-008 (almacenes), RF-ORG-009 (cajas/terminales), RF-ORG-010 (inactivación sin borrar
  historia) y RF-ORG-011 (consultar estructura corporativa).
- Fuera de alcance explícito (specs futuras): RF-ORG-003 (autorización sanitaria), RF-ORG-004/005/006 (Director
  Técnico/QF/personal técnico), RF-ORG-007 (horarios).
- Único CU documentado es `CU-ORG-001` ("Habilitar establecimiento farmacéutico y responsables"), con precondición
  "empresa operadora existente" — no hay CU propio de "registrar empresa" documentado; se diseña como capacidad
  base necesaria para que la precondición de CU-ORG-001 pueda cumplirse.
- Único endpoint candidato documentado (`07-api/02-recursos-endpoints-candidatos.md`, líneas 8-10) es
  `/api/v1/establecimientos` (GET/POST/PATCH) — sin marca NORM explícita, clasificado `POR_VALIDAR`. El resto del
  diseño de rutas (empresas, almacenes, terminales, prefijo `/organizacion`) es decisión técnica de este spec.

## Decisiones de alcance (confirmadas)

1. **`EmpresaOperadora` se reescribe completo** — se descarta `identifierType`/`identifierValue` genérico, se
   modela con `ruc` real validado con el regex SUNAT de la tabla (`^(10|20)[0-9]{9}$`) y se agregan todos los
   campos faltantes.
2. **Alcance de esta implementación:** los 4 agregados — Empresa Operadora, Establecimiento, Almacén y Terminal
   POS — con CRUD real (crear/consultar/listar/actualizar), como un solo plan de implementación.
3. **Paginación:** listados de los 4 agregados devuelven `page`/`size`/`totalElements`, mismo patrón que `security`
   (`PaginaResult<T>`/`PaginaResponse<T>`).
4. **Rutas REST:** recursos planos independientes bajo `/api/v1/organizacion/...`, filtrando por
   `empresaId`/`establecimientoId` vía query param (no anidamiento jerárquico en la URL).
5. **`GET /estructura-corporativa`** se mantiene como endpoint de lectura agregado aparte (sin prefijo
   `/organizacion`, igual a como ya lo consume el frontend), además de los CRUD individuales. No se migra
   `AsignacionRolForm.tsx`.
6. **Detalle de Almacén y Terminal POS:** campos completos de sus tablas reales (no solo lo que hoy consume el
   frontend).
7. **Plantilla arquitectónica de referencia: módulo `security`**, no `catalogo`. Patrón: puertos amplios
   (`OrganizacionReadPort`/`OrganizacionWritePort` en vez de un puerto por agregado), un adapter JDBC de lectura y
   un adapter JPA de escritura únicos para todo el módulo, mappers dedicados por capa
   (`api`/`application`/`infrastructure.persistence.read`/`infrastructure.persistence.write`).

## Arquitectura

Cuatro agregados en el mismo módulo `organizacion`, replicando la estructura exacta de `security`:

```
modules/organizacion/src/main/java/com/softprimesolutions/organizacion/
├── domain/
│   ├── model/          EmpresaOperadora, Establecimiento, Almacen, TerminalPos,
│   │                   EstadoEmpresaOperadora, EstadoEstablecimiento, TipoEstablecimiento,
│   │                   PerfilOperacion, TipoAlmacen, EstadoTerminalPos
│   └── valueobject/    EmpresaOperadoraId, EstablecimientoId, AlmacenId, TerminalPosId, TenantId
├── application/
│   ├── dto/
│   │   ├── command/    Crear/Actualizar × {EmpresaOperadora, Establecimiento, Almacen, TerminalPos}
│   │   ├── query/      Listar/Obtener × {EmpresaOperadora, Establecimiento, Almacen, TerminalPos}
│   │   │               + ObtenerEstructuraCorporativaQuery
│   │   └── result/     {X}Result × 4 + EstructuraCorporativaResult (árbol)
│   ├── port/
│   │   ├── in/         Crear/Actualizar/Listar/Obtener UseCase × 4 + ObtenerEstructuraCorporativaUseCase
│   │   └── out/        OrganizacionReadPort, OrganizacionWritePort
│   ├── usecase/
│   │   ├── command/    Crear/Actualizar Handler × 4
│   │   └── query/      Listar/Obtener Handler × 4 + ObtenerEstructuraCorporativaHandler
│   └── mapper/         OrganizacionApplicationMapper
├── infrastructure/
│   ├── persistence/
│   │   ├── write/{entity,repository,adapter,mapper}/
│   │   │   EmpresaOperadoraJpaEntity, EstablecimientoJpaEntity, AlmacenJpaEntity, TerminalPosJpaEntity
│   │   │   + sus JpaRepository, OrganizacionJpaWriteAdapter (único), OrganizacionWriteMapper
│   │   └── read/{projection,repository,adapter,mapper}/
│   │       EmpresaProjection, EstablecimientoProjection, AlmacenProjection, TerminalProjection
│   │       + OrganizacionJdbcReadRepository, OrganizacionJdbcReadAdapter (único), OrganizacionReadMapper
│   └── configuration/   OrganizacionModuleConfiguration
└── api/
    ├── controller/   EmpresaOperadoraController, EstablecimientoController, AlmacenController,
    │                 TerminalPosController, EstructuraCorporativaController
    ├── dto/{request,response}/   por agregado + PaginaResponse<T>
    └── mapper/       OrganizacionApiMapper
```

`build.gradle` del módulo gana dependencias a `shared-persistence` y `shared-web`.

## Modelo de dominio

Cada agregado sigue el patrón de `Rol.java`: constructor privado, factory `create(...)` (alta, devuelve
`Result<T, ErrorDetail>`), factory `restore(...)` (reconstrucción desde persistencia, sin validación), métodos de
comando (`updateDetails(...)`, `cambiarEstado(...)`) que también devuelven `Result<T, ErrorDetail>`, helpers
privados `invalid(field, message)`/`normalize`/`normalizeSpaces`, getters simples.

### `EmpresaOperadora`

Campos: `id (EmpresaOperadoraId)`, `tenantId (TenantId)`, `ruc`, `razonSocial`, `nombreComercial` (nullable),
`direccionFiscal` (nullable), `ubigeoFiscal` (nullable), `telefono` (nullable), `email` (nullable), `sitioWeb`
(nullable), `monedaFuncional` (default `"PEN"`), `zonaHoraria` (default `"America/Lima"`), `permiteVentaOnline`,
`estado (EstadoEmpresaOperadora: ACTIVO/SUSPENDIDO/BLOQUEADO)`, `createdAt`, `updatedAt`.

Validaciones: `ruc` obligatorio, exactamente 11 dígitos, match `^(10|20)[0-9]{9}$`; `razonSocial` 2-300 chars;
`nombreComercial` ≤300 si viene; `ubigeoFiscal` debe matchear `^[0-9]{6}$` si viene; `monedaFuncional` exactamente
3 chars; `zonaHoraria` no vacía. El `ruc` no se puede modificar tras creación (`updateDetails` no lo recibe).
Código de error de validación: `ORG_EMPRESA_INVALIDA`.

Métodos: `create(...)`, `restore(...)`, `updateDetails(razonSocial, nombreComercial, direccionFiscal, ubigeoFiscal,
telefono, email, sitioWeb, monedaFuncional, zonaHoraria, permiteVentaOnline, updatedAt)`,
`cambiarEstado(EstadoEmpresaOperadora, updatedAt)`.

### `Establecimiento`

Campos: `id`, `tenantId`, `empresaId (EmpresaOperadoraId)`, `codigo`, `nombre`,
`tipoEstablecimiento (TipoEstablecimiento, default BOTICA)`, `categoriaRegulatoriaCodigo` (nullable),
`codigoAnexoSunat` (default `"0000"`), `codigoDigemid` (nullable), `direccion` (nullable), `ubigeo` (nullable),
`referencia` (nullable), `latitud`/`longitud (BigDecimal, nullable)`, `telefono`/`email` (nullable), `esPrincipal`,
`permiteVentaOnline`, `permiteDelivery`, `perfilOperacion (PerfilOperacion: ONLINE/STORE_EDGE)`, `zonaHoraria`,
`estadoOperativo (EstadoEstablecimiento: ACTIVO/SUSPENDIDO/CLAUSURADO/REMODELACION)`, `createdAt`, `updatedAt`.

Validaciones: `empresaId` obligatorio; `codigo` 1-40 chars; `nombre` 2-250 chars; `codigoAnexoSunat` exactamente 4
dígitos; `codigoDigemid` ≤10 chars si viene; `ubigeo` 6 dígitos si viene. `empresaId` y `codigo` no se modifican
tras creación. Código de error: `ORG_ESTABLECIMIENTO_INVALIDO`.

Métodos: `create(...)`, `restore(...)`, `updateDetails(...)` (todos los campos excepto empresaId/codigo),
`cambiarEstadoOperativo(EstadoEstablecimiento, updatedAt)` — implementa RF-ORG-010: la transición a
SUSPENDIDO/CLAUSURADO no borra historia, solo cambia el estado.

### `Almacen`

Campos: `id`, `tenantId`, `establecimientoId (EstablecimientoId)`, `codigo`, `nombre`,
`tipo (TipoAlmacen: VENTA/GENERAL/CUARENTENA/REFRIGERADO/PSICOTROPICO/MERMA)`, `permiteLotes`, `permiteVencimiento`,
`permiteVenta`, `permiteDespacho`, `controlTemperatura`, `temperaturaMinC`/`temperaturaMaxC (BigDecimal, nullable)`,
`activo (boolean)`, `createdAt`, `updatedAt`.

**Nota:** la tabla `almacen` no tiene columna `estado` propia, solo `es_activo CHAR(1)` — por eso el campo es
`boolean activo`, no un enum de estado (a diferencia de Empresa/Establecimiento/Terminal que sí tienen `estado`).

Métodos: `create(...)`, `restore(...)`, `updateDetails(...)`, `activar()`/`desactivar(updatedAt)`.

### `TerminalPos`

Campos: `id`, `tenantId`, `establecimientoId`, `codigo`, `nombre`, `serieBoletaDefecto`/`serieFacturaDefecto`
(nullable), `numeroSerieEquipo` (nullable), `hostname` (nullable), `ipEquipo` (nullable, valida formato IP si
viene), `impresoraCodigo` (nullable), `storeEdgeHabilitado (boolean)`,
`estado (EstadoTerminalPos: ACTIVO/BLOQUEADO/MANTENIMIENTO)`, `createdAt`, `updatedAt`.

Métodos: `create(...)`, `restore(...)`, `updateDetails(...)`, `cambiarEstado(EstadoTerminalPos, updatedAt)`.

## Capa de aplicación

### Puertos de salida

```java
public interface OrganizacionReadPort {
    PaginaResult<EmpresaOperadoraResult> findEmpresas(UUID tenantId, String search, int page, int size);
    Optional<EmpresaOperadoraResult> findEmpresaById(UUID tenantId, UUID empresaId);

    PaginaResult<EstablecimientoResult> findEstablecimientos(
        UUID tenantId, UUID empresaId, String search, int page, int size);
    Optional<EstablecimientoResult> findEstablecimientoById(UUID tenantId, UUID establecimientoId);

    PaginaResult<AlmacenResult> findAlmacenes(
        UUID tenantId, UUID establecimientoId, String search, int page, int size);
    Optional<AlmacenResult> findAlmacenById(UUID tenantId, UUID almacenId);

    PaginaResult<TerminalPosResult> findTerminales(
        UUID tenantId, UUID establecimientoId, String search, int page, int size);
    Optional<TerminalPosResult> findTerminalById(UUID tenantId, UUID terminalId);

    EstructuraCorporativaResult findEstructuraCorporativa(UUID tenantId);
}

public interface OrganizacionWritePort {
    SaveEmpresaOutcome save(EmpresaOperadora empresa);
    SaveEstablecimientoOutcome save(Establecimiento establecimiento);
    SaveAlmacenOutcome save(Almacen almacen);
    SaveTerminalOutcome save(TerminalPos terminal);

    boolean tenantExists(UUID tenantId);
    boolean empresaBelongsToTenant(UUID empresaId, UUID tenantId);
    boolean establecimientoBelongsToTenant(UUID establecimientoId, UUID tenantId);

    enum SaveEmpresaOutcome { CREATED, UPDATED, TENANT_NOT_FOUND, DUPLICATE_RUC }
    enum SaveEstablecimientoOutcome { CREATED, UPDATED, EMPRESA_NOT_FOUND, DUPLICATE_CODIGO, DUPLICATE_DIGEMID }
    enum SaveAlmacenOutcome { CREATED, UPDATED, ESTABLECIMIENTO_NOT_FOUND, DUPLICATE_CODIGO }
    enum SaveTerminalOutcome { CREATED, UPDATED, ESTABLECIMIENTO_NOT_FOUND, DUPLICATE_CODIGO }
}
```

### Casos de uso

Por cada agregado: `Crear{X}UseCase`, `Actualizar{X}UseCase`, `Listar{X}UseCase`, `Obtener{X}UseCase` (16 puertos de
entrada), más `ObtenerEstructuraCorporativaUseCase`. Cada Handler sigue exactamente el patrón de
`CrearRolHandler`/`ListarRolesHandler`: construye/valida el agregado de dominio, llama al puerto de escritura,
traduce los outcomes a `StandardApplicationError` con `ErrorCategory` (`NOT_FOUND`/`CONFLICT`/`VALIDATION`), usa
`IdentifierGenerator` + `ClockPort` para altas. Los handlers de listado validan `page >= 0` y `1 <= size <= 100`
igual que `ListarRolesHandler`.

`EstructuraCorporativaResult` replica el shape ya consumido por el frontend: árbol de
`EmpresaNodo { id, legalName, tradeName, status, establecimientos: List<EstablecimientoNodo> } `, donde
`EstablecimientoNodo { id, code, name, status, timeZone, warehouses: List<Nodo>, cashRegisters: List<Nodo> }` y
`Nodo { id, code, name, status }` — mapea 1:1 a `CompanyStructure`/`EstablishmentStructure`/`OrganizationalNode`
del frontend.

## Infraestructura

- **Escritura:** `EmpresaOperadoraJpaEntity`, `EstablecimientoJpaEntity`, `AlmacenJpaEntity`, `TerminalPosJpaEntity`
  — mismo patrón de `RolJpaEntity`: `Long id` interno autogenerado + `uuid_publico UUID` como identidad expuesta al
  dominio, `tenant_id BIGINT` (FK a `sch_admin.tenant`, no UUID — el dominio usa `TenantId` como wrapper del
  `uuid_publico` del tenant, resuelto igual que en `security`). Sus `JpaRepository` (Spring Data), un
  `OrganizacionJpaWriteAdapter` único implementando `OrganizacionWritePort`, y `OrganizacionWriteMapper` (dominio
  ↔ entidad).
- **Lectura:** `OrganizacionJdbcReadRepository` (JDBC directo vía `JdbcClient`, paginación `LIMIT/OFFSET` igual que
  `IamJdbcReadRepository`), proyecciones `EmpresaProjection`/`EstablecimientoProjection`/`AlmacenProjection`/
  `TerminalProjection`, un `OrganizacionJdbcReadAdapter` único implementando `OrganizacionReadPort` (incluye el
  ensamblado del árbol completo para `findEstructuraCorporativa`, con 4 queries — empresas, establecimientos,
  almacenes, terminales — agrupadas en memoria), y `OrganizacionReadMapper`.
- **Configuración:** `OrganizacionModuleConfiguration` registra los ~17 handlers como `@Bean`.

## API REST

```
POST/GET   /api/v1/organizacion/empresas
GET/PUT    /api/v1/organizacion/empresas/{empresaId}
POST/GET   /api/v1/organizacion/establecimientos?empresaId=...
GET/PUT    /api/v1/organizacion/establecimientos/{establecimientoId}
POST/GET   /api/v1/organizacion/almacenes?establecimientoId=...
GET/PUT    /api/v1/organizacion/almacenes/{almacenId}
POST/GET   /api/v1/organizacion/terminales-pos?establecimientoId=...
GET/PUT    /api/v1/organizacion/terminales-pos/{terminalId}
GET        /api/v1/estructura-corporativa
```

Listados devuelven `PaginaResponse<T>` (`items`, `page`, `size`, `totalElements`), igual que `security`.
Autorización con `@PreAuthorize`, permisos separados gestionar/consultar por recurso:

```
organizacion.empresas.consultar / .gestionar
organizacion.establecimientos.consultar / .gestionar
organizacion.almacenes.consultar / .gestionar
organizacion.terminales-pos.consultar / .gestionar
```

`GET /api/v1/estructura-corporativa` requiere `organizacion.empresas.consultar` (lectura agregada de todo el
árbol).

## Migración Flyway

Las tablas de `sch_organizacion` ya existen y están aplicadas (`V002`) — no se modifican. Se agrega únicamente:

- `V025__seed_organizacion_permissions.sql` — sigue el patrón de `V021__seed_catalogo_permissions.sql`: inserta 8
  permisos (`organizacion.empresas.*`, `.establecimientos.*`, `.almacenes.*`, `.terminales-pos.*`) referenciando
  `sch_seguridad.modulo_sistema` código `'ORGANIZACION'` (ya sembrado en `V016__navegacion_dinamica_rbac.sql`, no
  requiere crear el módulo).

## Compatibilidad con el frontend existente

`GET /estructura-corporativa` se implementa con el mismo shape que ya consume `AsignacionRolForm.tsx` vía
`corporateStructureQuery` (`frontend/apps/erp-web/src/features/organizacion/api/organization.api.ts`) — cero
cambios en ese archivo, en `index.ts`, ni en los tipos `CorporateStructure`/`CompanyStructure`/
`EstablishmentStructure`/`OrganizationalNode`. El handler MSW de `handlers.ts` línea 32 se mantiene intacto (sigue
sirviendo para tests con MSW); al levantar el backend real contra un entorno con mock desactivado, la llamada HTTP
real reemplaza al mock sin cambios de contrato.

## Manejo de errores

`GlobalExceptionHandler`/`ProblemDetail` (RFC 9457) de `shared-web`, igual que `security`/`catalogo`:
- `VALIDATION` → 400 (campos de dominio inválidos: RUC mal formado, código fuera de rango, etc.)
- `NOT_FOUND` → 404 (tenant/empresa/establecimiento inexistente al crear un hijo o consultar por id)
- `CONFLICT` → 409 (RUC duplicado, código de establecimiento/almacén/terminal duplicado, DIGEMID duplicado)

## Fuera de alcance (specs futuras)

- RF-ORG-003: autorización sanitaria del establecimiento (`establecimiento_autorizacion_sanitaria`).
- RF-ORG-004/005/006: Director Técnico, QF asistentes, personal técnico (`profesional_farmaceutico`,
  `asignacion_profesional`).
- RF-ORG-007: horarios operativos.
- `ubicacion_almacen` (zonas/pasillos/racks internos de almacén).

Ninguno de estos datos aparece en `EstructuraCorporativaResult` ni en los DTOs de este spec.
