# Módulo Catálogo (BC-CAT — Catálogo Farmacéutico Regulatorio) — Design

## Contexto y motivación

Un primer intento de diseñar el módulo `catalogo` (`docs/superpowers/specs/2026-09-07-catalogo-producto-categoria-design.md`, ahora SUPERSEDIDO) creó un esquema `sch_catalogo` nuevo desde cero. Durante la ejecución del plan derivado de ese spec (Task 1, rama `developer`), se descubrió que ya existe un diseño de catálogo farmacéutico real y más maduro en `docs/cadena-farmacias-docs/database/migrations/V003__catalogo_farmaceutico.sql`, que **ya se ejecuta como Flyway real** (`bootstrap-app/build.gradle` agrega `docs/cadena-farmacias-docs/database` como `sourceSet` de recursos, y `application.yaml` apunta `spring.flyway.locations` a `classpath:migrations,classpath:db/migration`). Construir `sch_catalogo` habría duplicado ese dominio. La Task 1 del plan anterior fue revertida (commit `d4bc97b`, rama `developer`).

Este documento reemplaza el diseño anterior: define el módulo Catálogo mapeando JPA/JDBC directamente contra las tablas ya existentes de `sch_farmacia` (V003), sin crear ninguna tabla nueva.

El diseño se alinea con `docs/cadena-farmacias-docs/docs/04-dominio/03-agregados-entidades-value-objects.md` (`DOM-FAR-003`), sección 3 "BC-CAT — Catálogo Farmacéutico Regulatorio", que ya define dos agregados raíz para este bounded context: `ProductoRegulado` (identidad regulatoria) y `SKUComercial` (unidad comercial retail), con el invariante explícito: *"un SKU no puede cambiar silenciosamente de `ProductoReguladoId` después de tener movimientos históricos"*.

No existe código Java previo contra estas tablas (`modules/catalogo` y `modules/farmacia` son scaffolds vacíos, solo `package-info.java`) — este es el primer slice de dominio real para BC-CAT.

## Alcance

**Incluido — las 9 entidades del BC-CAT completo, con CRUD completo en todas, en este mismo slice:**

1. **5 catálogos de soporte** (listas de referencia regulatoria, PK `codigo` VARCHAR): `CondicionVenta`, `FormaFarmaceutica`, `ViaAdministracion`, `UnidadMedida`, `ClasificacionControlada`. Aunque son listas casi estáticas en la práctica (mantenidas por fuente oficial DIGEMID), este slice expone CRUD completo (crear, actualizar, activar/desactivar, listar) vía API — no solo lectura — para no bloquear la operación en un release cuando haga falta agregar un código nuevo.
2. **`PrincipioActivo`**: CRUD completo, más los casos de uso en `ProductoRegulado` para asociar/desasociar principios activos con su concentración/cantidad/unidad.
3. **`Marca`**: tenant-scoped, CRUD completo.
4. **`CategoriaProducto`**: tenant-scoped, jerárquica (padre/hijo), CRUD completo.
5. **`ProductoRegulado`** (Aggregate Root): ficha regulatoria global (no tenant-scoped), CRUD completo + asociar/desasociar `PrincipioActivo`.
6. **`SKUComercial`** (Aggregate Root): unidad comercial tenant-scoped, CRUD completo + gestión de códigos de barra (agregar, eliminar, marcar principal) como sub-colección del agregado.

**Decisiones de alcance explícitas:**

- Todo el dominio vive en `modules/catalogo` (ya scaffoldeado con `@ApplicationModule(id="catalogo", displayName="Catálogo", allowedDependencies={"organizacion::api"})`, y `api/package-info.java` con `@NamedInterface("api")`). `modules/farmacia` queda vacío/sin uso en este slice: el scaffold Modulith ya declara `farmacia` con `allowedDependencies = {"catalogo::api"}` (dependiente de `catalogo`, no al revés), consistente con que `BC-CAT` es un único bounded context sin una separación "Farmacia" distinta en el diseño estratégico (`docs/cadena-farmacias-docs/docs/04-dominio/02-bounded-contexts-context-map.md` no define un `BC-FAR` separado).
- **Multi-tenant selectivo**: `CategoriaProducto`, `Marca` y `SKUComercial` son tenant-scoped (columna `tenant_id`, FK a `sch_farmacia.tenant`). `ProductoRegulado`, `PrincipioActivo` y las 5 tablas de soporte son catálogos **globales compartidos entre tenants** (sin `tenant_id` en la tabla real) — igual que un registro sanitario DIGEMID real, que no es propiedad de una farmacia sino un dato nacional compartido. El `tenantId` sigue resolviéndose del claim `tid` del JWT en cada request (mismo patrón que `security`), pero solo se usa para las tres entidades tenant-scoped.
- **No se crea ninguna migración de esquema nueva** — todas las tablas ya existen y corren como Flyway real vía V003. Sí se crea una migración nueva **solo para sembrar permisos** (ver sección Persistencia), ya que V003 es DDL puro sin siembra de autorización.
- **Regla de dominio explícita**: `SKUComercial.create()` rechaza `tipoSku = REGULADO` sin `productoReguladoId` — invariante en Java, no solo el `CHECK ck_sku_tipo_producto` ya existente en la tabla real. Mismo patrón que la regla "medicamento requiere principio activo" del diseño anterior.
- **Códigos de barra como sub-colección con casos de uso dedicados**: `AgregarCodigoBarra`, `EliminarCodigoBarra`, `MarcarCodigoBarraPrincipal` — reflejan el modelo real N:1 (varios códigos por SKU, máximo uno `es_principal=true` activo), no un campo simple.
- **Seed de datos regulatorios base**: fuera de alcance de este slice. Los 5 catálogos de soporte y `principio_activo` se crean vacíos; poblarlos con los códigos oficiales DIGEMID/SUNAT es trabajo de una historia de datos maestros separada (requiere decidir la fuente y el proceso de carga, no una decisión de arquitectura de este módulo).
- **Activar/desactivar de las 9 entidades** sigue el patrón `SecurityControlService.changeXStatus`: `UPDATE` directo vía el puerto de escritura, sin reconstruir el agregado en dominio, devolviendo `Result<Unit, ApplicationError>`.

