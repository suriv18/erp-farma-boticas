# Diseño: API REST de TipoDocumentoIdentidad (catálogo SUNAT 06)

## Contexto

`service-botica` expone CRUD REST para 9 de los ~11 catálogos maestros de `sch_catalogo` bajo `/api/v1/catalogo/*`. Faltan controladores para `rubro_comercial` (ya completado en un ciclo previo) y `tipo_documento_identidad` (V018), que ya existe en BD desde la migración `V018__catalogo_tipo_documento_identidad_sunat.sql`.

`tipo_documento_identidad` es el catálogo SUNAT 06 (código de tipo de documento de identidad): tabla global compartida por todos los tenants, sin `tenant_id`. Su forma es equivalente a los "catálogos de soporte" ya implementados (`CondicionVenta`, `FormaFarmaceutica`, `ViaAdministracion`, `UnidadMedida`, `ClasificacionControlada`): PK natural `codigo` (string), sin agregado multi-tenant, sin identidad UUID. No sigue el patrón de `RubroComercial` (que es un agregado `AggregateRoot` con `TenantId` y `RubroComercialId` UUID).

Este documento cubre el slice completo (dominio → aplicación → infraestructura → API REST) para `tipo_documento_identidad`, replicando exactamente el patrón arquitectónico de `CondicionVenta` (el catálogo de soporte más representativo, con el mayor número de campos).

## Forma de los datos (tabla `sch_catalogo.tipo_documento_identidad`)

```sql
codigo          VARCHAR(2) NOT NULL PK   -- regex ^[0-9A-Z]$ (un solo carácter alfanumérico)
sigla           VARCHAR(30) NOT NULL     -- no vacío tras trim
denominacion    VARCHAR(200) NOT NULL    -- no vacío tras trim
max             SMALLINT NULL            -- > 0 si no es NULL
min             SMALLINT NULL            -- > 0 si no es NULL; min <= max si ambos no son NULL
es_activo       CHAR(1) NOT NULL DEFAULT '1'  -- '0'/'1'
```

No tiene columnas de vigencia (`vigente_desde`/`vigente_hasta`) ni `fuente`/`version_fuente` como `CondicionVenta`; es más simple.

## Componentes

### 1. Dominio — `domain/model/soporte/TipoDocumentoIdentidad.java`

VO inmutable plano (sin `AggregateRoot`, sin `TenantId`), mismo estilo que `CondicionVenta`:

- Campos: `codigo` (String), `sigla` (String), `denominacion` (String), `max` (Integer, nullable), `min` (Integer, nullable), `estado` (`EstadoCatalogoSoporte`, reutilizado — ya existe y es genérico ACTIVO/INACTIVO).
- `create(codigo, sigla, denominacion, max, min)` → `Result<TipoDocumentoIdentidad, ErrorDetail>`:
  - `codigo`: obligatorio, normalizado a mayúsculas, debe cumplir `^[0-9A-Z]$` (exactamente 1 carácter alfanumérico en mayúscula).
  - `sigla`: obligatorio tras trim/normalización de espacios, máx 30 caracteres.
  - `denominacion`: obligatorio tras trim/normalización de espacios, máx 200 caracteres (sin mínimo de 2 explícito en el CHECK de BD, pero se aplica el mismo mínimo de 2 que el resto de catálogos por consistencia con la regla de negocio general de "no vacío significativo").
  - `max`: si no es null, debe ser > 0.
  - `min`: si no es null, debe ser > 0; si ambos `min` y `max` no son null, `min <= max`.
  - Código de error: `CAT_TIPO_DOCUMENTO_IDENTIDAD_INVALIDO`.
- `restore(codigo, sigla, denominacion, max, min, estado)` → reconstrucción desde persistencia, sin validar.
- Getters: `codigo()`, `sigla()`, `denominacion()`, `max()`, `min()`, `estado()`.

### 2. Aplicación

**DTOs** (`application/dto/`):
- `command/CrearTipoDocumentoIdentidadCommand(String codigo, String sigla, String denominacion, Integer max, Integer min)`
- `command/ActualizarTipoDocumentoIdentidadCommand(String codigo, String sigla, String denominacion, Integer max, Integer min)`
- `query/ConsultarTipoDocumentoIdentidadQuery(String codigo)`
- `query/ListarTiposDocumentoIdentidadQuery(String estado)`
- `result/TipoDocumentoIdentidadResult(String codigo, String sigla, String denominacion, Integer max, Integer min, String estado)`

**Puertos in** (`application/port/in/`):
- `CrearTipoDocumentoIdentidadUseCase`
- `ActualizarTipoDocumentoIdentidadUseCase`
- `ConsultarTipoDocumentoIdentidadUseCase`
- `ListarTiposDocumentoIdentidadUseCase`

**Puerto out** — ampliar `CatalogoSoportePort` (no crear uno nuevo, mismo patrón que los demás catálogos de soporte comparten un único puerto):
```java
SaveOutcome save(TipoDocumentoIdentidad tipoDocumentoIdentidad);
Optional<TipoDocumentoIdentidad> findTipoDocumentoIdentidadByCodigo(String codigo);
boolean tipoDocumentoIdentidadExists(String codigo);
boolean changeTipoDocumentoIdentidadStatus(String codigo, String status, Instant changedAt);
```

