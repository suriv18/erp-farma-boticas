> **SUPERSEDIDO:** este diseño creaba un esquema `sch_catalogo` nuevo, duplicando el dominio ya definido en `docs/cadena-farmacias-docs/database/migrations/V003__catalogo_farmaceutico.sql` (`sch_farmacia.categoria_producto`/`producto_regulado`/`sku_comercial`/etc.), que ya se ejecuta como Flyway real. El módulo Catálogo real se diseña en un documento nuevo mapeando JPA contra esas tablas existentes. Se conserva este archivo solo como registro histórico de la primera iteración de diseño.

# Módulo Catálogo: Producto + Categoría — Design

## Contexto y motivación

El frontend real tiene 7 de 10 módulos de negocio como placeholders vacíos (`ModulePlaceholderPage` reciclado), entre ellos `catalogo`. Existe un prototipo standalone (`app-botica/`) con una UI de referencia para Catálogo, Inventario, POS, Compras, Caja, Clientes y Seguridad — útil como referencia de UX/interacción, pero sin backend, sin Tailwind, y con un modelo de dominio (`Product`/`InventoryItem` en `app-botica/src/types.ts`) que mezcla responsabilidades de catálogo (ficha maestra) e inventario (stock, lote, costo) en un solo tipo.

Se decidió portar primero **Catálogo** (backend real) porque **Inventario** depende de él: un `InventoryItem` necesita referenciar un producto existente, y no tiene sentido modelar el producto dos veces.

Este documento cubre **solo el backend de Catálogo** (agregados `Producto` y `Categoria`, con persistencia y API REST reales). El frontend de Catálogo y el módulo Inventario completo son trabajo posterior, con su propio spec.

## Alcance

**Incluido:**
- Agregado `Categoria`: CRUD completo (crear, actualizar, desactivar, reactivar, listar).
- Agregado `Producto`: CRUD completo (crear, actualizar, desactivar, reactivar, consultar por id, listar paginado con filtros), con el set de campos regulatorios completo visto en el prototipo (DIGEMID, principio activo, condición de venta, etc.).
- Persistencia real: JPA para escritura, JDBC para lectura, migración Flyway, nuevo esquema `sch_catalogo`.
- API REST completa con autorización basada en permisos nuevos.
- Multi-tenant: cada empresa (tenant) tiene su propio catálogo de productos y categorías.

**Explícitamente fuera de alcance de este slice:**
- Stock, costo, lote, vencimiento, ubicación física — pertenecen al módulo Inventario (spec siguiente).
- Flujo de aprobación/validación de catálogo (`catalogValidation` del prototipo) — no hay reglas de aprobación definidas todavía.
- Jerarquía de categorías (padre/hijo) — categorías son planas.
- Frontend de Catálogo (portar la vista del prototipo a Tailwind/`ui-web`) — se hace después de tener esta API.
- Integración con el módulo `organizacion` (`EmpresaOperadora`) — ese agregado es dominio puro sin persistencia todavía; en su lugar, el scoping multi-tenant usa el mismo `tenantId` (claim `tid` del JWT) que ya resuelve `security` en cada request. Se reconsiderará la relación con `organizacion::api` cuando ese módulo tenga persistencia real.

## Arquitectura

Sigue exactamente el mismo patrón de Clean Architecture + DDD + Ports & Adapters + CQRS ya usado y maduro en `modules/security` — no el estilo más nuevo/simplificado de `modules/organizacion` (que todavía no tiene infraestructura ni controller de referencia). Esto da consistencia con el módulo más completo del backend y evita introducir una segunda convención en el repo.

Como en `security`, `catalogo` no define infraestructura propia común: reutiliza los tipos base ya existentes en los módulos transversales `shared-*` (no hay un módulo literal `common`):
- `shared-kernel`: `Result<T,E>`, `ErrorDetail`, `AggregateRoot`, `EntityId<T>` — sin dependencia de Spring.
- `shared-application`: `Command<R>`, `CommandHandler<C,R>`, `Query<R>`, `QueryHandler<Q,R>`, `ApplicationError`, `ErrorCategory`, `StandardApplicationError`, `IdentifierGenerator`, `ClockPort`.
- `shared-web`: `GlobalExceptionHandler`, `ApplicationErrorHttpMapper` (traduce `ApplicationError` → `ProblemDetail` RFC 9457 según `ErrorCategory`).
- `shared-persistence`: solo config técnica de transacciones (sin repositorios concretos).