**Explícitamente fuera de alcance:**

- Frontend de Catálogo — spec/plan separado, después de tener esta API real.
- Módulo Inventario — spec/plan separado; consumirá `SKUComercial` vía la API de `catalogo`.
- Seed de datos regulatorios (códigos DIGEMID/SUNAT reales) — historia de datos maestros separada.
- Jerarquía de categorías más allá de un nivel padre/hijo simple (el campo `nivel`/`orden` ya existe en la tabla; este slice permite asignar `categoriaPadreId` pero no valida profundidad máxima ni reordenamiento masivo).
- `ProductoRegulatorioSnapshot` (Value Object descrito en `DOM-FAR-003` §3.3, para congelar la regla vigente en operaciones históricas como ventas) — pertenece al momento en que otro BC (Ventas/Inventario) consuma el catálogo, no a este slice de mantenimiento del catálogo mismo.

## Arquitectura

Sigue exactamente el patrón de `modules/security` (no el CQRS-puro más simple de `modules/organizacion`, que todavía no tiene infraestructura ni controller de referencia).

Con 9 entidades, un único `CatalogoWritePort`/`CatalogoReadPort` (como hace `security` con `IamWritePort`/`IamReadPort` para Usuario+Rol+Permiso+Asignación) llegaría a 25-30 métodos — demasiado para un solo archivo legible. Se divide en **3 puertos de escritura por afinidad** (y el read side se consolida en un único `CatalogoReadPort`, ver razón abajo):

- **`CatalogoSoportePort`**: las 5 tablas de soporte (`CondicionVenta`, `FormaFarmaceutica`, `ViaAdministracion`, `UnidadMedida`, `ClasificacionControlada`) + `PrincipioActivo` — comparten la misma forma de CRUD simple (crear, actualizar, cambiar estado), agrupadas porque ninguna tiene relaciones complejas propias.
- **`CatalogoComercialPort`**: `Marca`, `CategoriaProducto`, `SKUComercial` (incluye alta/edición del SKU y gestión de sus códigos de barra) — todas tenant-scoped, todas referenciadas desde el punto de venta/comercial.
- **`ProductoReguladoPort`**: `ProductoRegulado` + su relación N:M con `PrincipioActivo` — aislado porque es el agregado con más columnas e invariantes regulatorias, y es catálogo global (no tenant-scoped), a diferencia de los otros dos puertos.

**Read side consolidado en un único `CatalogoReadPort`**: a diferencia de la escritura (que tiene reglas y outcomes distintos por familia), las consultas son mayormente proyecciones planas + filtros + paginación. Un solo puerto de lectura con ~12 métodos de consulta es más fácil de inyectar en los controllers (cada controller solo necesita un `CatalogoReadPort`, no tres) y evita triplicar el wiring de Spring para algo que no tiene lógica de negocio distinta por familia.

Activar/desactivar de las 9 entidades vive en un puerto `in` transversal (`CatalogoControlUseCase`), igual que `SecurityControlUseCase` agrupa el cambio de estado de Usuario/Rol/Dispositivo en `security`.

### Estructura de paquetes

