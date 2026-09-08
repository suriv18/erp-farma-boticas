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

Sigue Clean Architecture + DDD + Ports & Adapters + CQRS, como el resto de `service-botica`, combinando dos convenciones ya presentes en el repo:

- **`domain/`, `api/` (commands/queries), `application/` (handlers)**: siguen el estilo CQRS-puro más reciente usado en `modules/organizacion` — commands/resultados como records en `api/`, handlers en `application/` con puertos en `application/port/`, sin capa `port/in` separada ni DTOs HTTP en esta capa.
- **`infrastructure/persistence/{read,write}`, `api/controller`**: `organizacion` no tiene ejemplos de estas capas todavía, así que siguen el estilo ya maduro de `modules/security` (entidades JPA + repos Spring Data + adapters en `write`; projections + `JdbcClient` + mapper + adapter en `read`; controller con `.fold(success, ProblemDetail)`).

No hay import cruzado entre `Producto` y `Categoria` como módulos separados — ambos viven dentro de `catalogo`, en paquetes `domain/producto` y `domain/categoria`.

### Estructura de paquetes

```
modules/catalogo/src/main/java/com/softprimesolutions/catalogo/
├── package-info.java                          (ya existe, @ApplicationModule)
├── domain/
│   ├── categoria/
│   │   ├── Categoria.java                     (AggregateRoot)
│   │   ├── CategoriaId.java                   (record VO)
│   │   └── EstadoCategoria.java               (enum: ACTIVA, INACTIVA)
│   └── producto/
│       ├── Producto.java                      (AggregateRoot)
│       ├── ProductoId.java                    (record VO)
│       ├── TipoProducto.java                  (enum: MEDICAMENTO, DISPOSITIVO_MEDICO, PRODUCTO_SANITARIO, SUPLEMENTO_ALIMENTO, ARTICULO_NO_SANITARIO)
│       ├── CondicionVenta.java                (enum: SIN_RECETA, CON_RECETA, RECETA_RETENIDA)
│       └── EstadoProducto.java                (enum: ACTIVO, INACTIVO)
├── api/
│   ├── package-info.java                      (ya existe, @NamedInterface("api"))
│   ├── categoria/
│   │   ├── CrearCategoriaCommand.java
│   │   ├── ActualizarCategoriaCommand.java
│   │   ├── DesactivarCategoriaCommand.java
│   │   ├── ReactivarCategoriaCommand.java
│   │   ├── ListarCategoriasQuery.java
│   │   ├── CategoriaResultado.java             (record de resultado, usado por commands y query)
│   │   └── controller/CategoriaController.java
│   └── producto/
│       ├── CrearProductoCommand.java
│       ├── ActualizarProductoCommand.java
│       ├── DesactivarProductoCommand.java
│       ├── ReactivarProductoCommand.java
│       ├── ConsultarProductoQuery.java
│       ├── ListarProductosQuery.java
│       ├── ProductoResultado.java
│       ├── ProductoResumen.java                (record ligero para listados)
│       └── controller/ProductoController.java
├── application/
│   ├── categoria/
│   │   ├── CrearCategoriaHandler.java
│   │   ├── ActualizarCategoriaHandler.java
│   │   ├── DesactivarCategoriaHandler.java
│   │   ├── ReactivarCategoriaHandler.java
│   │   ├── ListarCategoriasHandler.java
│   │   └── port/
│   │       ├── CategoriaWritePort.java
│   │       └── CategoriaReadPort.java
│   └── producto/
│       ├── CrearProductoHandler.java
│       ├── ActualizarProductoHandler.java
│       ├── DesactivarProductoHandler.java
│       ├── ReactivarProductoHandler.java
│       ├── ConsultarProductoHandler.java
│       ├── ListarProductosHandler.java
│       └── port/
│           ├── ProductoWritePort.java
│           └── ProductoReadPort.java
└── infrastructure/
    └── persistence/
        ├── write/
        │   ├── entity/{CategoriaJpaEntity,ProductoJpaEntity}.java
        │   ├── repository/{CategoriaJpaRepository,ProductoJpaRepository}.java
        │   ├── mapper/{CategoriaWriteMapper,ProductoWriteMapper}.java
        │   └── adapter/{CategoriaJpaWriteAdapter,ProductoJpaWriteAdapter}.java
        └── read/
            ├── projection/{CategoriaProjection,ProductoProjection}.java
            ├── repository/CatalogoJdbcReadRepository.java
            ├── mapper/CatalogoReadMapper.java
            └── adapter/{CategoriaJdbcReadAdapter,ProductoJdbcReadAdapter}.java
```

## Dominio

### `Categoria`