`TenantId` **no** vive en ningún `shared-*` — `security` define su propio `TenantId` (record sobre `UUID`) dentro de su paquete `domain/valueobject`, y `catalogo` hace lo mismo con el suyo, sin compartir el tipo entre módulos (no existe todavía un `ActorId`/`TenantId` reutilizable transversal, según lo documentado en `CLAUDE.md`).

Capas, siguiendo la misma nomenclatura de `security`:

- `domain/model/` — agregados (`Categoria`, `Producto`) y enums de estado (`EstadoCategoria`, `EstadoProducto`, `TipoProducto`, `CondicionVenta`).
- `domain/valueobject/` — IDs fuertes (`CategoriaId`, `ProductoId`, reutilizando el mismo patrón `record` con `UUID` que `RolId`/`UsuarioId`; `TenantId` propio del módulo, igual que `security` define su propio `TenantId` en vez de compartir uno).
- `application/dto/{command,query,result}/` — records de entrada/salida.
- `application/port/in/` — una interfaz `@FunctionalInterface` por caso de uso (`CrearCategoriaUseCase`, `ListarCategoriasUseCase`, etc.), método único `execute(...)`.
- `application/port/out/` — dos puertos por módulo (no uno por agregado, igual que `IamWritePort`/`IamReadPort` agrupan Usuario+Rol+Permiso): `CatalogoWritePort` (con outcomes enum como `SaveCategoriaOutcome`, `SaveProductoOutcome`) y `CatalogoReadPort`.
- `application/usecase/{command,query}/` — handlers, uno por caso de uso, implementando su interfaz de `port/in`.
- `application/mapper/` — `CatalogoApplicationMapper` (dominio → `*Result`), estático, igual que `IamApplicationMapper`.
- `infrastructure/persistence/write/{entity,repository,adapter,mapper}` — JPA.
- `infrastructure/persistence/read/{projection,repository,adapter,mapper}` — JDBC vía `JdbcClient`.
- `api/controller/` — `CategoriaController`, `ProductoController`, inyectando los puertos `in` (no los handlers directamente), con un `CatalogoControllerSupport` package-private igual a `IamControllerSupport`.
- `api/dto/{request,response}/` — DTOs HTTP con validación Jakarta.
- `api/mapper/` — `CatalogoApiMapper` estático (DTO ↔ Command/Query ↔ Result).

Activar/desactivar (`Categoria`/`Producto`) sigue el patrón de `SecurityControlService.changeUserStatus`: es un `UPDATE` directo vía el puerto de escritura (`changeCategoriaStatus`/`changeProductoStatus`), sin reconstruir el agregado completo desde el dominio — el dominio no necesita un método `activate()/deactivate()` porque la transición no tiene invariantes propias más allá del enum válido.

No hay import cruzado entre `Categoria` y `Producto` como módulos separados — ambos viven dentro de `catalogo`, agrupados por sufijo de nombre de clase (`CrearCategoriaX` vs `CrearProductoX`), igual que `security` agrupa Usuario/Rol/Permiso en el mismo árbol de paquetes sin subcarpetas por agregado.

### Estructura de paquetes