```
modules/catalogo/src/main/java/com/softprimesolutions/catalogo/
├── package-info.java                          (ya existe, @ApplicationModule)
├── domain/
│   ├── model/
│   │   ├── soporte/
│   │   │   ├── CondicionVenta.java            (AggregateRoot; PK codigo)
│   │   │   ├── FormaFarmaceutica.java
│   │   │   ├── ViaAdministracion.java
│   │   │   ├── UnidadMedida.java
│   │   │   ├── ClasificacionControlada.java
│   │   │   └── EstadoCatalogoSoporte.java     (enum: ACTIVO, INACTIVO — compartido por las 5)
│   │   ├── PrincipioActivo.java               (AggregateRoot; BIGINT+UUID)
│   │   ├── EstadoPrincipioActivo.java         (enum: ACTIVO, INACTIVO)
│   │   ├── Marca.java                         (AggregateRoot)
│   │   ├── EstadoMarca.java                   (enum: ACTIVO, INACTIVO)
│   │   ├── CategoriaProducto.java             (AggregateRoot; jerárquica)
│   │   ├── EstadoCategoriaProducto.java       (enum: ACTIVO, INACTIVO)
│   │   ├── ProductoRegulado.java              (AggregateRoot)
│   │   ├── PrincipioActivoAsociado.java       (Entity/VO hijo de ProductoRegulado: principioActivoId, concentracionTexto, cantidad, unidadMedidaCodigo, esPrincipal, orden)
│   │   ├── EstadoRegulatorio.java             (enum: VIGENTE, VENCIDO, SUSPENDIDO, CANCELADO, POR_VALIDAR)
│   │   ├── SKUComercial.java                  (AggregateRoot)
│   │   ├── CodigoBarraSku.java                (Entity/VO hijo de SKUComercial: codigoBarra, tipoCodigo, esPrincipal, vigenteDesde, vigenteHasta, estado)
│   │   ├── TipoSku.java                       (enum: REGULADO, NO_REGULADO)
│   │   └── EstadoComercialSku.java            (enum: ACTIVO, INACTIVO, BLOQUEADO, DESCONTINUADO)
│   └── valueobject/
│       ├── TenantId.java                      (record VO sobre UUID, propio del módulo)
│       ├── PrincipioActivoId.java             (record VO sobre UUID)
│       ├── MarcaId.java                       (record VO sobre UUID)
│       ├── CategoriaProductoId.java           (record VO sobre UUID)
│       ├── ProductoReguladoId.java            (record VO sobre UUID)
│       └── SkuId.java                         (record VO sobre UUID)
├── api/
│   ├── package-info.java                      (ya existe, @NamedInterface("api"))
│   ├── controller/
│   │   ├── CatalogoControllerSupport.java     (package-private, Result→ProblemDetail)
│   │   ├── CatalogoSoporteController.java     (5 tablas de soporte, un controller con sub-rutas por tipo)
│   │   ├── PrincipioActivoController.java
│   │   ├── MarcaController.java
│   │   ├── CategoriaProductoController.java
│   │   ├── ProductoReguladoController.java
│   │   └── SkuComercialController.java
│   ├── dto/
│   │   ├── request/  (Crear/Actualizar/CambiarEstado por cada entidad, + AsociarPrincipioActivoRequest, AgregarCodigoBarraRequest)
│   │   └── response/ (Response por cada entidad, + PaginaResponse<T> genérico)
│   └── mapper/
│       └── CatalogoApiMapper.java
├── application/
│   ├── dto/
│   │   ├── command/   (records Crear*/Actualizar*/Asociar*/Agregar*/Eliminar*/Marcar* por caso de uso)
│   │   ├── query/     (records Listar*/Consultar* por caso de uso)
│   │   └── result/    (records *Result por entidad + PaginaResult<T> genérico)
│   ├── mapper/
│   │   └── CatalogoApplicationMapper.java     (dominio → *Result, estático)
│   ├── port/
│   │   ├── in/        (una interfaz @FunctionalInterface por caso de uso — ver Casos de Uso)
│   │   └── out/
│   │       ├── CatalogoSoportePort.java
│   │       ├── CatalogoComercialPort.java
│   │       ├── ProductoReguladoPort.java
│   │       └── CatalogoReadPort.java
│   └── usecase/
│       ├── command/   (handlers Crear*/Actualizar*Handler + CatalogoControlService)
│       └── query/     (handlers Listar*/Consultar*Handler)
└── infrastructure/
    ├── configuration/
    │   └── CatalogoModuleConfiguration.java   (@Configuration, wiring de todos los @Bean UseCase)
    └── persistence/
        ├── write/
        │   ├── entity/      (una *JpaEntity por tabla real, incluye ProductoPrincipioActivoJpaEntity y SkuCodigoBarraJpaEntity)
        │   ├── repository/  (un *JpaRepository Spring Data por entidad)
        │   ├── mapper/      (CatalogoSoporteWriteMapper, CatalogoComercialWriteMapper, ProductoReguladoWriteMapper)
        │   └── adapter/     (CatalogoSoporteJpaWriteAdapter, CatalogoComercialJpaWriteAdapter, ProductoReguladoJpaWriteAdapter — cada uno implementa su puerto)
        └── read/
            ├── projection/  (un *Projection record por entidad consultable)
            ├── repository/
            │   └── CatalogoJdbcReadRepository.java  (SQL crudo vía JdbcClient, todas las queries)
            ├── mapper/
            │   └── CatalogoReadMapper.java
            └── adapter/
                └── CatalogoJdbcReadAdapter.java      (implementa CatalogoReadPort completo)
```

## Dominio

Todos los agregados siguen el patrón `Rol`/`Usuario` de `security`: factory estático `create(...)` que valida invariantes y devuelve `Result<T, ErrorDetail>`; factory `restore(...)` para reconstrucción desde persistencia sin re-validar. Activar/desactivar no pasa por el agregado (ver Arquitectura).

### Catálogos de soporte (5 clases estructuralmente idénticas)

`CondicionVenta`, `FormaFarmaceutica`, `ViaAdministracion`, `UnidadMedida`, `ClasificacionControlada` — cada uno con su propio código de error (`CAT_CONDICION_VENTA_INVALIDA`, etc.) pero la misma forma:

- **`CondicionVenta`**: `codigo` (identidad, VARCHAR(30), único, no autogenerado por el sistema — lo asigna quien registra el dato regulatorio), `denominacion` (2-200 chars), `requiereReceta` (boolean), `requiereRetencion` (boolean), `fuente` (opcional, hasta 300 chars), `versionFuente` (opcional, hasta 100 chars), `vigenteDesde`/`vigenteHasta` (LocalDate, opcionales), `estado` (ACTIVO por defecto). Factory: `CondicionVenta.create(String codigo, String denominacion, boolean requiereReceta, boolean requiereRetencion, String fuente, String versionFuente, LocalDate vigenteDesde, LocalDate vigenteHasta): Result<CondicionVenta, ErrorDetail>`.
- **`FormaFarmaceutica`**, **`ViaAdministracion`**: `codigo` (VARCHAR(30)), `denominacion` (2-200 chars), `fuente` (opcional). Factory: `create(String codigo, String denominacion, String fuente): Result<T, ErrorDetail>`.
- **`UnidadMedida`**: `codigo` (VARCHAR(30)), `denominacion` (2-150 chars), `simbolo` (opcional, hasta 30 chars), `permiteDecimal` (boolean), `fuente` (opcional). Factory: `create(String codigo, String denominacion, String simbolo, boolean permiteDecimal, String fuente): Result<UnidadMedida, ErrorDetail>`.
- **`ClasificacionControlada`**: `codigo` (VARCHAR(40)), `denominacion` (2-200 chars), `normaFuente` (opcional, hasta 300 chars), `requiereRecetaEspecial` (boolean), `retieneReceta` (boolean), `vigenciaRecetaDias` (entero positivo, opcional). Factory: `create(String codigo, String denominacion, String normaFuente, boolean requiereRecetaEspecial, boolean retieneReceta, Integer vigenciaRecetaDias): Result<ClasificacionControlada, ErrorDetail>` — invariante: si `vigenciaRecetaDias != null`, debe ser `> 0` (igual al `CHECK` real).

Invariante común a las 5: `codigo` obligatorio y normalizado a mayúsculas (igual que `Rol.code()` en `security`), `denominacion` obligatoria dentro de longitud.

### `PrincipioActivo`

Campos: `id` (PrincipioActivoId/UUID), `codigoFuente` (opcional, hasta 80 chars), `denominacion` (obligatoria, 2-300 chars), `nombreNormalizado` (opcional, hasta 300 chars), `fuente` (opcional, hasta 300 chars), `estado` (ACTIVO por defecto).

Factory: `PrincipioActivo.create(PrincipioActivoId id, String codigoFuente, String denominacion, String nombreNormalizado, String fuente): Result<PrincipioActivo, ErrorDetail>`. Código de error: `CAT_PRINCIPIO_ACTIVO_INVALIDO`.

### `Marca`

Campos: `id` (MarcaId), `tenantId` (TenantId), `codigo` (2-50 chars, único por tenant), `nombre` (2-180 chars), `descripcion` (opcional, hasta 500 chars), `estado` (ACTIVO por defecto).

Factory: `Marca.create(MarcaId id, TenantId tenantId, String codigo, String nombre, String descripcion): Result<Marca, ErrorDetail>`. Código de error: `CAT_MARCA_INVALIDA`. Unicidad de `(tenantId, codigo)` se valida en el `WritePort` (outcome `DUPLICATE_CODE`), no en el dominio.

### `CategoriaProducto`

Campos: `id` (CategoriaProductoId), `tenantId`, `categoriaPadreId` (CategoriaProductoId, opcional — auto-referencia), `codigo` (2-50 chars, único por tenant), `nombre` (2-180 chars), `descripcion` (opcional, hasta 500 chars), `nivel` (entero, mínimo 1, default 1), `orden` (entero, mínimo 0, default 0), `estado` (ACTIVO por defecto).

Factory: `CategoriaProducto.create(CategoriaProductoId id, TenantId tenantId, CategoriaProductoId categoriaPadreId, String codigo, String nombre, String descripcion, int nivel, int orden): Result<CategoriaProducto, ErrorDetail>`. Código de error: `CAT_CATEGORIA_INVALIDA`. Invariante: `nivel >= 1` (igual al `CHECK ck_categoria_nivel` real). La consistencia de que `categoriaPadreId` exista y pertenezca al mismo tenant se valida en el handler vía `CatalogoComercialPort`, no en el dominio (el dominio no consulta persistencia).

### `ProductoRegulado` (Aggregate Root)

Campos: `id` (ProductoReguladoId), `tipoProducto` (String, 2-40 chars — libre, refleja `tipo_producto VARCHAR(40)` real sin enum cerrado porque V003 no define una lista fija), `rubroCodigo` (opcional, hasta 50 chars), `tipoRegistro`/`numeroRegistro` (ambos opcionales, pero deben informarse juntos — igual al `CHECK ck_producto_regulado_registro` real), `denominacion` (obligatoria, 2-500 chars), `concentracionTexto` (opcional, hasta 300 chars), `presentacionRegulatoria` (opcional, hasta 500 chars), `formaFarmaceuticaCodigo`/`viaAdministracionCodigo`/`unidadMedidaCodigo`/`condicionVentaCodigo`/`clasificacionControladaCodigo` (todos opcionales, referencian los catálogos de soporte — no se valida existencia en dominio, eso es responsabilidad del handler vía `CatalogoSoportePort`), `clasificacionAtc` (opcional, hasta 30 chars), `tipoLiberacion`/`origenFabricacion` (opcionales, hasta 40 chars), `paisOrigen` (opcional, hasta 100 chars), `subpartidaNacional` (opcional, hasta 30 chars), `titularRegistro`/`fabricante`/`importador` (opcionales, hasta 300 chars), `establecimientoExpendio` (opcional, hasta 200 chars), `vigenteDesde`/`vigenteHasta` (LocalDate, opcionales — invariante: si ambos presentes, `vigenteHasta >= vigenteDesde`, igual al `CHECK` real), `estadoRegulatorio` (EstadoRegulatorio, VIGENTE por defecto), `fuente`/`versionFuente` (opcionales), `principiosActivos` (lista inmutable de `PrincipioActivoAsociado`), `createdAt`, `updatedAt`.