Campos: `id` (CategoriaId), `tenantId` (UUID), `nombre` (2-100 chars, normalizado trim+colapso espacios, único por tenant), `descripcion` (opcional, hasta 500 chars), `estado` (ACTIVA por defecto al crear), `createdAt`, `updatedAt`.

Invariantes: `id`, `tenantId`, `nombre` obligatorios; `nombre` dentro de longitud; unicidad de nombre por tenant se valida en el `WritePort` (outcome `DUPLICATE_NAME`), no en el dominio (el dominio no consulta persistencia).

Factory: `Categoria.register(CategoriaId, UUID tenantId, String nombre, String descripcion, Instant createdAt): Result<Categoria, ErrorDetail>`, más `restore(...)` para reconstrucción desde persistencia, y métodos de transición `activate()`/`deactivate()` que devuelven una nueva instancia con `estado` actualizado (los agregados son inmutables, siguiendo el patrón de `EmpresaOperadora`/`Usuario`).

Código de error: `CAT_CATEGORIA_INVALIDA`.

### `Producto`

Campos: `id` (ProductoId), `tenantId`, `categoriaId` (CategoriaId, obligatorio — no se valida existencia en dominio, eso es responsabilidad del handler vía `CategoriaReadPort`), `nombre` (2-200 chars), `tipo` (TipoProducto), `laboratorio` (opcional, hasta 150 chars), `unidadMedida` (obligatorio, hasta 30 chars, ej. "Tableta", "Frasco"), `presentacion` (opcional, hasta 150 chars), `unidadesPorPaquete` (entero positivo, default 1), `codigoBarras` (opcional, hasta 40 chars), `precioVenta` (BigDecimal, > 0), `condicionVenta` (opcional salvo excepción abajo), `esGenerico` (boolean, default false), `esGenericoEsencial` (boolean, default false), `grupoTerapeutico` (opcional, hasta 150 chars), `codigoDigemid` (opcional, hasta 40 chars), `principioActivo` (opcional salvo excepción abajo, hasta 200 chars), `concentracion` (opcional, hasta 60 chars), `requiereLote` (boolean, default false), `requiereVencimiento` (boolean, default false), `estado` (ACTIVO por defecto), `createdAt`, `updatedAt`.

**Invariante de negocio clave**: si `tipo == MEDICAMENTO`, entonces `principioActivo` y `condicionVenta` son obligatorios (no nulos/vacíos). Para los demás tipos, ambos campos son opcionales.

Factory: `Producto.register(ProductoId, UUID tenantId, CategoriaId, String nombre, TipoProducto, String laboratorio, String unidadMedida, String presentacion, int unidadesPorPaquete, String codigoBarras, BigDecimal precioVenta, CondicionVenta, boolean esGenerico, boolean esGenericoEsencial, String grupoTerapeutico, String codigoDigemid, String principioActivo, String concentracion, boolean requiereLote, boolean requiereVencimiento, Instant createdAt): Result<Producto, ErrorDetail>`, más `restore(...)` y transiciones `activate()`/`deactivate()`.

Código de error: `CAT_PRODUCTO_INVALIDO`.

## Casos de uso (Commands/Queries)

Todos los commands/queries llevan `tenantId` (UUID público de `sch_farmacia.tenant`) explícito como campo (no hay contexto de actor compartido — cada módulo lo resuelve por su cuenta, igual que `security`). Los adapters de persistencia resuelven ese UUID al `id` interno (`BIGINT`) de `sch_farmacia.tenant` antes de leer/escribir, con el mismo patrón `findTenantInternalId(UUID): Optional<Long>` ya usado en `LocalAuthJdbcAdapter`.

**Categoría:**
- `CrearCategoriaCommand(UUID tenantId, String nombre, String descripcion)` → `CategoriaResultado`
- `ActualizarCategoriaCommand(UUID tenantId, UUID categoriaId, String nombre, String descripcion)` → `CategoriaResultado`
- `DesactivarCategoriaCommand(UUID tenantId, UUID categoriaId)` → `CategoriaResultado`
- `ReactivarCategoriaCommand(UUID tenantId, UUID categoriaId)` → `CategoriaResultado`
- `ListarCategoriasQuery(UUID tenantId, String estado)` → `List<CategoriaResultado>` (sin paginar; el volumen esperado de categorías es bajo)