```
modules/catalogo/src/main/java/com/softprimesolutions/catalogo/
├── package-info.java                          (ya existe, @ApplicationModule)
├── domain/
│   ├── model/
│   │   ├── Categoria.java                     (AggregateRoot)
│   │   ├── EstadoCategoria.java               (enum: ACTIVA, INACTIVA)
│   │   ├── Producto.java                      (AggregateRoot)
│   │   ├── EstadoProducto.java                (enum: ACTIVO, INACTIVO)
│   │   ├── TipoProducto.java                  (enum: MEDICAMENTO, DISPOSITIVO_MEDICO, PRODUCTO_SANITARIO, SUPLEMENTO_ALIMENTO, ARTICULO_NO_SANITARIO)
│   │   └── CondicionVenta.java                (enum: SIN_RECETA, CON_RECETA, RECETA_RETENIDA)
│   └── valueobject/
│       ├── CategoriaId.java                   (record VO sobre UUID)
│       ├── ProductoId.java                    (record VO sobre UUID)
│       └── TenantId.java                      (record VO sobre UUID, propio del módulo)
├── api/
│   ├── package-info.java                      (ya existe, @NamedInterface("api"))
│   ├── controller/
│   │   ├── CategoriaController.java
│   │   ├── ProductoController.java
│   │   └── CatalogoControllerSupport.java     (package-private, Result→ProblemDetail)
│   ├── dto/
│   │   ├── request/
│   │   │   ├── CrearCategoriaRequest.java
│   │   │   ├── ActualizarCategoriaRequest.java
│   │   │   ├── CambiarEstadoRequest.java       (reutilizado por categoria y producto: {"status": "..."})
│   │   │   ├── CrearProductoRequest.java
│   │   │   └── ActualizarProductoRequest.java
│   │   └── response/
│   │       ├── CategoriaResponse.java
│   │       ├── ProductoResponse.java
│   │       └── PaginaResponse.java             (genérico, igual a security)
│   └── mapper/
│       └── CatalogoApiMapper.java
├── application/
│   ├── dto/
│   │   ├── command/
│   │   │   ├── CrearCategoriaCommand.java
│   │   │   ├── ActualizarCategoriaCommand.java
│   │   │   ├── CrearProductoCommand.java
│   │   │   └── ActualizarProductoCommand.java
│   │   ├── query/
│   │   │   ├── ListarCategoriasQuery.java
│   │   │   ├── ConsultarProductoQuery.java
│   │   │   └── ListarProductosQuery.java
│   │   └── result/
│   │       ├── CategoriaResult.java
│   │       ├── ProductoResult.java
│   │       └── PaginaResult.java               (genérico, igual a security)
│   ├── mapper/
│   │   └── CatalogoApplicationMapper.java
│   ├── port/
│   │   ├── in/
│   │   │   ├── CrearCategoriaUseCase.java
│   │   │   ├── ActualizarCategoriaUseCase.java
│   │   │   ├── ListarCategoriasUseCase.java
│   │   │   ├── CrearProductoUseCase.java
│   │   │   ├── ActualizarProductoUseCase.java
│   │   │   ├── ConsultarProductoUseCase.java
│   │   │   ├── ListarProductosUseCase.java
│   │   │   └── CatalogoControlUseCase.java      (activar/desactivar categoria y producto)
│   │   └── out/
│   │       ├── CatalogoWritePort.java
│   │       └── CatalogoReadPort.java
│   └── usecase/
│       ├── command/
│       │   ├── CrearCategoriaHandler.java
│       │   ├── ActualizarCategoriaHandler.java
│       │   ├── CrearProductoHandler.java
│       │   ├── ActualizarProductoHandler.java
│       │   └── CatalogoControlService.java      (implementa CatalogoControlUseCase)
│       └── query/
│           ├── ListarCategoriasHandler.java
│           ├── ConsultarProductoHandler.java
│           └── ListarProductosHandler.java
└── infrastructure/
    └── persistence/
        ├── write/
        │   ├── entity/
        │   │   ├── CategoriaJpaEntity.java
        │   │   └── ProductoJpaEntity.java
        │   ├── repository/
        │   │   ├── CategoriaJpaRepository.java
        │   │   └── ProductoJpaRepository.java
        │   ├── mapper/
        │   │   └── CatalogoWriteMapper.java
        │   └── adapter/
        │       └── CatalogoJpaWriteAdapter.java  (implementa CatalogoWritePort completo)
        └── read/
            ├── projection/
            │   ├── CategoriaProjection.java
            │   └── ProductoProjection.java
            ├── repository/
            │   └── CatalogoJdbcReadRepository.java
            ├── mapper/
            │   └── CatalogoReadMapper.java
            └── adapter/
                └── CatalogoJdbcReadAdapter.java  (implementa CatalogoReadPort completo)
```

## Dominio

Siguiendo el patrón de `Rol`/`Usuario` en `security`: el agregado expone un factory estático `create(...)` (equivalente a `register`) que valida invariantes y devuelve `Result<T, ErrorDetail>`, y un factory `restore(...)` para reconstrucción desde persistencia sin validar de nuevo. **Activar/desactivar no pasa por el agregado** — sigue el patrón de `SecurityControlService.changeUserStatus`: es un `UPDATE` directo emitido por el puerto de escritura (`CatalogoWritePort.changeCategoriaStatus`/`changeProductoStatus`), devolviendo `boolean` (encontrado/no encontrado), sin reconstruir ni revalidar el agregado completo.