`PrincipioActivoAsociado` (Entity hija, no Aggregate Root propio): `principioActivoId` (PrincipioActivoId), `concentracionTexto` (opcional), `cantidad` (BigDecimal, positivo si se informa), `unidadMedidaCodigo` (opcional), `esPrincipal` (boolean, default true), `orden` (short, default 1).

Factory: `ProductoRegulado.create(ProductoReguladoId id, String tipoProducto, String rubroCodigo, String tipoRegistro, String numeroRegistro, String denominacion, String concentracionTexto, String presentacionRegulatoria, String formaFarmaceuticaCodigo, String viaAdministracionCodigo, String unidadMedidaCodigo, String condicionVentaCodigo, String clasificacionAtc, String clasificacionControladaCodigo, String tipoLiberacion, String origenFabricacion, String paisOrigen, String subpartidaNacional, String titularRegistro, String fabricante, String importador, String establecimientoExpendio, LocalDate vigenteDesde, LocalDate vigenteHasta, String fuente, String versionFuente, Instant createdAt): Result<ProductoRegulado, ErrorDetail>` — se crea con `principiosActivos` vacío y `estadoRegulatorio = VIGENTE`; asociar principios activos es un caso de uso separado. Código de error: `CAT_PRODUCTO_REGULADO_INVALIDO`.

### `SKUComercial` (Aggregate Root)

Campos: `id` (SkuId), `tenantId`, `productoReguladoId` (ProductoReguladoId, opcional — obligatorio si `tipoSku = REGULADO`), `categoriaId` (CategoriaProductoId, opcional), `marcaId` (MarcaId, opcional), `tipoSku` (TipoSku, obligatorio), `codigoInterno` (2-60 chars, único por tenant), `descripcionComercial` (obligatoria, 2-500 chars), `nombreCorto` (opcional, hasta 200 chars), `presentacionComercial` (opcional, hasta 300 chars), `unidadVentaCodigo`/`unidadContenidoCodigo` (opcionales, referencian `UnidadMedida`), `contenido` (BigDecimal, opcional, positivo si se informa), `pesoGramos`/`altoCm`/`anchoCm`/`largoCm` (BigDecimal, opcionales, positivos si se informan), `permiteVentaFraccion` (boolean, default false), `factorFraccion` (BigDecimal, obligatorio si `permiteVentaFraccion = true`, debe ser null si es false — igual al `CHECK ck_sku_factor` real), `requiereLote`/`requiereVencimiento` (boolean, default true cada uno — distinto del slice anterior, refleja el default real de la tabla), `afectoIgv` (boolean, default true), `stockMinimoDefault` (BigDecimal, >= 0, default 0), `stockMaximoDefault` (BigDecimal, opcional, si se informa debe ser `>= stockMinimoDefault` — igual al `CHECK ck_sku_stock_default` real), `imagenUri` (opcional), `estadoComercial` (EstadoComercialSku, ACTIVO por defecto), `codigosBarra` (lista inmutable de `CodigoBarraSku`), `createdBy` (obligatorio), `createdAt`, `updatedBy`, `updatedAt`.

`CodigoBarraSku` (Entity hija): `codigoBarra` (obligatorio, único por tenant), `tipoCodigo` (default "EAN13"), `esPrincipal` (boolean), `vigenteDesde`/`vigenteHasta` (opcionales), `estado` (ACTIVO por defecto).

**Invariante de negocio clave**: si `tipoSku == REGULADO`, entonces `productoReguladoId` es obligatorio (no nulo). Si `tipoSku == NO_REGULADO`, `productoReguladoId` puede ser nulo o informado (el `CHECK` real solo exige la asociación para REGULADO, no la prohíbe para NO_REGULADO).

Factory: `SKUComercial.create(SkuId id, TenantId tenantId, ProductoReguladoId productoReguladoId, CategoriaProductoId categoriaId, MarcaId marcaId, TipoSku tipoSku, String codigoInterno, String descripcionComercial, String nombreCorto, String presentacionComercial, String unidadVentaCodigo, BigDecimal contenido, String unidadContenidoCodigo, BigDecimal pesoGramos, BigDecimal altoCm, BigDecimal anchoCm, BigDecimal largoCm, boolean permiteVentaFraccion, BigDecimal factorFraccion, boolean requiereLote, boolean requiereVencimiento, boolean afectoIgv, BigDecimal stockMinimoDefault, BigDecimal stockMaximoDefault, String imagenUri, String createdBy, Instant createdAt): Result<SKUComercial, ErrorDetail>` — se crea con `codigosBarra` vacío; agregar códigos es un caso de uso separado. Código de error: `CAT_SKU_INVALIDO`.

## Casos de uso (puertos `in` + Commands/Queries)

Cada caso de uso es una interfaz `@FunctionalInterface` en `application/port/in/`, implementada por un handler, igual que `CrearRolUseCase`/`CrearRolHandler`. Los commands/queries de entidades tenant-scoped llevan `tenantId: UUID` explícito; los de entidades globales (soporte, PrincipioActivo, ProductoRegulado) no.