**Producto:**
- `CrearProductoCommand(UUID tenantId, UUID categoriaId, String nombre, TipoProducto tipo, String laboratorio, String unidadMedida, String presentacion, Integer unidadesPorPaquete, String codigoBarras, BigDecimal precioVenta, CondicionVenta condicionVenta, boolean esGenerico, boolean esGenericoEsencial, String grupoTerapeutico, String codigoDigemid, String principioActivo, String concentracion, boolean requiereLote, boolean requiereVencimiento)` → `ProductoResultado`
- `ActualizarProductoCommand(...)` (mismos campos + `productoId`) → `ProductoResultado`
- `DesactivarProductoCommand(UUID tenantId, UUID productoId)` → `ProductoResultado`
- `ReactivarProductoCommand(UUID tenantId, UUID productoId)` → `ProductoResultado`
- `ConsultarProductoQuery(UUID tenantId, UUID productoId)` → `ProductoResultado`
- `ListarProductosQuery(UUID tenantId, String texto, UUID categoriaId, TipoProducto tipo, String estado, int page, int size)` → página de `ProductoResumen`

### Manejo de errores de aplicación

- Validación de dominio fallida → `ApplicationError` categoría `VALIDATION`, código heredado del dominio (`CAT_CATEGORIA_INVALIDA`/`CAT_PRODUCTO_INVALIDO`).
- Nombre de categoría duplicado → `CAT_CATEGORIA_DUPLICADA`, categoría `CONFLICT`.
- Código de barras duplicado → `CAT_PRODUCTO_CODIGO_BARRAS_DUPLICADO`, categoría `CONFLICT`.
- Categoría referenciada no existe (al crear/actualizar producto) → `CAT_CATEGORIA_NO_ENCONTRADA`, categoría `NOT_FOUND` (el handler de producto consulta `CategoriaReadPort` antes de persistir).
- Producto/categoría no encontrado por id (consultar/actualizar/desactivar/reactivar) → `CAT_PRODUCTO_NO_ENCONTRADO` / `CAT_CATEGORIA_NO_ENCONTRADA`, categoría `NOT_FOUND`.

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
POST   /api/v1/catalogo/categorias/{id}/desactivar     catalogo.categorias.gestionar
POST   /api/v1/catalogo/categorias/{id}/reactivar      catalogo.categorias.gestionar
GET    /api/v1/catalogo/categorias                     catalogo.categorias.consultar

POST   /api/v1/catalogo/productos                      catalogo.productos.gestionar
PUT    /api/v1/catalogo/productos/{id}                 catalogo.productos.gestionar
POST   /api/v1/catalogo/productos/{id}/desactivar      catalogo.productos.gestionar
POST   /api/v1/catalogo/productos/{id}/reactivar       catalogo.productos.gestionar
GET    /api/v1/catalogo/productos/{id}                 catalogo.productos.consultar
GET    /api/v1/catalogo/productos                      catalogo.productos.consultar
         ?q=&categoriaId=&tipo=&estado=&page=&size=
```

`tenantId` se extrae del JWT (`@AuthenticationPrincipal Jwt jwt`, claim `tid`) en cada endpoint, igual que `LocalAuthController.tenantId(Jwt)` — no viaja en el body ni en query params.

Respuestas de error siguen `ProblemDetail` (RFC 9457) vía `GlobalExceptionHandler`/`ErrorCategory` → status HTTP existente en `shared-web`.

## Testing

Siguiendo TDD y el patrón de test existente (`RegistrarEmpresaOperadoraHandlerTest`, `IdentidadTest`, `UsuarioTest`):

- **Dominio**: `CategoriaTest`, `ProductoTest` — invariantes, normalización, la regla condicional de `MEDICAMENTO`.
- **Aplicación**: un test de handler por caso de uso, con fakes manuales de los puertos (`@FunctionalInterface`, mismo patrón que `CapturingRepository` en `organizacion`) — casos: éxito, error de validación, duplicado (nombre/código de barras), categoría no encontrada, entidad no encontrada.
- **Integración** (`bootstrap-app`): un test de API HTTP end-to-end (`CatalogoApiIntegrationTest`, siguiendo el patrón de `IamApiIntegrationTest` con Testcontainers+Postgres real) que cubre: crear categoría → crear producto referenciándola → listar con filtros → actualizar → desactivar → intentos con datos inválidos devuelven `ProblemDetail` correcto.
- **Migración**: test de verificación de esquema (patrón `MigrationV021Test`) confirmando que `sch_catalogo.categoria`/`producto` existen con las constraints esperadas.

## Fuera de alcance / seguimientos

1. **Frontend de Catálogo**: spec separado, después de tener esta API real y probada.
2. **Módulo Inventario**: spec separado, consume `Producto` vía la API HTTP de `catalogo` (o vía `catalogo::api` si Spring Modulith event-driven aplica — a decidir en ese spec).
3. **Relación con `organizacion`**: cuando `EmpresaOperadora` tenga persistencia real, evaluar si `Producto`/`Categoria` deben referenciar `empresaOperadoraId` en vez de (o junto a) `tenantId` crudo del JWT.
4. **Flujo de aprobación de catálogo** (`catalogValidation` del prototipo): sin definir, no incluido.