### `Categoria`

Campos: `id` (CategoriaId), `tenantId` (TenantId propio del módulo, envuelve UUID), `nombre` (2-100 chars, normalizado trim+colapso espacios, único por tenant), `descripcion` (opcional, hasta 500 chars), `estado` (ACTIVA por defecto al crear), `createdAt`, `updatedAt`.

Invariantes: `id`, `tenantId`, `nombre` obligatorios; `nombre` dentro de longitud; unicidad de nombre por tenant se valida en el `WritePort` (outcome `DUPLICATE_NAME`), no en el dominio (el dominio no consulta persistencia).

Factory: `Categoria.create(CategoriaId, TenantId, String nombre, String descripcion, Instant createdAt): Result<Categoria, ErrorDetail>`, más `restore(CategoriaId, TenantId, String nombre, String descripcion, EstadoCategoria, Instant createdAt, Instant updatedAt): Categoria`.

Código de error: `CAT_CATEGORIA_INVALIDA`.

### `Producto`

Campos: `id` (ProductoId), `tenantId` (TenantId), `categoriaId` (CategoriaId, obligatorio — no se valida existencia en dominio, eso es responsabilidad del handler vía `CatalogoReadPort`/`CatalogoWritePort`), `nombre` (2-200 chars), `tipo` (TipoProducto), `laboratorio` (opcional, hasta 150 chars), `unidadMedida` (obligatorio, hasta 30 chars, ej. "Tableta", "Frasco"), `presentacion` (opcional, hasta 150 chars), `unidadesPorPaquete` (entero positivo, default 1), `codigoBarras` (opcional, hasta 40 chars), `precioVenta` (BigDecimal, > 0), `condicionVenta` (opcional salvo excepción abajo), `esGenerico` (boolean, default false), `esGenericoEsencial` (boolean, default false), `grupoTerapeutico` (opcional, hasta 150 chars), `codigoDigemid` (opcional, hasta 40 chars), `principioActivo` (opcional salvo excepción abajo, hasta 200 chars), `concentracion` (opcional, hasta 60 chars), `requiereLote` (boolean, default false), `requiereVencimiento` (boolean, default false), `estado` (ACTIVO por defecto), `createdAt`, `updatedAt`.

**Invariante de negocio clave**: si `tipo == MEDICAMENTO`, entonces `principioActivo` y `condicionVenta` son obligatorios (no nulos/vacíos). Para los demás tipos, ambos campos son opcionales.

Factory: `Producto.create(ProductoId, TenantId, CategoriaId, String nombre, TipoProducto, String laboratorio, String unidadMedida, String presentacion, int unidadesPorPaquete, String codigoBarras, BigDecimal precioVenta, CondicionVenta, boolean esGenerico, boolean esGenericoEsencial, String grupoTerapeutico, String codigoDigemid, String principioActivo, String concentracion, boolean requiereLote, boolean requiereVencimiento, Instant createdAt): Result<Producto, ErrorDetail>`, más `restore(...)` equivalente con todos los campos + `EstadoProducto` + `updatedAt`.

Código de error: `CAT_PRODUCTO_INVALIDO`.

## Casos de uso (puertos `in` + Commands/Queries)

Cada caso de uso es una interfaz `@FunctionalInterface` en `application/port/in/` con un único método `execute(...)`, implementada por un handler en `application/usecase/{command,query}/` — igual que `CrearRolUseCase`/`CrearRolHandler` en `security`. Todos los commands/queries llevan `tenantId` (UUID público de `sch_farmacia.tenant`) explícito como campo (no hay contexto de actor compartido — cada módulo lo resuelve por su cuenta, igual que `security`). Los adapters de persistencia resuelven ese UUID al `id` interno (`BIGINT`) de `sch_farmacia.tenant` antes de leer/escribir, con el mismo patrón `findTenantInternalId(UUID): Optional<Long>` ya usado en `LocalAuthJdbcAdapter`.