**Catálogos de soporte** (uno por entidad, 5 veces esta forma — se listan una vez, sustituyendo `X` por cada nombre):
- `CrearXUseCase.execute(CrearXCommand)` → `Result<XResult, ApplicationError>`
- `ActualizarXUseCase.execute(ActualizarXCommand)` → `Result<XResult, ApplicationError>`
- `ListarXUseCase.execute(ListarXQuery(String estado))` → `Result<List<XResult>, ApplicationError>`

**`PrincipioActivo`:**
- `CrearPrincipioActivoUseCase.execute(CrearPrincipioActivoCommand(String codigoFuente, String denominacion, String nombreNormalizado, String fuente))` → `Result<PrincipioActivoResult, ApplicationError>`
- `ActualizarPrincipioActivoUseCase.execute(ActualizarPrincipioActivoCommand(UUID principioActivoId, ...))` → `Result<PrincipioActivoResult, ApplicationError>`
- `ListarPrincipioActivoUseCase.execute(ListarPrincipioActivoQuery(String texto, String estado))` → `Result<List<PrincipioActivoResult>, ApplicationError>`

**`Marca`:**
- `CrearMarcaUseCase.execute(CrearMarcaCommand(UUID tenantId, String codigo, String nombre, String descripcion))` → `Result<MarcaResult, ApplicationError>`
- `ActualizarMarcaUseCase.execute(ActualizarMarcaCommand(UUID tenantId, UUID marcaId, ...))` → `Result<MarcaResult, ApplicationError>`
- `ListarMarcasUseCase.execute(ListarMarcasQuery(UUID tenantId, String estado))` → `Result<List<MarcaResult>, ApplicationError>`

**`CategoriaProducto`:**
- `CrearCategoriaProductoUseCase.execute(CrearCategoriaProductoCommand(UUID tenantId, UUID categoriaPadreId, String codigo, String nombre, String descripcion, int nivel, int orden))` → `Result<CategoriaProductoResult, ApplicationError>`
- `ActualizarCategoriaProductoUseCase.execute(ActualizarCategoriaProductoCommand(UUID tenantId, UUID categoriaId, ...))` → `Result<CategoriaProductoResult, ApplicationError>`
- `ListarCategoriasProductoUseCase.execute(ListarCategoriasProductoQuery(UUID tenantId, UUID categoriaPadreId, String estado))` → `Result<List<CategoriaProductoResult>, ApplicationError>`

**`ProductoRegulado`:**
- `CrearProductoReguladoUseCase.execute(CrearProductoReguladoCommand(String tipoProducto, ..., 26 campos según dominio))` → `Result<ProductoReguladoResult, ApplicationError>`
- `ActualizarProductoReguladoUseCase.execute(ActualizarProductoReguladoCommand(UUID productoReguladoId, ...))` → `Result<ProductoReguladoResult, ApplicationError>`
- `ConsultarProductoReguladoUseCase.execute(ConsultarProductoReguladoQuery(UUID productoReguladoId))` → `Result<ProductoReguladoResult, ApplicationError>`
- `ListarProductosReguladosUseCase.execute(ListarProductosReguladosQuery(String texto, String condicionVentaCodigo, String estadoRegulatorio, int page, int size))` → `Result<PaginaResult<ProductoReguladoResumen>, ApplicationError>`
- `AsociarPrincipioActivoUseCase.execute(AsociarPrincipioActivoCommand(UUID productoReguladoId, UUID principioActivoId, String concentracionTexto, BigDecimal cantidad, String unidadMedidaCodigo, boolean esPrincipal, short orden))` → `Result<ProductoReguladoResult, ApplicationError>`
- `DesasociarPrincipioActivoUseCase.execute(DesasociarPrincipioActivoCommand(UUID productoReguladoId, UUID principioActivoId))` → `Result<ProductoReguladoResult, ApplicationError>`

**`SKUComercial`:**
- `CrearSkuUseCase.execute(CrearSkuCommand(UUID tenantId, UUID productoReguladoId, UUID categoriaId, UUID marcaId, String tipoSku, String codigoInterno, ..., 20 campos según dominio))` → `Result<SkuResult, ApplicationError>`
- `ActualizarSkuUseCase.execute(ActualizarSkuCommand(UUID tenantId, UUID skuId, ...))` → `Result<SkuResult, ApplicationError>`
- `ConsultarSkuUseCase.execute(ConsultarSkuQuery(UUID tenantId, UUID skuId))` → `Result<SkuResult, ApplicationError>`
- `ListarSkusUseCase.execute(ListarSkusQuery(UUID tenantId, String texto, UUID categoriaId, UUID marcaId, String tipoSku, String estadoComercial, int page, int size))` → `Result<PaginaResult<SkuResumen>, ApplicationError>`
- `AgregarCodigoBarraUseCase.execute(AgregarCodigoBarraCommand(UUID tenantId, UUID skuId, String codigoBarra, String tipoCodigo, LocalDate vigenteDesde, LocalDate vigenteHasta))` → `Result<SkuResult, ApplicationError>`
- `EliminarCodigoBarraUseCase.execute(EliminarCodigoBarraCommand(UUID tenantId, UUID skuId, String codigoBarra))` → `Result<SkuResult, ApplicationError>`
- `MarcarCodigoBarraPrincipalUseCase.execute(MarcarCodigoBarraPrincipalCommand(UUID tenantId, UUID skuId, String codigoBarra))` → `Result<SkuResult, ApplicationError>`