**Handlers** (`application/usecase/command|query/`), espejo de `CrearCondicionVentaHandler`/`ActualizarCondicionVentaHandler`/`ConsultarCondicionVentaHandler`/`ListarCondicionesVentaHandler`:
- `CrearTipoDocumentoIdentidadHandler`: valida no-duplicado (`tipoDocumentoIdentidadExists`), construye el dominio vía `create`, persiste, mapea a `Result`.
- `ActualizarTipoDocumentoIdentidadHandler`: reconstruye, valida, persiste (`SaveOutcome.UPDATED`/`NOT_FOUND`).
- `ConsultarTipoDocumentoIdentidadHandler`: `findTipoDocumentoIdentidadByCodigo`, mapea o error `NOT_FOUND`.
- `ListarTiposDocumentoIdentidadHandler`: lista completa filtrable por `estado`.
- `CatalogoApplicationMapper`: agregar `toResult(TipoDocumentoIdentidad)`.
- `CatalogoControlService`: agregar `changeTipoDocumentoIdentidadStatus(codigo, status)` delegando al puerto (mismo patrón que `changeCondicionVentaStatus`).

### 3. Infraestructura

- `infrastructure/persistence/write/entity/TipoDocumentoIdentidadJpaEntity.java`: `@Id` `codigo` (String), columnas `sigla`, `denominacion`, `max`, `min`, `esActivo` (CHAR(1) mapeado igual que las demás entidades de soporte).
- `infrastructure/persistence/write/repository/TipoDocumentoIdentidadJpaRepository.java`: `JpaRepository<TipoDocumentoIdentidadJpaEntity, String>`.
- `CatalogoSoporteWriteMapper`: agregar `toEntity(TipoDocumentoIdentidad)` / `toDomain(TipoDocumentoIdentidadJpaEntity)`.
- `CatalogoSoporteJpaWriteAdapter`: implementar los 4 métodos nuevos del puerto (save con detección de duplicado, find, exists, changeStatus), siguiendo exactamente el bloque existente de `CondicionVenta`.
- `CatalogoJdbcReadRepository` / `CatalogoJdbcReadAdapter`: agregar `findTipoDocumentoIdentidadByCodigo` y `findTiposDocumentoIdentidad(estado)` (lista simple sin paginar, igual que `findCondicionesVenta`) para el lado de lectura usado por los handlers de consulta/listado.
- `CatalogoModuleConfiguration`: registrar el bean del repositorio y cablear el nuevo caso de uso si aplica (seguir el wiring existente de `CondicionVenta*`).

### 4. API REST

- `api/dto/request/TipoDocumentoIdentidadRequest.java`: `codigo`, `sigla`, `denominacion`, `max` (nullable), `min` (nullable), con `jakarta.validation` (`@NotBlank`, `@Size`) espejando las constraints de dominio.
- `api/dto/response/TipoDocumentoIdentidadResponse.java`: mismos campos + `estado`.
- `CatalogoApiMapper`: `toCreateCommand`, `toUpdateCommand`, `toResponse` para `TipoDocumentoIdentidad`.
- `api/controller/TipoDocumentoIdentidadController.java`, ruta base `/api/v1/catalogo/tipos-documento-identidad`, mismas 5 operaciones y mismas authorities que `CondicionVentaController` (`catalogo.soporte.gestionar` / `catalogo.soporte.consultar` — recurso de catálogo de soporte compartido, no una authority nueva):
  - `POST` → crear (201)
  - `PUT /{codigo}` → actualizar (200)
  - `PATCH /{codigo}/estado` → cambiar estado (204), usa `CambiarEstadoGlobalRequest` existente
  - `GET /{codigo}` → consultar (200)
  - `GET` (`?estado=`) → listar (200)

### 5. Tests

TDD estricto por capa (test-first), replicando la cobertura existente para `CondicionVenta`:
- `domain/model/soporte/TipoDocumentoIdentidadTest.java`: casos válidos + cada regla de validación (código vacío/no-1-char/no-alfanumérico-mayúscula, sigla vacía/larga, denominación vacía/larga, max/min inválidos, min > max).
- `application/usecase/command/CrearTipoDocumentoIdentidadHandlerTest.java`: creación exitosa + duplicado.
- `application/usecase/query/ListarTiposDocumentoIdentidadHandlerTest.java`: listar sin filtro y filtrado por estado (mismo patrón que `ListarCondicionesVentaHandlerTest`, que ya cubre el read adapter JDBC vía H2/test profile).

No se agregan tests de `Actualizar`/`Consultar` handler como unidades nuevas más allá de lo que ya cubre el patrón (el plan original tampoco los tenía para todos los catálogos de soporte); se valida su comportamiento vía el test de listar/crear y, si el checklist de implementación lo requiere, un test de actualización exitosa análogo a `CrearTipoDocumentoIdentidadHandlerTest`.

## Fuera de alcance

- No se toca `rubro_comercial` (ya completo).
- No se agregan nuevas authorities RBAC — se reutilizan `catalogo.soporte.gestionar`/`catalogo.soporte.consultar`.
- No se pagina el listado (los catálogos de soporte no lo hacen; solo `ProductoRegulado`/`SKUComercial` son paginados).
- No se crea agregado `AggregateRoot` ni `TenantId` — este catálogo es global, no multi-tenant.