**Categoría:**
- `CrearCategoriaUseCase.execute(CrearCategoriaCommand(UUID tenantId, String nombre, String descripcion))` → `Result<CategoriaResult, ApplicationError>`
- `ActualizarCategoriaUseCase.execute(ActualizarCategoriaCommand(UUID tenantId, UUID categoriaId, String nombre, String descripcion))` → `Result<CategoriaResult, ApplicationError>`
- `ListarCategoriasUseCase.execute(ListarCategoriasQuery(UUID tenantId, String estado))` → `Result<List<CategoriaResult>, ApplicationError>` (sin paginar; el volumen esperado de categorías es bajo)

**Producto:**
- `CrearProductoUseCase.execute(CrearProductoCommand(UUID tenantId, UUID categoriaId, String nombre, TipoProducto tipo, String laboratorio, String unidadMedida, String presentacion, Integer unidadesPorPaquete, String codigoBarras, BigDecimal precioVenta, CondicionVenta condicionVenta, boolean esGenerico, boolean esGenericoEsencial, String grupoTerapeutico, String codigoDigemid, String principioActivo, String concentracion, boolean requiereLote, boolean requiereVencimiento))` → `Result<ProductoResult, ApplicationError>`
- `ActualizarProductoUseCase.execute(ActualizarProductoCommand(...))` (mismos campos + `productoId`) → `Result<ProductoResult, ApplicationError>`
- `ConsultarProductoUseCase.execute(ConsultarProductoQuery(UUID tenantId, UUID productoId))` → `Result<ProductoResult, ApplicationError>`
- `ListarProductosUseCase.execute(ListarProductosQuery(UUID tenantId, String texto, UUID categoriaId, TipoProducto tipo, String estado, int page, int size))` → `Result<PaginaResult<ProductoResult>, ApplicationError>`

**Control de estado (activar/desactivar), un único puerto para ambos agregados — igual patrón que `SecurityControlUseCase`:**
- `CatalogoControlUseCase.changeCategoriaStatus(UUID tenantId, UUID categoriaId, String status)` → `Result<Unit, ApplicationError>`
- `CatalogoControlUseCase.changeProductoStatus(UUID tenantId, UUID productoId, String status)` → `Result<Unit, ApplicationError>`

Implementado por `CatalogoControlService` en `application/usecase/command/`, igual que `SecurityControlService`.

### Manejo de errores de aplicación

- Validación de dominio fallida → `ApplicationError` categoría `VALIDATION`, código heredado del dominio (`CAT_CATEGORIA_INVALIDA`/`CAT_PRODUCTO_INVALIDO`).
- Nombre de categoría duplicado → `CAT_CATEGORIA_DUPLICADA`, categoría `CONFLICT`.
- Código de barras duplicado → `CAT_PRODUCTO_CODIGO_BARRAS_DUPLICADO`, categoría `CONFLICT`.
- Categoría referenciada no existe (al crear/actualizar producto) → `CAT_CATEGORIA_NO_ENCONTRADA`, categoría `NOT_FOUND` (el handler de producto consulta `CatalogoWritePort.categoriaExists` antes de persistir).
- Producto/categoría no encontrado por id (consultar/actualizar/cambiar estado) → `CAT_PRODUCTO_NO_ENCONTRADO` / `CAT_CATEGORIA_NO_ENCONTRADA`, categoría `NOT_FOUND`.
- Estado inválido en cambio de estado (valor fuera de `ACTIVA/INACTIVA` o `ACTIVO/INACTIVO`) → `CAT_ESTADO_INVALIDO`, categoría `VALIDATION` (igual patrón que `SecurityControlService.invalidStatus`).

## Persistencia

Nuevo esquema `sch_catalogo`, migración `V022__catalogo_producto_categoria.sql` en `bootstrap-app/src/main/resources/db/migration/` (siguiendo la convención V018+ ya usada, no la carpeta legada de `docs/`).