**Control de estado (activar/desactivar), un único puerto transversal, igual patrón que `SecurityControlUseCase`:**
- `CatalogoControlUseCase.changeCondicionVentaStatus/changeFormaFarmaceuticaStatus/changeViaAdministracionStatus/changeUnidadMedidaStatus/changeClasificacionControladaStatus/changePrincipioActivoStatus/changeMarcaStatus/changeCategoriaProductoStatus/changeProductoReguladoStatus/changeSkuStatus(...)` → cada uno `Result<Unit, ApplicationError>`, implementado por `CatalogoControlService`.

### Manejo de errores de aplicación

- Validación de dominio fallida → `ApplicationError` categoría `VALIDATION`, código heredado del dominio (`CAT_*_INVALIDA`/`CAT_*_INVALIDO`).
- Código/nombre duplicado (soporte, Marca, CategoriaProducto, SKUComercial `codigoInterno`/`codigoBarra`) → `CAT_*_DUPLICADO`, categoría `CONFLICT`.
- Referencia no encontrada (categoría padre, forma farmacéutica, condición de venta, producto regulado al crear SKU, etc.) → `CAT_*_NO_ENCONTRADA`/`CAT_*_NO_ENCONTRADO`, categoría `NOT_FOUND`.
- Entidad no encontrada por id (actualizar/consultar/cambiar estado) → `CAT_*_NO_ENCONTRADO`, categoría `NOT_FOUND`.
- Estado inválido en cambio de estado → `CAT_ESTADO_INVALIDO`, categoría `VALIDATION`.
- Paginación inválida (page/size fuera de rango) → `CAT_PAGINACION_INVALIDA`, categoría `VALIDATION`.

## Persistencia

**Sin migración de esquema nueva** — todas las tablas (`sch_farmacia.condicion_venta`, `forma_farmaceutica`, `via_administracion`, `unidad_medida`, `clasificacion_controlada`, `principio_activo`, `categoria_producto`, `marca`, `producto_regulado`, `producto_principio_activo`, `sku_comercial`, `sku_codigo_barra`) ya existen vía `docs/cadena-farmacias-docs/database/migrations/V003__catalogo_farmaceutico.sql`, que corre como Flyway real. Las entidades JPA de este módulo mapean exactamente esas columnas — ver el archivo V003 citado como fuente de verdad de nombres de columna, tipos y constraints.

**Sí se crea una migración nueva, solo para sembrar permisos** (V003 es DDL puro sin autorización): `service-botica/bootstrap-app/src/main/resources/db/migration/V022__seed_catalogo_permissions.sql`, siguiendo exactamente el patrón de `V018__seed_security_administration_permissions.sql` (`INSERT ... SELECT ... CROSS JOIN (VALUES ...) WHERE m.codigo = 'CATALOGO' ON CONFLICT (codigo) DO UPDATE ...`). El módulo `CATALOGO` en `sch_seguridad.modulo_sistema` **ya existe** (sembrado en `docs/cadena-farmacias-docs/database/migrations/V016__navegacion_dinamica_rbac.sql:84`) — la migración nueva solo agrega permisos, no el módulo.

Permisos a sembrar (por familia, no uno por tabla — evita explosión de permisos):
- `catalogo.soporte.consultar`, `catalogo.soporte.gestionar` (cubre las 5 tablas de soporte)
- `catalogo.principios-activos.consultar`, `catalogo.principios-activos.gestionar`
- `catalogo.marcas.consultar`, `catalogo.marcas.gestionar`
- `catalogo.categorias.consultar`, `catalogo.categorias.gestionar`
- `catalogo.productos-regulados.consultar`, `catalogo.productos-regulados.gestionar`
- `catalogo.skus.consultar`, `catalogo.skus.gestionar`

## API REST

Autorización con `@PreAuthorize("hasAuthority(...)")`, resuelta en cada request desde BD (igual que `security`). `tenantId` viaja como `@RequestParam UUID tenantId` en los endpoints de entidades tenant-scoped (Marca, CategoriaProducto, SKUComercial) — mismo patrón que `RolController`/`UsuarioController`. Las entidades globales (soporte, PrincipioActivo, ProductoRegulado) no llevan `tenantId`.

```
# Soporte (5 tablas, un controller con sub-recursos)
POST   /api/v1/catalogo/soporte/condiciones-venta                 catalogo.soporte.gestionar
PUT    /api/v1/catalogo/soporte/condiciones-venta/{codigo}         catalogo.soporte.gestionar
PATCH  /api/v1/catalogo/soporte/condiciones-venta/{codigo}/estado  catalogo.soporte.gestionar
GET    /api/v1/catalogo/soporte/condiciones-venta                  catalogo.soporte.consultar
# (mismo patrón de 4 endpoints para: formas-farmaceuticas, vias-administracion, unidades-medida, clasificaciones-controladas)

POST   /api/v1/catalogo/principios-activos                        catalogo.principios-activos.gestionar
PUT    /api/v1/catalogo/principios-activos/{id}                    catalogo.principios-activos.gestionar
PATCH  /api/v1/catalogo/principios-activos/{id}/estado              catalogo.principios-activos.gestionar
GET    /api/v1/catalogo/principios-activos                          catalogo.principios-activos.consultar   ?texto=&estado=

POST   /api/v1/catalogo/marcas                                     catalogo.marcas.gestionar
PUT    /api/v1/catalogo/marcas/{id}                                 catalogo.marcas.gestionar
PATCH  /api/v1/catalogo/marcas/{id}/estado                          catalogo.marcas.gestionar
GET    /api/v1/catalogo/marcas                                     catalogo.marcas.consultar               ?tenantId=&estado=

POST   /api/v1/catalogo/categorias                                 catalogo.categorias.gestionar
PUT    /api/v1/catalogo/categorias/{id}                             catalogo.categorias.gestionar
PATCH  /api/v1/catalogo/categorias/{id}/estado                      catalogo.categorias.gestionar
GET    /api/v1/catalogo/categorias                                  catalogo.categorias.consultar          ?tenantId=&categoriaPadreId=&estado=

POST   /api/v1/catalogo/productos-regulados                        catalogo.productos-regulados.gestionar
PUT    /api/v1/catalogo/productos-regulados/{id}                    catalogo.productos-regulados.gestionar
PATCH  /api/v1/catalogo/productos-regulados/{id}/estado             catalogo.productos-regulados.gestionar
GET    /api/v1/catalogo/productos-regulados/{id}                    catalogo.productos-regulados.consultar
GET    /api/v1/catalogo/productos-regulados                         catalogo.productos-regulados.consultar ?q=&condicionVentaCodigo=&estadoRegulatorio=&page=&size=
POST   /api/v1/catalogo/productos-regulados/{id}/principios-activos          catalogo.productos-regulados.gestionar
DELETE /api/v1/catalogo/productos-regulados/{id}/principios-activos/{paId}   catalogo.productos-regulados.gestionar

POST   /api/v1/catalogo/skus                                       catalogo.skus.gestionar
PUT    /api/v1/catalogo/skus/{id}                                   catalogo.skus.gestionar
PATCH  /api/v1/catalogo/skus/{id}/estado                            catalogo.skus.gestionar
GET    /api/v1/catalogo/skus/{id}                                   catalogo.skus.consultar                ?tenantId=
GET    /api/v1/catalogo/skus                                        catalogo.skus.consultar                ?tenantId=&q=&categoriaId=&marcaId=&tipoSku=&estadoComercial=&page=&size=
POST   /api/v1/catalogo/skus/{id}/codigos-barra                     catalogo.skus.gestionar
DELETE /api/v1/catalogo/skus/{id}/codigos-barra/{codigoBarra}        catalogo.skus.gestionar
PATCH  /api/v1/catalogo/skus/{id}/codigos-barra/{codigoBarra}/principal  catalogo.skus.gestionar
```

Respuestas de error siguen `ProblemDetail` (RFC 9457) vía `GlobalExceptionHandler`/`ApplicationErrorHttpMapper` (`shared-web`).

## Testing

Siguiendo TDD y el patrón de test existente en `security` (fakes manuales implementando los puertos `@FunctionalInterface`, no mocks de librería):

- **Dominio**: un test por agregado/entidad (9 clases de test) — invariantes, normalización, la regla condicional `REGULADO`/`NO_REGULADO` en `SKUComercial`, la regla de fechas coherentes en `ProductoRegulado`, la regla `factorFraccion` condicional en `SKUComercial`.
- **Aplicación**: un test de handler por caso de uso, con fakes de `CatalogoSoportePort`/`CatalogoComercialPort`/`ProductoReguladoPort`/`CatalogoReadPort` — casos: éxito, error de validación, duplicado, referencia no encontrada, entidad no encontrada, estado inválido.
- **Integración** (`bootstrap-app`): un test de API HTTP end-to-end (`CatalogoApiIntegrationTest`, copiando anotaciones exactas de `IamApiIntegrationTest`) que cubre al menos un flujo por familia: crear condición de venta → crear producto regulado referenciándola → asociar principio activo → crear SKU regulado referenciando el producto regulado → agregar código de barra → marcarlo principal → listar con filtros → cambiar estados → intentos inválidos devuelven `ProblemDetail` correcto (incluyendo el caso `tipoSku=REGULADO` sin `productoReguladoId`).
- **Migración de permisos**: test de verificación (patrón `MigrationV021Test`) confirmando que los 12 permisos nuevos existen en `sch_seguridad.permiso` con `modulo_id` apuntando al módulo `CATALOGO` ya sembrado.

## Fuera de alcance / seguimientos

1. **Frontend de Catálogo**: spec separado, después de tener esta API real y probada.
2. **Módulo Inventario**: spec separado, consumirá `SKUComercial` (no un `Producto` genérico) vía la API de `catalogo`.
3. **Seed de datos regulatorios reales** (códigos DIGEMID de `condicion_venta`, `forma_farmaceutica`, etc.): historia de datos maestros separada — requiere decidir fuente y proceso de carga.
4. **`ProductoRegulatorioSnapshot`**: value object para congelar la regla vigente en operaciones históricas (ventas) — pertenece al BC que consuma el catálogo (Ventas/Inventario), no a este módulo.
5. **Jerarquía de categorías avanzada** (validación de profundidad máxima, reordenamiento masivo, prevención de ciclos más allá de la FK compuesta ya existente) — no incluida en este slice.
6. **Relación con `organizacion`**: igual que en el diseño anterior, `catalogo` no depende hoy de `organizacion::api` (aunque el scaffold lo permite) porque `EmpresaOperadora` no tiene persistencia real todavía.