```sql
CREATE SCHEMA IF NOT EXISTS sch_catalogo;

CREATE TABLE sch_catalogo.categoria (
    id              BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico    UUID NOT NULL DEFAULT uuidv7(),
    tenant_id       BIGINT NOT NULL REFERENCES sch_farmacia.tenant(id),
    nombre          VARCHAR(100) NOT NULL,
    descripcion     VARCHAR(500),
    estado          VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ,
    CONSTRAINT pk_cat_categoria PRIMARY KEY (id),
    CONSTRAINT uk_cat_categoria_uuid UNIQUE (uuid_publico),
    CONSTRAINT uk_cat_categoria_tenant_nombre UNIQUE (tenant_id, nombre),
    CONSTRAINT ck_cat_categoria_estado CHECK (estado IN ('ACTIVA','INACTIVA'))
);

CREATE TABLE sch_catalogo.producto (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    uuid_publico            UUID NOT NULL DEFAULT uuidv7(),
    tenant_id               BIGINT NOT NULL REFERENCES sch_farmacia.tenant(id),
    categoria_id            BIGINT NOT NULL,
    nombre                  VARCHAR(200) NOT NULL,
    tipo                    VARCHAR(30) NOT NULL,
    laboratorio             VARCHAR(150),
    unidad_medida           VARCHAR(30) NOT NULL,
    presentacion            VARCHAR(150),
    unidades_por_paquete    INTEGER NOT NULL DEFAULT 1,
    codigo_barras           VARCHAR(40),
    precio_venta            NUMERIC(12,2) NOT NULL,
    condicion_venta         VARCHAR(20),
    es_generico             BOOLEAN NOT NULL DEFAULT FALSE,
    es_generico_esencial    BOOLEAN NOT NULL DEFAULT FALSE,
    grupo_terapeutico       VARCHAR(150),
    codigo_digemid          VARCHAR(40),
    principio_activo        VARCHAR(200),
    concentracion           VARCHAR(60),
    requiere_lote           BOOLEAN NOT NULL DEFAULT FALSE,
    requiere_vencimiento    BOOLEAN NOT NULL DEFAULT FALSE,
    estado                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ,
    CONSTRAINT pk_cat_producto PRIMARY KEY (id),
    CONSTRAINT uk_cat_producto_uuid UNIQUE (uuid_publico),
    CONSTRAINT fk_cat_producto_categoria FOREIGN KEY (categoria_id) REFERENCES sch_catalogo.categoria(id),
    CONSTRAINT uk_cat_producto_tenant_codigo_barras UNIQUE (tenant_id, codigo_barras),
    CONSTRAINT ck_cat_producto_tipo CHECK (tipo IN (
        'MEDICAMENTO','DISPOSITIVO_MEDICO','PRODUCTO_SANITARIO','SUPLEMENTO_ALIMENTO','ARTICULO_NO_SANITARIO')),
    CONSTRAINT ck_cat_producto_condicion_venta CHECK (
        condicion_venta IS NULL OR condicion_venta IN ('SIN_RECETA','CON_RECETA','RECETA_RETENIDA')),
    CONSTRAINT ck_cat_producto_estado CHECK (estado IN ('ACTIVO','INACTIVO')),
    CONSTRAINT ck_cat_producto_precio_venta CHECK (precio_venta > 0),
    CONSTRAINT ck_cat_producto_medicamento_requiere_datos CHECK (
        tipo <> 'MEDICAMENTO' OR (principio_activo IS NOT NULL AND condicion_venta IS NOT NULL))
);

CREATE INDEX ix_cat_producto_tenant_categoria ON sch_catalogo.producto(tenant_id, categoria_id);
CREATE INDEX ix_cat_producto_tenant_estado ON sch_catalogo.producto(tenant_id, estado);
```

Nota: `uk_cat_producto_tenant_codigo_barras UNIQUE (tenant_id, codigo_barras)` permite múltiples productos sin código de barras (NULL) por tenant, ya que PostgreSQL no aplica unicidad entre NULLs — igual al patrón ya usado para `username` en `V021`.

Nuevos permisos sembrados en la misma migración (mismo patrón que `V018__seed_security_administration_permissions.sql`): `catalogo.productos.gestionar`, `catalogo.productos.consultar`, `catalogo.categorias.gestionar`, `catalogo.categorias.consultar`.

## API REST

Autorización con `@PreAuthorize("hasAuthority(...)")`, resuelta en cada request desde BD (igual que `security`), no desde claims del JWT.

```
POST   /api/v1/catalogo/categorias                    catalogo.categorias.gestionar
PUT    /api/v1/catalogo/categorias/{id}                catalogo.categorias.gestionar
PATCH  /api/v1/catalogo/categorias/{id}/estado         catalogo.categorias.gestionar   body: {"status":"ACTIVA"|"INACTIVA"}
GET    /api/v1/catalogo/categorias                     catalogo.categorias.consultar   ?estado=

POST   /api/v1/catalogo/productos                      catalogo.productos.gestionar
PUT    /api/v1/catalogo/productos/{id}                 catalogo.productos.gestionar
PATCH  /api/v1/catalogo/productos/{id}/estado          catalogo.productos.gestionar    body: {"status":"ACTIVO"|"INACTIVO"}
GET    /api/v1/catalogo/productos/{id}                 catalogo.productos.consultar
GET    /api/v1/catalogo/productos                      catalogo.productos.consultar
         ?q=&categoriaId=&tipo=&estado=&page=&size=
```

Sigue el mismo patrón HTTP que `RolController`/`UsuarioController`/`SecurityControlController` en `security` (`PATCH /{id}/estado` con body `{"status": "..."}`, en vez de dos endpoints `POST .../desactivar` + `POST .../reactivar`).

`tenantId` viaja como `@RequestParam UUID tenantId` obligatorio en **todos** los endpoints de Catálogo (crear, actualizar, cambiar estado, consultar, listar) — es el mismo patrón que usan de forma consistente `RolController`, `UsuarioController` y `SecurityControlController` en `security` para toda gestión de entidades (no el patrón de `LocalAuthController`, que extrae `tenantId` del JWT solo porque ahí todavía no existe una sesión autenticada con la que resolverlo de otra forma). En los endpoints con body (`POST`/`PUT`/`PATCH`), `tenantId` viaja tanto en el `@RequestParam` de la URL como, cuando el DTO de request ya lo requiere para el Command (`CrearCategoriaCommand`, `CrearProductoCommand`), dentro del propio body — replicando exactamente cómo `CrearRolRequest`/`CrearUsuarioRequest` ya incluyen `tenantId` como campo del JSON en `security`.

Respuestas de error siguen `ProblemDetail` (RFC 9457) vía `GlobalExceptionHandler`/`ErrorCategory` → status HTTP existente en `shared-web`.

## Testing

Siguiendo TDD y el patrón de test existente en `security` (`IdentidadTest`, `UsuarioTest` para dominio; `CrearUsuarioHandlerTest`, `ReemplazarPermisosRolHandlerTest` para aplicación, con fakes manuales — no mocks de librería — implementando los puertos `@FunctionalInterface`):

- **Dominio**: `CategoriaTest`, `ProductoTest` — invariantes, normalización, la regla condicional de `MEDICAMENTO`.
- **Aplicación**: un test de handler por caso de uso, con un fake manual de `CatalogoWritePort`/`CatalogoReadPort` (clase estática privada anidada implementando la interfaz, igual que `CapturingRepository`/`StubWritePort` en los tests existentes) — casos: éxito, error de validación, duplicado (nombre/código de barras), categoría no encontrada, entidad no encontrada, estado inválido.
- **Integración** (`bootstrap-app`): un test de API HTTP end-to-end (`CatalogoApiIntegrationTest`, copiando las anotaciones de clase exactas de `IamApiIntegrationTest` — `@Transactional @ActiveProfiles("test") @Import(PostgresTestContainerConfiguration.class) @AutoConfigureMockMvc @SpringBootTest`) que cubre: crear categoría → crear producto referenciándola → listar con filtros → actualizar → cambiar estado → intentos con datos inválidos devuelven `ProblemDetail` correcto.
- **Migración**: test de verificación de esquema (patrón `MigrationV021Test`) confirmando que `sch_catalogo.categoria`/`producto` existen con las constraints esperadas.

## Fuera de alcance / seguimientos

1. **Frontend de Catálogo**: spec separado, después de tener esta API real y probada.
2. **Módulo Inventario**: spec separado, consume `Producto` vía la API HTTP de `catalogo` (o vía `catalogo::api` si Spring Modulith event-driven aplica — a decidir en ese spec).
3. **Relación con `organizacion`**: cuando `EmpresaOperadora` tenga persistencia real, evaluar si `Producto`/`Categoria` deben referenciar `empresaOperadoraId` en vez de (o junto a) `tenantId` crudo del JWT.
4. **Flujo de aprobación de catálogo** (`catalogValidation` del prototipo): sin definir, no incluido.
