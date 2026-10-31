# Catálogo APIs Full Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Completar el API REST "full" del módulo `catalogo`: agregar GET-by-id a los 8 catálogos existentes que carecen de él, construir `RubroComercial` y `TipoDocumentoIdentidad` de cero (dominio → aplicación → infraestructura → API), sembrar sus permisos, y cubrir los 10 catálogos tocados con tests de integración.

**Architecture:** Clean Architecture + DDD + Ports & Adapters + CQRS + Result Pattern, siguiendo exactamente los patrones ya establecidos en `service-botica/modules/catalogo/`: patrón "comercial" (UUID, multi-tenant, paginado, escritura JPA + lectura JDBC) para `RubroComercial`, y un patrón nuevo "global sin tenant" para `TipoDocumentoIdentidad` (PK `codigo VARCHAR(2)`, catálogo SUNAT compartido, sin paginación, sin POST). Los GET-by-id nuevos reconstruyen el agregado de dominio desde el `write port` (JPA), igual que `ConsultarSkuHandler`/`CatalogoComercialJpaWriteAdapter.findSkuById` y `ConsultarProductoReguladoHandler`/`ProductoReguladoPort.findById` — NO se usa el `CatalogoReadPort` (JDBC) para GET-by-id porque ese puerto ya está dedicado a listados/proyecciones planas.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Modulith 2.1, Gradle 9.5.1, JPA (escritura), JdbcClient (lectura), JUnit 5 + MockMvc + Testcontainers PostgreSQL (perfil `test`), Flyway.

## Global Constraints

- `domain/` sin dependencias de Spring/JPA; usa `Result<T, ErrorDetail>` (`com.softprimesolutions.shared.kernel.result.Result`, `com.softprimesolutions.shared.kernel.error.ErrorDetail`) para validación de invariantes.
- `application/` (port/in = casos de uso, port/out = puertos, usecase/{command,query}) devuelve `Result<T, ApplicationError>` (`com.softprimesolutions.shared.application.error.ApplicationError`/`StandardApplicationError`/`ErrorCategory`), nunca `null` ni excepciones para casos esperados.
- `infrastructure/`: escritura vía JPA en `persistence/write`, lectura vía JDBC en `persistence/read`. Los GET-by-id nuevos de este plan van en `persistence/write` (reconstrucción de agregado), no en `persistence/read`.
- `api/` es la única superficie pública del módulo — nunca se importan paquetes internos de otro módulo. Todos los controllers usan `CatalogoApiMapper` (mapper API↔application) y `CatalogoControllerSupport::problem` para errores; toda lógica de mapeo dominio↔application usa `CatalogoApplicationMapper`. NO crear mappers nuevos: extender los existentes.
- ArchUnit + Spring Modulith `ApplicationModules.verify()` corren en `.\gradlew.bat check` — no romper reglas de dependencia entre módulos. Este plan no cambia `build.gradle` de ningún módulo ni agrega dependencias nuevas entre subproyectos.
- Las tablas de `RubroComercial` (`sch_catalogo.rubro_comercial`, en `docs/cadena-farmacias-docs/database/migrations/V003__catalogo_farmaceutico.sql`) y `TipoDocumentoIdentidad` (`sch_catalogo.tipo_documento_identidad`, en `docs/cadena-farmacias-docs/database/migrations/V018__catalogo_tipo_documento_identidad_sunat.sql`) YA EXISTEN. Este plan NO modifica ningún script bajo `docs/cadena-farmacias-docs/database/migrations/`. La única migración nueva es `service-botica/bootstrap-app/src/main/resources/db/migration/V022__seed_catalogo_permissions_rubro_tipo_documento.sql` (numeración continúa desde V021, la última existente).
- `rubro_comercial` tiene columnas `es_activo CHAR(1)` (NO tiene columna `estado VARCHAR(20)` como `marca`/`categoria_producto`) — el estado ACTIVO/INACTIVO expuesto por la API se deriva de `es_activo` (`'1'` → `ACTIVO`, `'0'` → `INACTIVO`) tanto en lectura como en el PATCH de cambio de estado.
- `tipo_documento_identidad` es un catálogo GLOBAL (sin `tenant_id`, PK `codigo VARCHAR(2)`, 13 filas semilla ya insertadas por V018) — la API NO expone POST (crear) ni DELETE, solo GET/{codigo}, GET (lista, sin paginar) y PATCH/{codigo}/estado. El estado ACTIVO/INACTIVO se deriva de `es_activo` igual que `rubro_comercial`.
- Todos los tests de integración nuevos van en `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/api/`, se ejecutan vía `:bootstrap-app:test`, usan `@Import(PostgresTestContainerConfiguration.class)` + `@ActiveProfiles("test")` + Testcontainers PostgreSQL 18 real (NO H2 — el perfil `test` usa `ddl-auto: validate` contra Flyway real, ver `bootstrap-app/src/test/resources/application-test.yaml`), siguiendo el patrón exacto de `CatalogoComercialApiIntegrationTest.java` y `CatalogoSoporteApiIntegrationTest.java` (ya existentes en ese mismo paquete).
- Comando de verificación tras cada tarea: `cd service-botica && .\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.catalogo.api.*"` (PowerShell) para los tests de integración, y `.\gradlew.bat :modules:catalogo:test` para tests unitarios del módulo. Verificación final completa: `.\gradlew.bat check --warning-mode all`.

---

## Resumen de catálogos y su estado de GET-by-id (verificado por inspección directa del código, no asumido)

| Catálogo | Patrón | PK expuesta | ¿Tiene GET-by-id? |
|---|---|---|---|
| CategoriaProducto | comercial (UUID, paginado, tenant) | UUID | NO → Tarea 1 |
| Marca | comercial (UUID, paginado, tenant) | UUID | NO → Tarea 2 |
| PrincipioActivo | UUID global, sin paginar | UUID | NO → Tarea 3 |
| CondicionVenta | soporte (código, sin paginar, global) | código | NO → Tarea 4 |
| FormaFarmaceutica | soporte | código | NO → Tarea 5 |
| ViaAdministracion | soporte | código | NO → Tarea 6 |
| UnidadMedida | soporte | código | NO → Tarea 7 |
| ClasificacionControlada | soporte | código | NO → Tarea 8 |
| ProductoRegulado | dedicado | UUID | SÍ (ya tiene `GET /{productoReguladoId}`) |
| Sku | comercial (UUID, paginado, tenant) | UUID | SÍ (ya tiene `GET /{skuId}`) |
| RubroComercial | comercial (UUID, paginado, tenant) — NUEVO | UUID | Se construye completo → Tarea 9 |
| TipoDocumentoIdentidad | global sin tenant — NUEVO | código VARCHAR(2) | Se construye completo → Tarea 10 |

Nota: el prompt original estimaba "7" catálogos con GET-by-id faltante; la inspección directa confirma **8** (3 comerciales/UUID: CategoriaProducto, Marca, PrincipioActivo + 5 soporte/código: CondicionVenta, FormaFarmaceutica, ViaAdministracion, UnidadMedida, ClasificacionControlada).

---

## Tarea 1: GET /api/v1/catalogo/categorias/{categoriaId}

**Files:**
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoComercialPort.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoComercialJpaWriteAdapter.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarCategoriaProductoQuery.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarCategoriaProductoUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarCategoriaProductoHandler.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/mapper/CatalogoApiMapper.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/CategoriaProductoController.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarCategoriaProductoHandlerTest.java`

**Interfaces:**
- Consumes: `CategoriaProducto.restore(CategoriaProductoId id, TenantId tenantId, CategoriaProductoId categoriaPadreId, String codigo, String nombre, String descripcion, int nivel, int orden, EstadoCategoriaProducto estado)` (ya existe en `domain/model/CategoriaProducto.java`); `CatalogoApplicationMapper.toResult(CategoriaProducto)` → `CategoriaProductoResult` (ya existe); `CategoriaProductoJpaRepository.findByUuidPublico(UUID)` → `Optional<CategoriaProductoJpaEntity>` (ya existe); `CatalogoApiMapper.toResponse(CategoriaProductoResult)` → `CategoriaProductoResponse` (ya existe).
- Produces: `ConsultarCategoriaProductoUseCase.execute(ConsultarCategoriaProductoQuery)` → `Result<CategoriaProductoResult, ApplicationError>`; `CatalogoComercialPort.findCategoriaById(UUID tenantId, UUID categoriaId)` → `Optional<CategoriaProducto>`.

- [x] **Step 1: Escribir el test unitario del handler (falla primero)**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarCategoriaProductoHandlerTest.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarCategoriaProductoQuery;
import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.catalogo.domain.model.CategoriaProducto;
import com.softprimesolutions.catalogo.domain.model.EstadoCategoriaProducto;
import com.softprimesolutions.catalogo.domain.valueobject.CategoriaProductoId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarCategoriaProductoHandlerTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID CATEGORIA_ID = UUID.randomUUID();

    @Test
    void returnsCategoriaResultWhenFound() {
        var categoria = CategoriaProducto.restore(
                new CategoriaProductoId(CATEGORIA_ID), new TenantId(TENANT_ID), null, "MEDICAMENTOS",
                "Medicamentos", "Categoria raiz", 1, 1, EstadoCategoriaProducto.ACTIVO);
        CatalogoComercialPort port = new StubCatalogoComercialPort(Optional.of(categoria));
        var handler = new ConsultarCategoriaProductoHandler(port);

        var result = handler.execute(new ConsultarCategoriaProductoQuery(TENANT_ID, CATEGORIA_ID));

        assertThat(result.isSuccess()).isTrue();
        result.fold(
                success -> {
                    assertThat(success.id()).isEqualTo(CATEGORIA_ID);
                    assertThat(success.codigo()).isEqualTo("MEDICAMENTOS");
                    return null;
                },
                failure -> null);
    }

    @Test
    void returnsNotFoundWhenMissing() {
        CatalogoComercialPort port = new StubCatalogoComercialPort(Optional.empty());
        var handler = new ConsultarCategoriaProductoHandler(port);

        var result = handler.execute(new ConsultarCategoriaProductoQuery(TENANT_ID, CATEGORIA_ID));

        assertThat(result.isFailure()).isTrue();
        result.fold(
                success -> null,
                failure -> {
                    assertThat(failure.code()).isEqualTo("CAT_CATEGORIA_PRODUCTO_NO_ENCONTRADA");
                    assertThat(failure.category()).isEqualTo(ErrorCategory.NOT_FOUND);
                    return null;
                });
    }

    private record StubCatalogoComercialPort(Optional<CategoriaProducto> categoria) implements CatalogoComercialPort {

        @Override
        public SaveMarcaOutcome save(com.softprimesolutions.catalogo.domain.model.Marca marca) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveCategoriaOutcome save(CategoriaProducto categoria) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveSkuOutcome save(com.softprimesolutions.catalogo.domain.model.SKUComercial sku) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<com.softprimesolutions.catalogo.domain.model.SKUComercial> findSkuById(
                UUID tenantId, UUID skuId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CategoriaProducto> findCategoriaById(UUID tenantId, UUID categoriaId) {
            return categoria;
        }

        @Override
        public boolean categoriaExists(UUID tenantId, UUID categoriaId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean marcaExists(UUID tenantId, UUID marcaId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeMarcaStatus(UUID tenantId, UUID marcaId, String status, java.time.Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeCategoriaStatus(
                UUID tenantId, UUID categoriaId, String status, java.time.Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeSkuStatus(UUID tenantId, UUID skuId, String status, java.time.Instant changedAt) {
            throw new UnsupportedOperationException();
        }
    }
}
```

- [x] **Step 2: Ejecutar el test y verificar que falla por compilación (los tipos aún no existen)**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ConsultarCategoriaProductoHandlerTest"`
Expected: FAIL — error de compilación (`ConsultarCategoriaProductoQuery`, `ConsultarCategoriaProductoHandler`, `CatalogoComercialPort.findCategoriaById` no existen).

- [x] **Step 3: Agregar `findCategoriaById` a `CatalogoComercialPort`**

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoComercialPort.java`, agregar el método a la interfaz (después de `findSkuById`):

```java
    Optional<CategoriaProducto> findCategoriaById(UUID tenantId, UUID categoriaId);
```

El archivo completo queda:

```java
package com.softprimesolutions.catalogo.application.port.out;

import com.softprimesolutions.catalogo.domain.model.CategoriaProducto;
import com.softprimesolutions.catalogo.domain.model.Marca;
import com.softprimesolutions.catalogo.domain.model.SKUComercial;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CatalogoComercialPort {

    SaveMarcaOutcome save(Marca marca);

    SaveCategoriaOutcome save(CategoriaProducto categoria);

    SaveSkuOutcome save(SKUComercial sku);

    Optional<SKUComercial> findSkuById(UUID tenantId, UUID skuId);

    Optional<Marca> findMarcaById(UUID tenantId, UUID marcaId);

    Optional<CategoriaProducto> findCategoriaById(UUID tenantId, UUID categoriaId);

    boolean categoriaExists(UUID tenantId, UUID categoriaId);

    boolean marcaExists(UUID tenantId, UUID marcaId);

    boolean changeMarcaStatus(UUID tenantId, UUID marcaId, String status, Instant changedAt);

    boolean changeCategoriaStatus(UUID tenantId, UUID categoriaId, String status, Instant changedAt);

    boolean changeSkuStatus(UUID tenantId, UUID skuId, String status, Instant changedAt);

    enum SaveMarcaOutcome { CREATED, UPDATED, DUPLICATE_CODIGO, TENANT_NOT_FOUND, NOT_FOUND }

    enum SaveCategoriaOutcome { CREATED, UPDATED, DUPLICATE_CODIGO, TENANT_NOT_FOUND, CATEGORIA_PADRE_NOT_FOUND, NOT_FOUND }

    enum SaveSkuOutcome {
        CREATED, UPDATED, DUPLICATE_CODIGO_INTERNO, DUPLICATE_CODIGO_BARRA, TENANT_NOT_FOUND,
        PRODUCTO_REGULADO_NOT_FOUND, CATEGORIA_NOT_FOUND, MARCA_NOT_FOUND, NOT_FOUND
    }
}
```

(Nota: `findMarcaById` se agrega aquí también porque la Tarea 2 modifica el mismo archivo — si esta Tarea 1 se ejecuta antes que la Tarea 2, dejar solo la firma de `findCategoriaById` en este paso y `findMarcaById` se agregará en la Tarea 2 sin conflicto ya que son líneas independientes.)

- [x] **Step 4: Implementar `findCategoriaById` en `CatalogoComercialJpaWriteAdapter`**

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoComercialJpaWriteAdapter.java`, agregar el método (después de `findSkuById`, antes de `categoriaExists`):

```java
    @Override
    @Transactional(readOnly = true)
    public Optional<CategoriaProducto> findCategoriaById(UUID tenantId, UUID categoriaId) {
        var tenantInternalId = findTenantId(tenantId);
        if (tenantInternalId.isEmpty()) return Optional.empty();
        var entity = categoriaRepository.findByUuidPublico(categoriaId);
        if (entity.isEmpty() || !entity.get().getTenantId().equals(tenantInternalId.get())) {
            return Optional.empty();
        }

        var categoriaPadreId = entity.get().getCategoriaPadreId() == null ? null
                : findCategoriaUuid(entity.get().getCategoriaPadreId());

        var categoria = CategoriaProducto.restore(
                new CategoriaProductoId(entity.get().getUuidPublico()), new TenantId(tenantId),
                categoriaPadreId == null ? null : new CategoriaProductoId(categoriaPadreId),
                entity.get().getCodigo(), entity.get().getNombre(), entity.get().getDescripcion(),
                entity.get().getNivel(), entity.get().getOrden(),
                EstadoCategoriaProducto.valueOf(entity.get().getEstado()));
        return Optional.of(categoria);
    }

```

Agregar el import faltante al inicio del archivo (junto a los demás imports de `domain.model`):

```java
import com.softprimesolutions.catalogo.domain.model.EstadoCategoriaProducto;
```

- [x] **Step 5: Crear `ConsultarCategoriaProductoQuery`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarCategoriaProductoQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.UUID;

public record ConsultarCategoriaProductoQuery(UUID tenantId, UUID categoriaId)
        implements Query<CategoriaProductoResult> {
}
```

- [x] **Step 6: Crear `ConsultarCategoriaProductoUseCase`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarCategoriaProductoUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarCategoriaProductoQuery;
import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ConsultarCategoriaProductoUseCase {
    Result<CategoriaProductoResult, ApplicationError> execute(ConsultarCategoriaProductoQuery query);
}
```

- [x] **Step 7: Crear `ConsultarCategoriaProductoHandler`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarCategoriaProductoHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarCategoriaProductoQuery;
import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ConsultarCategoriaProductoUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarCategoriaProductoHandler implements ConsultarCategoriaProductoUseCase {

    private final CatalogoComercialPort comercialPort;

    public ConsultarCategoriaProductoHandler(CatalogoComercialPort comercialPort) {
        this.comercialPort = Objects.requireNonNull(comercialPort, "comercialPort es obligatorio");
    }

    @Override
    public Result<CategoriaProductoResult, ApplicationError> execute(ConsultarCategoriaProductoQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return comercialPort.findCategoriaById(query.tenantId(), query.categoriaId())
                .map(CatalogoApplicationMapper::toResult)
                .map(Result::<CategoriaProductoResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CAT_CATEGORIA_PRODUCTO_NO_ENCONTRADA", "La categoría indicada no existe.",
                        ErrorCategory.NOT_FOUND)));
    }
}
```

- [x] **Step 8: Ejecutar el test unitario y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ConsultarCategoriaProductoHandlerTest"`
Expected: PASS (2 tests).

- [x] **Step 9: Agregar el método al mapper API**

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/mapper/CatalogoApiMapper.java`, el método `toResponse(CategoriaProductoResult)` ya existe y es reutilizable tal cual — no se necesita agregar nada nuevo al mapper para este endpoint (el mismo `toResponse` sirve para GET-by-id y para cada item de la lista paginada).

- [x] **Step 10: Agregar el endpoint GET/{categoriaId} al controller**

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/CategoriaProductoController.java`, agregar el campo, el parámetro de constructor y el método. Archivo completo:

```java
package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoTenantRequest;
import com.softprimesolutions.catalogo.api.dto.request.CategoriaProductoRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.query.ConsultarCategoriaProductoQuery;
import com.softprimesolutions.catalogo.application.dto.query.ListarCategoriasProductoQuery;
import com.softprimesolutions.catalogo.application.port.in.ActualizarCategoriaProductoUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.ConsultarCategoriaProductoUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearCategoriaProductoUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarCategoriasProductoUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/catalogo/categorias")
public class CategoriaProductoController {

    private final CrearCategoriaProductoUseCase createCategoria;
    private final ActualizarCategoriaProductoUseCase updateCategoria;
    private final ConsultarCategoriaProductoUseCase getCategoria;
    private final ListarCategoriasProductoUseCase listCategorias;
    private final CatalogoControlUseCase control;

    public CategoriaProductoController(
            CrearCategoriaProductoUseCase createCategoria,
            ActualizarCategoriaProductoUseCase updateCategoria,
            ConsultarCategoriaProductoUseCase getCategoria,
            ListarCategoriasProductoUseCase listCategorias,
            CatalogoControlUseCase control) {
        this.createCategoria = createCategoria;
        this.updateCategoria = updateCategoria;
        this.getCategoria = getCategoria;
        this.listCategorias = listCategorias;
        this.control = control;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalogo.categorias.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody CategoriaProductoRequest request) {
        return createCategoria.execute(CatalogoApiMapper.toCreateCommand(request)).fold(
                result -> ResponseEntity.status(201).body(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PutMapping("/{categoriaId}")
    @PreAuthorize("hasAuthority('catalogo.categorias.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable UUID categoriaId, @Valid @RequestBody CategoriaProductoRequest request) {
        return updateCategoria.execute(CatalogoApiMapper.toUpdateCommand(categoriaId, request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{categoriaId}/estado")
    @PreAuthorize("hasAuthority('catalogo.categorias.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable UUID categoriaId, @Valid @RequestBody CambiarEstadoTenantRequest request) {
        return control.changeCategoriaProductoStatus(request.tenantId(), categoriaId, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), CatalogoControllerSupport::problem);
    }

    @GetMapping("/{categoriaId}")
    @PreAuthorize("hasAuthority('catalogo.categorias.consultar')")
    public ResponseEntity<?> get(@PathVariable UUID categoriaId, @RequestParam UUID tenantId) {
        return getCategoria.execute(new ConsultarCategoriaProductoQuery(tenantId, categoriaId)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.categorias.consultar')")
    public ResponseEntity<?> list(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoriaPadreId,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return listCategorias.execute(
                        new ListarCategoriasProductoQuery(tenantId, q, categoriaPadreId, estado, page, size))
                .fold(result -> ResponseEntity.ok(CatalogoApiMapper.toCategoriaPage(result)),
                        CatalogoControllerSupport::problem);
    }
}
```

- [x] **Step 11: Cablear el nuevo bean en `CatalogoModuleConfiguration`**

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java`, agregar el import:

```java
import com.softprimesolutions.catalogo.application.port.in.ConsultarCategoriaProductoUseCase;
```

(insertarlo en orden alfabético junto a los demás imports de `application.port.in`, después de `ConsultarProductoReguladoUseCase` — antes de `CrearCategoriaProductoUseCase`)

```java
import com.softprimesolutions.catalogo.application.usecase.query.ConsultarCategoriaProductoHandler;
```

(insertarlo junto a los demás imports de `application.usecase.query`, antes de `ConsultarProductoReguladoHandler`)

Y agregar el bean en la sección `// ---- Categorías de producto ----`:

```java
    @Bean
    ConsultarCategoriaProductoUseCase consultarCategoriaProductoUseCase(CatalogoComercialPort comercialPort) {
        return new ConsultarCategoriaProductoHandler(comercialPort);
    }
```

La sección completa queda:

```java
    // ---- Categorías de producto ----

    @Bean
    CrearCategoriaProductoUseCase crearCategoriaProductoUseCase(
            CatalogoComercialPort comercialPort, IdentifierGenerator catalogoIdentifierGenerator) {
        return new CrearCategoriaProductoHandler(comercialPort, catalogoIdentifierGenerator);
    }

    @Bean
    ActualizarCategoriaProductoUseCase actualizarCategoriaProductoUseCase(CatalogoComercialPort comercialPort) {
        return new ActualizarCategoriaProductoHandler(comercialPort);
    }

    @Bean
    ConsultarCategoriaProductoUseCase consultarCategoriaProductoUseCase(CatalogoComercialPort comercialPort) {
        return new ConsultarCategoriaProductoHandler(comercialPort);
    }

    @Bean
    ListarCategoriasProductoUseCase listarCategoriasProductoUseCase(CatalogoReadPort readPort) {
        return new ListarCategoriasProductoHandler(readPort);
    }
```

- [x] **Step 12: Actualizar el fake `CatalogoComercialPort` de `CatalogoControlServiceTest`**

El Step 3 de esta tarea agregó `findCategoriaById` y `findMarcaById` a la interfaz `CatalogoComercialPort`. La clase `FakeCatalogoComercialPort` dentro de `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlServiceTest.java` implementa esa interfaz y dejará de compilar si no se le agregan los dos métodos nuevos.

Editar ese archivo: dentro de la clase interna `private static final class FakeCatalogoComercialPort implements CatalogoComercialPort { ... }`, agregar los dos métodos que faltan (después de `findSkuById`, antes de `categoriaExists`):

```java
        @Override
        public Optional<CategoriaProducto> findCategoriaById(UUID tenantId, UUID categoriaId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Marca> findMarcaById(UUID tenantId, UUID marcaId) {
            throw new UnsupportedOperationException();
        }
```

No se necesitan casos de prueba nuevos para estos dos métodos aquí — `CatalogoControlServiceTest` prueba únicamente los métodos `change*Status`; los otros ya lanzan `UnsupportedOperationException()` en este fake por el mismo motivo (no los usa `CatalogoControlService`).

- [x] **Step 13: Ejecutar los tests del módulo para confirmar que `CatalogoControlServiceTest` sigue compilando**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.command.CatalogoControlServiceTest"`
Expected: BUILD SUCCESSFUL, los 4 tests existentes siguen pasando.

- [x] **Step 14: Compilar el módulo completo**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL.

- [x] **Step 15: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoComercialPort.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoComercialJpaWriteAdapter.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarCategoriaProductoQuery.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarCategoriaProductoUseCase.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarCategoriaProductoHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/CategoriaProductoController.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarCategoriaProductoHandlerTest.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlServiceTest.java
git commit -m "feat(catalogo): agregar GET /categorias/{categoriaId}"
```

---

## Tarea 2: GET /api/v1/catalogo/marcas/{marcaId}

**Files:**
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoComercialPort.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoComercialJpaWriteAdapter.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarMarcaQuery.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarMarcaUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarMarcaHandler.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/MarcaController.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarMarcaHandlerTest.java`

**Interfaces:**
- Consumes: `Marca.restore(MarcaId id, TenantId tenantId, String codigo, String nombre, String descripcion, EstadoMarca estado)` (ya existe); `CatalogoApplicationMapper.toResult(Marca)` → `MarcaResult` (ya existe); `MarcaJpaRepository.findByUuidPublico(UUID)` → `Optional<MarcaJpaEntity>` (ya existe); `CatalogoApiMapper.toResponse(MarcaResult)` → `MarcaResponse` (ya existe).
- Produces: `ConsultarMarcaUseCase.execute(ConsultarMarcaQuery)` → `Result<MarcaResult, ApplicationError>`; `CatalogoComercialPort.findMarcaById(UUID tenantId, UUID marcaId)` → `Optional<Marca>`.

- [x] **Step 1: Escribir el test unitario del handler (falla primero)**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarMarcaHandlerTest.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarMarcaQuery;
import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.catalogo.domain.model.CategoriaProducto;
import com.softprimesolutions.catalogo.domain.model.EstadoMarca;
import com.softprimesolutions.catalogo.domain.model.Marca;
import com.softprimesolutions.catalogo.domain.model.SKUComercial;
import com.softprimesolutions.catalogo.domain.valueobject.MarcaId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarMarcaHandlerTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID MARCA_ID = UUID.randomUUID();

    @Test
    void returnsMarcaResultWhenFound() {
        var marca = Marca.restore(
                new MarcaId(MARCA_ID), new TenantId(TENANT_ID), "GENFAR", "Genfar", "Marca generica",
                EstadoMarca.ACTIVO);
        CatalogoComercialPort port = new StubCatalogoComercialPort(Optional.of(marca));
        var handler = new ConsultarMarcaHandler(port);

        var result = handler.execute(new ConsultarMarcaQuery(TENANT_ID, MARCA_ID));

        assertThat(result.isSuccess()).isTrue();
        result.fold(
                success -> {
                    assertThat(success.id()).isEqualTo(MARCA_ID);
                    assertThat(success.codigo()).isEqualTo("GENFAR");
                    return null;
                },
                failure -> null);
    }

    @Test
    void returnsNotFoundWhenMissing() {
        CatalogoComercialPort port = new StubCatalogoComercialPort(Optional.empty());
        var handler = new ConsultarMarcaHandler(port);

        var result = handler.execute(new ConsultarMarcaQuery(TENANT_ID, MARCA_ID));

        assertThat(result.isFailure()).isTrue();
        result.fold(
                success -> null,
                failure -> {
                    assertThat(failure.code()).isEqualTo("CAT_MARCA_NO_ENCONTRADA");
                    assertThat(failure.category()).isEqualTo(ErrorCategory.NOT_FOUND);
                    return null;
                });
    }

    private record StubCatalogoComercialPort(Optional<Marca> marca) implements CatalogoComercialPort {

        @Override
        public SaveMarcaOutcome save(Marca marca) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveCategoriaOutcome save(CategoriaProducto categoria) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveSkuOutcome save(SKUComercial sku) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<SKUComercial> findSkuById(UUID tenantId, UUID skuId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Marca> findMarcaById(UUID tenantId, UUID marcaId) {
            return marca;
        }

        @Override
        public Optional<CategoriaProducto> findCategoriaById(UUID tenantId, UUID categoriaId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean categoriaExists(UUID tenantId, UUID categoriaId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean marcaExists(UUID tenantId, UUID marcaId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeMarcaStatus(UUID tenantId, UUID marcaId, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeCategoriaStatus(UUID tenantId, UUID categoriaId, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeSkuStatus(UUID tenantId, UUID skuId, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }
    }
}
```

- [x] **Step 2: Ejecutar el test y verificar que falla por compilación**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ConsultarMarcaHandlerTest"`
Expected: FAIL — error de compilación (`ConsultarMarcaQuery`, `ConsultarMarcaHandler`, `CatalogoComercialPort.findMarcaById` no existen todavía si la Tarea 1 no se ejecutó primero).

- [x] **Step 3: Agregar `findMarcaById` a `CatalogoComercialPort` (si la Tarea 1 no lo agregó ya)**

Verificar si `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoComercialPort.java` ya contiene `Optional<Marca> findMarcaById(UUID tenantId, UUID marcaId);`. Si NO está presente, agregarlo junto a `findSkuById`:

```java
    Optional<Marca> findMarcaById(UUID tenantId, UUID marcaId);
```

- [x] **Step 4: Implementar `findMarcaById` en `CatalogoComercialJpaWriteAdapter`**

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoComercialJpaWriteAdapter.java`, agregar el método (después de `findSkuById`, antes de `findCategoriaById` si ya existe, o antes de `categoriaExists`):

```java
    @Override
    @Transactional(readOnly = true)
    public Optional<Marca> findMarcaById(UUID tenantId, UUID marcaId) {
        var tenantInternalId = findTenantId(tenantId);
        if (tenantInternalId.isEmpty()) return Optional.empty();
        var entity = marcaRepository.findByUuidPublico(marcaId);
        if (entity.isEmpty() || !entity.get().getTenantId().equals(tenantInternalId.get())) {
            return Optional.empty();
        }

        var marca = Marca.restore(
                new MarcaId(entity.get().getUuidPublico()), new TenantId(tenantId), entity.get().getCodigo(),
                entity.get().getNombre(), entity.get().getDescripcion(),
                EstadoMarca.valueOf(entity.get().getEstado()));
        return Optional.of(marca);
    }

```

Agregar el import faltante al inicio del archivo (junto a los demás imports de `domain.model`):

```java
import com.softprimesolutions.catalogo.domain.model.EstadoMarca;
```

(Si la Tarea 1 ya se ejecutó, el archivo ya tiene el import `EstadoCategoriaProducto` — agregar `EstadoMarca` en orden alfabético justo antes.)

- [x] **Step 5: Crear `ConsultarMarcaQuery`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarMarcaQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.UUID;

public record ConsultarMarcaQuery(UUID tenantId, UUID marcaId) implements Query<MarcaResult> {
}
```

- [x] **Step 6: Crear `ConsultarMarcaUseCase`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarMarcaUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarMarcaQuery;
import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ConsultarMarcaUseCase {
    Result<MarcaResult, ApplicationError> execute(ConsultarMarcaQuery query);
}
```

- [x] **Step 7: Crear `ConsultarMarcaHandler`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarMarcaHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarMarcaQuery;
import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ConsultarMarcaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarMarcaHandler implements ConsultarMarcaUseCase {

    private final CatalogoComercialPort comercialPort;

    public ConsultarMarcaHandler(CatalogoComercialPort comercialPort) {
        this.comercialPort = Objects.requireNonNull(comercialPort, "comercialPort es obligatorio");
    }

    @Override
    public Result<MarcaResult, ApplicationError> execute(ConsultarMarcaQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return comercialPort.findMarcaById(query.tenantId(), query.marcaId())
                .map(CatalogoApplicationMapper::toResult)
                .map(Result::<MarcaResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CAT_MARCA_NO_ENCONTRADA", "La marca indicada no existe.", ErrorCategory.NOT_FOUND)));
    }
}
```

- [x] **Step 8: Ejecutar el test unitario y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ConsultarMarcaHandlerTest"`
Expected: PASS (2 tests).

- [x] **Step 9: Agregar el endpoint GET/{marcaId} al controller**

`CatalogoApiMapper.toResponse(MarcaResult)` ya existe y es reutilizable — no requiere cambios en el mapper.

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/MarcaController.java`, archivo completo:

```java
package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoTenantRequest;
import com.softprimesolutions.catalogo.api.dto.request.MarcaRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.query.ConsultarMarcaQuery;
import com.softprimesolutions.catalogo.application.dto.query.ListarMarcasQuery;
import com.softprimesolutions.catalogo.application.port.in.ActualizarMarcaUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.ConsultarMarcaUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearMarcaUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarMarcasUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/catalogo/marcas")
public class MarcaController {

    private final CrearMarcaUseCase createMarca;
    private final ActualizarMarcaUseCase updateMarca;
    private final ConsultarMarcaUseCase getMarca;
    private final ListarMarcasUseCase listMarcas;
    private final CatalogoControlUseCase control;

    public MarcaController(
            CrearMarcaUseCase createMarca, ActualizarMarcaUseCase updateMarca, ConsultarMarcaUseCase getMarca,
            ListarMarcasUseCase listMarcas, CatalogoControlUseCase control) {
        this.createMarca = createMarca;
        this.updateMarca = updateMarca;
        this.getMarca = getMarca;
        this.listMarcas = listMarcas;
        this.control = control;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalogo.marcas.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody MarcaRequest request) {
        return createMarca.execute(CatalogoApiMapper.toCreateCommand(request)).fold(
                result -> ResponseEntity.status(201).body(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PutMapping("/{marcaId}")
    @PreAuthorize("hasAuthority('catalogo.marcas.gestionar')")
    public ResponseEntity<?> update(@PathVariable UUID marcaId, @Valid @RequestBody MarcaRequest request) {
        return updateMarca.execute(CatalogoApiMapper.toUpdateCommand(marcaId, request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{marcaId}/estado")
    @PreAuthorize("hasAuthority('catalogo.marcas.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable UUID marcaId, @Valid @RequestBody CambiarEstadoTenantRequest request) {
        return control.changeMarcaStatus(request.tenantId(), marcaId, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), CatalogoControllerSupport::problem);
    }

    @GetMapping("/{marcaId}")
    @PreAuthorize("hasAuthority('catalogo.marcas.consultar')")
    public ResponseEntity<?> get(@PathVariable UUID marcaId, @RequestParam UUID tenantId) {
        return getMarca.execute(new ConsultarMarcaQuery(tenantId, marcaId)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.marcas.consultar')")
    public ResponseEntity<?> list(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return listMarcas.execute(new ListarMarcasQuery(tenantId, q, estado, page, size)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toMarcaPage(result)),
                CatalogoControllerSupport::problem);
    }
}
```

- [x] **Step 10: Cablear el nuevo bean en `CatalogoModuleConfiguration`**

Agregar los imports:

```java
import com.softprimesolutions.catalogo.application.port.in.ConsultarMarcaUseCase;
```

(orden alfabético, junto a `application.port.in`, antes de `CrearCategoriaProductoUseCase`)

```java
import com.softprimesolutions.catalogo.application.usecase.query.ConsultarMarcaHandler;
```

(orden alfabético, junto a `application.usecase.query`)

Y agregar el bean en la sección `// ---- Marcas ----`:

```java
    @Bean
    ConsultarMarcaUseCase consultarMarcaUseCase(CatalogoComercialPort comercialPort) {
        return new ConsultarMarcaHandler(comercialPort);
    }
```

La sección completa queda:

```java
    // ---- Marcas ----

    @Bean
    CrearMarcaUseCase crearMarcaUseCase(
            CatalogoComercialPort comercialPort, IdentifierGenerator catalogoIdentifierGenerator) {
        return new CrearMarcaHandler(comercialPort, catalogoIdentifierGenerator);
    }

    @Bean
    ActualizarMarcaUseCase actualizarMarcaUseCase(CatalogoComercialPort comercialPort) {
        return new ActualizarMarcaHandler(comercialPort);
    }

    @Bean
    ConsultarMarcaUseCase consultarMarcaUseCase(CatalogoComercialPort comercialPort) {
        return new ConsultarMarcaHandler(comercialPort);
    }

    @Bean
    ListarMarcasUseCase listarMarcasUseCase(CatalogoReadPort readPort) {
        return new ListarMarcasHandler(readPort);
    }
```

- [x] **Step 11: Compilar el módulo completo**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL.

- [x] **Step 12: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoComercialPort.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoComercialJpaWriteAdapter.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarMarcaQuery.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarMarcaUseCase.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarMarcaHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/MarcaController.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarMarcaHandlerTest.java
git commit -m "feat(catalogo): agregar GET /marcas/{marcaId}"
```

---

## Tarea 3: GET /api/v1/catalogo/principios-activos/{principioActivoId}

**Files:**
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoSoportePort.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoSoporteJpaWriteAdapter.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarPrincipioActivoQuery.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarPrincipioActivoUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarPrincipioActivoHandler.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/PrincipioActivoController.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarPrincipioActivoHandlerTest.java`

**Interfaces:**
- Consumes: `PrincipioActivo.restore(PrincipioActivoId id, String codigoFuente, String denominacion, String nombreNormalizado, String fuente, EstadoPrincipioActivo estado)` (ya existe); `CatalogoApplicationMapper.toResult(PrincipioActivo)` → `PrincipioActivoResult` (ya existe); `PrincipioActivoJpaRepository.findByUuidPublico(UUID)` → `Optional<PrincipioActivoJpaEntity>` (ya existe); `CatalogoApiMapper.toResponse(PrincipioActivoResult)` → `PrincipioActivoResponse` (ya existe).
- Produces: `ConsultarPrincipioActivoUseCase.execute(ConsultarPrincipioActivoQuery)` → `Result<PrincipioActivoResult, ApplicationError>`; `CatalogoSoportePort.findPrincipioActivoById(UUID principioActivoId)` → `Optional<PrincipioActivo>`.

- [x] **Step 1: Escribir el test unitario del handler (falla primero)**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarPrincipioActivoHandlerTest.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarPrincipioActivoQuery;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.EstadoPrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import com.softprimesolutions.catalogo.domain.valueobject.PrincipioActivoId;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarPrincipioActivoHandlerTest {

    private static final UUID PRINCIPIO_ACTIVO_ID = UUID.randomUUID();

    @Test
    void returnsPrincipioActivoResultWhenFound() {
        var principioActivo = PrincipioActivo.restore(
                new PrincipioActivoId(PRINCIPIO_ACTIVO_ID), "PARAC-500", "Paracetamol", "PARACETAMOL", "DIGEMID",
                EstadoPrincipioActivo.ACTIVO);
        CatalogoSoportePort port = new StubCatalogoSoportePort(Optional.of(principioActivo));
        var handler = new ConsultarPrincipioActivoHandler(port);

        var result = handler.execute(new ConsultarPrincipioActivoQuery(PRINCIPIO_ACTIVO_ID));

        assertThat(result.isSuccess()).isTrue();
        result.fold(
                success -> {
                    assertThat(success.id()).isEqualTo(PRINCIPIO_ACTIVO_ID);
                    assertThat(success.denominacion()).isEqualTo("Paracetamol");
                    return null;
                },
                failure -> null);
    }

    @Test
    void returnsNotFoundWhenMissing() {
        CatalogoSoportePort port = new StubCatalogoSoportePort(Optional.empty());
        var handler = new ConsultarPrincipioActivoHandler(port);

        var result = handler.execute(new ConsultarPrincipioActivoQuery(PRINCIPIO_ACTIVO_ID));

        assertThat(result.isFailure()).isTrue();
        result.fold(
                success -> null,
                failure -> {
                    assertThat(failure.code()).isEqualTo("CAT_PRINCIPIO_ACTIVO_NO_ENCONTRADO");
                    assertThat(failure.category()).isEqualTo(ErrorCategory.NOT_FOUND);
                    return null;
                });
    }

    private record StubCatalogoSoportePort(Optional<PrincipioActivo> principioActivo) implements CatalogoSoportePort {

        @Override
        public SaveOutcome save(CondicionVenta condicionVenta) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(FormaFarmaceutica formaFarmaceutica) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(ViaAdministracion viaAdministracion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(UnidadMedida unidadMedida) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(ClasificacionControlada clasificacionControlada) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SavePrincipioActivoOutcome save(PrincipioActivo principioActivo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<PrincipioActivo> findPrincipioActivoById(UUID principioActivoId) {
            return principioActivo;
        }

        @Override
        public boolean condicionVentaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean formaFarmaceuticaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean viaAdministracionExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean unidadMedidaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean clasificacionControladaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean principioActivoExists(UUID principioActivoId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeCondicionVentaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeFormaFarmaceuticaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeViaAdministracionStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeUnidadMedidaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeClasificacionControladaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changePrincipioActivoStatus(UUID principioActivoId, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }
    }
}
```

- [x] **Step 2: Ejecutar el test y verificar que falla por compilación**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ConsultarPrincipioActivoHandlerTest"`
Expected: FAIL — error de compilación (`findPrincipioActivoById` no existe en `CatalogoSoportePort`).

- [x] **Step 3: Agregar `findPrincipioActivoById` a `CatalogoSoportePort`**

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoSoportePort.java`, agregar el método (después de `save(PrincipioActivo)`, antes de `condicionVentaExists`). El archivo completo queda:

```java
package com.softprimesolutions.catalogo.application.port.out;

import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CatalogoSoportePort {

    SaveOutcome save(CondicionVenta condicionVenta);

    SaveOutcome save(FormaFarmaceutica formaFarmaceutica);

    SaveOutcome save(ViaAdministracion viaAdministracion);

    SaveOutcome save(UnidadMedida unidadMedida);

    SaveOutcome save(ClasificacionControlada clasificacionControlada);

    SavePrincipioActivoOutcome save(PrincipioActivo principioActivo);

    Optional<CondicionVenta> findCondicionVentaByCodigo(String codigo);

    Optional<FormaFarmaceutica> findFormaFarmaceuticaByCodigo(String codigo);

    Optional<ViaAdministracion> findViaAdministracionByCodigo(String codigo);

    Optional<UnidadMedida> findUnidadMedidaByCodigo(String codigo);

    Optional<ClasificacionControlada> findClasificacionControladaByCodigo(String codigo);

    Optional<PrincipioActivo> findPrincipioActivoById(UUID principioActivoId);

    boolean condicionVentaExists(String codigo);

    boolean formaFarmaceuticaExists(String codigo);

    boolean viaAdministracionExists(String codigo);

    boolean unidadMedidaExists(String codigo);

    boolean clasificacionControladaExists(String codigo);

    boolean principioActivoExists(UUID principioActivoId);

    boolean changeCondicionVentaStatus(String codigo, String status, Instant changedAt);

    boolean changeFormaFarmaceuticaStatus(String codigo, String status, Instant changedAt);

    boolean changeViaAdministracionStatus(String codigo, String status, Instant changedAt);

    boolean changeUnidadMedidaStatus(String codigo, String status, Instant changedAt);

    boolean changeClasificacionControladaStatus(String codigo, String status, Instant changedAt);

    boolean changePrincipioActivoStatus(UUID principioActivoId, String status, Instant changedAt);

    enum SaveOutcome { CREATED, UPDATED, DUPLICATE_CODIGO, NOT_FOUND }

    enum SavePrincipioActivoOutcome { CREATED, UPDATED, NOT_FOUND, DUPLICATE_DENOMINACION }
}
```

(Nota: este archivo también incluye ya las firmas `findCondicionVentaByCodigo`, `findFormaFarmaceuticaByCodigo`, `findViaAdministracionByCodigo`, `findUnidadMedidaByCodigo`, `findClasificacionControladaByCodigo` que agregan las Tareas 4-8 — si esta Tarea 3 se ejecuta antes, agregar SOLO la línea `Optional<PrincipioActivo> findPrincipioActivoById(UUID principioActivoId);` y dejar que las Tareas 4-8 agreguen las suyas sin conflicto, ya que son líneas independientes en la interfaz.)

- [x] **Step 4: Implementar `findPrincipioActivoById` en `CatalogoSoporteJpaWriteAdapter`**

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoSoporteJpaWriteAdapter.java`, agregar el método (después de `save(PrincipioActivo)`, antes de `condicionVentaExists`):

```java
    @Override
    @Transactional(readOnly = true)
    public Optional<PrincipioActivo> findPrincipioActivoById(UUID principioActivoId) {
        return principioActivoRepository.findByUuidPublico(principioActivoId)
                .map(entity -> PrincipioActivo.restore(
                        new PrincipioActivoId(entity.getUuidPublico()), entity.getCodigoFuente(),
                        entity.getDenominacion(), entity.getNombreNormalizado(), entity.getFuente(),
                        EstadoPrincipioActivo.valueOf(entity.getEstado())));
    }

```

Agregar los imports faltantes al inicio del archivo:

```java
import com.softprimesolutions.catalogo.domain.model.EstadoPrincipioActivo;
import com.softprimesolutions.catalogo.domain.valueobject.PrincipioActivoId;
```

(orden alfabético junto a los imports existentes de `domain.model.PrincipioActivo` y `domain.model.soporte.*`)

- [x] **Step 5: Crear `ConsultarPrincipioActivoQuery`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarPrincipioActivoQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.UUID;

public record ConsultarPrincipioActivoQuery(UUID principioActivoId) implements Query<PrincipioActivoResult> {
}
```

- [x] **Step 6: Crear `ConsultarPrincipioActivoUseCase`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarPrincipioActivoUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarPrincipioActivoQuery;
import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ConsultarPrincipioActivoUseCase {
    Result<PrincipioActivoResult, ApplicationError> execute(ConsultarPrincipioActivoQuery query);
}
```

- [x] **Step 7: Crear `ConsultarPrincipioActivoHandler`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarPrincipioActivoHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarPrincipioActivoQuery;
import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ConsultarPrincipioActivoUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarPrincipioActivoHandler implements ConsultarPrincipioActivoUseCase {

    private final CatalogoSoportePort soportePort;

    public ConsultarPrincipioActivoHandler(CatalogoSoportePort soportePort) {
        this.soportePort = Objects.requireNonNull(soportePort, "soportePort es obligatorio");
    }

    @Override
    public Result<PrincipioActivoResult, ApplicationError> execute(ConsultarPrincipioActivoQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return soportePort.findPrincipioActivoById(query.principioActivoId())
                .map(CatalogoApplicationMapper::toResult)
                .map(Result::<PrincipioActivoResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CAT_PRINCIPIO_ACTIVO_NO_ENCONTRADO", "El principio activo indicado no existe.",
                        ErrorCategory.NOT_FOUND)));
    }
}
```

- [x] **Step 8: Ejecutar el test unitario y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ConsultarPrincipioActivoHandlerTest"`
Expected: PASS (2 tests).

- [x] **Step 9: Agregar el endpoint GET/{principioActivoId} al controller**

`CatalogoApiMapper.toResponse(PrincipioActivoResult)` ya existe — no requiere cambios en el mapper.

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/PrincipioActivoController.java`, archivo completo:

```java
package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoGlobalRequest;
import com.softprimesolutions.catalogo.api.dto.request.PrincipioActivoRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.query.ConsultarPrincipioActivoQuery;
import com.softprimesolutions.catalogo.application.dto.query.ListarPrincipioActivoQuery;
import com.softprimesolutions.catalogo.application.port.in.ActualizarPrincipioActivoUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.ConsultarPrincipioActivoUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearPrincipioActivoUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarPrincipioActivoUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/catalogo/principios-activos")
public class PrincipioActivoController {

    private final CrearPrincipioActivoUseCase createPrincipioActivo;
    private final ActualizarPrincipioActivoUseCase updatePrincipioActivo;
    private final ConsultarPrincipioActivoUseCase getPrincipioActivo;
    private final ListarPrincipioActivoUseCase listPrincipiosActivos;
    private final CatalogoControlUseCase control;

    public PrincipioActivoController(
            CrearPrincipioActivoUseCase createPrincipioActivo,
            ActualizarPrincipioActivoUseCase updatePrincipioActivo,
            ConsultarPrincipioActivoUseCase getPrincipioActivo,
            ListarPrincipioActivoUseCase listPrincipiosActivos,
            CatalogoControlUseCase control) {
        this.createPrincipioActivo = createPrincipioActivo;
        this.updatePrincipioActivo = updatePrincipioActivo;
        this.getPrincipioActivo = getPrincipioActivo;
        this.listPrincipiosActivos = listPrincipiosActivos;
        this.control = control;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalogo.principios-activos.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody PrincipioActivoRequest request) {
        return createPrincipioActivo.execute(CatalogoApiMapper.toCreateCommand(request)).fold(
                result -> ResponseEntity.status(201).body(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PutMapping("/{principioActivoId}")
    @PreAuthorize("hasAuthority('catalogo.principios-activos.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable UUID principioActivoId, @Valid @RequestBody PrincipioActivoRequest request) {
        return updatePrincipioActivo.execute(CatalogoApiMapper.toUpdateCommand(principioActivoId, request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{principioActivoId}/estado")
    @PreAuthorize("hasAuthority('catalogo.principios-activos.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable UUID principioActivoId, @Valid @RequestBody CambiarEstadoGlobalRequest request) {
        return control.changePrincipioActivoStatus(principioActivoId, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), CatalogoControllerSupport::problem);
    }

    @GetMapping("/{principioActivoId}")
    @PreAuthorize("hasAuthority('catalogo.principios-activos.consultar')")
    public ResponseEntity<?> get(@PathVariable UUID principioActivoId) {
        return getPrincipioActivo.execute(new ConsultarPrincipioActivoQuery(principioActivoId)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.principios-activos.consultar')")
    public ResponseEntity<?> list(
            @RequestParam(required = false) String texto, @RequestParam(required = false) String estado) {
        return listPrincipiosActivos.execute(new ListarPrincipioActivoQuery(texto, estado)).fold(
                result -> ResponseEntity.ok(result.stream().map(CatalogoApiMapper::toResponse).toList()),
                CatalogoControllerSupport::problem);
    }
}
```

- [x] **Step 10: Cablear el nuevo bean en `CatalogoModuleConfiguration`**

Agregar los imports:

```java
import com.softprimesolutions.catalogo.application.port.in.ConsultarPrincipioActivoUseCase;
```

(orden alfabético, junto a `application.port.in`)

```java
import com.softprimesolutions.catalogo.application.usecase.query.ConsultarPrincipioActivoHandler;
```

(orden alfabético, junto a `application.usecase.query`)

Y agregar el bean en la sección `// ---- Principios activos ----`:

```java
    @Bean
    ConsultarPrincipioActivoUseCase consultarPrincipioActivoUseCase(CatalogoSoportePort soportePort) {
        return new ConsultarPrincipioActivoHandler(soportePort);
    }
```

La sección completa queda:

```java
    // ---- Principios activos ----

    @Bean
    CrearPrincipioActivoUseCase crearPrincipioActivoUseCase(
            CatalogoSoportePort soportePort, IdentifierGenerator catalogoIdentifierGenerator) {
        return new CrearPrincipioActivoHandler(soportePort, catalogoIdentifierGenerator);
    }

    @Bean
    ActualizarPrincipioActivoUseCase actualizarPrincipioActivoUseCase(CatalogoSoportePort soportePort) {
        return new ActualizarPrincipioActivoHandler(soportePort);
    }

    @Bean
    ConsultarPrincipioActivoUseCase consultarPrincipioActivoUseCase(CatalogoSoportePort soportePort) {
        return new ConsultarPrincipioActivoHandler(soportePort);
    }

    @Bean
    ListarPrincipioActivoUseCase listarPrincipioActivoUseCase(CatalogoReadPort readPort) {
        return new ListarPrincipioActivoHandler(readPort);
    }
```

- [x] **Step 11: Actualizar el fake `CatalogoSoportePort` de `CatalogoControlServiceTest`**

El Step 3 de esta tarea agregó seis métodos `find*` a la interfaz `CatalogoSoportePort` (`findCondicionVentaByCodigo`, `findFormaFarmaceuticaByCodigo`, `findViaAdministracionByCodigo`, `findUnidadMedidaByCodigo`, `findClasificacionControladaByCodigo`, `findPrincipioActivoById`). La clase `FakeCatalogoSoportePort` dentro de `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlServiceTest.java` implementa esa interfaz y dejará de compilar si no se le agregan los seis métodos nuevos.

Editar ese archivo: dentro de la clase interna `private static final class FakeCatalogoSoportePort implements CatalogoSoportePort { ... }`, agregar los seis métodos que faltan (después de `save(PrincipioActivo)`, antes de `condicionVentaExists`):

```java
        @Override
        public Optional<CondicionVenta> findCondicionVentaByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<FormaFarmaceutica> findFormaFarmaceuticaByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ViaAdministracion> findViaAdministracionByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<UnidadMedida> findUnidadMedidaByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ClasificacionControlada> findClasificacionControladaByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<PrincipioActivo> findPrincipioActivoById(UUID principioActivoId) {
            throw new UnsupportedOperationException();
        }
```

`java.util.Optional` ya está importado en este archivo (usado por `FakeCatalogoComercialPort.findSkuById` y `FakeProductoReguladoPort.findById`).

- [x] **Step 12: Ejecutar los tests del módulo para confirmar que `CatalogoControlServiceTest` sigue compilando**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.command.CatalogoControlServiceTest"`
Expected: BUILD SUCCESSFUL, los 4 tests existentes siguen pasando.

- [x] **Step 13: Compilar el módulo completo**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL.

- [x] **Step 14: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoSoportePort.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoSoporteJpaWriteAdapter.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarPrincipioActivoQuery.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarPrincipioActivoUseCase.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarPrincipioActivoHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/PrincipioActivoController.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarPrincipioActivoHandlerTest.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlServiceTest.java
git commit -m "feat(catalogo): agregar GET /principios-activos/{principioActivoId}"
```

---

## Tarea 4: GET /api/v1/catalogo/condiciones-venta/{codigo}

**Files:**
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoSoportePort.java` (agrega `findCondicionVentaByCodigo` si la Tarea 3 no lo hizo ya)
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoSoporteJpaWriteAdapter.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarCondicionVentaQuery.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarCondicionVentaUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarCondicionVentaHandler.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/CondicionVentaController.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarCondicionVentaHandlerTest.java`

**Interfaces:**
- Consumes: `CondicionVenta.restore(String codigo, String denominacion, boolean requiereReceta, boolean requiereRetencion, String fuente, String versionFuente, LocalDate vigenteDesde, LocalDate vigenteHasta, EstadoCatalogoSoporte estado)` (ya existe); `CatalogoApplicationMapper.toResult(CondicionVenta)` → `CondicionVentaResult` (ya existe); `CondicionVentaJpaRepository.findById(String)` → `Optional<CondicionVentaJpaEntity>` (heredado de `JpaRepository`, ya existe); `CatalogoApiMapper.toResponse(CondicionVentaResult)` → `CondicionVentaResponse` (ya existe).
- Produces: `ConsultarCondicionVentaUseCase.execute(ConsultarCondicionVentaQuery)` → `Result<CondicionVentaResult, ApplicationError>`; `CatalogoSoportePort.findCondicionVentaByCodigo(String codigo)` → `Optional<CondicionVenta>`.

- [x] **Step 1: Escribir el test unitario del handler (falla primero)**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarCondicionVentaHandlerTest.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarCondicionVentaQuery;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.EstadoCatalogoSoporte;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarCondicionVentaHandlerTest {

    @Test
    void returnsCondicionVentaResultWhenFound() {
        var condicionVenta = CondicionVenta.restore(
                "VENTA_LIBRE", "Venta libre", false, false, "DIGEMID", "2026.1", null, null,
                EstadoCatalogoSoporte.ACTIVO);
        CatalogoSoportePort port = new StubCatalogoSoportePort(Optional.of(condicionVenta));
        var handler = new ConsultarCondicionVentaHandler(port);

        var result = handler.execute(new ConsultarCondicionVentaQuery("VENTA_LIBRE"));

        assertThat(result.isSuccess()).isTrue();
        result.fold(
                success -> {
                    assertThat(success.codigo()).isEqualTo("VENTA_LIBRE");
                    assertThat(success.denominacion()).isEqualTo("Venta libre");
                    return null;
                },
                failure -> null);
    }

    @Test
    void returnsNotFoundWhenMissing() {
        CatalogoSoportePort port = new StubCatalogoSoportePort(Optional.empty());
        var handler = new ConsultarCondicionVentaHandler(port);

        var result = handler.execute(new ConsultarCondicionVentaQuery("NO_EXISTE"));

        assertThat(result.isFailure()).isTrue();
        result.fold(
                success -> null,
                failure -> {
                    assertThat(failure.code()).isEqualTo("CAT_CONDICION_VENTA_NO_ENCONTRADA");
                    assertThat(failure.category()).isEqualTo(ErrorCategory.NOT_FOUND);
                    return null;
                });
    }

    private record StubCatalogoSoportePort(Optional<CondicionVenta> condicionVenta) implements CatalogoSoportePort {

        @Override
        public SaveOutcome save(CondicionVenta condicionVenta) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(FormaFarmaceutica formaFarmaceutica) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(ViaAdministracion viaAdministracion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(UnidadMedida unidadMedida) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(ClasificacionControlada clasificacionControlada) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SavePrincipioActivoOutcome save(PrincipioActivo principioActivo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CondicionVenta> findCondicionVentaByCodigo(String codigo) {
            return condicionVenta;
        }

        @Override
        public Optional<FormaFarmaceutica> findFormaFarmaceuticaByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ViaAdministracion> findViaAdministracionByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<UnidadMedida> findUnidadMedidaByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ClasificacionControlada> findClasificacionControladaByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<PrincipioActivo> findPrincipioActivoById(UUID principioActivoId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean condicionVentaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean formaFarmaceuticaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean viaAdministracionExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean unidadMedidaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean clasificacionControladaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean principioActivoExists(UUID principioActivoId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeCondicionVentaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeFormaFarmaceuticaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeViaAdministracionStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeUnidadMedidaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeClasificacionControladaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changePrincipioActivoStatus(UUID principioActivoId, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }
    }
}
```

- [x] **Step 2: Ejecutar el test y verificar que falla por compilación**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ConsultarCondicionVentaHandlerTest"`
Expected: FAIL — error de compilación.

- [x] **Step 3: Agregar `findCondicionVentaByCodigo` a `CatalogoSoportePort` (si la Tarea 3 no lo agregó ya)**

Verificar que `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoSoportePort.java` contenga:

```java
    Optional<CondicionVenta> findCondicionVentaByCodigo(String codigo);
```

Si la Tarea 3 ya se ejecutó, esta línea ya existe (ver el archivo completo mostrado en la Tarea 3, Step 3) — no duplicar.

- [x] **Step 4: Implementar `findCondicionVentaByCodigo` en `CatalogoSoporteJpaWriteAdapter`**

Agregar el método (después de `save(CondicionVenta)`, antes de `save(FormaFarmaceutica)` o en la sección de `findXByCodigo` si ya existe alguna de las Tareas 3/5/6/7/8):

```java
    @Override
    @Transactional(readOnly = true)
    public Optional<CondicionVenta> findCondicionVentaByCodigo(String codigo) {
        return condicionVentaRepository.findById(codigo)
                .map(entity -> CondicionVenta.restore(
                        entity.getCodigo(), entity.getDenominacion(), entity.isRequiereReceta(),
                        entity.isRequiereRetencion(), entity.getFuente(), entity.getVersionFuente(),
                        entity.getVigenteDesde(), entity.getVigenteHasta(),
                        EstadoCatalogoSoporte.valueOf(entity.getEstado())));
    }

```

Agregar el import faltante si no está presente:

```java
import com.softprimesolutions.catalogo.domain.model.soporte.EstadoCatalogoSoporte;
```

- [x] **Step 5: Crear `ConsultarCondicionVentaQuery`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarCondicionVentaQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.shared.application.cqrs.Query;

public record ConsultarCondicionVentaQuery(String codigo) implements Query<CondicionVentaResult> {
}
```

- [x] **Step 6: Crear `ConsultarCondicionVentaUseCase`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarCondicionVentaUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarCondicionVentaQuery;
import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ConsultarCondicionVentaUseCase {
    Result<CondicionVentaResult, ApplicationError> execute(ConsultarCondicionVentaQuery query);
}
```

- [x] **Step 7: Crear `ConsultarCondicionVentaHandler`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarCondicionVentaHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarCondicionVentaQuery;
import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ConsultarCondicionVentaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarCondicionVentaHandler implements ConsultarCondicionVentaUseCase {

    private final CatalogoSoportePort soportePort;

    public ConsultarCondicionVentaHandler(CatalogoSoportePort soportePort) {
        this.soportePort = Objects.requireNonNull(soportePort, "soportePort es obligatorio");
    }

    @Override
    public Result<CondicionVentaResult, ApplicationError> execute(ConsultarCondicionVentaQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return soportePort.findCondicionVentaByCodigo(query.codigo())
                .map(CatalogoApplicationMapper::toResult)
                .map(Result::<CondicionVentaResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CAT_CONDICION_VENTA_NO_ENCONTRADA", "La condición de venta indicada no existe.",
                        ErrorCategory.NOT_FOUND)));
    }
}
```

- [x] **Step 8: Ejecutar el test unitario y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ConsultarCondicionVentaHandlerTest"`
Expected: PASS (2 tests).

- [x] **Step 9: Agregar el endpoint GET/{codigo} al controller**

`CatalogoApiMapper.toResponse(CondicionVentaResult)` ya existe — no requiere cambios en el mapper.

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/CondicionVentaController.java`, archivo completo:

```java
package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoGlobalRequest;
import com.softprimesolutions.catalogo.api.dto.request.CondicionVentaRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.query.ConsultarCondicionVentaQuery;
import com.softprimesolutions.catalogo.application.dto.query.ListarCondicionesVentaQuery;
import com.softprimesolutions.catalogo.application.port.in.ActualizarCondicionVentaUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.ConsultarCondicionVentaUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearCondicionVentaUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarCondicionesVentaUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/catalogo/condiciones-venta")
public class CondicionVentaController {

    private final CrearCondicionVentaUseCase crearCondicionVenta;
    private final ActualizarCondicionVentaUseCase actualizarCondicionVenta;
    private final ConsultarCondicionVentaUseCase consultarCondicionVenta;
    private final ListarCondicionesVentaUseCase listarCondicionesVenta;
    private final CatalogoControlUseCase control;

    public CondicionVentaController(
            CrearCondicionVentaUseCase crearCondicionVenta,
            ActualizarCondicionVentaUseCase actualizarCondicionVenta,
            ConsultarCondicionVentaUseCase consultarCondicionVenta,
            ListarCondicionesVentaUseCase listarCondicionesVenta,
            CatalogoControlUseCase control) {
        this.crearCondicionVenta = crearCondicionVenta;
        this.actualizarCondicionVenta = actualizarCondicionVenta;
        this.consultarCondicionVenta = consultarCondicionVenta;
        this.listarCondicionesVenta = listarCondicionesVenta;
        this.control = control;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody CondicionVentaRequest request) {
        return crearCondicionVenta.execute(CatalogoApiMapper.toCreateCommand(request)).fold(
                result -> ResponseEntity.status(201).body(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> update(@PathVariable String codigo, @Valid @RequestBody CondicionVentaRequest request) {
        return actualizarCondicionVenta.execute(CatalogoApiMapper.toUpdateCommand(request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{codigo}/estado")
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable String codigo, @Valid @RequestBody CambiarEstadoGlobalRequest request) {
        return control.changeCondicionVentaStatus(codigo, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), CatalogoControllerSupport::problem);
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("hasAuthority('catalogo.soporte.consultar')")
    public ResponseEntity<?> get(@PathVariable String codigo) {
        return consultarCondicionVenta.execute(new ConsultarCondicionVentaQuery(codigo)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.soporte.consultar')")
    public ResponseEntity<?> list(@RequestParam(required = false) String estado) {
        return listarCondicionesVenta.execute(new ListarCondicionesVentaQuery(estado)).fold(
                result -> ResponseEntity.ok(result.stream().map(CatalogoApiMapper::toResponse).toList()),
                CatalogoControllerSupport::problem);
    }
}
```

- [x] **Step 10: Cablear el nuevo bean en `CatalogoModuleConfiguration`**

Agregar los imports:

```java
import com.softprimesolutions.catalogo.application.port.in.ConsultarCondicionVentaUseCase;
```

```java
import com.softprimesolutions.catalogo.application.usecase.query.ConsultarCondicionVentaHandler;
```

(orden alfabético en sus respectivos grupos de import)

Y agregar el bean en la sección `// ---- Soporte: condiciones de venta ----`:

```java
    @Bean
    ConsultarCondicionVentaUseCase consultarCondicionVentaUseCase(CatalogoSoportePort soportePort) {
        return new ConsultarCondicionVentaHandler(soportePort);
    }
```

La sección completa queda:

```java
    // ---- Soporte: condiciones de venta ----

    @Bean
    CrearCondicionVentaUseCase crearCondicionVentaUseCase(CatalogoSoportePort soportePort) {
        return new CrearCondicionVentaHandler(soportePort);
    }

    @Bean
    ActualizarCondicionVentaUseCase actualizarCondicionVentaUseCase(CatalogoSoportePort soportePort) {
        return new ActualizarCondicionVentaHandler(soportePort);
    }

    @Bean
    ConsultarCondicionVentaUseCase consultarCondicionVentaUseCase(CatalogoSoportePort soportePort) {
        return new ConsultarCondicionVentaHandler(soportePort);
    }

    @Bean
    ListarCondicionesVentaUseCase listarCondicionesVentaUseCase(CatalogoReadPort readPort) {
        return new ListarCondicionesVentaHandler(readPort);
    }
```

- [x] **Step 11: Compilar el módulo completo**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL.

- [x] **Step 12: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoSoportePort.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoSoporteJpaWriteAdapter.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarCondicionVentaQuery.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarCondicionVentaUseCase.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarCondicionVentaHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/CondicionVentaController.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarCondicionVentaHandlerTest.java
git commit -m "feat(catalogo): agregar GET /condiciones-venta/{codigo}"
```

---

## Tarea 5: GET /api/v1/catalogo/formas-farmaceuticas/{codigo}

**Files:**
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoSoportePort.java` (agrega `findFormaFarmaceuticaByCodigo` si no existe ya)
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoSoporteJpaWriteAdapter.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarFormaFarmaceuticaQuery.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarFormaFarmaceuticaUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarFormaFarmaceuticaHandler.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/FormaFarmaceuticaController.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarFormaFarmaceuticaHandlerTest.java`

**Interfaces:**
- Consumes: `FormaFarmaceutica.restore(String codigo, String denominacion, String fuente, EstadoCatalogoSoporte estado)` (ya existe); `CatalogoApplicationMapper.toResult(FormaFarmaceutica)` → `FormaFarmaceuticaResult` (ya existe); `FormaFarmaceuticaJpaRepository.findById(String)` (heredado); `CatalogoApiMapper.toResponse(FormaFarmaceuticaResult)` → `FormaFarmaceuticaResponse` (ya existe).
- Produces: `ConsultarFormaFarmaceuticaUseCase.execute(ConsultarFormaFarmaceuticaQuery)` → `Result<FormaFarmaceuticaResult, ApplicationError>`; `CatalogoSoportePort.findFormaFarmaceuticaByCodigo(String codigo)` → `Optional<FormaFarmaceutica>`.

- [x] **Step 1: Escribir el test unitario del handler (falla primero)**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarFormaFarmaceuticaHandlerTest.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarFormaFarmaceuticaQuery;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.EstadoCatalogoSoporte;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarFormaFarmaceuticaHandlerTest {

    @Test
    void returnsFormaFarmaceuticaResultWhenFound() {
        var formaFarmaceutica = FormaFarmaceutica.restore("TABLETA", "Tableta", "DIGEMID", EstadoCatalogoSoporte.ACTIVO);
        CatalogoSoportePort port = new StubCatalogoSoportePort(Optional.of(formaFarmaceutica));
        var handler = new ConsultarFormaFarmaceuticaHandler(port);

        var result = handler.execute(new ConsultarFormaFarmaceuticaQuery("TABLETA"));

        assertThat(result.isSuccess()).isTrue();
        result.fold(
                success -> {
                    assertThat(success.codigo()).isEqualTo("TABLETA");
                    assertThat(success.denominacion()).isEqualTo("Tableta");
                    return null;
                },
                failure -> null);
    }

    @Test
    void returnsNotFoundWhenMissing() {
        CatalogoSoportePort port = new StubCatalogoSoportePort(Optional.empty());
        var handler = new ConsultarFormaFarmaceuticaHandler(port);

        var result = handler.execute(new ConsultarFormaFarmaceuticaQuery("NO_EXISTE"));

        assertThat(result.isFailure()).isTrue();
        result.fold(
                success -> null,
                failure -> {
                    assertThat(failure.code()).isEqualTo("CAT_FORMA_FARMACEUTICA_NO_ENCONTRADA");
                    assertThat(failure.category()).isEqualTo(ErrorCategory.NOT_FOUND);
                    return null;
                });
    }

    private record StubCatalogoSoportePort(Optional<FormaFarmaceutica> formaFarmaceutica)
            implements CatalogoSoportePort {

        @Override
        public SaveOutcome save(CondicionVenta condicionVenta) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(FormaFarmaceutica formaFarmaceutica) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(ViaAdministracion viaAdministracion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(UnidadMedida unidadMedida) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(ClasificacionControlada clasificacionControlada) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SavePrincipioActivoOutcome save(PrincipioActivo principioActivo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CondicionVenta> findCondicionVentaByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<FormaFarmaceutica> findFormaFarmaceuticaByCodigo(String codigo) {
            return formaFarmaceutica;
        }

        @Override
        public Optional<ViaAdministracion> findViaAdministracionByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<UnidadMedida> findUnidadMedidaByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ClasificacionControlada> findClasificacionControladaByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<PrincipioActivo> findPrincipioActivoById(UUID principioActivoId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean condicionVentaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean formaFarmaceuticaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean viaAdministracionExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean unidadMedidaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean clasificacionControladaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean principioActivoExists(UUID principioActivoId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeCondicionVentaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeFormaFarmaceuticaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeViaAdministracionStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeUnidadMedidaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeClasificacionControladaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changePrincipioActivoStatus(UUID principioActivoId, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }
    }
}
```

- [x] **Step 2: Ejecutar el test y verificar que falla por compilación**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ConsultarFormaFarmaceuticaHandlerTest"`
Expected: FAIL.

- [x] **Step 3: Agregar `findFormaFarmaceuticaByCodigo` a `CatalogoSoportePort` (si no existe ya)**

Verificar/agregar en la interfaz:

```java
    Optional<FormaFarmaceutica> findFormaFarmaceuticaByCodigo(String codigo);
```

- [x] **Step 4: Implementar `findFormaFarmaceuticaByCodigo` en `CatalogoSoporteJpaWriteAdapter`**

Agregar el método:

```java
    @Override
    @Transactional(readOnly = true)
    public Optional<FormaFarmaceutica> findFormaFarmaceuticaByCodigo(String codigo) {
        return formaFarmaceuticaRepository.findById(codigo)
                .map(entity -> FormaFarmaceutica.restore(
                        entity.getCodigo(), entity.getDenominacion(), entity.getFuente(),
                        EstadoCatalogoSoporte.valueOf(entity.getEstado())));
    }

```

- [x] **Step 5: Crear `ConsultarFormaFarmaceuticaQuery`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarFormaFarmaceuticaQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.shared.application.cqrs.Query;

public record ConsultarFormaFarmaceuticaQuery(String codigo) implements Query<FormaFarmaceuticaResult> {
}
```

- [x] **Step 6: Crear `ConsultarFormaFarmaceuticaUseCase`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarFormaFarmaceuticaUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarFormaFarmaceuticaQuery;
import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ConsultarFormaFarmaceuticaUseCase {
    Result<FormaFarmaceuticaResult, ApplicationError> execute(ConsultarFormaFarmaceuticaQuery query);
}
```

- [x] **Step 7: Crear `ConsultarFormaFarmaceuticaHandler`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarFormaFarmaceuticaHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarFormaFarmaceuticaQuery;
import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ConsultarFormaFarmaceuticaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarFormaFarmaceuticaHandler implements ConsultarFormaFarmaceuticaUseCase {

    private final CatalogoSoportePort soportePort;

    public ConsultarFormaFarmaceuticaHandler(CatalogoSoportePort soportePort) {
        this.soportePort = Objects.requireNonNull(soportePort, "soportePort es obligatorio");
    }

    @Override
    public Result<FormaFarmaceuticaResult, ApplicationError> execute(ConsultarFormaFarmaceuticaQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return soportePort.findFormaFarmaceuticaByCodigo(query.codigo())
                .map(CatalogoApplicationMapper::toResult)
                .map(Result::<FormaFarmaceuticaResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CAT_FORMA_FARMACEUTICA_NO_ENCONTRADA", "La forma farmacéutica indicada no existe.",
                        ErrorCategory.NOT_FOUND)));
    }
}
```

- [x] **Step 8: Ejecutar el test unitario y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ConsultarFormaFarmaceuticaHandlerTest"`
Expected: PASS (2 tests).

- [x] **Step 9: Agregar el endpoint GET/{codigo} al controller**

`CatalogoApiMapper.toResponse(FormaFarmaceuticaResult)` ya existe — no requiere cambios en el mapper.

En `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/FormaFarmaceuticaController.java`, archivo completo:

```java
package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoGlobalRequest;
import com.softprimesolutions.catalogo.api.dto.request.FormaFarmaceuticaRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.query.ConsultarFormaFarmaceuticaQuery;
import com.softprimesolutions.catalogo.application.dto.query.ListarFormasFarmaceuticasQuery;
import com.softprimesolutions.catalogo.application.port.in.ActualizarFormaFarmaceuticaUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.ConsultarFormaFarmaceuticaUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearFormaFarmaceuticaUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarFormasFarmaceuticasUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/catalogo/formas-farmaceuticas")
public class FormaFarmaceuticaController {

    private final CrearFormaFarmaceuticaUseCase crearFormaFarmaceutica;
    private final ActualizarFormaFarmaceuticaUseCase actualizarFormaFarmaceutica;
    private final ConsultarFormaFarmaceuticaUseCase consultarFormaFarmaceutica;
    private final ListarFormasFarmaceuticasUseCase listarFormasFarmaceuticas;
    private final CatalogoControlUseCase control;

    public FormaFarmaceuticaController(
            CrearFormaFarmaceuticaUseCase crearFormaFarmaceutica,
            ActualizarFormaFarmaceuticaUseCase actualizarFormaFarmaceutica,
            ConsultarFormaFarmaceuticaUseCase consultarFormaFarmaceutica,
            ListarFormasFarmaceuticasUseCase listarFormasFarmaceuticas,
            CatalogoControlUseCase control) {
        this.crearFormaFarmaceutica = crearFormaFarmaceutica;
        this.actualizarFormaFarmaceutica = actualizarFormaFarmaceutica;
        this.consultarFormaFarmaceutica = consultarFormaFarmaceutica;
        this.listarFormasFarmaceuticas = listarFormasFarmaceuticas;
        this.control = control;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody FormaFarmaceuticaRequest request) {
        return crearFormaFarmaceutica.execute(CatalogoApiMapper.toCreateCommand(request)).fold(
                result -> ResponseEntity.status(201).body(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable String codigo, @Valid @RequestBody FormaFarmaceuticaRequest request) {
        return actualizarFormaFarmaceutica.execute(CatalogoApiMapper.toUpdateCommand(request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{codigo}/estado")
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable String codigo, @Valid @RequestBody CambiarEstadoGlobalRequest request) {
        return control.changeFormaFarmaceuticaStatus(codigo, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), CatalogoControllerSupport::problem);
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("hasAuthority('catalogo.soporte.consultar')")
    public ResponseEntity<?> get(@PathVariable String codigo) {
        return consultarFormaFarmaceutica.execute(new ConsultarFormaFarmaceuticaQuery(codigo)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.soporte.consultar')")
    public ResponseEntity<?> list(@RequestParam(required = false) String estado) {
        return listarFormasFarmaceuticas.execute(new ListarFormasFarmaceuticasQuery(estado)).fold(
                result -> ResponseEntity.ok(result.stream().map(CatalogoApiMapper::toResponse).toList()),
                CatalogoControllerSupport::problem);
    }
}
```

- [x] **Step 10: Cablear el nuevo bean en `CatalogoModuleConfiguration`**

Agregar los imports:

```java
import com.softprimesolutions.catalogo.application.port.in.ConsultarFormaFarmaceuticaUseCase;
```

```java
import com.softprimesolutions.catalogo.application.usecase.query.ConsultarFormaFarmaceuticaHandler;
```

Y agregar el bean en la sección `// ---- Soporte: formas farmacéuticas ----`:

```java
    @Bean
    ConsultarFormaFarmaceuticaUseCase consultarFormaFarmaceuticaUseCase(CatalogoSoportePort soportePort) {
        return new ConsultarFormaFarmaceuticaHandler(soportePort);
    }
```

La sección completa queda:

```java
    // ---- Soporte: formas farmacéuticas ----

    @Bean
    CrearFormaFarmaceuticaUseCase crearFormaFarmaceuticaUseCase(CatalogoSoportePort soportePort) {
        return new CrearFormaFarmaceuticaHandler(soportePort);
    }

    @Bean
    ActualizarFormaFarmaceuticaUseCase actualizarFormaFarmaceuticaUseCase(CatalogoSoportePort soportePort) {
        return new ActualizarFormaFarmaceuticaHandler(soportePort);
    }

    @Bean
    ConsultarFormaFarmaceuticaUseCase consultarFormaFarmaceuticaUseCase(CatalogoSoportePort soportePort) {
        return new ConsultarFormaFarmaceuticaHandler(soportePort);
    }

    @Bean
    ListarFormasFarmaceuticasUseCase listarFormasFarmaceuticasUseCase(CatalogoReadPort readPort) {
        return new ListarFormasFarmaceuticasHandler(readPort);
    }
```

- [x] **Step 11: Compilar el módulo completo**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL.

- [x] **Step 12: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoSoportePort.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoSoporteJpaWriteAdapter.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarFormaFarmaceuticaQuery.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarFormaFarmaceuticaUseCase.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarFormaFarmaceuticaHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/FormaFarmaceuticaController.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarFormaFarmaceuticaHandlerTest.java
git commit -m "feat(catalogo): agregar GET /formas-farmaceuticas/{codigo}"
```

---
## Tarea 6: GET /api/v1/catalogo/vias-administracion/{codigo}

**Files:**
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoSoportePort.java` (agrega `findViaAdministracionByCodigo` si una tarea anterior no lo hizo ya)
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoSoporteJpaWriteAdapter.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarViaAdministracionQuery.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarViaAdministracionUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarViaAdministracionHandler.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/ViaAdministracionController.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarViaAdministracionHandlerTest.java`

**Interfaces:**
- Consumes: `ViaAdministracion.restore(String codigo, String denominacion, String fuente, EstadoCatalogoSoporte estado)` (ya existe); `CatalogoApplicationMapper.toResult(ViaAdministracion)` → `ViaAdministracionResult` (ya existe); `ViaAdministracionJpaRepository.findById(String)` → `Optional<ViaAdministracionJpaEntity>` (ya existe).
- Produces: `ConsultarViaAdministracionUseCase.execute(ConsultarViaAdministracionQuery)` → `Result<ViaAdministracionResult, ApplicationError>`; `CatalogoSoportePort.findViaAdministracionByCodigo(String codigo)` → `Optional<ViaAdministracion>`.

- [x] **Step 1: Escribir el test unitario del handler (falla primero)**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarViaAdministracionHandlerTest.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarViaAdministracionQuery;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.EstadoCatalogoSoporte;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarViaAdministracionHandlerTest {

    @Test
    void returnsViaAdministracionResultWhenFound() {
        var viaAdministracion = ViaAdministracion.restore(
                "ORAL", "Via oral", "DIGEMID", EstadoCatalogoSoporte.ACTIVO);
        CatalogoSoportePort port = new StubCatalogoSoportePort(Optional.of(viaAdministracion));
        var handler = new ConsultarViaAdministracionHandler(port);

        var result = handler.execute(new ConsultarViaAdministracionQuery("ORAL"));

        assertThat(result.isSuccess()).isTrue();
        result.fold(
                success -> {
                    assertThat(success.codigo()).isEqualTo("ORAL");
                    assertThat(success.denominacion()).isEqualTo("Via oral");
                    return null;
                },
                failure -> null);
    }

    @Test
    void returnsNotFoundWhenMissing() {
        CatalogoSoportePort port = new StubCatalogoSoportePort(Optional.empty());
        var handler = new ConsultarViaAdministracionHandler(port);

        var result = handler.execute(new ConsultarViaAdministracionQuery("NO_EXISTE"));

        assertThat(result.isFailure()).isTrue();
        result.fold(
                success -> null,
                failure -> {
                    assertThat(failure.code()).isEqualTo("CAT_VIA_ADMINISTRACION_NO_ENCONTRADA");
                    assertThat(failure.category()).isEqualTo(ErrorCategory.NOT_FOUND);
                    return null;
                });
    }

    private record StubCatalogoSoportePort(Optional<ViaAdministracion> viaAdministracion)
            implements CatalogoSoportePort {

        @Override
        public SaveOutcome save(CondicionVenta condicionVenta) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(FormaFarmaceutica formaFarmaceutica) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(ViaAdministracion viaAdministracion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(UnidadMedida unidadMedida) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(ClasificacionControlada clasificacionControlada) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SavePrincipioActivoOutcome save(PrincipioActivo principioActivo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CondicionVenta> findCondicionVentaByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<FormaFarmaceutica> findFormaFarmaceuticaByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ViaAdministracion> findViaAdministracionByCodigo(String codigo) {
            return viaAdministracion;
        }

        @Override
        public Optional<UnidadMedida> findUnidadMedidaByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ClasificacionControlada> findClasificacionControladaByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<PrincipioActivo> findPrincipioActivoById(UUID principioActivoId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean condicionVentaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean formaFarmaceuticaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean viaAdministracionExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean unidadMedidaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean clasificacionControladaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean principioActivoExists(UUID principioActivoId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeCondicionVentaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeFormaFarmaceuticaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeViaAdministracionStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeUnidadMedidaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeClasificacionControladaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changePrincipioActivoStatus(UUID principioActivoId, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }
    }
}
```

- [x] **Step 2: Ejecutar el test y verificar que falla por compilación**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ConsultarViaAdministracionHandlerTest"`
Expected: FAIL — error de compilación.

- [x] **Step 3: Agregar `findViaAdministracionByCodigo` a `CatalogoSoportePort` (si una tarea anterior no lo agregó ya)**

Verificar que `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoSoportePort.java` contenga:

```java
    Optional<ViaAdministracion> findViaAdministracionByCodigo(String codigo);
```

- [x] **Step 4: Implementar `findViaAdministracionByCodigo` en `CatalogoSoporteJpaWriteAdapter`**

Agregar el método (cerca de `save(ViaAdministracion)`, o junto a otros `find*ByCodigo` si ya existen):

```java
    @Override
    @Transactional(readOnly = true)
    public Optional<ViaAdministracion> findViaAdministracionByCodigo(String codigo) {
        return viaAdministracionRepository.findById(codigo)
                .map(entity -> ViaAdministracion.restore(
                        entity.getCodigo(), entity.getDenominacion(), entity.getFuente(),
                        EstadoCatalogoSoporte.valueOf(entity.getEstado())));
    }
```

- [x] **Step 5: Crear la query, el puerto de entrada y el handler**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarViaAdministracionQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.shared.application.cqrs.Query;

public record ConsultarViaAdministracionQuery(String codigo) implements Query<ViaAdministracionResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarViaAdministracionUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarViaAdministracionQuery;
import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ConsultarViaAdministracionUseCase {
    Result<ViaAdministracionResult, ApplicationError> execute(ConsultarViaAdministracionQuery query);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarViaAdministracionHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarViaAdministracionQuery;
import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ConsultarViaAdministracionUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarViaAdministracionHandler implements ConsultarViaAdministracionUseCase {

    private final CatalogoSoportePort soportePort;

    public ConsultarViaAdministracionHandler(CatalogoSoportePort soportePort) {
        this.soportePort = Objects.requireNonNull(soportePort, "soportePort es obligatorio");
    }

    @Override
    public Result<ViaAdministracionResult, ApplicationError> execute(ConsultarViaAdministracionQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return soportePort.findViaAdministracionByCodigo(query.codigo())
                .map(CatalogoApplicationMapper::toResult)
                .map(Result::<ViaAdministracionResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CAT_VIA_ADMINISTRACION_NO_ENCONTRADA", "La vía de administración indicada no existe.",
                        ErrorCategory.NOT_FOUND)));
    }
}
```

- [x] **Step 6: Ejecutar el test unitario y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ConsultarViaAdministracionHandlerTest"`
Expected: BUILD SUCCESSFUL, 2 tests passed.

- [x] **Step 7: Agregar el endpoint GET al controller**

Editar `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/ViaAdministracionController.java` para que quede:

```java
package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoGlobalRequest;
import com.softprimesolutions.catalogo.api.dto.request.ViaAdministracionRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.query.ConsultarViaAdministracionQuery;
import com.softprimesolutions.catalogo.application.dto.query.ListarViasAdministracionQuery;
import com.softprimesolutions.catalogo.application.port.in.ActualizarViaAdministracionUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.ConsultarViaAdministracionUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearViaAdministracionUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarViasAdministracionUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/catalogo/vias-administracion")
public class ViaAdministracionController {

    private final CrearViaAdministracionUseCase crearViaAdministracion;
    private final ActualizarViaAdministracionUseCase actualizarViaAdministracion;
    private final ConsultarViaAdministracionUseCase consultarViaAdministracion;
    private final ListarViasAdministracionUseCase listarViasAdministracion;
    private final CatalogoControlUseCase control;

    public ViaAdministracionController(
            CrearViaAdministracionUseCase crearViaAdministracion,
            ActualizarViaAdministracionUseCase actualizarViaAdministracion,
            ConsultarViaAdministracionUseCase consultarViaAdministracion,
            ListarViasAdministracionUseCase listarViasAdministracion,
            CatalogoControlUseCase control) {
        this.crearViaAdministracion = crearViaAdministracion;
        this.actualizarViaAdministracion = actualizarViaAdministracion;
        this.consultarViaAdministracion = consultarViaAdministracion;
        this.listarViasAdministracion = listarViasAdministracion;
        this.control = control;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody ViaAdministracionRequest request) {
        return crearViaAdministracion.execute(CatalogoApiMapper.toCreateCommand(request)).fold(
                result -> ResponseEntity.status(201).body(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable String codigo, @Valid @RequestBody ViaAdministracionRequest request) {
        return actualizarViaAdministracion.execute(CatalogoApiMapper.toUpdateCommand(request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{codigo}/estado")
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable String codigo, @Valid @RequestBody CambiarEstadoGlobalRequest request) {
        return control.changeViaAdministracionStatus(codigo, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), CatalogoControllerSupport::problem);
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("hasAuthority('catalogo.soporte.consultar')")
    public ResponseEntity<?> get(@PathVariable String codigo) {
        return consultarViaAdministracion.execute(new ConsultarViaAdministracionQuery(codigo)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.soporte.consultar')")
    public ResponseEntity<?> list(@RequestParam(required = false) String estado) {
        return listarViasAdministracion.execute(new ListarViasAdministracionQuery(estado)).fold(
                result -> ResponseEntity.ok(result.stream().map(CatalogoApiMapper::toResponse).toList()),
                CatalogoControllerSupport::problem);
    }
}
```

- [x] **Step 8: Cablear el nuevo bean en `CatalogoModuleConfiguration`**

Agregar los imports:

```java
import com.softprimesolutions.catalogo.application.port.in.ConsultarViaAdministracionUseCase;
```

```java
import com.softprimesolutions.catalogo.application.usecase.query.ConsultarViaAdministracionHandler;
```

Agregar el bean en la sección `// ---- Soporte: vías de administración ----`, junto a `listarViasAdministracionUseCase`:

```java
    @Bean
    ConsultarViaAdministracionUseCase consultarViaAdministracionUseCase(CatalogoSoportePort soportePort) {
        return new ConsultarViaAdministracionHandler(soportePort);
    }
```

- [x] **Step 9: Compilar el módulo completo**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL.

- [x] **Step 10: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoSoportePort.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoSoporteJpaWriteAdapter.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarViaAdministracionQuery.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarViaAdministracionUseCase.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarViaAdministracionHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/ViaAdministracionController.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarViaAdministracionHandlerTest.java
git commit -m "feat(catalogo): agregar GET /vias-administracion/{codigo}"
```

---

## Tarea 7: GET /api/v1/catalogo/unidades-medida/{codigo}

**Files:**
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoSoportePort.java` (agrega `findUnidadMedidaByCodigo` si una tarea anterior no lo hizo ya)
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoSoporteJpaWriteAdapter.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarUnidadMedidaQuery.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarUnidadMedidaUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarUnidadMedidaHandler.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/UnidadMedidaController.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarUnidadMedidaHandlerTest.java`

**Interfaces:**
- Consumes: `UnidadMedida.restore(String codigo, String denominacion, String simbolo, boolean permiteDecimal, String fuente, EstadoCatalogoSoporte estado)` (ya existe); `CatalogoApplicationMapper.toResult(UnidadMedida)` → `UnidadMedidaResult` (ya existe); `UnidadMedidaJpaRepository.findById(String)` → `Optional<UnidadMedidaJpaEntity>` (ya existe).
- Produces: `ConsultarUnidadMedidaUseCase.execute(ConsultarUnidadMedidaQuery)` → `Result<UnidadMedidaResult, ApplicationError>`; `CatalogoSoportePort.findUnidadMedidaByCodigo(String codigo)` → `Optional<UnidadMedida>`.

- [x] **Step 1: Escribir el test unitario del handler (falla primero)**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarUnidadMedidaHandlerTest.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarUnidadMedidaQuery;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.EstadoCatalogoSoporte;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarUnidadMedidaHandlerTest {

    @Test
    void returnsUnidadMedidaResultWhenFound() {
        var unidadMedida = UnidadMedida.restore(
                "MG", "Miligramo", "mg", true, "DIGEMID", EstadoCatalogoSoporte.ACTIVO);
        CatalogoSoportePort port = new StubCatalogoSoportePort(Optional.of(unidadMedida));
        var handler = new ConsultarUnidadMedidaHandler(port);

        var result = handler.execute(new ConsultarUnidadMedidaQuery("MG"));

        assertThat(result.isSuccess()).isTrue();
        result.fold(
                success -> {
                    assertThat(success.codigo()).isEqualTo("MG");
                    assertThat(success.simbolo()).isEqualTo("mg");
                    return null;
                },
                failure -> null);
    }

    @Test
    void returnsNotFoundWhenMissing() {
        CatalogoSoportePort port = new StubCatalogoSoportePort(Optional.empty());
        var handler = new ConsultarUnidadMedidaHandler(port);

        var result = handler.execute(new ConsultarUnidadMedidaQuery("NO_EXISTE"));

        assertThat(result.isFailure()).isTrue();
        result.fold(
                success -> null,
                failure -> {
                    assertThat(failure.code()).isEqualTo("CAT_UNIDAD_MEDIDA_NO_ENCONTRADA");
                    assertThat(failure.category()).isEqualTo(ErrorCategory.NOT_FOUND);
                    return null;
                });
    }

    private record StubCatalogoSoportePort(Optional<UnidadMedida> unidadMedida) implements CatalogoSoportePort {

        @Override
        public SaveOutcome save(CondicionVenta condicionVenta) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(FormaFarmaceutica formaFarmaceutica) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(ViaAdministracion viaAdministracion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(UnidadMedida unidadMedida) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(ClasificacionControlada clasificacionControlada) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SavePrincipioActivoOutcome save(PrincipioActivo principioActivo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CondicionVenta> findCondicionVentaByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<FormaFarmaceutica> findFormaFarmaceuticaByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ViaAdministracion> findViaAdministracionByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<UnidadMedida> findUnidadMedidaByCodigo(String codigo) {
            return unidadMedida;
        }

        @Override
        public Optional<ClasificacionControlada> findClasificacionControladaByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<PrincipioActivo> findPrincipioActivoById(UUID principioActivoId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean condicionVentaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean formaFarmaceuticaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean viaAdministracionExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean unidadMedidaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean clasificacionControladaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean principioActivoExists(UUID principioActivoId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeCondicionVentaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeFormaFarmaceuticaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeViaAdministracionStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeUnidadMedidaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeClasificacionControladaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changePrincipioActivoStatus(UUID principioActivoId, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }
    }
}
```

- [x] **Step 2: Ejecutar el test y verificar que falla por compilación**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ConsultarUnidadMedidaHandlerTest"`
Expected: FAIL — error de compilación.

- [x] **Step 3: Agregar `findUnidadMedidaByCodigo` a `CatalogoSoportePort` (si una tarea anterior no lo agregó ya)**

Verificar que `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoSoportePort.java` contenga:

```java
    Optional<UnidadMedida> findUnidadMedidaByCodigo(String codigo);
```

- [x] **Step 4: Implementar `findUnidadMedidaByCodigo` en `CatalogoSoporteJpaWriteAdapter`**

Agregar el método (cerca de `save(UnidadMedida)`, o junto a otros `find*ByCodigo` si ya existen):

```java
    @Override
    @Transactional(readOnly = true)
    public Optional<UnidadMedida> findUnidadMedidaByCodigo(String codigo) {
        return unidadMedidaRepository.findById(codigo)
                .map(entity -> UnidadMedida.restore(
                        entity.getCodigo(), entity.getDenominacion(), entity.getSimbolo(),
                        entity.isPermiteDecimal(), entity.getFuente(),
                        EstadoCatalogoSoporte.valueOf(entity.getEstado())));
    }
```

- [x] **Step 5: Crear la query, el puerto de entrada y el handler**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarUnidadMedidaQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.shared.application.cqrs.Query;

public record ConsultarUnidadMedidaQuery(String codigo) implements Query<UnidadMedidaResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarUnidadMedidaUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarUnidadMedidaQuery;
import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ConsultarUnidadMedidaUseCase {
    Result<UnidadMedidaResult, ApplicationError> execute(ConsultarUnidadMedidaQuery query);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarUnidadMedidaHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarUnidadMedidaQuery;
import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ConsultarUnidadMedidaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarUnidadMedidaHandler implements ConsultarUnidadMedidaUseCase {

    private final CatalogoSoportePort soportePort;

    public ConsultarUnidadMedidaHandler(CatalogoSoportePort soportePort) {
        this.soportePort = Objects.requireNonNull(soportePort, "soportePort es obligatorio");
    }

    @Override
    public Result<UnidadMedidaResult, ApplicationError> execute(ConsultarUnidadMedidaQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return soportePort.findUnidadMedidaByCodigo(query.codigo())
                .map(CatalogoApplicationMapper::toResult)
                .map(Result::<UnidadMedidaResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CAT_UNIDAD_MEDIDA_NO_ENCONTRADA", "La unidad de medida indicada no existe.",
                        ErrorCategory.NOT_FOUND)));
    }
}
```

- [x] **Step 6: Ejecutar el test unitario y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ConsultarUnidadMedidaHandlerTest"`
Expected: BUILD SUCCESSFUL, 2 tests passed.

- [x] **Step 7: Agregar el endpoint GET al controller**

Editar `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/UnidadMedidaController.java` para que quede:

```java
package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoGlobalRequest;
import com.softprimesolutions.catalogo.api.dto.request.UnidadMedidaRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.query.ConsultarUnidadMedidaQuery;
import com.softprimesolutions.catalogo.application.dto.query.ListarUnidadesMedidaQuery;
import com.softprimesolutions.catalogo.application.port.in.ActualizarUnidadMedidaUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.ConsultarUnidadMedidaUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearUnidadMedidaUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarUnidadesMedidaUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/catalogo/unidades-medida")
public class UnidadMedidaController {

    private final CrearUnidadMedidaUseCase crearUnidadMedida;
    private final ActualizarUnidadMedidaUseCase actualizarUnidadMedida;
    private final ConsultarUnidadMedidaUseCase consultarUnidadMedida;
    private final ListarUnidadesMedidaUseCase listarUnidadesMedida;
    private final CatalogoControlUseCase control;

    public UnidadMedidaController(
            CrearUnidadMedidaUseCase crearUnidadMedida,
            ActualizarUnidadMedidaUseCase actualizarUnidadMedida,
            ConsultarUnidadMedidaUseCase consultarUnidadMedida,
            ListarUnidadesMedidaUseCase listarUnidadesMedida,
            CatalogoControlUseCase control) {
        this.crearUnidadMedida = crearUnidadMedida;
        this.actualizarUnidadMedida = actualizarUnidadMedida;
        this.consultarUnidadMedida = consultarUnidadMedida;
        this.listarUnidadesMedida = listarUnidadesMedida;
        this.control = control;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody UnidadMedidaRequest request) {
        return crearUnidadMedida.execute(CatalogoApiMapper.toCreateCommand(request)).fold(
                result -> ResponseEntity.status(201).body(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable String codigo, @Valid @RequestBody UnidadMedidaRequest request) {
        return actualizarUnidadMedida.execute(CatalogoApiMapper.toUpdateCommand(request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{codigo}/estado")
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable String codigo, @Valid @RequestBody CambiarEstadoGlobalRequest request) {
        return control.changeUnidadMedidaStatus(codigo, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), CatalogoControllerSupport::problem);
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("hasAuthority('catalogo.soporte.consultar')")
    public ResponseEntity<?> get(@PathVariable String codigo) {
        return consultarUnidadMedida.execute(new ConsultarUnidadMedidaQuery(codigo)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.soporte.consultar')")
    public ResponseEntity<?> list(@RequestParam(required = false) String estado) {
        return listarUnidadesMedida.execute(new ListarUnidadesMedidaQuery(estado)).fold(
                result -> ResponseEntity.ok(result.stream().map(CatalogoApiMapper::toResponse).toList()),
                CatalogoControllerSupport::problem);
    }
}
```

- [x] **Step 8: Cablear el nuevo bean en `CatalogoModuleConfiguration`**

Agregar los imports:

```java
import com.softprimesolutions.catalogo.application.port.in.ConsultarUnidadMedidaUseCase;
```

```java
import com.softprimesolutions.catalogo.application.usecase.query.ConsultarUnidadMedidaHandler;
```

Agregar el bean en la sección `// ---- Soporte: unidades de medida ----`, junto a `listarUnidadesMedidaUseCase`:

```java
    @Bean
    ConsultarUnidadMedidaUseCase consultarUnidadMedidaUseCase(CatalogoSoportePort soportePort) {
        return new ConsultarUnidadMedidaHandler(soportePort);
    }
```

- [x] **Step 9: Compilar el módulo completo**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL.

- [x] **Step 10: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoSoportePort.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoSoporteJpaWriteAdapter.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarUnidadMedidaQuery.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarUnidadMedidaUseCase.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarUnidadMedidaHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/UnidadMedidaController.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarUnidadMedidaHandlerTest.java
git commit -m "feat(catalogo): agregar GET /unidades-medida/{codigo}"
```

---

## Tarea 8: GET /api/v1/catalogo/clasificaciones-controladas/{codigo}

**Files:**
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoSoportePort.java` (agrega `findClasificacionControladaByCodigo` si una tarea anterior no lo hizo ya)
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoSoporteJpaWriteAdapter.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarClasificacionControladaQuery.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarClasificacionControladaUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarClasificacionControladaHandler.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/ClasificacionControladaController.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarClasificacionControladaHandlerTest.java`

**Interfaces:**
- Consumes: `ClasificacionControlada.restore(String codigo, String denominacion, String normaFuente, boolean requiereRecetaEspecial, boolean retieneReceta, Integer vigenciaRecetaDias, EstadoCatalogoSoporte estado)` (ya existe); `CatalogoApplicationMapper.toResult(ClasificacionControlada)` → `ClasificacionControladaResult` (ya existe); `ClasificacionControladaJpaRepository.findById(String)` → `Optional<ClasificacionControladaJpaEntity>` (ya existe).
- Produces: `ConsultarClasificacionControladaUseCase.execute(ConsultarClasificacionControladaQuery)` → `Result<ClasificacionControladaResult, ApplicationError>`; `CatalogoSoportePort.findClasificacionControladaByCodigo(String codigo)` → `Optional<ClasificacionControlada>`.

- [x] **Step 1: Escribir el test unitario del handler (falla primero)**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarClasificacionControladaHandlerTest.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarClasificacionControladaQuery;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.EstadoCatalogoSoporte;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarClasificacionControladaHandlerTest {

    @Test
    void returnsClasificacionControladaResultWhenFound() {
        var clasificacionControlada = ClasificacionControlada.restore(
                "LISTA_II", "Lista II", "D.S. 023-2001-SA", true, true, 30, EstadoCatalogoSoporte.ACTIVO);
        CatalogoSoportePort port = new StubCatalogoSoportePort(Optional.of(clasificacionControlada));
        var handler = new ConsultarClasificacionControladaHandler(port);

        var result = handler.execute(new ConsultarClasificacionControladaQuery("LISTA_II"));

        assertThat(result.isSuccess()).isTrue();
        result.fold(
                success -> {
                    assertThat(success.codigo()).isEqualTo("LISTA_II");
                    assertThat(success.vigenciaRecetaDias()).isEqualTo(30);
                    return null;
                },
                failure -> null);
    }

    @Test
    void returnsNotFoundWhenMissing() {
        CatalogoSoportePort port = new StubCatalogoSoportePort(Optional.empty());
        var handler = new ConsultarClasificacionControladaHandler(port);

        var result = handler.execute(new ConsultarClasificacionControladaQuery("NO_EXISTE"));

        assertThat(result.isFailure()).isTrue();
        result.fold(
                success -> null,
                failure -> {
                    assertThat(failure.code()).isEqualTo("CAT_CLASIFICACION_CONTROLADA_NO_ENCONTRADA");
                    assertThat(failure.category()).isEqualTo(ErrorCategory.NOT_FOUND);
                    return null;
                });
    }

    private record StubCatalogoSoportePort(Optional<ClasificacionControlada> clasificacionControlada)
            implements CatalogoSoportePort {

        @Override
        public SaveOutcome save(CondicionVenta condicionVenta) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(FormaFarmaceutica formaFarmaceutica) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(ViaAdministracion viaAdministracion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(UnidadMedida unidadMedida) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(ClasificacionControlada clasificacionControlada) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SavePrincipioActivoOutcome save(PrincipioActivo principioActivo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CondicionVenta> findCondicionVentaByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<FormaFarmaceutica> findFormaFarmaceuticaByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ViaAdministracion> findViaAdministracionByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<UnidadMedida> findUnidadMedidaByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ClasificacionControlada> findClasificacionControladaByCodigo(String codigo) {
            return clasificacionControlada;
        }

        @Override
        public Optional<PrincipioActivo> findPrincipioActivoById(UUID principioActivoId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean condicionVentaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean formaFarmaceuticaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean viaAdministracionExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean unidadMedidaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean clasificacionControladaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean principioActivoExists(UUID principioActivoId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeCondicionVentaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeFormaFarmaceuticaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeViaAdministracionStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeUnidadMedidaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeClasificacionControladaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changePrincipioActivoStatus(UUID principioActivoId, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }
    }
}
```

- [x] **Step 2: Ejecutar el test y verificar que falla por compilación**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ConsultarClasificacionControladaHandlerTest"`
Expected: FAIL — error de compilación.

- [x] **Step 3: Agregar `findClasificacionControladaByCodigo` a `CatalogoSoportePort` (si una tarea anterior no lo agregó ya)**

Verificar que `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoSoportePort.java` contenga:

```java
    Optional<ClasificacionControlada> findClasificacionControladaByCodigo(String codigo);
```

- [x] **Step 4: Implementar `findClasificacionControladaByCodigo` en `CatalogoSoporteJpaWriteAdapter`**

Agregar el método (cerca de `save(ClasificacionControlada)`, o junto a otros `find*ByCodigo` si ya existen):

```java
    @Override
    @Transactional(readOnly = true)
    public Optional<ClasificacionControlada> findClasificacionControladaByCodigo(String codigo) {
        return clasificacionControladaRepository.findById(codigo)
                .map(entity -> ClasificacionControlada.restore(
                        entity.getCodigo(), entity.getDenominacion(), entity.getNormaFuente(),
                        entity.isRequiereRecetaEspecial(), entity.isRetieneReceta(),
                        entity.getVigenciaRecetaDias(), EstadoCatalogoSoporte.valueOf(entity.getEstado())));
    }
```

- [x] **Step 5: Crear la query, el puerto de entrada y el handler**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarClasificacionControladaQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.shared.application.cqrs.Query;

public record ConsultarClasificacionControladaQuery(String codigo) implements Query<ClasificacionControladaResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarClasificacionControladaUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarClasificacionControladaQuery;
import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ConsultarClasificacionControladaUseCase {
    Result<ClasificacionControladaResult, ApplicationError> execute(ConsultarClasificacionControladaQuery query);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarClasificacionControladaHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarClasificacionControladaQuery;
import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ConsultarClasificacionControladaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarClasificacionControladaHandler implements ConsultarClasificacionControladaUseCase {

    private final CatalogoSoportePort soportePort;

    public ConsultarClasificacionControladaHandler(CatalogoSoportePort soportePort) {
        this.soportePort = Objects.requireNonNull(soportePort, "soportePort es obligatorio");
    }

    @Override
    public Result<ClasificacionControladaResult, ApplicationError> execute(
            ConsultarClasificacionControladaQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return soportePort.findClasificacionControladaByCodigo(query.codigo())
                .map(CatalogoApplicationMapper::toResult)
                .map(Result::<ClasificacionControladaResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CAT_CLASIFICACION_CONTROLADA_NO_ENCONTRADA",
                        "La clasificación controlada indicada no existe.", ErrorCategory.NOT_FOUND)));
    }
}
```

- [x] **Step 6: Ejecutar el test unitario y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ConsultarClasificacionControladaHandlerTest"`
Expected: BUILD SUCCESSFUL, 2 tests passed.

- [x] **Step 7: Agregar el endpoint GET al controller**

Editar `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/ClasificacionControladaController.java` para que quede:

```java
package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoGlobalRequest;
import com.softprimesolutions.catalogo.api.dto.request.ClasificacionControladaRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.query.ConsultarClasificacionControladaQuery;
import com.softprimesolutions.catalogo.application.dto.query.ListarClasificacionesControladasQuery;
import com.softprimesolutions.catalogo.application.port.in.ActualizarClasificacionControladaUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.ConsultarClasificacionControladaUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearClasificacionControladaUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarClasificacionesControladasUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/catalogo/clasificaciones-controladas")
public class ClasificacionControladaController {

    private final CrearClasificacionControladaUseCase crearClasificacionControlada;
    private final ActualizarClasificacionControladaUseCase actualizarClasificacionControlada;
    private final ConsultarClasificacionControladaUseCase consultarClasificacionControlada;
    private final ListarClasificacionesControladasUseCase listarClasificacionesControladas;
    private final CatalogoControlUseCase control;

    public ClasificacionControladaController(
            CrearClasificacionControladaUseCase crearClasificacionControlada,
            ActualizarClasificacionControladaUseCase actualizarClasificacionControlada,
            ConsultarClasificacionControladaUseCase consultarClasificacionControlada,
            ListarClasificacionesControladasUseCase listarClasificacionesControladas,
            CatalogoControlUseCase control) {
        this.crearClasificacionControlada = crearClasificacionControlada;
        this.actualizarClasificacionControlada = actualizarClasificacionControlada;
        this.consultarClasificacionControlada = consultarClasificacionControlada;
        this.listarClasificacionesControladas = listarClasificacionesControladas;
        this.control = control;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody ClasificacionControladaRequest request) {
        return crearClasificacionControlada.execute(CatalogoApiMapper.toCreateCommand(request)).fold(
                result -> ResponseEntity.status(201).body(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable String codigo, @Valid @RequestBody ClasificacionControladaRequest request) {
        return actualizarClasificacionControlada.execute(CatalogoApiMapper.toUpdateCommand(request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{codigo}/estado")
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable String codigo, @Valid @RequestBody CambiarEstadoGlobalRequest request) {
        return control.changeClasificacionControladaStatus(codigo, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), CatalogoControllerSupport::problem);
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("hasAuthority('catalogo.soporte.consultar')")
    public ResponseEntity<?> get(@PathVariable String codigo) {
        return consultarClasificacionControlada.execute(new ConsultarClasificacionControladaQuery(codigo)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.soporte.consultar')")
    public ResponseEntity<?> list(@RequestParam(required = false) String estado) {
        return listarClasificacionesControladas.execute(new ListarClasificacionesControladasQuery(estado)).fold(
                result -> ResponseEntity.ok(result.stream().map(CatalogoApiMapper::toResponse).toList()),
                CatalogoControllerSupport::problem);
    }
}
```

- [x] **Step 8: Cablear el nuevo bean en `CatalogoModuleConfiguration`**

Agregar los imports:

```java
import com.softprimesolutions.catalogo.application.port.in.ConsultarClasificacionControladaUseCase;
```

```java
import com.softprimesolutions.catalogo.application.usecase.query.ConsultarClasificacionControladaHandler;
```

Agregar el bean en la sección `// ---- Soporte: clasificaciones controladas ----`, junto a `listarClasificacionesControladasUseCase`:

```java
    @Bean
    ConsultarClasificacionControladaUseCase consultarClasificacionControladaUseCase(
            CatalogoSoportePort soportePort) {
        return new ConsultarClasificacionControladaHandler(soportePort);
    }
```

- [x] **Step 9: Compilar el módulo completo**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL.

- [x] **Step 10: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoSoportePort.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoSoporteJpaWriteAdapter.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarClasificacionControladaQuery.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarClasificacionControladaUseCase.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarClasificacionControladaHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/ClasificacionControladaController.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarClasificacionControladaHandlerTest.java
git commit -m "feat(catalogo): agregar GET /clasificaciones-controladas/{codigo}"
```

---

Con esto se completa la Parte A (los 8 catálogos con GET-by-id faltante). `ProductoRegulado` y `SKUComercial` ya tenían GET-by-id antes de este plan; no requieren cambios.

---

## Tarea 9: Dominio de `RubroComercial`

Construye el agregado de dominio `RubroComercial`, siguiendo exactamente el patrón de `Marca` (`service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/Marca.java`) pero con dos campos adicionales (`esFarmaceutico`, `orden`) y sin columna `estado` propia — la tabla `sch_catalogo.rubro_comercial` (ya existe, ver `docs/cadena-farmacias-docs/database/migrations/V003__catalogo_farmaceutico.sql` líneas 212-235) solo tiene `es_activo CHAR(1)`, por lo que el dominio expone un enum `EstadoRubroComercial { ACTIVO, INACTIVO }` derivado de ese campo en la capa de persistencia (no hay columna `estado VARCHAR(20)` que mapear 1:1).

Columnas de `sch_catalogo.rubro_comercial`: `id BIGINT` (PK interna), `uuid_publico UUID`, `tenant_id BIGINT`, `codigo VARCHAR(50)`, `nombre VARCHAR(120)`, `descripcion VARCHAR(300)`, `es_farmaceutico BOOLEAN NOT NULL DEFAULT FALSE`, `orden INTEGER NOT NULL DEFAULT 0`, `es_activo CHAR(1) NOT NULL DEFAULT '1'`, `created_at`, `created_by`, `updated_at`, `updated_by`. Índice único `uk_rubro_comercial_codigo` sobre `(tenant_id, codigo) WHERE es_activo = '1'`.

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/valueobject/RubroComercialId.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoRubroComercial.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/RubroComercial.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/RubroComercialTest.java`

**Interfaces:**
- Consumes: `AggregateRoot` (`com.softprimesolutions.shared.kernel.domain.AggregateRoot`, ya existe), `Result`/`ErrorDetail` (`com.softprimesolutions.shared.kernel.result.Result`, `com.softprimesolutions.shared.kernel.error.ErrorDetail`, ya existen), `TenantId` (`com.softprimesolutions.catalogo.domain.valueobject.TenantId`, ya existe).
- Produces: `RubroComercialId(UUID value)` (record `EntityId`-like, mismo patrón que `MarcaId`); `EstadoRubroComercial { ACTIVO, INACTIVO }`; `RubroComercial.create(RubroComercialId id, TenantId tenantId, String codigo, String nombre, String descripcion, boolean esFarmaceutico, int orden) -> Result<RubroComercial, ErrorDetail>`; `RubroComercial.restore(RubroComercialId id, TenantId tenantId, String codigo, String nombre, String descripcion, boolean esFarmaceutico, int orden, EstadoRubroComercial estado) -> RubroComercial`; getters `id()`, `tenantId()`, `codigo()`, `nombre()`, `descripcion()`, `esFarmaceutico()`, `orden()`, `estado()`.

- [x] **Step 1: Escribir el test de dominio (falla primero)**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/RubroComercialTest.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.catalogo.domain.valueobject.RubroComercialId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RubroComercialTest {

    @Test
    void createsRubroComercialWithValidData() {
        var id = new RubroComercialId(UUID.randomUUID());
        var tenantId = new TenantId(UUID.randomUUID());

        var result = RubroComercial.create(id, tenantId, "FARMA", "Farmacéutico", "Línea farmacéutica", true, 1);

        assertThat(result.isSuccess()).isTrue();
        result.fold(
                rubro -> {
                    assertThat(rubro.codigo()).isEqualTo("FARMA");
                    assertThat(rubro.nombre()).isEqualTo("Farmacéutico");
                    assertThat(rubro.esFarmaceutico()).isTrue();
                    assertThat(rubro.orden()).isEqualTo(1);
                    assertThat(rubro.estado()).isEqualTo(EstadoRubroComercial.ACTIVO);
                    return null;
                },
                error -> {
                    throw new AssertionError("expected success but got " + error);
                });
    }

    @Test
    void rejectsCodigoTooShort() {
        var id = new RubroComercialId(UUID.randomUUID());
        var tenantId = new TenantId(UUID.randomUUID());

        var result = RubroComercial.create(id, tenantId, "F", "Farmacéutico", null, true, 1);

        assertThat(result.isFailure()).isTrue();
        result.fold(
                rubro -> {
                    throw new AssertionError("expected failure but got " + rubro);
                },
                error -> {
                    assertThat(error.code()).isEqualTo("CAT_RUBRO_COMERCIAL_INVALIDO");
                    assertThat(error.metadata()).containsEntry("field", "codigo");
                    return null;
                });
    }

    @Test
    void rejectsNegativeOrden() {
        var id = new RubroComercialId(UUID.randomUUID());
        var tenantId = new TenantId(UUID.randomUUID());

        var result = RubroComercial.create(id, tenantId, "FARMA", "Farmacéutico", null, true, -1);

        assertThat(result.isFailure()).isTrue();
        result.fold(
                rubro -> {
                    throw new AssertionError("expected failure but got " + rubro);
                },
                error -> {
                    assertThat(error.code()).isEqualTo("CAT_RUBRO_COMERCIAL_INVALIDO");
                    assertThat(error.metadata()).containsEntry("field", "orden");
                    return null;
                });
    }

    @Test
    void restoresRubroComercialWithGivenState() {
        var id = new RubroComercialId(UUID.randomUUID());
        var tenantId = new TenantId(UUID.randomUUID());

        var rubro = RubroComercial.restore(
                id, tenantId, "BEBE", "Bebé", "Línea infantil", false, 2, EstadoRubroComercial.INACTIVO);

        assertThat(rubro.codigo()).isEqualTo("BEBE");
        assertThat(rubro.esFarmaceutico()).isFalse();
        assertThat(rubro.estado()).isEqualTo(EstadoRubroComercial.INACTIVO);
    }
}
```

- [x] **Step 2: Ejecutar el test y verificar que falla por compilación**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.RubroComercialTest"`
Expected: FAIL — error de compilación (`RubroComercialId`, `RubroComercial`, `EstadoRubroComercial` no existen).

- [x] **Step 3: Crear el value object `RubroComercialId`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/valueobject/RubroComercialId.java`:

```java
package com.softprimesolutions.catalogo.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record RubroComercialId(UUID value) {

    public RubroComercialId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
```

- [x] **Step 4: Crear el enum `EstadoRubroComercial`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoRubroComercial.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

public enum EstadoRubroComercial {
    ACTIVO,
    INACTIVO
}
```

- [x] **Step 5: Crear el agregado `RubroComercial`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/RubroComercial.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

import com.softprimesolutions.catalogo.domain.valueobject.RubroComercialId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Map;

/** Rubro comercial (macro-línea): clasifica el surtido en Farma, Bebé, Cosméticos, Retail, etc. */
public final class RubroComercial extends AggregateRoot {

    private static final int CODIGO_MIN_LENGTH = 2;
    private static final int CODIGO_MAX_LENGTH = 50;
    private static final int NOMBRE_MIN_LENGTH = 2;
    private static final int NOMBRE_MAX_LENGTH = 120;
    private static final int DESCRIPCION_MAX_LENGTH = 300;

    private final RubroComercialId id;
    private final TenantId tenantId;
    private final String codigo;
    private final String nombre;
    private final String descripcion;
    private final boolean esFarmaceutico;
    private final int orden;
    private final EstadoRubroComercial estado;

    private RubroComercial(
            RubroComercialId id, TenantId tenantId, String codigo, String nombre, String descripcion,
            boolean esFarmaceutico, int orden, EstadoRubroComercial estado) {
        this.id = id;
        this.tenantId = tenantId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.esFarmaceutico = esFarmaceutico;
        this.orden = orden;
        this.estado = estado;
    }

    public static Result<RubroComercial, ErrorDetail> create(
            RubroComercialId id, TenantId tenantId, String codigo, String nombre, String descripcion,
            boolean esFarmaceutico, int orden) {
        if (id == null) return invalid("id", "La identidad del rubro comercial es obligatoria.");
        if (tenantId == null) return invalid("tenantId", "El tenant es obligatorio.");

        var normalizedCodigo = normalizeSpaces(codigo);
        if (normalizedCodigo == null || normalizedCodigo.length() < CODIGO_MIN_LENGTH
                || normalizedCodigo.length() > CODIGO_MAX_LENGTH) {
            return invalid("codigo", "El código debe tener entre 2 y 50 caracteres.");
        }

        var normalizedNombre = normalizeSpaces(nombre);
        if (normalizedNombre == null || normalizedNombre.length() < NOMBRE_MIN_LENGTH
                || normalizedNombre.length() > NOMBRE_MAX_LENGTH) {
            return invalid("nombre", "El nombre debe tener entre 2 y 120 caracteres.");
        }

        var normalizedDescripcion = normalizeNullable(descripcion);
        if (!withinLength(normalizedDescripcion, DESCRIPCION_MAX_LENGTH)) {
            return invalid("descripcion", "La descripción no debe exceder 300 caracteres.");
        }

        if (orden < 0) return invalid("orden", "El orden debe ser mayor o igual a 0.");

        return Result.success(new RubroComercial(
                id, tenantId, normalizedCodigo, normalizedNombre, normalizedDescripcion, esFarmaceutico, orden,
                EstadoRubroComercial.ACTIVO));
    }

    public static RubroComercial restore(
            RubroComercialId id, TenantId tenantId, String codigo, String nombre, String descripcion,
            boolean esFarmaceutico, int orden, EstadoRubroComercial estado) {
        return new RubroComercial(id, tenantId, codigo, nombre, descripcion, esFarmaceutico, orden, estado);
    }

    private static Result<RubroComercial, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("CAT_RUBRO_COMERCIAL_INVALIDO", message, Map.of("field", field)));
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeSpaces(String value) {
        var normalized = normalize(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", " ");
    }

    private static String normalizeNullable(String value) {
        var normalized = normalizeSpaces(value);
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }

    private static boolean withinLength(String value, int maximum) {
        return value == null || value.length() <= maximum;
    }

    public RubroComercialId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public String codigo() { return codigo; }
    public String nombre() { return nombre; }
    public String descripcion() { return descripcion; }
    public boolean esFarmaceutico() { return esFarmaceutico; }
    public int orden() { return orden; }
    public EstadoRubroComercial estado() { return estado; }
}
```

- [x] **Step 6: Ejecutar el test de dominio y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.RubroComercialTest"`
Expected: BUILD SUCCESSFUL, 4 tests passed.

- [x] **Step 7: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/valueobject/RubroComercialId.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoRubroComercial.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/RubroComercial.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/RubroComercialTest.java
git commit -m "feat(catalogo): agregar agregado de dominio RubroComercial"
```

---

## Tarea 10: Capa de aplicación de `RubroComercial` (DTOs, puertos in, handlers CRUD + consulta + listado)

Construye la capa `application` completa siguiendo exactamente el patrón de `Marca`: `CrearMarcaCommand`/`ActualizarMarcaCommand`/`MarcaResult`/`ListarMarcasQuery`/`CrearMarcaUseCase`/`ActualizarMarcaUseCase`/`ListarMarcasUseCase`/`CrearMarcaHandler`/`ActualizarMarcaHandler`/`ListarMarcasHandler` (todos en `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/`). `ConsultarMarcaQuery`/`ConsultarMarcaUseCase`/`ConsultarMarcaHandler` (creados en la Tarea 2 de este mismo plan) también sirven de referencia directa para `ConsultarRubroComercialQuery`.

Esta tarea NO incluye el cambio de estado (`changeRubroComercialStatus`) — eso se agrega a `CatalogoControlUseCase`/`CatalogoControlService` en la Tarea 12 para mantener consistencia con cómo el resto de catálogos centralizan el cambio de estado en ese servicio transversal.

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/RubroComercialResult.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/CrearRubroComercialCommand.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/ActualizarRubroComercialCommand.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarRubroComercialQuery.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ListarRubrosComercialesQuery.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/CrearRubroComercialUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ActualizarRubroComercialUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarRubroComercialUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ListarRubrosComercialesUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/RubroComercialPort.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearRubroComercialHandler.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarRubroComercialHandler.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarRubroComercialHandler.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ListarRubrosComercialesHandler.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/mapper/CatalogoApplicationMapper.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CrearRubroComercialHandlerTest.java`

**Interfaces:**
- Consumes: `RubroComercial`, `RubroComercialId`, `EstadoRubroComercial` (Tarea 9); `TenantId` (existente).
- Produces: `RubroComercialResult(UUID id, UUID tenantId, String codigo, String nombre, String descripcion, boolean esFarmaceutico, int orden, String estado)`; `CrearRubroComercialCommand(UUID tenantId, String codigo, String nombre, String descripcion, boolean esFarmaceutico, int orden)`; `ActualizarRubroComercialCommand(UUID tenantId, UUID rubroComercialId, String codigo, String nombre, String descripcion, boolean esFarmaceutico, int orden)`; `ConsultarRubroComercialQuery(UUID tenantId, UUID rubroComercialId)`; `ListarRubrosComercialesQuery(UUID tenantId, String texto, Boolean esFarmaceutico, String estado, int page, int size)`; interfaz `RubroComercialPort` con `save(RubroComercial) -> SaveOutcome`, `findById(UUID tenantId, UUID rubroComercialId) -> Optional<RubroComercial>`, `changeStatus(UUID tenantId, UUID rubroComercialId, String status, Instant changedAt) -> boolean`, enum `SaveOutcome { CREATED, UPDATED, DUPLICATE_CODIGO, TENANT_NOT_FOUND, NOT_FOUND }`; `CatalogoApplicationMapper.toResult(RubroComercial) -> RubroComercialResult` (nuevo overload).

- [x] **Step 1: Escribir el test unitario del handler de creación (falla primero)**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CrearRubroComercialHandlerTest.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.catalogo.application.dto.command.CrearRubroComercialCommand;
import com.softprimesolutions.catalogo.application.port.out.RubroComercialPort;
import com.softprimesolutions.catalogo.domain.model.RubroComercial;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrearRubroComercialHandlerTest {

    @Test
    void createsRubroComercialWhenValid() {
        var tenantId = UUID.randomUUID();
        var port = new StubRubroComercialPort(RubroComercialPort.SaveOutcome.CREATED);
        var handler = new CrearRubroComercialHandler(port, UUID::randomUUID);

        var result = handler.execute(
                new CrearRubroComercialCommand(tenantId, "FARMA", "Farmacéutico", "Línea farmacéutica", true, 1));

        assertThat(result.isSuccess()).isTrue();
        result.fold(
                success -> {
                    assertThat(success.codigo()).isEqualTo("FARMA");
                    assertThat(success.esFarmaceutico()).isTrue();
                    assertThat(success.estado()).isEqualTo("ACTIVO");
                    return null;
                },
                error -> {
                    throw new AssertionError("expected success but got " + error);
                });
    }

    @Test
    void rejectsDuplicateCodigo() {
        var port = new StubRubroComercialPort(RubroComercialPort.SaveOutcome.DUPLICATE_CODIGO);
        var handler = new CrearRubroComercialHandler(port, UUID::randomUUID);

        var result = handler.execute(
                new CrearRubroComercialCommand(UUID.randomUUID(), "FARMA", "Farmacéutico", null, true, 1));

        assertThat(result.isFailure()).isTrue();
        result.fold(
                success -> {
                    throw new AssertionError("expected failure but got " + success);
                },
                error -> {
                    assertThat(error.code()).isEqualTo("CAT_RUBRO_COMERCIAL_DUPLICADO");
                    assertThat(error.category()).isEqualTo(ErrorCategory.CONFLICT);
                    return null;
                });
    }

    @Test
    void rejectsUnknownTenant() {
        var port = new StubRubroComercialPort(RubroComercialPort.SaveOutcome.TENANT_NOT_FOUND);
        var handler = new CrearRubroComercialHandler(port, UUID::randomUUID);

        var result = handler.execute(
                new CrearRubroComercialCommand(UUID.randomUUID(), "FARMA", "Farmacéutico", null, true, 1));

        assertThat(result.isFailure()).isTrue();
        result.fold(
                success -> {
                    throw new AssertionError("expected failure but got " + success);
                },
                error -> {
                    assertThat(error.code()).isEqualTo("CAT_TENANT_NO_ENCONTRADO");
                    assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
                    return null;
                });
    }

    private record StubRubroComercialPort(RubroComercialPort.SaveOutcome outcome) implements RubroComercialPort {

        @Override
        public SaveOutcome save(RubroComercial rubroComercial) {
            return outcome;
        }

        @Override
        public Optional<RubroComercial> findById(UUID tenantId, UUID rubroComercialId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeStatus(UUID tenantId, UUID rubroComercialId, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }
    }
}
```

- [x] **Step 2: Ejecutar el test y verificar que falla por compilación**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.command.CrearRubroComercialHandlerTest"`
Expected: FAIL — error de compilación.

- [x] **Step 3: Crear `RubroComercialResult`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/RubroComercialResult.java`:

```java
package com.softprimesolutions.catalogo.application.dto.result;

import java.util.UUID;

public record RubroComercialResult(
        UUID id, UUID tenantId, String codigo, String nombre, String descripcion, boolean esFarmaceutico,
        int orden, String estado) {
}
```

- [x] **Step 4: Crear los commands y queries**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/CrearRubroComercialCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.util.UUID;

public record CrearRubroComercialCommand(
        UUID tenantId, String codigo, String nombre, String descripcion, boolean esFarmaceutico, int orden)
        implements Command<RubroComercialResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/ActualizarRubroComercialCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.util.UUID;

public record ActualizarRubroComercialCommand(
        UUID tenantId, UUID rubroComercialId, String codigo, String nombre, String descripcion,
        boolean esFarmaceutico, int orden) implements Command<RubroComercialResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarRubroComercialQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.UUID;

public record ConsultarRubroComercialQuery(UUID tenantId, UUID rubroComercialId)
        implements Query<RubroComercialResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ListarRubrosComercialesQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.UUID;

public record ListarRubrosComercialesQuery(
        UUID tenantId, String texto, Boolean esFarmaceutico, String estado, int page, int size)
        implements Query<PaginaResult<RubroComercialResult>> {
}
```

- [x] **Step 5: Crear el puerto de salida `RubroComercialPort`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/RubroComercialPort.java`:

```java
package com.softprimesolutions.catalogo.application.port.out;

import com.softprimesolutions.catalogo.domain.model.RubroComercial;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RubroComercialPort {

    SaveOutcome save(RubroComercial rubroComercial);

    Optional<RubroComercial> findById(UUID tenantId, UUID rubroComercialId);

    boolean changeStatus(UUID tenantId, UUID rubroComercialId, String status, Instant changedAt);

    enum SaveOutcome { CREATED, UPDATED, DUPLICATE_CODIGO, TENANT_NOT_FOUND, NOT_FOUND }
}
```

- [x] **Step 6: Crear los puertos de entrada (use cases)**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/CrearRubroComercialUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.CrearRubroComercialCommand;
import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface CrearRubroComercialUseCase {
    Result<RubroComercialResult, ApplicationError> execute(CrearRubroComercialCommand command);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ActualizarRubroComercialUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarRubroComercialCommand;
import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ActualizarRubroComercialUseCase {
    Result<RubroComercialResult, ApplicationError> execute(ActualizarRubroComercialCommand command);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarRubroComercialUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarRubroComercialQuery;
import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ConsultarRubroComercialUseCase {
    Result<RubroComercialResult, ApplicationError> execute(ConsultarRubroComercialQuery query);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ListarRubrosComercialesUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ListarRubrosComercialesQuery;
import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ListarRubrosComercialesUseCase {
    Result<PaginaResult<RubroComercialResult>, ApplicationError> execute(ListarRubrosComercialesQuery query);
}
```

- [x] **Step 7: Agregar `toResult(RubroComercial)` a `CatalogoApplicationMapper`**

Editar `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/mapper/CatalogoApplicationMapper.java`. Agregar el import:

```java
import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
import com.softprimesolutions.catalogo.domain.model.RubroComercial;
```

(insertarlos en orden alfabético junto a los demás imports `dto.result.*` y `domain.model.*`)

Agregar el método, después de `toResult(CategoriaProducto)` y antes de `toResult(ProductoRegulado)`:

```java
    public static RubroComercialResult toResult(RubroComercial rubro) {
        return new RubroComercialResult(
                rubro.id().value(), rubro.tenantId().value(), rubro.codigo(), rubro.nombre(),
                rubro.descripcion(), rubro.esFarmaceutico(), rubro.orden(), rubro.estado().name());
    }
```

- [x] **Step 8: Crear los handlers de comando**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearRubroComercialHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.CrearRubroComercialCommand;
import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.CrearRubroComercialUseCase;
import com.softprimesolutions.catalogo.application.port.out.RubroComercialPort;
import com.softprimesolutions.catalogo.domain.model.RubroComercial;
import com.softprimesolutions.catalogo.domain.valueobject.RubroComercialId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearRubroComercialHandler implements CrearRubroComercialUseCase {

    private final RubroComercialPort writePort;
    private final IdentifierGenerator identifierGenerator;

    public CrearRubroComercialHandler(RubroComercialPort writePort, IdentifierGenerator identifierGenerator) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.identifierGenerator = Objects.requireNonNull(identifierGenerator, "identifierGenerator es obligatorio");
    }

    @Override
    public Result<RubroComercialResult, ApplicationError> execute(CrearRubroComercialCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var rubro = RubroComercial.create(
                new RubroComercialId(identifierGenerator.next()),
                command.tenantId() == null ? null : new TenantId(command.tenantId()),
                command.codigo(), command.nombre(), command.descripcion(), command.esFarmaceutico(),
                command.orden());
        return rubro.fold(this::persist, this::validationFailure);
    }

    private Result<RubroComercialResult, ApplicationError> persist(RubroComercial rubro) {
        var outcome = writePort.save(rubro);
        if (outcome == RubroComercialPort.SaveOutcome.TENANT_NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_TENANT_NO_ENCONTRADO", "El tenant indicado no existe.", ErrorCategory.NOT_FOUND));
        }
        if (outcome == RubroComercialPort.SaveOutcome.DUPLICATE_CODIGO) {
            return Result.failure(new StandardApplicationError(
                    "CAT_RUBRO_COMERCIAL_DUPLICADO", "Ya existe un rubro comercial con el código indicado.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(CatalogoApplicationMapper.toResult(rubro));
    }

    private Result<RubroComercialResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarRubroComercialHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarRubroComercialCommand;
import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ActualizarRubroComercialUseCase;
import com.softprimesolutions.catalogo.application.port.out.RubroComercialPort;
import com.softprimesolutions.catalogo.domain.model.RubroComercial;
import com.softprimesolutions.catalogo.domain.valueobject.RubroComercialId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarRubroComercialHandler implements ActualizarRubroComercialUseCase {

    private final RubroComercialPort writePort;

    public ActualizarRubroComercialHandler(RubroComercialPort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<RubroComercialResult, ApplicationError> execute(ActualizarRubroComercialCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var rubro = RubroComercial.create(
                new RubroComercialId(command.rubroComercialId()),
                command.tenantId() == null ? null : new TenantId(command.tenantId()),
                command.codigo(), command.nombre(), command.descripcion(), command.esFarmaceutico(),
                command.orden());
        return rubro.fold(this::persist, this::validationFailure);
    }

    private Result<RubroComercialResult, ApplicationError> persist(RubroComercial rubro) {
        var outcome = writePort.save(rubro);
        if (outcome == RubroComercialPort.SaveOutcome.NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_RUBRO_COMERCIAL_NO_ENCONTRADO", "El rubro comercial indicado no existe.",
                    ErrorCategory.NOT_FOUND));
        }
        if (outcome == RubroComercialPort.SaveOutcome.DUPLICATE_CODIGO) {
            return Result.failure(new StandardApplicationError(
                    "CAT_RUBRO_COMERCIAL_DUPLICADO", "Ya existe un rubro comercial con el código indicado.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(CatalogoApplicationMapper.toResult(rubro));
    }

    private Result<RubroComercialResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

- [x] **Step 9: Crear los handlers de consulta**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarRubroComercialHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarRubroComercialQuery;
import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ConsultarRubroComercialUseCase;
import com.softprimesolutions.catalogo.application.port.out.RubroComercialPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarRubroComercialHandler implements ConsultarRubroComercialUseCase {

    private final RubroComercialPort rubroComercialPort;

    public ConsultarRubroComercialHandler(RubroComercialPort rubroComercialPort) {
        this.rubroComercialPort = Objects.requireNonNull(rubroComercialPort, "rubroComercialPort es obligatorio");
    }

    @Override
    public Result<RubroComercialResult, ApplicationError> execute(ConsultarRubroComercialQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return rubroComercialPort.findById(query.tenantId(), query.rubroComercialId())
                .map(CatalogoApplicationMapper::toResult)
                .map(Result::<RubroComercialResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CAT_RUBRO_COMERCIAL_NO_ENCONTRADO", "El rubro comercial no existe en el tenant indicado.",
                        ErrorCategory.NOT_FOUND)));
    }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ListarRubrosComercialesHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ListarRubrosComercialesQuery;
import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
import com.softprimesolutions.catalogo.application.port.in.ListarRubrosComercialesUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Map;
import java.util.Objects;

public final class ListarRubrosComercialesHandler implements ListarRubrosComercialesUseCase {

    private final CatalogoReadPort readPort;

    public ListarRubrosComercialesHandler(CatalogoReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<PaginaResult<RubroComercialResult>, ApplicationError> execute(
            ListarRubrosComercialesQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        if (query.page() < 0 || query.size() < 1 || query.size() > 100) {
            return Result.failure(new StandardApplicationError(
                    "CAT_PAGINACION_INVALIDA",
                    "page debe ser mayor o igual a 0 y size debe estar entre 1 y 100.",
                    ErrorCategory.VALIDATION,
                    Map.of("page", query.page(), "size", query.size())));
        }
        return Result.success(readPort.findRubrosComerciales(
                query.tenantId(), query.texto(), query.esFarmaceutico(), query.estado(), query.page(),
                query.size()));
    }
}
```

Nota: `ListarRubrosComercialesHandler` usa `CatalogoReadPort.findRubrosComerciales(...)` (lectura JDBC), NO `RubroComercialPort` — el listado paginado sigue el mismo patrón CQRS que `findMarcas`/`findCategoriasProducto`. Ese método se agrega a `CatalogoReadPort` en la Tarea 11 (infraestructura de lectura). Si la Tarea 11 no se ha ejecutado todavía, este archivo no compilará hasta entonces — es una dependencia esperada entre estas dos tareas del mismo Part B.

- [x] **Step 10: Ejecutar el test del handler de creación y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.command.CrearRubroComercialHandlerTest"`
Expected: BUILD SUCCESSFUL, 3 tests passed. (Nota: el módulo completo no compilará hasta que la Tarea 11 agregue `findRubrosComerciales` a `CatalogoReadPort`; ejecutar el test con `--tests` filtra la compilación pero Gradle igual compila todo el `sourceSet` de test primero — si eso falla, ver la Tarea 11 antes de continuar. En la práctica, ejecutar las Tareas 10 y 11 en secuencia sin intercalar otro trabajo evita este problema.)

- [x] **Step 11: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/RubroComercialResult.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/CrearRubroComercialCommand.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/ActualizarRubroComercialCommand.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarRubroComercialQuery.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ListarRubrosComercialesQuery.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/CrearRubroComercialUseCase.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ActualizarRubroComercialUseCase.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarRubroComercialUseCase.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ListarRubrosComercialesUseCase.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/RubroComercialPort.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearRubroComercialHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarRubroComercialHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarRubroComercialHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ListarRubrosComercialesHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/mapper/CatalogoApplicationMapper.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CrearRubroComercialHandlerTest.java
git commit -m "feat(catalogo): agregar capa de aplicacion de RubroComercial"
```

---

## Tarea 11: Infraestructura de `RubroComercial` (JPA write + JDBC read) y wiring del módulo

Construye la entidad JPA, el repositorio, el adaptador de escritura (`RubroComercialPort`), y agrega el lado de lectura JDBC (`CatalogoReadPort.findRubrosComerciales`) a `CatalogoJdbcReadRepository`/`CatalogoJdbcReadAdapter`. Finalmente cablea los 4 nuevos beans en `CatalogoModuleConfiguration`.

La tabla `sch_catalogo.rubro_comercial` NO tiene columna `estado VARCHAR(20)` (a diferencia de `marca`/`categoria_producto`), solo `es_activo CHAR(1)`. La capa de persistencia traduce: `es_activo = '1'` → `EstadoRubroComercial.ACTIVO` / `es_activo = '0'` → `EstadoRubroComercial.INACTIVO`, y en sentido inverso al escribir.

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/RubroComercialJpaEntity.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/RubroComercialJpaRepository.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/RubroComercialJpaWriteAdapter.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/mapper/CatalogoComercialWriteMapper.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoReadPort.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/read/repository/CatalogoJdbcReadRepository.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/read/adapter/CatalogoJdbcReadAdapter.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ListarRubrosComercialesHandlerTest.java`

**Interfaces:**
- Consumes: `RubroComercial`, `RubroComercialId`, `EstadoRubroComercial` (Tarea 9); `RubroComercialResult`, `RubroComercialPort`, `CatalogoApplicationMapper.toResult(RubroComercial)` (Tarea 10).
- Produces: `RubroComercialJpaEntity` (JPA entity mapeada a `sch_catalogo.rubro_comercial`); `RubroComercialJpaRepository extends JpaRepository<RubroComercialJpaEntity, Long>` con `findByUuidPublico(UUID)` y `existsByTenantIdAndCodigo(Long, String)`; `RubroComercialJpaWriteAdapter implements RubroComercialPort`; `CatalogoReadPort.findRubrosComerciales(UUID tenantId, String texto, Boolean esFarmaceutico, String estado, int page, int size) -> PaginaResult<RubroComercialResult>` (nuevo método de interfaz).

- [x] **Step 1: Escribir el test del handler de listado (falla primero, usa un stub de `CatalogoReadPort`)**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ListarRubrosComercialesHandlerTest.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.catalogo.application.dto.query.ListarRubrosComercialesQuery;
import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResumen;
import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
import com.softprimesolutions.catalogo.application.dto.result.SkuResumen;
import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.catalogo.application.port.out.CatalogoReadPort;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListarRubrosComercialesHandlerTest {

    @Test
    void returnsPageFromReadPort() {
        var tenantId = UUID.randomUUID();
        var rubroId = UUID.randomUUID();
        var page = new PaginaResult<>(
                List.of(new RubroComercialResult(rubroId, tenantId, "FARMA", "Farmacéutico", null, true, 1, "ACTIVO")),
                0, 20, 1);
        var readPort = new StubCatalogoReadPort(page);
        var handler = new ListarRubrosComercialesHandler(readPort);

        var result = handler.execute(new ListarRubrosComercialesQuery(tenantId, null, null, null, 0, 20));

        assertThat(result.isSuccess()).isTrue();
        result.fold(
                success -> {
                    assertThat(success.items()).hasSize(1);
                    assertThat(success.items().get(0).codigo()).isEqualTo("FARMA");
                    return null;
                },
                error -> {
                    throw new AssertionError("expected success but got " + error);
                });
    }

    @Test
    void rejectsInvalidPageSize() {
        var readPort = new StubCatalogoReadPort(new PaginaResult<>(List.of(), 0, 20, 0));
        var handler = new ListarRubrosComercialesHandler(readPort);

        var result = handler.execute(new ListarRubrosComercialesQuery(UUID.randomUUID(), null, null, null, 0, 0));

        assertThat(result.isFailure()).isTrue();
    }

    private record StubCatalogoReadPort(PaginaResult<RubroComercialResult> page) implements CatalogoReadPort {

        @Override
        public List<CondicionVentaResult> findCondicionesVenta(String estado) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<FormaFarmaceuticaResult> findFormasFarmaceuticas(String estado) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ViaAdministracionResult> findViasAdministracion(String estado) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<UnidadMedidaResult> findUnidadesMedida(String estado) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ClasificacionControladaResult> findClasificacionesControladas(String estado) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<PrincipioActivoResult> findPrincipiosActivos(String texto, String estado) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PaginaResult<MarcaResult> findMarcas(UUID tenantId, String texto, String estado, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PaginaResult<CategoriaProductoResult> findCategoriasProducto(
                UUID tenantId, String texto, UUID categoriaPadreId, String estado, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PaginaResult<ProductoReguladoResumen> findProductosRegulados(
                String texto, String condicionVentaCodigo, String estadoRegulatorio, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PaginaResult<SkuResumen> findSkus(
                UUID tenantId, String texto, UUID categoriaId, UUID marcaId, String tipoSku, String estado,
                int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PaginaResult<RubroComercialResult> findRubrosComerciales(
                UUID tenantId, String texto, Boolean esFarmaceutico, String estado, int page, int size) {
            return page;
        }
    }
}
```

- [x] **Step 2: Ejecutar el test y verificar que falla por compilación**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ListarRubrosComercialesHandlerTest"`
Expected: FAIL — error de compilación (`findRubrosComerciales` no existe en `CatalogoReadPort`).

- [x] **Step 3: Agregar `findRubrosComerciales` a `CatalogoReadPort`**

Editar `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoReadPort.java` para que quede:

```java
package com.softprimesolutions.catalogo.application.port.out;

import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResumen;
import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
import com.softprimesolutions.catalogo.application.dto.result.SkuResumen;
import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import java.util.List;
import java.util.UUID;

public interface CatalogoReadPort {

    List<CondicionVentaResult> findCondicionesVenta(String estado);

    List<FormaFarmaceuticaResult> findFormasFarmaceuticas(String estado);

    List<ViaAdministracionResult> findViasAdministracion(String estado);

    List<UnidadMedidaResult> findUnidadesMedida(String estado);

    List<ClasificacionControladaResult> findClasificacionesControladas(String estado);

    List<PrincipioActivoResult> findPrincipiosActivos(String texto, String estado);

    PaginaResult<MarcaResult> findMarcas(UUID tenantId, String texto, String estado, int page, int size);

    PaginaResult<CategoriaProductoResult> findCategoriasProducto(
            UUID tenantId, String texto, UUID categoriaPadreId, String estado, int page, int size);

    PaginaResult<ProductoReguladoResumen> findProductosRegulados(
            String texto, String condicionVentaCodigo, String estadoRegulatorio, int page, int size);

    PaginaResult<SkuResumen> findSkus(
            UUID tenantId, String texto, UUID categoriaId, UUID marcaId, String tipoSku, String estado,
            int page, int size);

    PaginaResult<RubroComercialResult> findRubrosComerciales(
            UUID tenantId, String texto, Boolean esFarmaceutico, String estado, int page, int size);
}
```

- [x] **Step 4: Crear la entidad JPA `RubroComercialJpaEntity`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/RubroComercialJpaEntity.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rubro_comercial", schema = "sch_catalogo")
public class RubroComercialJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 300)
    private String descripcion;

    @Column(name = "es_farmaceutico", nullable = false)
    private boolean esFarmaceutico;

    @Column(nullable = false)
    private int orden;

    @Column(name = "es_activo", nullable = false, length = 1)
    private String esActivo;

    @Column(name = "created_by", nullable = false, length = 15)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_by", length = 15)
    private String updatedBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected RubroComercialJpaEntity() {
    }

    public RubroComercialJpaEntity(
            UUID uuidPublico, Long tenantId, String codigo, String nombre, String descripcion,
            boolean esFarmaceutico, int orden, String esActivo, String createdBy, Instant createdAt) {
        this.uuidPublico = uuidPublico;
        this.tenantId = tenantId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.esFarmaceutico = esFarmaceutico;
        this.orden = orden;
        this.esActivo = esActivo;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getTenantId() { return tenantId; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public boolean isEsFarmaceutico() { return esFarmaceutico; }
    public int getOrden() { return orden; }
    public String getEsActivo() { return esActivo; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public String getUpdatedBy() { return updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

- [x] **Step 5: Crear el repositorio `RubroComercialJpaRepository`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/RubroComercialJpaRepository.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.RubroComercialJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RubroComercialJpaRepository extends JpaRepository<RubroComercialJpaEntity, Long> {
    Optional<RubroComercialJpaEntity> findByUuidPublico(UUID uuidPublico);
    boolean existsByTenantIdAndCodigo(Long tenantId, String codigo);
}
```

- [x] **Step 6: Agregar `toEntity(RubroComercial, Long)` a `CatalogoComercialWriteMapper`**

Editar `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/mapper/CatalogoComercialWriteMapper.java` para que quede:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.mapper;

import com.softprimesolutions.catalogo.domain.model.CategoriaProducto;
import com.softprimesolutions.catalogo.domain.model.EstadoRubroComercial;
import com.softprimesolutions.catalogo.domain.model.Marca;
import com.softprimesolutions.catalogo.domain.model.RubroComercial;
import com.softprimesolutions.catalogo.domain.model.SKUComercial;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.CategoriaProductoJpaEntity;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.MarcaJpaEntity;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.RubroComercialJpaEntity;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.SkuComercialJpaEntity;
import java.time.Instant;

public final class CatalogoComercialWriteMapper {

    private static final String SYSTEM_ACTOR = "SYSTEM";

    private CatalogoComercialWriteMapper() {
    }

    public static MarcaJpaEntity toEntity(Marca marca, Long tenantId) {
        return new MarcaJpaEntity(
                marca.id().value(), tenantId, marca.codigo(), marca.nombre(), marca.descripcion(),
                marca.estado().name(), SYSTEM_ACTOR, Instant.now());
    }

    public static CategoriaProductoJpaEntity toEntity(
            CategoriaProducto categoria, Long tenantId, Long categoriaPadreId) {
        return new CategoriaProductoJpaEntity(
                categoria.id().value(), tenantId, categoriaPadreId, categoria.codigo(), categoria.nombre(),
                categoria.descripcion(), categoria.nivel(), categoria.orden(), categoria.estado().name(),
                SYSTEM_ACTOR, Instant.now());
    }

    public static SkuComercialJpaEntity toEntity(
            SKUComercial sku, Long tenantId, Long productoReguladoId, Long categoriaId, Long marcaId) {
        return new SkuComercialJpaEntity(
                sku.id().value(), tenantId, productoReguladoId, categoriaId, marcaId, sku.tipoSku().name(),
                sku.codigoInterno(), sku.descripcionComercial(), sku.nombreCorto(), sku.presentacionComercial(),
                sku.unidadVentaCodigo(), sku.contenido(), sku.unidadContenidoCodigo(), sku.pesoGramos(),
                sku.altoCm(), sku.anchoCm(), sku.largoCm(), sku.permiteVentaFraccion(), sku.factorFraccion(),
                sku.unidadFraccionCodigo(), sku.requiereLote(), sku.requiereVencimiento(), sku.afectoIgv(),
                sku.stockMinimoDefault(), sku.stockMaximoDefault(), sku.imagenUri(), sku.estado().name(),
                sku.createdBy(), sku.createdAt(), sku.updatedBy(), sku.updatedAt());
    }

    public static RubroComercialJpaEntity toEntity(RubroComercial rubro, Long tenantId) {
        return new RubroComercialJpaEntity(
                rubro.id().value(), tenantId, rubro.codigo(), rubro.nombre(), rubro.descripcion(),
                rubro.esFarmaceutico(), rubro.orden(), toEsActivo(rubro.estado()), SYSTEM_ACTOR, Instant.now());
    }

    public static String toEsActivo(EstadoRubroComercial estado) {
        return estado == EstadoRubroComercial.ACTIVO ? "1" : "0";
    }

    public static EstadoRubroComercial toEstadoRubroComercial(String esActivo) {
        return "1".equals(esActivo) ? EstadoRubroComercial.ACTIVO : EstadoRubroComercial.INACTIVO;
    }
}
```

- [x] **Step 7: Crear el adaptador de escritura `RubroComercialJpaWriteAdapter`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/RubroComercialJpaWriteAdapter.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.adapter;

import com.softprimesolutions.catalogo.application.port.out.RubroComercialPort;
import com.softprimesolutions.catalogo.domain.model.RubroComercial;
import com.softprimesolutions.catalogo.domain.valueobject.RubroComercialId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.mapper.CatalogoComercialWriteMapper;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.RubroComercialJpaRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class RubroComercialJpaWriteAdapter implements RubroComercialPort {

    private final RubroComercialJpaRepository repository;
    private final JdbcClient jdbcClient;
    private final EntityManager entityManager;

    public RubroComercialJpaWriteAdapter(
            RubroComercialJpaRepository repository, JdbcClient jdbcClient, EntityManager entityManager) {
        this.repository = repository;
        this.jdbcClient = jdbcClient;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public SaveOutcome save(RubroComercial rubroComercial) {
        var tenantId = findTenantId(
                rubroComercial.tenantId() == null ? null : rubroComercial.tenantId().value());
        if (tenantId.isEmpty()) return SaveOutcome.TENANT_NOT_FOUND;

        var existing = repository.findByUuidPublico(rubroComercial.id().value());
        if (existing.isEmpty()) {
            if (repository.existsByTenantIdAndCodigo(tenantId.get(), rubroComercial.codigo())) {
                return SaveOutcome.DUPLICATE_CODIGO;
            }
            try {
                repository.saveAndFlush(CatalogoComercialWriteMapper.toEntity(rubroComercial, tenantId.get()));
                return SaveOutcome.CREATED;
            } catch (DataIntegrityViolationException exception) {
                return SaveOutcome.DUPLICATE_CODIGO;
            }
        }

        var entity = existing.get();
        if (!entity.getCodigo().equals(rubroComercial.codigo())
                && repository.existsByTenantIdAndCodigo(tenantId.get(), rubroComercial.codigo())) {
            return SaveOutcome.DUPLICATE_CODIGO;
        }
        jdbcClient.sql("""
                        UPDATE sch_catalogo.rubro_comercial
                           SET codigo = :codigo, nombre = :nombre, descripcion = :descripcion,
                               es_farmaceutico = :esFarmaceutico, orden = :orden,
                               updated_by = :updatedBy, updated_at = :updatedAt
                         WHERE uuid_publico = :rubroComercialId
                        """)
                .param("codigo", rubroComercial.codigo())
                .param("nombre", rubroComercial.nombre())
                .param("descripcion", rubroComercial.descripcion())
                .param("esFarmaceutico", rubroComercial.esFarmaceutico())
                .param("orden", rubroComercial.orden())
                .param("updatedBy", "SYSTEM")
                .param("updatedAt", toOffsetDateTime(Instant.now()))
                .param("rubroComercialId", rubroComercial.id().value())
                .update();
        return SaveOutcome.UPDATED;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RubroComercial> findById(UUID tenantId, UUID rubroComercialId) {
        var tenantInternalId = findTenantId(tenantId);
        if (tenantInternalId.isEmpty()) return Optional.empty();
        var entity = repository.findByUuidPublico(rubroComercialId);
        if (entity.isEmpty() || !entity.get().getTenantId().equals(tenantInternalId.get())) {
            return Optional.empty();
        }
        var rubro = RubroComercial.restore(
                new RubroComercialId(entity.get().getUuidPublico()), new TenantId(tenantId),
                entity.get().getCodigo(), entity.get().getNombre(), entity.get().getDescripcion(),
                entity.get().isEsFarmaceutico(), entity.get().getOrden(),
                CatalogoComercialWriteMapper.toEstadoRubroComercial(entity.get().getEsActivo()));
        return Optional.of(rubro);
    }

    @Override
    @Transactional
    public boolean changeStatus(UUID tenantId, UUID rubroComercialId, String status, Instant changedAt) {
        var tenantInternalId = findTenantId(tenantId);
        if (tenantInternalId.isEmpty()) return false;
        var esActivo = "ACTIVO".equals(status) ? "1" : "0";
        var updated = jdbcClient.sql("""
                        UPDATE sch_catalogo.rubro_comercial SET es_activo = :esActivo
                         WHERE tenant_id = :tenantId AND uuid_publico = :rubroComercialId
                        """)
                .param("esActivo", esActivo).param("tenantId", tenantInternalId.get())
                .param("rubroComercialId", rubroComercialId)
                .update() == 1;
        if (updated) entityManager.clear();
        return updated;
    }

    private Optional<Long> findTenantId(UUID tenantUuid) {
        if (tenantUuid == null) return Optional.empty();
        return jdbcClient.sql("SELECT id FROM sch_admin.tenant WHERE uuid_publico = :tenantUuid")
                .param("tenantUuid", tenantUuid).query(Long.class).optional();
    }

    private static OffsetDateTime toOffsetDateTime(Instant value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
```

- [x] **Step 8: Agregar `findRubrosComerciales`/`countRubrosComerciales` a `CatalogoJdbcReadRepository`**

Editar `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/read/repository/CatalogoJdbcReadRepository.java`. Agregar el import:

```java
import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
```

Agregar, después de `countCategoriasProducto` y antes de la sección `PRODUCTO_REGULADO_FILTER`:

```java
    private static final String RUBRO_COMERCIAL_FROM = """
            FROM sch_catalogo.rubro_comercial r
            JOIN sch_admin.tenant t ON t.id = r.tenant_id
            """;

    private static final String RUBRO_COMERCIAL_FILTER = """
             WHERE t.uuid_publico = :tenantId
               AND (:texto = '' OR LOWER(r.nombre) LIKE :pattern OR LOWER(r.codigo) LIKE :pattern)
               AND (CAST(:esFarmaceutico AS BOOLEAN) IS NULL OR r.es_farmaceutico = CAST(:esFarmaceutico AS BOOLEAN))
               AND (:estado = '' OR r.es_activo = CASE WHEN :estado = 'ACTIVO' THEN '1' ELSE '0' END)
            """;

    public List<RubroComercialResult> findRubrosComerciales(
            UUID tenantId, String texto, Boolean esFarmaceutico, String estado, int offset, int limit) {
        var textFilter = normalizeSearch(texto);
        var filter = normalizeStatus(estado);
        return jdbcClient.sql("SELECT r.uuid_publico, t.uuid_publico AS tenant_uuid, r.codigo, r.nombre, "
                        + "r.descripcion, r.es_farmaceutico, r.orden, r.es_activo " + RUBRO_COMERCIAL_FROM
                        + RUBRO_COMERCIAL_FILTER + " ORDER BY r.orden, r.nombre LIMIT :limit OFFSET :offset")
                .param("tenantId", tenantId)
                .param("texto", textFilter)
                .param("pattern", '%' + textFilter + '%')
                .param("esFarmaceutico", esFarmaceutico)
                .param("estado", filter)
                .param("limit", limit)
                .param("offset", offset)
                .query((rs, rowNumber) -> new RubroComercialResult(
                        rs.getObject("uuid_publico", UUID.class), rs.getObject("tenant_uuid", UUID.class),
                        rs.getString("codigo"), rs.getString("nombre"), rs.getString("descripcion"),
                        rs.getBoolean("es_farmaceutico"), rs.getInt("orden"),
                        "1".equals(rs.getString("es_activo")) ? "ACTIVO" : "INACTIVO"))
                .list();
    }

    public long countRubrosComerciales(UUID tenantId, String texto, Boolean esFarmaceutico, String estado) {
        var textFilter = normalizeSearch(texto);
        var filter = normalizeStatus(estado);
        return jdbcClient.sql("SELECT COUNT(*) " + RUBRO_COMERCIAL_FROM + RUBRO_COMERCIAL_FILTER)
                .param("tenantId", tenantId)
                .param("texto", textFilter)
                .param("pattern", '%' + textFilter + '%')
                .param("esFarmaceutico", esFarmaceutico)
                .param("estado", filter)
                .query(Long.class).single();
    }
```

- [x] **Step 9: Agregar `findRubrosComerciales` a `CatalogoJdbcReadAdapter`**

Editar `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/read/adapter/CatalogoJdbcReadAdapter.java`. Agregar el import:

```java
import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
```

Agregar el método, después de `findSkus` (implementación de la interfaz `CatalogoReadPort`):

```java
    @Override
    @Transactional(readOnly = true)
    public PaginaResult<RubroComercialResult> findRubrosComerciales(
            UUID tenantId, String texto, Boolean esFarmaceutico, String estado, int page, int size) {
        var items = repository.findRubrosComerciales(tenantId, texto, esFarmaceutico, estado, page * size, size);
        var total = repository.countRubrosComerciales(tenantId, texto, esFarmaceutico, estado);
        return new PaginaResult<>(items, page, size, total);
    }
```

- [x] **Step 10: Ejecutar el test del handler de listado y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ListarRubrosComercialesHandlerTest"`
Expected: BUILD SUCCESSFUL, 2 tests passed.

- [x] **Step 11: Cablear los 4 beans de RubroComercial en `CatalogoModuleConfiguration`**

Editar `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java`. Agregar los imports:

```java
import com.softprimesolutions.catalogo.application.port.in.ActualizarRubroComercialUseCase;
import com.softprimesolutions.catalogo.application.port.in.ConsultarRubroComercialUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearRubroComercialUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarRubrosComercialesUseCase;
import com.softprimesolutions.catalogo.application.port.out.RubroComercialPort;
import com.softprimesolutions.catalogo.application.usecase.command.ActualizarRubroComercialHandler;
import com.softprimesolutions.catalogo.application.usecase.command.CrearRubroComercialHandler;
import com.softprimesolutions.catalogo.application.usecase.query.ConsultarRubroComercialHandler;
import com.softprimesolutions.catalogo.application.usecase.query.ListarRubrosComercialesHandler;
```

Agregar una nueva sección `// ---- Rubros comerciales ----` justo después de la sección `// ---- Categorías de producto ----` (antes de `// ---- Productos regulados ----`):

```java
    // ---- Rubros comerciales ----

    @Bean
    CrearRubroComercialUseCase crearRubroComercialUseCase(
            RubroComercialPort rubroComercialPort, IdentifierGenerator catalogoIdentifierGenerator) {
        return new CrearRubroComercialHandler(rubroComercialPort, catalogoIdentifierGenerator);
    }

    @Bean
    ActualizarRubroComercialUseCase actualizarRubroComercialUseCase(RubroComercialPort rubroComercialPort) {
        return new ActualizarRubroComercialHandler(rubroComercialPort);
    }

    @Bean
    ConsultarRubroComercialUseCase consultarRubroComercialUseCase(RubroComercialPort rubroComercialPort) {
        return new ConsultarRubroComercialHandler(rubroComercialPort);
    }

    @Bean
    ListarRubrosComercialesUseCase listarRubrosComercialesUseCase(CatalogoReadPort readPort) {
        return new ListarRubrosComercialesHandler(readPort);
    }
```

- [x] **Step 12: Compilar el módulo completo**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL.

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test`
Expected: BUILD SUCCESSFUL (todos los tests unitarios del módulo, incluidos los de `RubroComercial`, pasan).

- [x] **Step 13: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/RubroComercialJpaEntity.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/RubroComercialJpaRepository.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/RubroComercialJpaWriteAdapter.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/mapper/CatalogoComercialWriteMapper.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoReadPort.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/read/repository/CatalogoJdbcReadRepository.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/read/adapter/CatalogoJdbcReadAdapter.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ListarRubrosComercialesHandlerTest.java
git commit -m "feat(catalogo): agregar infraestructura JPA/JDBC de RubroComercial y cablear beans"
```

---

## Tarea 12: API REST de `RubroComercial` (DTOs, `CatalogoApiMapper`, controller, `changeRubroComercialStatus`)

Cierra el Part B: agrega `changeRubroComercialStatus` a `CatalogoControlUseCase`/`CatalogoControlService` (siguiendo exactamente el patrón de `changeMarcaStatus`), los DTOs HTTP, los métodos nuevos en `CatalogoApiMapper`, y el controller `RubroComercialController` con los 5 endpoints: `POST`, `PUT /{id}`, `PATCH /{id}/estado`, `GET /{id}`, `GET` (lista paginada con filtros `q`/`esFarmaceutico`/`estado`).

**Files:**
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/CatalogoControlUseCase.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlService.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/request/RubroComercialRequest.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/response/RubroComercialResponse.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/mapper/CatalogoApiMapper.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/RubroComercialController.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlServiceTest.java` (modificar — agregar un caso para `changeRubroComercialStatus`; leer el archivo existente antes de editar para no romper los tests ya presentes)

**Interfaces:**
- Consumes: `RubroComercialPort.changeStatus(UUID, UUID, String, Instant)` (Tarea 11); `CrearRubroComercialUseCase`/`ActualizarRubroComercialUseCase`/`ConsultarRubroComercialUseCase`/`ListarRubrosComercialesUseCase` (Tarea 10); `RubroComercialResult` (Tarea 10); `PaginaResponse<T>` (existente); `CambiarEstadoTenantRequest` (existente, mismo DTO que usa `Marca`/`CategoriaProducto`).
- Produces: `CatalogoControlUseCase.changeRubroComercialStatus(UUID tenantId, UUID rubroComercialId, String status) -> Result<Unit, ApplicationError>`; `RubroComercialRequest(UUID tenantId, String codigo, String nombre, String descripcion, boolean esFarmaceutico, int orden)`; `RubroComercialResponse(UUID id, UUID tenantId, String codigo, String nombre, String descripcion, boolean esFarmaceutico, int orden, String estado)`; endpoints REST bajo `/api/v1/catalogo/rubros-comerciales`.

- [x] **Step 1: Agregar `changeRubroComercialStatus` a `CatalogoControlUseCase`**

Editar `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/CatalogoControlUseCase.java` para que quede:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.shared.kernel.result.Unit;
import java.util.UUID;

public interface CatalogoControlUseCase {

    Result<Unit, ApplicationError> changeCondicionVentaStatus(String codigo, String status);

    Result<Unit, ApplicationError> changeFormaFarmaceuticaStatus(String codigo, String status);

    Result<Unit, ApplicationError> changeViaAdministracionStatus(String codigo, String status);

    Result<Unit, ApplicationError> changeUnidadMedidaStatus(String codigo, String status);

    Result<Unit, ApplicationError> changeClasificacionControladaStatus(String codigo, String status);

    Result<Unit, ApplicationError> changePrincipioActivoStatus(UUID principioActivoId, String status);

    Result<Unit, ApplicationError> changeMarcaStatus(UUID tenantId, UUID marcaId, String status);

    Result<Unit, ApplicationError> changeCategoriaProductoStatus(UUID tenantId, UUID categoriaId, String status);

    Result<Unit, ApplicationError> changeProductoReguladoStatus(UUID productoReguladoId, String status);

    Result<Unit, ApplicationError> changeSkuStatus(UUID tenantId, UUID skuId, String status);

    Result<Unit, ApplicationError> changeRubroComercialStatus(UUID tenantId, UUID rubroComercialId, String status);
}
```

- [x] **Step 2: Implementar `changeRubroComercialStatus` en `CatalogoControlService`**

Editar `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlService.java`. Agregar el import:

```java
import com.softprimesolutions.catalogo.application.port.out.RubroComercialPort;
```

Agregar un campo `rubroComercialPort` y su inicialización en el constructor. El constructor completo pasa a ser:

```java
    private final CatalogoSoportePort soportePort;
    private final CatalogoComercialPort comercialPort;
    private final ProductoReguladoPort productoReguladoPort;
    private final RubroComercialPort rubroComercialPort;
    private final ClockPort clock;

    public CatalogoControlService(
            CatalogoSoportePort soportePort, CatalogoComercialPort comercialPort,
            ProductoReguladoPort productoReguladoPort, RubroComercialPort rubroComercialPort, ClockPort clock) {
        this.soportePort = Objects.requireNonNull(soportePort, "soportePort es obligatorio");
        this.comercialPort = Objects.requireNonNull(comercialPort, "comercialPort es obligatorio");
        this.productoReguladoPort = Objects.requireNonNull(productoReguladoPort, "productoReguladoPort es obligatorio");
        this.rubroComercialPort = Objects.requireNonNull(rubroComercialPort, "rubroComercialPort es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }
```

Agregar el método, después de `changeSkuStatus` y antes de `normalizeStatus`:

```java
    @Override
    public Result<Unit, ApplicationError> changeRubroComercialStatus(
            UUID tenantId, UUID rubroComercialId, String status) {
        var normalized = normalizeStatus(status);
        if (!COMERCIAL_STATUSES.contains(normalized)) return invalidStatus(COMERCIAL_STATUSES);
        return rubroComercialPort.changeStatus(tenantId, rubroComercialId, normalized, clock.now())
                ? Result.success(Unit.INSTANCE)
                : notFound("CAT_RUBRO_COMERCIAL_NO_ENCONTRADO", "El rubro comercial no existe en el tenant indicado.");
    }
```

Este método reutiliza el `Set<String> COMERCIAL_STATUSES = Set.of("ACTIVO", "INACTIVO")` que ya existe en la clase (el mismo usado por `changeMarcaStatus`/`changeCategoriaProductoStatus`).

- [x] **Step 3: Actualizar el test existente `CatalogoControlServiceTest`**

El archivo usa stubs manuales (clases internas `private static final class Fake*Port implements *Port`), no Mockito. Las Tareas 1 y 3 de este plan ya agregaron a `FakeCatalogoComercialPort` los métodos `findCategoriaById`/`findMarcaById`, y a `FakeCatalogoSoportePort` los seis métodos `find*ByCodigo`/`findPrincipioActivoById` — este Step asume que esas dos tareas ya se ejecutaron (se ejecutan antes en la numeración de este plan). Ahora hace falta: (a) agregar una cuarta clase fake `FakeRubroComercialPort implementa RubroComercialPort`, (b) actualizar las 4 llamadas al constructor `new CatalogoControlService(...)` de 4 a 5 argumentos, y (c) agregar un test para `changeRubroComercialStatus`.

Reescribir `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlServiceTest.java` completo:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.application.port.out.ProductoReguladoPort;
import com.softprimesolutions.catalogo.application.port.out.RubroComercialPort;
import com.softprimesolutions.catalogo.domain.model.CategoriaProducto;
import com.softprimesolutions.catalogo.domain.model.Marca;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.ProductoRegulado;
import com.softprimesolutions.catalogo.domain.model.RubroComercial;
import com.softprimesolutions.catalogo.domain.model.SKUComercial;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogoControlServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("172e0f26-a765-46f3-841c-4a11407ccf5b");

    @Test
    void rejectsAnInvalidStatus() {
        var soportePort = new FakeCatalogoSoportePort(true);
        var service = new CatalogoControlService(
                soportePort, new FakeCatalogoComercialPort(true), new FakeProductoReguladoPort(true),
                new FakeRubroComercialPort(true), () -> Instant.parse("2026-09-07T12:00:00Z"));

        var result = service.changeCondicionVentaStatus("SIN-RECETA", "ELIMINADO");

        assertTrue(result.isFailure());
        assertEquals("CAT_ESTADO_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void changesCondicionVentaStatusWithAValidValue() {
        var soportePort = new FakeCatalogoSoportePort(true);
        var service = new CatalogoControlService(
                soportePort, new FakeCatalogoComercialPort(true), new FakeProductoReguladoPort(true),
                new FakeRubroComercialPort(true), () -> Instant.parse("2026-09-07T12:00:00Z"));

        var result = service.changeCondicionVentaStatus("SIN-RECETA", "INACTIVO");

        assertTrue(result.isSuccess());
    }

    @Test
    void reportsNotFoundWhenMarcaDoesNotExist() {
        var service = new CatalogoControlService(
                new FakeCatalogoSoportePort(true), new FakeCatalogoComercialPort(false),
                new FakeProductoReguladoPort(true), new FakeRubroComercialPort(true),
                () -> Instant.parse("2026-09-07T12:00:00Z"));

        var result = service.changeMarcaStatus(TENANT_ID, UUID.randomUUID(), "ACTIVO");

        assertTrue(result.isFailure());
        assertEquals("CAT_MARCA_NO_ENCONTRADA", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void changesProductoReguladoStatusSuccessfully() {
        var service = new CatalogoControlService(
                new FakeCatalogoSoportePort(true), new FakeCatalogoComercialPort(true),
                new FakeProductoReguladoPort(true), new FakeRubroComercialPort(true),
                () -> Instant.parse("2026-09-07T12:00:00Z"));

        var result = service.changeProductoReguladoStatus(UUID.randomUUID(), "VIGENTE");

        assertTrue(result.isSuccess());
    }

    @Test
    void changesRubroComercialStatusWhenValid() {
        var service = new CatalogoControlService(
                new FakeCatalogoSoportePort(true), new FakeCatalogoComercialPort(true),
                new FakeProductoReguladoPort(true), new FakeRubroComercialPort(true),
                () -> Instant.parse("2026-09-07T12:00:00Z"));

        var result = service.changeRubroComercialStatus(TENANT_ID, UUID.randomUUID(), "INACTIVO");

        assertTrue(result.isSuccess());
    }

    @Test
    void reportsNotFoundWhenRubroComercialDoesNotExist() {
        var service = new CatalogoControlService(
                new FakeCatalogoSoportePort(true), new FakeCatalogoComercialPort(true),
                new FakeProductoReguladoPort(true), new FakeRubroComercialPort(false),
                () -> Instant.parse("2026-09-07T12:00:00Z"));

        var result = service.changeRubroComercialStatus(TENANT_ID, UUID.randomUUID(), "ACTIVO");

        assertTrue(result.isFailure());
        assertEquals("CAT_RUBRO_COMERCIAL_NO_ENCONTRADO", result.fold(value -> null, error -> error.code()));
    }

    private static final class FakeCatalogoSoportePort implements CatalogoSoportePort {
        private final boolean found;

        private FakeCatalogoSoportePort(boolean found) { this.found = found; }

        @Override
        public SaveOutcome save(CondicionVenta condicionVenta) { throw new UnsupportedOperationException(); }

        @Override
        public SaveOutcome save(FormaFarmaceutica formaFarmaceutica) { throw new UnsupportedOperationException(); }

        @Override
        public SaveOutcome save(ViaAdministracion viaAdministracion) { throw new UnsupportedOperationException(); }

        @Override
        public SaveOutcome save(UnidadMedida unidadMedida) { throw new UnsupportedOperationException(); }

        @Override
        public SaveOutcome save(ClasificacionControlada clasificacionControlada) { throw new UnsupportedOperationException(); }

        @Override
        public SavePrincipioActivoOutcome save(PrincipioActivo principioActivo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<CondicionVenta> findCondicionVentaByCodigo(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<FormaFarmaceutica> findFormaFarmaceuticaByCodigo(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<ViaAdministracion> findViaAdministracionByCodigo(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<UnidadMedida> findUnidadMedidaByCodigo(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<ClasificacionControlada> findClasificacionControladaByCodigo(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<PrincipioActivo> findPrincipioActivoById(UUID principioActivoId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean condicionVentaExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean formaFarmaceuticaExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean viaAdministracionExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean unidadMedidaExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean clasificacionControladaExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean principioActivoExists(UUID principioActivoId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeCondicionVentaStatus(String codigo, String status, Instant changedAt) { return found; }

        @Override
        public boolean changeFormaFarmaceuticaStatus(String codigo, String status, Instant changedAt) { return found; }

        @Override
        public boolean changeViaAdministracionStatus(String codigo, String status, Instant changedAt) { return found; }

        @Override
        public boolean changeUnidadMedidaStatus(String codigo, String status, Instant changedAt) { return found; }

        @Override
        public boolean changeClasificacionControladaStatus(String codigo, String status, Instant changedAt) { return found; }

        @Override
        public boolean changePrincipioActivoStatus(UUID principioActivoId, String status, Instant changedAt) { return found; }
    }

    private static final class FakeCatalogoComercialPort implements CatalogoComercialPort {
        private final boolean found;

        private FakeCatalogoComercialPort(boolean found) { this.found = found; }

        @Override
        public SaveMarcaOutcome save(Marca marca) { throw new UnsupportedOperationException(); }

        @Override
        public SaveCategoriaOutcome save(CategoriaProducto categoria) { throw new UnsupportedOperationException(); }

        @Override
        public SaveSkuOutcome save(SKUComercial sku) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<SKUComercial> findSkuById(UUID tenantId, UUID skuId) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<CategoriaProducto> findCategoriaById(UUID tenantId, UUID categoriaId) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<Marca> findMarcaById(UUID tenantId, UUID marcaId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean categoriaExists(UUID tenantId, UUID categoriaId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean marcaExists(UUID tenantId, UUID marcaId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeMarcaStatus(UUID tenantId, UUID marcaId, String status, Instant changedAt) { return found; }

        @Override
        public boolean changeCategoriaStatus(UUID tenantId, UUID categoriaId, String status, Instant changedAt) { return found; }

        @Override
        public boolean changeSkuStatus(UUID tenantId, UUID skuId, String status, Instant changedAt) { return found; }
    }

    private static final class FakeProductoReguladoPort implements ProductoReguladoPort {
        private final boolean found;

        private FakeProductoReguladoPort(boolean found) { this.found = found; }

        @Override
        public SaveOutcome save(ProductoRegulado productoRegulado) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<ProductoRegulado> findById(UUID productoReguladoId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeStatus(UUID productoReguladoId, String status, Instant changedAt) { return found; }
    }

    private static final class FakeRubroComercialPort implements RubroComercialPort {
        private final boolean found;

        private FakeRubroComercialPort(boolean found) { this.found = found; }

        @Override
        public SaveOutcome save(RubroComercial rubroComercial) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<RubroComercial> findById(UUID tenantId, UUID rubroComercialId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeStatus(UUID tenantId, UUID rubroComercialId, String status, Instant changedAt) { return found; }
    }
}
```

- [x] **Step 4: Ejecutar los tests del módulo y verificar que compilan y pasan**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test`
Expected: BUILD SUCCESSFUL.

- [x] **Step 5: Crear los DTOs HTTP request/response**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/request/RubroComercialRequest.java`:

```java
package com.softprimesolutions.catalogo.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record RubroComercialRequest(
        @NotNull UUID tenantId,
        @NotBlank @Size(min = 2, max = 50) String codigo,
        @NotBlank @Size(min = 2, max = 120) String nombre,
        @Size(max = 300) String descripcion,
        boolean esFarmaceutico,
        @PositiveOrZero int orden) {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/response/RubroComercialResponse.java`:

```java
package com.softprimesolutions.catalogo.api.dto.response;

import java.util.UUID;

public record RubroComercialResponse(
        UUID id, UUID tenantId, String codigo, String nombre, String descripcion, boolean esFarmaceutico,
        int orden, String estado) {
}
```

- [x] **Step 6: Agregar los métodos de `RubroComercial` a `CatalogoApiMapper`**

Editar `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/mapper/CatalogoApiMapper.java`. Agregar los imports:

```java
import com.softprimesolutions.catalogo.api.dto.request.RubroComercialRequest;
import com.softprimesolutions.catalogo.api.dto.response.RubroComercialResponse;
import com.softprimesolutions.catalogo.application.dto.command.ActualizarRubroComercialCommand;
import com.softprimesolutions.catalogo.application.dto.command.CrearRubroComercialCommand;
import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
```

(insertarlos en orden alfabético junto a los demás imports de `api.dto.request`, `api.dto.response`, `application.dto.command`, `application.dto.result`)

Agregar los métodos, después de `toCategoriaPage` y antes de `toCreateCommand(ProductoReguladoRequest)`:

```java
    public static CrearRubroComercialCommand toCreateCommand(RubroComercialRequest request) {
        return new CrearRubroComercialCommand(
                request.tenantId(), request.codigo(), request.nombre(), request.descripcion(),
                request.esFarmaceutico(), request.orden());
    }

    public static ActualizarRubroComercialCommand toUpdateCommand(
            UUID rubroComercialId, RubroComercialRequest request) {
        return new ActualizarRubroComercialCommand(
                request.tenantId(), rubroComercialId, request.codigo(), request.nombre(), request.descripcion(),
                request.esFarmaceutico(), request.orden());
    }

    public static RubroComercialResponse toResponse(RubroComercialResult result) {
        return new RubroComercialResponse(
                result.id(), result.tenantId(), result.codigo(), result.nombre(), result.descripcion(),
                result.esFarmaceutico(), result.orden(), result.estado());
    }

    public static PaginaResponse<RubroComercialResponse> toRubroComercialPage(
            PaginaResult<RubroComercialResult> result) {
        return new PaginaResponse<>(
                result.items().stream().map(CatalogoApiMapper::toResponse).toList(),
                result.page(), result.size(), result.totalElements());
    }
```

- [x] **Step 7: Crear `RubroComercialController`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/RubroComercialController.java`:

```java
package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoTenantRequest;
import com.softprimesolutions.catalogo.api.dto.request.RubroComercialRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.query.ConsultarRubroComercialQuery;
import com.softprimesolutions.catalogo.application.dto.query.ListarRubrosComercialesQuery;
import com.softprimesolutions.catalogo.application.port.in.ActualizarRubroComercialUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.ConsultarRubroComercialUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearRubroComercialUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarRubrosComercialesUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/catalogo/rubros-comerciales")
public class RubroComercialController {

    private final CrearRubroComercialUseCase createRubroComercial;
    private final ActualizarRubroComercialUseCase updateRubroComercial;
    private final ConsultarRubroComercialUseCase getRubroComercial;
    private final ListarRubrosComercialesUseCase listRubrosComerciales;
    private final CatalogoControlUseCase control;

    public RubroComercialController(
            CrearRubroComercialUseCase createRubroComercial,
            ActualizarRubroComercialUseCase updateRubroComercial,
            ConsultarRubroComercialUseCase getRubroComercial,
            ListarRubrosComercialesUseCase listRubrosComerciales,
            CatalogoControlUseCase control) {
        this.createRubroComercial = createRubroComercial;
        this.updateRubroComercial = updateRubroComercial;
        this.getRubroComercial = getRubroComercial;
        this.listRubrosComerciales = listRubrosComerciales;
        this.control = control;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalogo.rubros-comerciales.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody RubroComercialRequest request) {
        return createRubroComercial.execute(CatalogoApiMapper.toCreateCommand(request)).fold(
                result -> ResponseEntity.status(201).body(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PutMapping("/{rubroComercialId}")
    @PreAuthorize("hasAuthority('catalogo.rubros-comerciales.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable UUID rubroComercialId, @Valid @RequestBody RubroComercialRequest request) {
        return updateRubroComercial.execute(CatalogoApiMapper.toUpdateCommand(rubroComercialId, request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{rubroComercialId}/estado")
    @PreAuthorize("hasAuthority('catalogo.rubros-comerciales.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable UUID rubroComercialId, @Valid @RequestBody CambiarEstadoTenantRequest request) {
        return control.changeRubroComercialStatus(request.tenantId(), rubroComercialId, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), CatalogoControllerSupport::problem);
    }

    @GetMapping("/{rubroComercialId}")
    @PreAuthorize("hasAuthority('catalogo.rubros-comerciales.consultar')")
    public ResponseEntity<?> get(@PathVariable UUID rubroComercialId, @RequestParam UUID tenantId) {
        return getRubroComercial.execute(new ConsultarRubroComercialQuery(tenantId, rubroComercialId)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.rubros-comerciales.consultar')")
    public ResponseEntity<?> list(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean esFarmaceutico,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return listRubrosComerciales.execute(
                        new ListarRubrosComercialesQuery(tenantId, q, esFarmaceutico, estado, page, size))
                .fold(result -> ResponseEntity.ok(CatalogoApiMapper.toRubroComercialPage(result)),
                        CatalogoControllerSupport::problem);
    }
}
```

- [x] **Step 8: Actualizar el bean `catalogoControlUseCase` en `CatalogoModuleConfiguration` para el nuevo parámetro**

Editar `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java`. El método `catalogoControlUseCase` (sección `// ---- Control de estado transversal ----`) pasa de:

```java
    @Bean
    CatalogoControlUseCase catalogoControlUseCase(
            CatalogoSoportePort soportePort, CatalogoComercialPort comercialPort,
            ProductoReguladoPort productoReguladoPort, ClockPort catalogoClockPort) {
        return new CatalogoControlService(soportePort, comercialPort, productoReguladoPort, catalogoClockPort);
    }
```

a:

```java
    @Bean
    CatalogoControlUseCase catalogoControlUseCase(
            CatalogoSoportePort soportePort, CatalogoComercialPort comercialPort,
            ProductoReguladoPort productoReguladoPort, RubroComercialPort rubroComercialPort,
            ClockPort catalogoClockPort) {
        return new CatalogoControlService(
                soportePort, comercialPort, productoReguladoPort, rubroComercialPort, catalogoClockPort);
    }
```

(`RubroComercialPort` ya está importado desde la Tarea 11).

- [x] **Step 9: Compilar el módulo completo y correr todos los tests unitarios**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL.

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test`
Expected: BUILD SUCCESSFUL.

- [x] **Step 10: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/CatalogoControlUseCase.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlService.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/request/RubroComercialRequest.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/response/RubroComercialResponse.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/mapper/CatalogoApiMapper.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/RubroComercialController.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlServiceTest.java
git commit -m "feat(catalogo): agregar API REST completa de RubroComercial"
```

---

Con esto se completa el Part B (`RubroComercial` completo: dominio, aplicación, infraestructura, API — Tareas 9 a 12).

---

## Tarea 13: Dominio y capa de aplicación de `TipoDocumentoIdentidad`

`TipoDocumentoIdentidad` es un catálogo GLOBAL de referencia SUNAT (catálogo 06), sin `tenant_id`, PK `codigo VARCHAR(2)`, 13 filas semilla ya insertadas por la migración `docs/cadena-farmacias-docs/database/migrations/V018__catalogo_tipo_documento_identidad_sunat.sql`. La tabla `sch_catalogo.tipo_documento_identidad` tiene columnas: `codigo VARCHAR(2)` (PK), `sigla VARCHAR(30)`, `denominacion VARCHAR(200)`, `max SMALLINT`, `min SMALLINT`, `es_activo CHAR(1) NOT NULL DEFAULT '1'`. NO tiene columna `estado VARCHAR(20)` — igual que `RubroComercial`, el estado ACTIVO/INACTIVO expuesto por la API se deriva de `es_activo`.

**No se revisó ninguna regla de negocio específica documentada sobre `tipo_documento_identidad` en `docs/cadena-farmacias-docs/docs/03-requerimientos/`** más allá de los comentarios `COMMENT ON` de la propia migración V018 (que ya se citan en el dominio de abajo). Por tratarse de un catálogo de referencia SUNAT con 13 filas fijas ya sembradas, la API NO expone creación (`POST`) ni eliminación (`DELETE`) — solo `GET /{codigo}`, `GET` (lista sin paginar, igual que los catálogos "soporte") y `PATCH /{codigo}/estado` para activar/desactivar el uso local de un código.

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/soporte/EstadoTipoDocumentoIdentidad.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/soporte/TipoDocumentoIdentidad.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/TipoDocumentoIdentidadResult.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarTipoDocumentoIdentidadQuery.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ListarTiposDocumentoIdentidadQuery.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarTipoDocumentoIdentidadUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ListarTiposDocumentoIdentidadUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/TipoDocumentoIdentidadPort.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarTipoDocumentoIdentidadHandler.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ListarTiposDocumentoIdentidadHandler.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/mapper/CatalogoApplicationMapper.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/soporte/TipoDocumentoIdentidadTest.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarTipoDocumentoIdentidadHandlerTest.java`

**Interfaces:**
- Consumes: `AggregateRoot`/`Result`/`ErrorDetail` (existentes); ninguna dependencia de otro catálogo.
- Produces: `EstadoTipoDocumentoIdentidad { ACTIVO, INACTIVO }`; `TipoDocumentoIdentidad.restore(String codigo, String sigla, String denominacion, Integer max, Integer min, EstadoTipoDocumentoIdentidad estado) -> TipoDocumentoIdentidad` (SOLO `restore`, sin `create` — no hay caso de uso de creación, las 13 filas ya existen); `TipoDocumentoIdentidadResult(String codigo, String sigla, String denominacion, Integer max, Integer min, String estado)`; `ConsultarTipoDocumentoIdentidadQuery(String codigo)`; `ListarTiposDocumentoIdentidadQuery(String estado)`; `TipoDocumentoIdentidadPort` con `findByCodigo(String) -> Optional<TipoDocumentoIdentidad>`, `findAll(String estado) -> List<TipoDocumentoIdentidad>`, `changeStatus(String codigo, String status) -> boolean`; `CatalogoApplicationMapper.toResult(TipoDocumentoIdentidad) -> TipoDocumentoIdentidadResult` (nuevo overload).

- [ ] **Step 1: Escribir el test de dominio (falla primero)**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/soporte/TipoDocumentoIdentidadTest.java`:

```java
package com.softprimesolutions.catalogo.domain.model.soporte;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TipoDocumentoIdentidadTest {

    @Test
    void restoresTipoDocumentoIdentidadWithGivenState() {
        var tipoDocumento = TipoDocumentoIdentidad.restore(
                "1", "DNI", "Documento Nacional de Identidad", 8, 8, EstadoTipoDocumentoIdentidad.ACTIVO);

        assertThat(tipoDocumento.codigo()).isEqualTo("1");
        assertThat(tipoDocumento.sigla()).isEqualTo("DNI");
        assertThat(tipoDocumento.denominacion()).isEqualTo("Documento Nacional de Identidad");
        assertThat(tipoDocumento.max()).isEqualTo(8);
        assertThat(tipoDocumento.min()).isEqualTo(8);
        assertThat(tipoDocumento.estado()).isEqualTo(EstadoTipoDocumentoIdentidad.ACTIVO);
    }

    @Test
    void allowsNullMaxAndMinForUnboundedCodes() {
        var tipoDocumento = TipoDocumentoIdentidad.restore(
                "7", "PAS", "Pasaporte", null, null, EstadoTipoDocumentoIdentidad.ACTIVO);

        assertThat(tipoDocumento.max()).isNull();
        assertThat(tipoDocumento.min()).isNull();
    }
}
```

- [ ] **Step 2: Ejecutar el test y verificar que falla por compilación**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.soporte.TipoDocumentoIdentidadTest"`
Expected: FAIL — error de compilación.

- [ ] **Step 3: Crear el enum `EstadoTipoDocumentoIdentidad`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/soporte/EstadoTipoDocumentoIdentidad.java`:

```java
package com.softprimesolutions.catalogo.domain.model.soporte;

public enum EstadoTipoDocumentoIdentidad {
    ACTIVO,
    INACTIVO
}
```

- [ ] **Step 4: Crear el agregado `TipoDocumentoIdentidad`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/soporte/TipoDocumentoIdentidad.java`:

```java
package com.softprimesolutions.catalogo.domain.model.soporte;

/**
 * Catálogo SUNAT 06 (código de tipo de documento de identidad). Catálogo global de referencia,
 * compartido por todos los tenants, con 13 filas semilla fijas — no admite creación ni eliminación
 * vía API, solo activar/desactivar el uso local de un código mediante {@link #estado()}.
 */
public final class TipoDocumentoIdentidad {

    private final String codigo;
    private final String sigla;
    private final String denominacion;
    private final Integer max;
    private final Integer min;
    private final EstadoTipoDocumentoIdentidad estado;

    private TipoDocumentoIdentidad(
            String codigo, String sigla, String denominacion, Integer max, Integer min,
            EstadoTipoDocumentoIdentidad estado) {
        this.codigo = codigo;
        this.sigla = sigla;
        this.denominacion = denominacion;
        this.max = max;
        this.min = min;
        this.estado = estado;
    }

    public static TipoDocumentoIdentidad restore(
            String codigo, String sigla, String denominacion, Integer max, Integer min,
            EstadoTipoDocumentoIdentidad estado) {
        return new TipoDocumentoIdentidad(codigo, sigla, denominacion, max, min, estado);
    }

    public String codigo() { return codigo; }
    public String sigla() { return sigla; }
    public String denominacion() { return denominacion; }
    public Integer max() { return max; }
    public Integer min() { return min; }
    public EstadoTipoDocumentoIdentidad estado() { return estado; }
}
```

- [ ] **Step 5: Ejecutar el test de dominio y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.soporte.TipoDocumentoIdentidadTest"`
Expected: BUILD SUCCESSFUL, 2 tests passed.

- [ ] **Step 6: Escribir el test unitario del handler de consulta (falla primero)**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarTipoDocumentoIdentidadHandlerTest.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarTipoDocumentoIdentidadQuery;
import com.softprimesolutions.catalogo.application.port.out.TipoDocumentoIdentidadPort;
import com.softprimesolutions.catalogo.domain.model.soporte.EstadoTipoDocumentoIdentidad;
import com.softprimesolutions.catalogo.domain.model.soporte.TipoDocumentoIdentidad;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConsultarTipoDocumentoIdentidadHandlerTest {

    @Test
    void returnsTipoDocumentoIdentidadWhenFound() {
        var tipoDocumento = TipoDocumentoIdentidad.restore(
                "1", "DNI", "Documento Nacional de Identidad", 8, 8, EstadoTipoDocumentoIdentidad.ACTIVO);
        var port = new StubTipoDocumentoIdentidadPort(Optional.of(tipoDocumento));
        var handler = new ConsultarTipoDocumentoIdentidadHandler(port);

        var result = handler.execute(new ConsultarTipoDocumentoIdentidadQuery("1"));

        assertThat(result.isSuccess()).isTrue();
        result.fold(
                success -> {
                    assertThat(success.codigo()).isEqualTo("1");
                    assertThat(success.sigla()).isEqualTo("DNI");
                    return null;
                },
                error -> {
                    throw new AssertionError("expected success but got " + error);
                });
    }

    @Test
    void returnsNotFoundWhenMissing() {
        var port = new StubTipoDocumentoIdentidadPort(Optional.empty());
        var handler = new ConsultarTipoDocumentoIdentidadHandler(port);

        var result = handler.execute(new ConsultarTipoDocumentoIdentidadQuery("Z"));

        assertThat(result.isFailure()).isTrue();
        result.fold(
                success -> {
                    throw new AssertionError("expected failure but got " + success);
                },
                error -> {
                    assertThat(error.code()).isEqualTo("CAT_TIPO_DOCUMENTO_IDENTIDAD_NO_ENCONTRADO");
                    assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
                    return null;
                });
    }

    private record StubTipoDocumentoIdentidadPort(Optional<TipoDocumentoIdentidad> tipoDocumento)
            implements TipoDocumentoIdentidadPort {

        @Override
        public Optional<TipoDocumentoIdentidad> findByCodigo(String codigo) {
            return tipoDocumento;
        }

        @Override
        public List<TipoDocumentoIdentidad> findAll(String estado) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeStatus(String codigo, String status) {
            throw new UnsupportedOperationException();
        }
    }
}
```

- [ ] **Step 7: Ejecutar el test y verificar que falla por compilación**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ConsultarTipoDocumentoIdentidadHandlerTest"`
Expected: FAIL — error de compilación.

- [ ] **Step 8: Crear `TipoDocumentoIdentidadResult`, las queries, el puerto de salida y los puertos de entrada**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/TipoDocumentoIdentidadResult.java`:

```java
package com.softprimesolutions.catalogo.application.dto.result;

public record TipoDocumentoIdentidadResult(
        String codigo, String sigla, String denominacion, Integer max, Integer min, String estado) {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarTipoDocumentoIdentidadQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.shared.application.cqrs.Query;

public record ConsultarTipoDocumentoIdentidadQuery(String codigo) implements Query<TipoDocumentoIdentidadResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ListarTiposDocumentoIdentidadQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.List;

public record ListarTiposDocumentoIdentidadQuery(String estado) implements Query<List<TipoDocumentoIdentidadResult>> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/TipoDocumentoIdentidadPort.java`:

```java
package com.softprimesolutions.catalogo.application.port.out;

import com.softprimesolutions.catalogo.domain.model.soporte.TipoDocumentoIdentidad;
import java.util.List;
import java.util.Optional;

public interface TipoDocumentoIdentidadPort {

    Optional<TipoDocumentoIdentidad> findByCodigo(String codigo);

    List<TipoDocumentoIdentidad> findAll(String estado);

    boolean changeStatus(String codigo, String status);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarTipoDocumentoIdentidadUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarTipoDocumentoIdentidadQuery;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ConsultarTipoDocumentoIdentidadUseCase {
    Result<TipoDocumentoIdentidadResult, ApplicationError> execute(ConsultarTipoDocumentoIdentidadQuery query);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ListarTiposDocumentoIdentidadUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ListarTiposDocumentoIdentidadQuery;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;

@FunctionalInterface
public interface ListarTiposDocumentoIdentidadUseCase {
    Result<List<TipoDocumentoIdentidadResult>, ApplicationError> execute(ListarTiposDocumentoIdentidadQuery query);
}
```

- [ ] **Step 9: Agregar `toResult(TipoDocumentoIdentidad)` a `CatalogoApplicationMapper`**

Editar `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/mapper/CatalogoApplicationMapper.java`. Agregar los imports:

```java
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.catalogo.domain.model.soporte.TipoDocumentoIdentidad;
```

(orden alfabético junto a los demás imports `dto.result.*` y `domain.model.soporte.*`)

Agregar el método, después de `toResult(ClasificacionControlada)` y antes de `toResult(PrincipioActivo)`:

```java
    public static TipoDocumentoIdentidadResult toResult(TipoDocumentoIdentidad tipoDocumento) {
        return new TipoDocumentoIdentidadResult(
                tipoDocumento.codigo(), tipoDocumento.sigla(), tipoDocumento.denominacion(), tipoDocumento.max(),
                tipoDocumento.min(), tipoDocumento.estado().name());
    }
```

- [ ] **Step 10: Crear los handlers**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarTipoDocumentoIdentidadHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarTipoDocumentoIdentidadQuery;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ConsultarTipoDocumentoIdentidadUseCase;
import com.softprimesolutions.catalogo.application.port.out.TipoDocumentoIdentidadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarTipoDocumentoIdentidadHandler implements ConsultarTipoDocumentoIdentidadUseCase {

    private final TipoDocumentoIdentidadPort port;

    public ConsultarTipoDocumentoIdentidadHandler(TipoDocumentoIdentidadPort port) {
        this.port = Objects.requireNonNull(port, "port es obligatorio");
    }

    @Override
    public Result<TipoDocumentoIdentidadResult, ApplicationError> execute(
            ConsultarTipoDocumentoIdentidadQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return port.findByCodigo(query.codigo())
                .map(CatalogoApplicationMapper::toResult)
                .map(Result::<TipoDocumentoIdentidadResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CAT_TIPO_DOCUMENTO_IDENTIDAD_NO_ENCONTRADO", "El tipo de documento de identidad indicado no existe.",
                        ErrorCategory.NOT_FOUND)));
    }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ListarTiposDocumentoIdentidadHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ListarTiposDocumentoIdentidadQuery;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ListarTiposDocumentoIdentidadUseCase;
import com.softprimesolutions.catalogo.application.port.out.TipoDocumentoIdentidadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;
import java.util.Objects;

public final class ListarTiposDocumentoIdentidadHandler implements ListarTiposDocumentoIdentidadUseCase {

    private final TipoDocumentoIdentidadPort port;

    public ListarTiposDocumentoIdentidadHandler(TipoDocumentoIdentidadPort port) {
        this.port = Objects.requireNonNull(port, "port es obligatorio");
    }

    @Override
    public Result<List<TipoDocumentoIdentidadResult>, ApplicationError> execute(
            ListarTiposDocumentoIdentidadQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return Result.success(port.findAll(query.estado()).stream()
                .map(CatalogoApplicationMapper::toResult)
                .toList());
    }
}
```

- [ ] **Step 11: Ejecutar los tests unitarios y verificar que pasan**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ConsultarTipoDocumentoIdentidadHandlerTest"`
Expected: BUILD SUCCESSFUL, 2 tests passed.

- [ ] **Step 12: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/soporte/EstadoTipoDocumentoIdentidad.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/soporte/TipoDocumentoIdentidad.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/TipoDocumentoIdentidadResult.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarTipoDocumentoIdentidadQuery.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ListarTiposDocumentoIdentidadQuery.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarTipoDocumentoIdentidadUseCase.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ListarTiposDocumentoIdentidadUseCase.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/TipoDocumentoIdentidadPort.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarTipoDocumentoIdentidadHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ListarTiposDocumentoIdentidadHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/mapper/CatalogoApplicationMapper.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/soporte/TipoDocumentoIdentidadTest.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarTipoDocumentoIdentidadHandlerTest.java
git commit -m "feat(catalogo): agregar dominio y aplicacion de TipoDocumentoIdentidad"
```

---

## Tarea 14: Infraestructura de `TipoDocumentoIdentidad` (JPA + JDBC), `changeTipoDocumentoIdentidadStatus` y wiring

Construye la entidad JPA (solo para leer/actualizar `es_activo` — no hay `save` de creación porque no hay endpoint `POST`), el repositorio, el adaptador de escritura que solo implementa `changeStatus`/`findByCodigo`/`findAll` (usando SQL directo vía `JdbcClient` para `findAll` porque es una lista completa sin filtros complejos, igual que `CatalogoSoporteJpaWriteAdapter` reutiliza JPA para todo lo demás — aquí se usa JPA para `findByCodigo`/`changeStatus` y una consulta JDBC simple para `findAll`), agrega `changeTipoDocumentoIdentidadStatus` a `CatalogoControlUseCase`/`CatalogoControlService`, y cablea los 3 beans en `CatalogoModuleConfiguration`.

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/TipoDocumentoIdentidadJpaEntity.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/TipoDocumentoIdentidadJpaRepository.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/TipoDocumentoIdentidadJpaWriteAdapter.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/CatalogoControlUseCase.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlService.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlServiceTest.java` (modificar — agregar caso para `changeTipoDocumentoIdentidadStatus`)

**Interfaces:**
- Consumes: `TipoDocumentoIdentidad`, `EstadoTipoDocumentoIdentidad`, `TipoDocumentoIdentidadPort` (Tarea 13).
- Produces: `TipoDocumentoIdentidadJpaEntity` (mapeada a `sch_catalogo.tipo_documento_identidad`, PK `codigo`); `TipoDocumentoIdentidadJpaRepository extends JpaRepository<TipoDocumentoIdentidadJpaEntity, String>`; `TipoDocumentoIdentidadJpaWriteAdapter implements TipoDocumentoIdentidadPort`; `CatalogoControlUseCase.changeTipoDocumentoIdentidadStatus(String codigo, String status) -> Result<Unit, ApplicationError>`.

- [ ] **Step 1: Crear la entidad JPA `TipoDocumentoIdentidadJpaEntity`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/TipoDocumentoIdentidadJpaEntity.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_documento_identidad", schema = "sch_catalogo")
public class TipoDocumentoIdentidadJpaEntity {

    @Id
    @Column(length = 2)
    private String codigo;

    @Column(nullable = false, length = 30)
    private String sigla;

    @Column(nullable = false, length = 200)
    private String denominacion;

    @Column(name = "max")
    private Short max;

    @Column(name = "min")
    private Short min;

    @Column(name = "es_activo", nullable = false, length = 1)
    private String esActivo;

    protected TipoDocumentoIdentidadJpaEntity() {
    }

    public String getCodigo() { return codigo; }
    public String getSigla() { return sigla; }
    public String getDenominacion() { return denominacion; }
    public Short getMax() { return max; }
    public Short getMin() { return min; }
    public String getEsActivo() { return esActivo; }
}
```

Nota: esta entidad no tiene constructor con parámetros ni se usa con `save()`/`saveAndFlush()` porque la API no expone creación — solo se lee vía `findById` y se actualiza `es_activo` vía SQL directo (`JdbcClient`), igual que el resto de columnas `estado`/`es_activo` en los demás adaptadores de este módulo.

- [ ] **Step 2: Crear el repositorio `TipoDocumentoIdentidadJpaRepository`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/TipoDocumentoIdentidadJpaRepository.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.TipoDocumentoIdentidadJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoDocumentoIdentidadJpaRepository
        extends JpaRepository<TipoDocumentoIdentidadJpaEntity, String> {
}
```

- [ ] **Step 3: Crear el adaptador `TipoDocumentoIdentidadJpaWriteAdapter`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/TipoDocumentoIdentidadJpaWriteAdapter.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.adapter;

import com.softprimesolutions.catalogo.application.port.out.TipoDocumentoIdentidadPort;
import com.softprimesolutions.catalogo.domain.model.soporte.EstadoTipoDocumentoIdentidad;
import com.softprimesolutions.catalogo.domain.model.soporte.TipoDocumentoIdentidad;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.TipoDocumentoIdentidadJpaRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class TipoDocumentoIdentidadJpaWriteAdapter implements TipoDocumentoIdentidadPort {

    private final TipoDocumentoIdentidadJpaRepository repository;
    private final JdbcClient jdbcClient;

    public TipoDocumentoIdentidadJpaWriteAdapter(
            TipoDocumentoIdentidadJpaRepository repository, JdbcClient jdbcClient) {
        this.repository = repository;
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TipoDocumentoIdentidad> findByCodigo(String codigo) {
        return repository.findById(codigo).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TipoDocumentoIdentidad> findAll(String estado) {
        var filter = estado == null ? "" : estado.trim().toUpperCase(Locale.ROOT);
        return jdbcClient.sql("""
                        SELECT codigo, sigla, denominacion, max, min, es_activo
                          FROM sch_catalogo.tipo_documento_identidad
                         WHERE :estado = ''
                            OR (:estado = 'ACTIVO' AND es_activo = '1')
                            OR (:estado = 'INACTIVO' AND es_activo = '0')
                         ORDER BY codigo
                        """)
                .param("estado", filter)
                .query((rs, rowNumber) -> new TipoDocumentoIdentidad[]{TipoDocumentoIdentidad.restore(
                        rs.getString("codigo"), rs.getString("sigla"), rs.getString("denominacion"),
                        (Integer) rs.getObject("max"), (Integer) rs.getObject("min"),
                        "1".equals(rs.getString("es_activo"))
                                ? EstadoTipoDocumentoIdentidad.ACTIVO
                                : EstadoTipoDocumentoIdentidad.INACTIVO)}[0])
                .list();
    }

    @Override
    @Transactional
    public boolean changeStatus(String codigo, String status) {
        var esActivo = "ACTIVO".equals(status) ? "1" : "0";
        return jdbcClient.sql(
                        "UPDATE sch_catalogo.tipo_documento_identidad SET es_activo = :esActivo WHERE codigo = :codigo")
                .param("esActivo", esActivo)
                .param("codigo", codigo)
                .update() == 1;
    }

    private TipoDocumentoIdentidad toDomain(
            com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.TipoDocumentoIdentidadJpaEntity entity) {
        return TipoDocumentoIdentidad.restore(
                entity.getCodigo(), entity.getSigla(), entity.getDenominacion(),
                entity.getMax() == null ? null : entity.getMax().intValue(),
                entity.getMin() == null ? null : entity.getMin().intValue(),
                "1".equals(entity.getEsActivo())
                        ? EstadoTipoDocumentoIdentidad.ACTIVO
                        : EstadoTipoDocumentoIdentidad.INACTIVO);
    }
}
```

- [ ] **Step 4: Agregar `changeTipoDocumentoIdentidadStatus` a `CatalogoControlUseCase`**

Editar `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/CatalogoControlUseCase.java` para que quede:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.shared.kernel.result.Unit;
import java.util.UUID;

public interface CatalogoControlUseCase {

    Result<Unit, ApplicationError> changeCondicionVentaStatus(String codigo, String status);

    Result<Unit, ApplicationError> changeFormaFarmaceuticaStatus(String codigo, String status);

    Result<Unit, ApplicationError> changeViaAdministracionStatus(String codigo, String status);

    Result<Unit, ApplicationError> changeUnidadMedidaStatus(String codigo, String status);

    Result<Unit, ApplicationError> changeClasificacionControladaStatus(String codigo, String status);

    Result<Unit, ApplicationError> changePrincipioActivoStatus(UUID principioActivoId, String status);

    Result<Unit, ApplicationError> changeMarcaStatus(UUID tenantId, UUID marcaId, String status);

    Result<Unit, ApplicationError> changeCategoriaProductoStatus(UUID tenantId, UUID categoriaId, String status);

    Result<Unit, ApplicationError> changeProductoReguladoStatus(UUID productoReguladoId, String status);

    Result<Unit, ApplicationError> changeSkuStatus(UUID tenantId, UUID skuId, String status);

    Result<Unit, ApplicationError> changeRubroComercialStatus(UUID tenantId, UUID rubroComercialId, String status);

    Result<Unit, ApplicationError> changeTipoDocumentoIdentidadStatus(String codigo, String status);
}
```

- [ ] **Step 5: Implementar `changeTipoDocumentoIdentidadStatus` en `CatalogoControlService`**

Editar `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlService.java`. Agregar el import:

```java
import com.softprimesolutions.catalogo.application.port.out.TipoDocumentoIdentidadPort;
```

Agregar un campo `tipoDocumentoIdentidadPort` y actualizar el constructor (pasa de 5 a 6 parámetros):

```java
    private final CatalogoSoportePort soportePort;
    private final CatalogoComercialPort comercialPort;
    private final ProductoReguladoPort productoReguladoPort;
    private final RubroComercialPort rubroComercialPort;
    private final TipoDocumentoIdentidadPort tipoDocumentoIdentidadPort;
    private final ClockPort clock;

    public CatalogoControlService(
            CatalogoSoportePort soportePort, CatalogoComercialPort comercialPort,
            ProductoReguladoPort productoReguladoPort, RubroComercialPort rubroComercialPort,
            TipoDocumentoIdentidadPort tipoDocumentoIdentidadPort, ClockPort clock) {
        this.soportePort = Objects.requireNonNull(soportePort, "soportePort es obligatorio");
        this.comercialPort = Objects.requireNonNull(comercialPort, "comercialPort es obligatorio");
        this.productoReguladoPort = Objects.requireNonNull(productoReguladoPort, "productoReguladoPort es obligatorio");
        this.rubroComercialPort = Objects.requireNonNull(rubroComercialPort, "rubroComercialPort es obligatorio");
        this.tipoDocumentoIdentidadPort = Objects.requireNonNull(
                tipoDocumentoIdentidadPort, "tipoDocumentoIdentidadPort es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }
```

Agregar el método, después de `changeRubroComercialStatus` y antes de `normalizeStatus`:

```java
    @Override
    public Result<Unit, ApplicationError> changeTipoDocumentoIdentidadStatus(String codigo, String status) {
        var normalized = normalizeStatus(status);
        if (!SOPORTE_STATUSES.contains(normalized)) return invalidStatus(SOPORTE_STATUSES);
        return tipoDocumentoIdentidadPort.changeStatus(codigo, normalized)
                ? Result.success(Unit.INSTANCE)
                : notFound(
                        "CAT_TIPO_DOCUMENTO_IDENTIDAD_NO_ENCONTRADO",
                        "El tipo de documento de identidad indicado no existe.");
    }
```

(reutiliza `SOPORTE_STATUSES = Set.of("ACTIVO", "INACTIVO")`, ya existente en la clase; `tipoDocumentoIdentidadPort.changeStatus` no toma `Instant changedAt` porque `TipoDocumentoIdentidadPort.changeStatus` no lo declara — a diferencia de los demás puertos, ver Tarea 13 Step 8).

- [ ] **Step 6: Actualizar `CatalogoControlServiceTest` con el quinto y sexto argumento del constructor y el fake `TipoDocumentoIdentidadPort`**

Reescribir `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlServiceTest.java` completo (toma como base la versión de la Tarea 12, Step 3, y le agrega el sexto argumento del constructor `FakeTipoDocumentoIdentidadPort` en las 6 invocaciones existentes más los dos tests nuevos):

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.application.port.out.ProductoReguladoPort;
import com.softprimesolutions.catalogo.application.port.out.RubroComercialPort;
import com.softprimesolutions.catalogo.application.port.out.TipoDocumentoIdentidadPort;
import com.softprimesolutions.catalogo.domain.model.CategoriaProducto;
import com.softprimesolutions.catalogo.domain.model.Marca;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.ProductoRegulado;
import com.softprimesolutions.catalogo.domain.model.RubroComercial;
import com.softprimesolutions.catalogo.domain.model.SKUComercial;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.TipoDocumentoIdentidad;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogoControlServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("172e0f26-a765-46f3-841c-4a11407ccf5b");

    @Test
    void rejectsAnInvalidStatus() {
        var soportePort = new FakeCatalogoSoportePort(true);
        var service = new CatalogoControlService(
                soportePort, new FakeCatalogoComercialPort(true), new FakeProductoReguladoPort(true),
                new FakeRubroComercialPort(true), new FakeTipoDocumentoIdentidadPort(true),
                () -> Instant.parse("2026-09-07T12:00:00Z"));

        var result = service.changeCondicionVentaStatus("SIN-RECETA", "ELIMINADO");

        assertTrue(result.isFailure());
        assertEquals("CAT_ESTADO_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void changesCondicionVentaStatusWithAValidValue() {
        var soportePort = new FakeCatalogoSoportePort(true);
        var service = new CatalogoControlService(
                soportePort, new FakeCatalogoComercialPort(true), new FakeProductoReguladoPort(true),
                new FakeRubroComercialPort(true), new FakeTipoDocumentoIdentidadPort(true),
                () -> Instant.parse("2026-09-07T12:00:00Z"));

        var result = service.changeCondicionVentaStatus("SIN-RECETA", "INACTIVO");

        assertTrue(result.isSuccess());
    }

    @Test
    void reportsNotFoundWhenMarcaDoesNotExist() {
        var service = new CatalogoControlService(
                new FakeCatalogoSoportePort(true), new FakeCatalogoComercialPort(false),
                new FakeProductoReguladoPort(true), new FakeRubroComercialPort(true),
                new FakeTipoDocumentoIdentidadPort(true), () -> Instant.parse("2026-09-07T12:00:00Z"));

        var result = service.changeMarcaStatus(TENANT_ID, UUID.randomUUID(), "ACTIVO");

        assertTrue(result.isFailure());
        assertEquals("CAT_MARCA_NO_ENCONTRADA", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void changesProductoReguladoStatusSuccessfully() {
        var service = new CatalogoControlService(
                new FakeCatalogoSoportePort(true), new FakeCatalogoComercialPort(true),
                new FakeProductoReguladoPort(true), new FakeRubroComercialPort(true),
                new FakeTipoDocumentoIdentidadPort(true), () -> Instant.parse("2026-09-07T12:00:00Z"));

        var result = service.changeProductoReguladoStatus(UUID.randomUUID(), "VIGENTE");

        assertTrue(result.isSuccess());
    }

    @Test
    void changesRubroComercialStatusWhenValid() {
        var service = new CatalogoControlService(
                new FakeCatalogoSoportePort(true), new FakeCatalogoComercialPort(true),
                new FakeProductoReguladoPort(true), new FakeRubroComercialPort(true),
                new FakeTipoDocumentoIdentidadPort(true), () -> Instant.parse("2026-09-07T12:00:00Z"));

        var result = service.changeRubroComercialStatus(TENANT_ID, UUID.randomUUID(), "INACTIVO");

        assertTrue(result.isSuccess());
    }

    @Test
    void reportsNotFoundWhenRubroComercialDoesNotExist() {
        var service = new CatalogoControlService(
                new FakeCatalogoSoportePort(true), new FakeCatalogoComercialPort(true),
                new FakeProductoReguladoPort(true), new FakeRubroComercialPort(false),
                new FakeTipoDocumentoIdentidadPort(true), () -> Instant.parse("2026-09-07T12:00:00Z"));

        var result = service.changeRubroComercialStatus(TENANT_ID, UUID.randomUUID(), "ACTIVO");

        assertTrue(result.isFailure());
        assertEquals("CAT_RUBRO_COMERCIAL_NO_ENCONTRADO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void changesTipoDocumentoIdentidadStatusWhenValid() {
        var service = new CatalogoControlService(
                new FakeCatalogoSoportePort(true), new FakeCatalogoComercialPort(true),
                new FakeProductoReguladoPort(true), new FakeRubroComercialPort(true),
                new FakeTipoDocumentoIdentidadPort(true), () -> Instant.parse("2026-09-07T12:00:00Z"));

        var result = service.changeTipoDocumentoIdentidadStatus("1", "INACTIVO");

        assertTrue(result.isSuccess());
    }

    @Test
    void reportsNotFoundWhenTipoDocumentoIdentidadDoesNotExist() {
        var service = new CatalogoControlService(
                new FakeCatalogoSoportePort(true), new FakeCatalogoComercialPort(true),
                new FakeProductoReguladoPort(true), new FakeRubroComercialPort(true),
                new FakeTipoDocumentoIdentidadPort(false), () -> Instant.parse("2026-09-07T12:00:00Z"));

        var result = service.changeTipoDocumentoIdentidadStatus("Z", "ACTIVO");

        assertTrue(result.isFailure());
        assertEquals(
                "CAT_TIPO_DOCUMENTO_IDENTIDAD_NO_ENCONTRADO", result.fold(value -> null, error -> error.code()));
    }

    private static final class FakeCatalogoSoportePort implements CatalogoSoportePort {
        private final boolean found;

        private FakeCatalogoSoportePort(boolean found) { this.found = found; }

        @Override
        public SaveOutcome save(CondicionVenta condicionVenta) { throw new UnsupportedOperationException(); }

        @Override
        public SaveOutcome save(FormaFarmaceutica formaFarmaceutica) { throw new UnsupportedOperationException(); }

        @Override
        public SaveOutcome save(ViaAdministracion viaAdministracion) { throw new UnsupportedOperationException(); }

        @Override
        public SaveOutcome save(UnidadMedida unidadMedida) { throw new UnsupportedOperationException(); }

        @Override
        public SaveOutcome save(ClasificacionControlada clasificacionControlada) { throw new UnsupportedOperationException(); }

        @Override
        public SavePrincipioActivoOutcome save(PrincipioActivo principioActivo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<CondicionVenta> findCondicionVentaByCodigo(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<FormaFarmaceutica> findFormaFarmaceuticaByCodigo(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<ViaAdministracion> findViaAdministracionByCodigo(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<UnidadMedida> findUnidadMedidaByCodigo(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<ClasificacionControlada> findClasificacionControladaByCodigo(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<PrincipioActivo> findPrincipioActivoById(UUID principioActivoId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean condicionVentaExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean formaFarmaceuticaExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean viaAdministracionExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean unidadMedidaExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean clasificacionControladaExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean principioActivoExists(UUID principioActivoId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeCondicionVentaStatus(String codigo, String status, Instant changedAt) { return found; }

        @Override
        public boolean changeFormaFarmaceuticaStatus(String codigo, String status, Instant changedAt) { return found; }

        @Override
        public boolean changeViaAdministracionStatus(String codigo, String status, Instant changedAt) { return found; }

        @Override
        public boolean changeUnidadMedidaStatus(String codigo, String status, Instant changedAt) { return found; }

        @Override
        public boolean changeClasificacionControladaStatus(String codigo, String status, Instant changedAt) { return found; }

        @Override
        public boolean changePrincipioActivoStatus(UUID principioActivoId, String status, Instant changedAt) { return found; }
    }

    private static final class FakeCatalogoComercialPort implements CatalogoComercialPort {
        private final boolean found;

        private FakeCatalogoComercialPort(boolean found) { this.found = found; }

        @Override
        public SaveMarcaOutcome save(Marca marca) { throw new UnsupportedOperationException(); }

        @Override
        public SaveCategoriaOutcome save(CategoriaProducto categoria) { throw new UnsupportedOperationException(); }

        @Override
        public SaveSkuOutcome save(SKUComercial sku) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<SKUComercial> findSkuById(UUID tenantId, UUID skuId) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<CategoriaProducto> findCategoriaById(UUID tenantId, UUID categoriaId) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<Marca> findMarcaById(UUID tenantId, UUID marcaId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean categoriaExists(UUID tenantId, UUID categoriaId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean marcaExists(UUID tenantId, UUID marcaId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeMarcaStatus(UUID tenantId, UUID marcaId, String status, Instant changedAt) { return found; }

        @Override
        public boolean changeCategoriaStatus(UUID tenantId, UUID categoriaId, String status, Instant changedAt) { return found; }

        @Override
        public boolean changeSkuStatus(UUID tenantId, UUID skuId, String status, Instant changedAt) { return found; }
    }

    private static final class FakeProductoReguladoPort implements ProductoReguladoPort {
        private final boolean found;

        private FakeProductoReguladoPort(boolean found) { this.found = found; }

        @Override
        public SaveOutcome save(ProductoRegulado productoRegulado) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<ProductoRegulado> findById(UUID productoReguladoId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeStatus(UUID productoReguladoId, String status, Instant changedAt) { return found; }
    }

    private static final class FakeRubroComercialPort implements RubroComercialPort {
        private final boolean found;

        private FakeRubroComercialPort(boolean found) { this.found = found; }

        @Override
        public SaveOutcome save(RubroComercial rubroComercial) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<RubroComercial> findById(UUID tenantId, UUID rubroComercialId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeStatus(UUID tenantId, UUID rubroComercialId, String status, Instant changedAt) { return found; }
    }

    private static final class FakeTipoDocumentoIdentidadPort implements TipoDocumentoIdentidadPort {
        private final boolean found;

        private FakeTipoDocumentoIdentidadPort(boolean found) { this.found = found; }

        @Override
        public Optional<TipoDocumentoIdentidad> findByCodigo(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public List<TipoDocumentoIdentidad> findAll(String estado) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeStatus(String codigo, String status) { return found; }
    }
}
```

- [ ] **Step 7: Ejecutar los tests del módulo y verificar que compilan y pasan**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.command.CatalogoControlServiceTest"`
Expected: BUILD SUCCESSFUL, 8 tests passed.

- [ ] **Step 8: Cablear los 3 beans de `TipoDocumentoIdentidad` en `CatalogoModuleConfiguration` y actualizar `catalogoControlUseCase`**

Editar `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java`. Agregar los imports:

```java
import com.softprimesolutions.catalogo.application.port.in.ConsultarTipoDocumentoIdentidadUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarTiposDocumentoIdentidadUseCase;
import com.softprimesolutions.catalogo.application.port.out.TipoDocumentoIdentidadPort;
import com.softprimesolutions.catalogo.application.usecase.query.ConsultarTipoDocumentoIdentidadHandler;
import com.softprimesolutions.catalogo.application.usecase.query.ListarTiposDocumentoIdentidadHandler;
```

Agregar una nueva sección `// ---- Tipos de documento de identidad ----` al final, justo antes de `// ---- Control de estado transversal ----`:

```java
    // ---- Tipos de documento de identidad ----

    @Bean
    ConsultarTipoDocumentoIdentidadUseCase consultarTipoDocumentoIdentidadUseCase(
            TipoDocumentoIdentidadPort tipoDocumentoIdentidadPort) {
        return new ConsultarTipoDocumentoIdentidadHandler(tipoDocumentoIdentidadPort);
    }

    @Bean
    ListarTiposDocumentoIdentidadUseCase listarTiposDocumentoIdentidadUseCase(
            TipoDocumentoIdentidadPort tipoDocumentoIdentidadPort) {
        return new ListarTiposDocumentoIdentidadHandler(tipoDocumentoIdentidadPort);
    }
```

Actualizar el bean `catalogoControlUseCase` (sección `// ---- Control de estado transversal ----`) de:

```java
    @Bean
    CatalogoControlUseCase catalogoControlUseCase(
            CatalogoSoportePort soportePort, CatalogoComercialPort comercialPort,
            ProductoReguladoPort productoReguladoPort, RubroComercialPort rubroComercialPort,
            ClockPort catalogoClockPort) {
        return new CatalogoControlService(
                soportePort, comercialPort, productoReguladoPort, rubroComercialPort, catalogoClockPort);
    }
```

a:

```java
    @Bean
    CatalogoControlUseCase catalogoControlUseCase(
            CatalogoSoportePort soportePort, CatalogoComercialPort comercialPort,
            ProductoReguladoPort productoReguladoPort, RubroComercialPort rubroComercialPort,
            TipoDocumentoIdentidadPort tipoDocumentoIdentidadPort, ClockPort catalogoClockPort) {
        return new CatalogoControlService(
                soportePort, comercialPort, productoReguladoPort, rubroComercialPort, tipoDocumentoIdentidadPort,
                catalogoClockPort);
    }
```

- [ ] **Step 9: Compilar el módulo completo y correr todos los tests unitarios**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL.

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 10: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/TipoDocumentoIdentidadJpaEntity.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/TipoDocumentoIdentidadJpaRepository.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/TipoDocumentoIdentidadJpaWriteAdapter.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/CatalogoControlUseCase.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlService.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/configuration/CatalogoModuleConfiguration.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlServiceTest.java
git commit -m "feat(catalogo): agregar infraestructura y control de estado de TipoDocumentoIdentidad"
```

---

## Tarea 15: API REST de `TipoDocumentoIdentidad` (DTOs, `CatalogoApiMapper`, controller)

Cierra el Part C. Endpoints: `GET /api/v1/catalogo/tipos-documento-identidad/{codigo}`, `GET /api/v1/catalogo/tipos-documento-identidad` (lista sin paginar, filtro opcional `estado`), `PATCH /api/v1/catalogo/tipos-documento-identidad/{codigo}/estado`. NO hay `POST` ni `DELETE` (catálogo SUNAT de referencia, 13 filas fijas ya sembradas por V018).

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/response/TipoDocumentoIdentidadResponse.java`
- Modify: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/mapper/CatalogoApiMapper.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/TipoDocumentoIdentidadController.java`

**Interfaces:**
- Consumes: `ConsultarTipoDocumentoIdentidadUseCase`/`ListarTiposDocumentoIdentidadUseCase` (Tarea 13); `CatalogoControlUseCase.changeTipoDocumentoIdentidadStatus` (Tarea 14); `CambiarEstadoGlobalRequest` (existente, mismo DTO que usa `CondicionVenta`/`FormaFarmaceutica`, sin `tenantId` porque el catálogo es global).
- Produces: `TipoDocumentoIdentidadResponse(String codigo, String sigla, String denominacion, Integer max, Integer min, String estado)`; endpoints REST bajo `/api/v1/catalogo/tipos-documento-identidad`.

- [ ] **Step 1: Crear `TipoDocumentoIdentidadResponse`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/response/TipoDocumentoIdentidadResponse.java`:

```java
package com.softprimesolutions.catalogo.api.dto.response;

public record TipoDocumentoIdentidadResponse(
        String codigo, String sigla, String denominacion, Integer max, Integer min, String estado) {
}
```

- [ ] **Step 2: Agregar `toResponse(TipoDocumentoIdentidadResult)` a `CatalogoApiMapper`**

Editar `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/mapper/CatalogoApiMapper.java`. Agregar los imports:

```java
import com.softprimesolutions.catalogo.api.dto.response.TipoDocumentoIdentidadResponse;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
```

(orden alfabético junto a los demás imports `api.dto.response.*` y `application.dto.result.*`)

Agregar el método, después de `toRubroComercialPage` (Tarea 12) y antes de `toCreateCommand(ProductoReguladoRequest)`:

```java
    public static TipoDocumentoIdentidadResponse toResponse(TipoDocumentoIdentidadResult result) {
        return new TipoDocumentoIdentidadResponse(
                result.codigo(), result.sigla(), result.denominacion(), result.max(), result.min(),
                result.estado());
    }
```

- [ ] **Step 3: Crear `TipoDocumentoIdentidadController`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/TipoDocumentoIdentidadController.java`:

```java
package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoGlobalRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.query.ConsultarTipoDocumentoIdentidadQuery;
import com.softprimesolutions.catalogo.application.dto.query.ListarTiposDocumentoIdentidadQuery;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.ConsultarTipoDocumentoIdentidadUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarTiposDocumentoIdentidadUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/catalogo/tipos-documento-identidad")
public class TipoDocumentoIdentidadController {

    private final ConsultarTipoDocumentoIdentidadUseCase getTipoDocumentoIdentidad;
    private final ListarTiposDocumentoIdentidadUseCase listTiposDocumentoIdentidad;
    private final CatalogoControlUseCase control;

    public TipoDocumentoIdentidadController(
            ConsultarTipoDocumentoIdentidadUseCase getTipoDocumentoIdentidad,
            ListarTiposDocumentoIdentidadUseCase listTiposDocumentoIdentidad,
            CatalogoControlUseCase control) {
        this.getTipoDocumentoIdentidad = getTipoDocumentoIdentidad;
        this.listTiposDocumentoIdentidad = listTiposDocumentoIdentidad;
        this.control = control;
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("hasAuthority('catalogo.tipos-documento-identidad.consultar')")
    public ResponseEntity<?> get(@PathVariable String codigo) {
        return getTipoDocumentoIdentidad.execute(new ConsultarTipoDocumentoIdentidadQuery(codigo)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.tipos-documento-identidad.consultar')")
    public ResponseEntity<?> list(@RequestParam(required = false) String estado) {
        return listTiposDocumentoIdentidad.execute(new ListarTiposDocumentoIdentidadQuery(estado)).fold(
                result -> ResponseEntity.ok(result.stream().map(CatalogoApiMapper::toResponse).toList()),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{codigo}/estado")
    @PreAuthorize("hasAuthority('catalogo.tipos-documento-identidad.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable String codigo, @Valid @RequestBody CambiarEstadoGlobalRequest request) {
        return control.changeTipoDocumentoIdentidadStatus(codigo, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), CatalogoControllerSupport::problem);
    }
}
```

Nota: el `PATCH /{codigo}/estado` exige `catalogo.tipos-documento-identidad.gestionar` (no `.consultar`) porque cambia estado, siguiendo el mismo criterio que separa `consultar`/`gestionar` en todos los demás catálogos de este módulo — aunque el catálogo en sí no admite POST/DELETE, sí admite esta única operación de escritura (activar/desactivar localmente un código).

- [ ] **Step 4: Compilar el módulo completo**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL.

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test`
Expected: BUILD SUCCESSFUL (todos los tests unitarios del módulo pasan, incluidos todos los agregados en las Tareas 1 a 15).

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/response/TipoDocumentoIdentidadResponse.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/mapper/CatalogoApiMapper.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/TipoDocumentoIdentidadController.java
git commit -m "feat(catalogo): agregar API REST de TipoDocumentoIdentidad"
```

---

Con esto se completa el Part C (`TipoDocumentoIdentidad` completo — Tareas 13 a 15).

---

## Tarea 16: Migración Flyway V022 — sembrar permisos de `RubroComercial` y `TipoDocumentoIdentidad`

La última migración existente en `service-botica/bootstrap-app/src/main/resources/db/migration/` es `V021__seed_catalogo_permissions.sql` (siembra 12 permisos bajo el módulo `CATALOGO`, ver `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/db/MigrationV022Test.java` — este test YA EXISTE en el repo y ya espera exactamente los 12 permisos de V021, por lo que NO debe modificarse; confirma que la numeración de la migración de esta tarea debe ser V022 porque el nombre de ese test ya reserva ese número).

Los controllers de `RubroComercial` (Tarea 12) y `TipoDocumentoIdentidad` (Tarea 15) usan las authorities `catalogo.rubros-comerciales.consultar`, `catalogo.rubros-comerciales.gestionar`, `catalogo.tipos-documento-identidad.consultar`, `catalogo.tipos-documento-identidad.gestionar` — siguiendo el mismo patrón kebab-case que el resto de authorities `catalogo.*` sembradas en V021 (`catalogo.marcas.consultar`, `catalogo.categorias.gestionar`, etc.).

**Files:**
- Create: `service-botica/bootstrap-app/src/main/resources/db/migration/V022__seed_catalogo_rubro_tipo_documento_permissions.sql`
- Test: `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/db/MigrationV023Test.java`

**Interfaces:**
- Consumes: tabla `sch_seguridad.permiso` y `sch_seguridad.modulo_sistema` (ya existen, módulo `CATALOGO` ya sembrado por V021 — ver `service-botica/bootstrap-app/src/main/resources/db/migration/V021__seed_catalogo_permissions.sql`).
- Produces: 4 filas nuevas en `sch_seguridad.permiso` con `codigo` = `catalogo.rubros-comerciales.consultar`, `catalogo.rubros-comerciales.gestionar`, `catalogo.tipos-documento-identidad.consultar`, `catalogo.tipos-documento-identidad.gestionar`.

Nota importante de numeración: el archivo de test ya existente en el repo se llama `MigrationV022Test.java` y ya prueba el contenido de `V021__seed_catalogo_permissions.sql` (12 permisos) — es decir, ese test fue nombrado pensando en una migración V022 que en este plan realmente termina siendo la migración de ESTA tarea. Para no confundir esa numeración, esta tarea crea la migración SQL como `V022__seed_catalogo_rubro_tipo_documento_permissions.sql` (siguiente número real disponible tras V021) y un test NUEVO llamado `MigrationV023Test.java` seria incorrecto si V022 ya está tomado por el test existente — **antes de escribir esta tarea, quien la ejecute debe correr** `ls service-botica/bootstrap-app/src/main/resources/db/migration/` **y confirmar cuál es el último número real de migración SQL presente en disco en ese momento** (puede haber cambiado desde que se escribió este plan si otro trabajo se fusionó primero), y nombrar el archivo `V0XX__seed_catalogo_rubro_tipo_documento_permissions.sql` con XX = último número + 1. Este plan asume que V021 sigue siendo la última migración SQL en disco (confirmado por inspección directa al escribir este plan) y por lo tanto usa V022 para el archivo SQL nuevo; el test de verificación de esta tarea se llama `MigrationV022RubroTipoDocumentoTest.java` (no `MigrationV023Test.java`) para evitar cualquier expectativa de numeración secuencial de nombres de clase Java, que no está atada 1:1 al número de migración.

- [ ] **Step 1: Confirmar la última migración SQL real en disco**

Run: `cd service-botica && ls bootstrap-app/src/main/resources/db/migration/`
Expected: el archivo con el número más alto es `V021__seed_catalogo_permissions.sql`. Si aparece un `V022__*.sql` ya existente (agregado por otro trabajo en paralelo), renombrar el archivo de esta tarea al siguiente número disponible y ajustar el nombre del test del Step 3 en consecuencia.

- [ ] **Step 2: Escribir el test de verificación (falla primero)**

Crear `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/db/MigrationV022RubroTipoDocumentoTest.java`:

```java
package com.softprimesolutions.catalogo.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.testsupport.PostgresTestContainerConfiguration;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RecordApplicationEvents
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@SpringBootTest
class MigrationV022RubroTipoDocumentoTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void seedsFourRubroComercialAndTipoDocumentoIdentidadPermissionsUnderTheExistingModule() {
        var jdbc = JdbcClient.create(dataSource);

        var permissionCount = jdbc.sql("""
                        SELECT COUNT(*) FROM sch_seguridad.permiso p
                         JOIN sch_seguridad.modulo_sistema m ON m.id = p.modulo_id
                        WHERE m.codigo = 'CATALOGO'
                          AND p.codigo IN (
                            'catalogo.rubros-comerciales.consultar', 'catalogo.rubros-comerciales.gestionar',
                            'catalogo.tipos-documento-identidad.consultar',
                            'catalogo.tipos-documento-identidad.gestionar'
                          )
                        """)
                .query(Long.class).single();
        assertThat(permissionCount).isEqualTo(4L);
    }

    @Test
    void keepsThePreviousTwelveCatalogoPermissionsIntact() {
        var jdbc = JdbcClient.create(dataSource);

        var permissionCount = jdbc.sql("""
                        SELECT COUNT(*) FROM sch_seguridad.permiso p
                         JOIN sch_seguridad.modulo_sistema m ON m.id = p.modulo_id
                        WHERE m.codigo = 'CATALOGO'
                        """)
                .query(Long.class).single();
        assertThat(permissionCount).isEqualTo(16L);
    }
}
```

- [ ] **Step 3: Ejecutar el test y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.catalogo.db.MigrationV022RubroTipoDocumentoTest"`
Expected: FAIL — `permissionCount` es 0 (la migración todavía no existe) o Flyway falla al no encontrar el archivo si el test referencia algo inexistente; en cualquier caso, el test no pasa antes del Step 4.

- [ ] **Step 4: Crear la migración `V022__seed_catalogo_rubro_tipo_documento_permissions.sql`**

Crear `service-botica/bootstrap-app/src/main/resources/db/migration/V022__seed_catalogo_rubro_tipo_documento_permissions.sql`:

```sql
INSERT INTO sch_seguridad.permiso
    (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
SELECT m.id, seed.codigo, seed.recurso, seed.accion, seed.nombre, seed.descripcion, seed.es_critico, 'ACTIVO'
FROM sch_seguridad.modulo_sistema m
CROSS JOIN (VALUES
    ('catalogo.rubros-comerciales.consultar', 'RUBRO_COMERCIAL', 'CONSULTAR', 'Consultar rubros comerciales',
     'Permite consultar los rubros comerciales (macro-lineas Farma, Bebe, Cosmeticos, Retail) del tenant.', FALSE),
    ('catalogo.rubros-comerciales.gestionar', 'RUBRO_COMERCIAL', 'GESTIONAR', 'Gestionar rubros comerciales',
     'Permite crear, editar y cambiar el estado de rubros comerciales.', TRUE),
    ('catalogo.tipos-documento-identidad.consultar', 'TIPO_DOCUMENTO_IDENTIDAD', 'CONSULTAR',
     'Consultar tipos de documento de identidad',
     'Permite consultar el catalogo SUNAT de tipos de documento de identidad.', FALSE),
    ('catalogo.tipos-documento-identidad.gestionar', 'TIPO_DOCUMENTO_IDENTIDAD', 'GESTIONAR',
     'Gestionar tipos de documento de identidad',
     'Permite activar o desactivar localmente el uso de un codigo del catalogo SUNAT de tipos de documento de identidad.',
     TRUE)
) AS seed(codigo, recurso, accion, nombre, descripcion, es_critico)
WHERE m.codigo = 'CATALOGO'
ON CONFLICT (codigo) WHERE es_activo = '1' DO UPDATE SET
    modulo_id = EXCLUDED.modulo_id,
    recurso = EXCLUDED.recurso,
    accion = EXCLUDED.accion,
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    es_critico = EXCLUDED.es_critico,
    estado = 'ACTIVO';
```

- [ ] **Step 5: Ejecutar el test y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.catalogo.db.MigrationV022RubroTipoDocumentoTest"`
Expected: BUILD SUCCESSFUL, 2 tests passed.

- [ ] **Step 6: Ejecutar también el test de migración existente `MigrationV022Test` para confirmar que sigue pasando (no se tocó V021)**

Run: `cd service-botica && .\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.catalogo.db.MigrationV022Test"`
Expected: BUILD SUCCESSFUL, 1 test passed (los 12 permisos originales de V021 siguen intactos).

- [ ] **Step 7: Commit**

```bash
git add service-botica/bootstrap-app/src/main/resources/db/migration/V022__seed_catalogo_rubro_tipo_documento_permissions.sql service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/db/MigrationV022RubroTipoDocumentoTest.java
git commit -m "feat(catalogo): sembrar permisos de RubroComercial y TipoDocumentoIdentidad (V022)"
```

---

## Tarea 17: Cobertura de integración GET-by-id en `CatalogoComercialApiIntegrationTest` (CategoriaProducto, Marca)

Agrega aserciones `GET /{id}` a los tests de ciclo de vida ya existentes de `CategoriaProducto` y `Marca` en `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/api/CatalogoComercialApiIntegrationTest.java` (este archivo YA EXISTE y ya cubre create→update→estado→lista para ambos catálogos — ver `managesCategoriaProductoLifecycleAndListing` y `managesMarcaLifecycleAndListing`). `ProductoRegulado` y `Sku` ya tienen su GET-by-id cubierto en este mismo archivo (`managesProductoReguladoLifecycleAndPrincipiosActivos` ya hace `GET /productos-regulados/{id}`, `managesSkuLifecycleAndFractionSale` ya hace `GET /skus/{id}`) — no se tocan.

**Files:**
- Modify: `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/api/CatalogoComercialApiIntegrationTest.java`

**Interfaces:**
- Consumes: `GET /api/v1/catalogo/categorias/{categoriaId}?tenantId=...` (Tarea 1), `GET /api/v1/catalogo/marcas/{marcaId}?tenantId=...` (Tarea 2), authorities `catalogo.categorias.consultar`/`catalogo.marcas.consultar` (ya cubiertas por el helper `consultor()` existente en este archivo).
- Produces: ninguna — solo agrega aserciones a tests existentes.

- [ ] **Step 1: Agregar la aserción GET-by-id a `managesCategoriaProductoLifecycleAndListing`**

Editar `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/api/CatalogoComercialApiIntegrationTest.java`. Dentro del método `managesCategoriaProductoLifecycleAndListing` (que ya existe, líneas 62-120 aproximadamente), insertar el siguiente bloque justo después del `PUT` que actualiza la categoría y antes de la creación de la subcategoría (es decir, después del bloque `mockMvc.perform(put("/api/v1/catalogo/categorias/{categoriaId}", categoriaId)...)` que ya existe, y antes de `var subCategoriaResponse = mockMvc.perform(post(...))`):

```java
        mockMvc.perform(get("/api/v1/catalogo/categorias/{categoriaId}", categoriaId)
                        .with(consultor())
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoriaId))
                .andExpect(jsonPath("$.codigo").value("MEDICAMENTOS"))
                .andExpect(jsonPath("$.nombre").value("Medicamentos actualizados"));

        mockMvc.perform(get("/api/v1/catalogo/categorias/{categoriaId}", UUID.randomUUID())
                        .with(consultor())
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isNotFound());
```

El método completo `managesCategoriaProductoLifecycleAndListing` queda:

```java
    @Test
    void managesCategoriaProductoLifecycleAndListing() throws Exception {
        var createResponse = mockMvc.perform(post("/api/v1/catalogo/categorias")
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","codigo":"MEDICAMENTOS","nombre":"Medicamentos",
                                 "descripcion":"Categoria raiz","nivel":1,"orden":1}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("MEDICAMENTOS"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andReturn().getResponse().getContentAsString();
        String categoriaId = JsonPath.read(createResponse, "$.id");

        mockMvc.perform(put("/api/v1/catalogo/categorias/{categoriaId}", categoriaId)
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","codigo":"MEDICAMENTOS","nombre":"Medicamentos actualizados",
                                 "descripcion":"Categoria raiz actualizada","nivel":1,"orden":1}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Medicamentos actualizados"));

        mockMvc.perform(get("/api/v1/catalogo/categorias/{categoriaId}", categoriaId)
                        .with(consultor())
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoriaId))
                .andExpect(jsonPath("$.codigo").value("MEDICAMENTOS"))
                .andExpect(jsonPath("$.nombre").value("Medicamentos actualizados"));

        mockMvc.perform(get("/api/v1/catalogo/categorias/{categoriaId}", UUID.randomUUID())
                        .with(consultor())
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isNotFound());

        var subCategoriaResponse = mockMvc.perform(post("/api/v1/catalogo/categorias")
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","categoriaPadreId":"%s","codigo":"ANALGESICOS",
                                 "nombre":"Analgesicos","nivel":2,"orden":1}
                                """.formatted(TENANT_ID, categoriaId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoriaPadreId").value(categoriaId))
                .andReturn().getResponse().getContentAsString();
        String subCategoriaId = JsonPath.read(subCategoriaResponse, "$.id");

        mockMvc.perform(patch("/api/v1/catalogo/categorias/{categoriaId}/estado", categoriaId)
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","status":"INACTIVO"}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/catalogo/categorias")
                        .with(consultor())
                        .param("tenantId", TENANT_ID.toString())
                        .param("categoriaPadreId", categoriaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id=='%s')]".formatted(subCategoriaId)).exists());

        // Sin categoriaPadreId (filtro UUID opcional omitido): confirma el fix del bind NULL sin tipo.
        mockMvc.perform(get("/api/v1/catalogo/categorias")
                        .with(consultor())
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id=='%s')]".formatted(categoriaId)).exists())
                .andExpect(jsonPath("$.items[?(@.id=='%s')]".formatted(subCategoriaId)).exists());
    }
```

- [ ] **Step 2: Agregar la aserción GET-by-id a `managesMarcaLifecycleAndListing`**

En el mismo archivo, dentro del método `managesMarcaLifecycleAndListing`, insertar el bloque GET-by-id justo después del `PUT` que actualiza la marca y antes del `PATCH` de cambio de estado. El método completo queda:

```java
    @Test
    void managesMarcaLifecycleAndListing() throws Exception {
        var createResponse = mockMvc.perform(post("/api/v1/catalogo/marcas")
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","codigo":"GENFAR","nombre":"Genfar","descripcion":"Marca generica"}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("GENFAR"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andReturn().getResponse().getContentAsString();
        String marcaId = JsonPath.read(createResponse, "$.id");

        mockMvc.perform(put("/api/v1/catalogo/marcas/{marcaId}", marcaId)
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","codigo":"GENFAR","nombre":"Genfar actualizada",
                                 "descripcion":"Marca generica actualizada"}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Genfar actualizada"));

        mockMvc.perform(get("/api/v1/catalogo/marcas/{marcaId}", marcaId)
                        .with(consultor())
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(marcaId))
                .andExpect(jsonPath("$.nombre").value("Genfar actualizada"));

        mockMvc.perform(get("/api/v1/catalogo/marcas/{marcaId}", UUID.randomUUID())
                        .with(consultor())
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/catalogo/marcas/{marcaId}/estado", marcaId)
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","status":"INACTIVO"}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/catalogo/marcas")
                        .with(consultor())
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id=='%s')].estado".formatted(marcaId)).value("INACTIVO"));
    }
```

- [ ] **Step 3: Ejecutar los dos tests modificados**

Run: `cd service-botica && .\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.catalogo.api.CatalogoComercialApiIntegrationTest.managesCategoriaProductoLifecycleAndListing" --tests "com.softprimesolutions.catalogo.api.CatalogoComercialApiIntegrationTest.managesMarcaLifecycleAndListing"`
Expected: BUILD SUCCESSFUL, 2 tests passed. (Requiere Docker en ejecución para Testcontainers PostgreSQL.)

- [ ] **Step 4: Ejecutar todo el archivo para confirmar que no se rompió nada más**

Run: `cd service-botica && .\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.catalogo.api.CatalogoComercialApiIntegrationTest"`
Expected: BUILD SUCCESSFUL, 9 tests passed (los 7 ya existentes más ninguno nuevo — se agregaron aserciones a métodos existentes, no métodos nuevos).

- [ ] **Step 5: Commit**

```bash
git add service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/api/CatalogoComercialApiIntegrationTest.java
git commit -m "test(catalogo): cubrir GET /categorias/{id} y GET /marcas/{id} en el test de integracion comercial"
```

---

## Tarea 18: Cobertura de integración GET-by-id/codigo en `CatalogoSoporteApiIntegrationTest` (6 catálogos)

Agrega aserciones `GET /{codigo}` (o `GET /{principioActivoId}` para PrincipioActivo) a los 5 tests de ciclo de vida de catálogos de soporte más el de PrincipioActivo, todos ya existentes en `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/api/CatalogoSoporteApiIntegrationTest.java`.

**Files:**
- Modify: `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/api/CatalogoSoporteApiIntegrationTest.java`

**Interfaces:**
- Consumes: `GET /api/v1/catalogo/condiciones-venta/{codigo}` (Tarea 4), `GET /api/v1/catalogo/formas-farmaceuticas/{codigo}` (Tarea 5), `GET /api/v1/catalogo/vias-administracion/{codigo}` (Tarea 6), `GET /api/v1/catalogo/unidades-medida/{codigo}` (Tarea 7), `GET /api/v1/catalogo/clasificaciones-controladas/{codigo}` (Tarea 8), `GET /api/v1/catalogo/principios-activos/{principioActivoId}` (Tarea 3); authorities `catalogo.soporte.gestionar`/`catalogo.principios-activos.gestionar` (ya cubiertas por los helpers `soporteAdmin()`/`principiosActivosAdmin()` existentes en este archivo — no hace falta un authority `.consultar` separado porque estos tests siempre usan el admin que ya tiene ambos).
- Produces: ninguna — solo agrega aserciones a tests existentes.

- [ ] **Step 1: Agregar la aserción GET-by-codigo a `managesCondicionVentaCreateUpdateStatusAndList`**

Editar `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/api/CatalogoSoporteApiIntegrationTest.java`. El método `managesCondicionVentaCreateUpdateStatusAndList` completo queda (se inserta el GET después del PUT y antes del PATCH de estado):

```java
    @Test
    void managesCondicionVentaCreateUpdateStatusAndList() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo/condiciones-venta")
                        .with(soporteAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo":"VENTA_LIBRE",
                                  "denominacion":"Venta libre",
                                  "requiereReceta":false,
                                  "requiereRetencion":false,
                                  "fuente":"DIGEMID",
                                  "versionFuente":"2026.1",
                                  "vigenteDesde":"2026-01-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("VENTA_LIBRE"))
                .andExpect(jsonPath("$.denominacion").value("Venta libre"))
                .andExpect(jsonPath("$.requiereReceta").value(false))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));

        mockMvc.perform(put("/api/v1/catalogo/condiciones-venta/{codigo}", "VENTA_LIBRE")
                        .with(soporteAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo":"VENTA_LIBRE",
                                  "denominacion":"Venta libre sin receta",
                                  "requiereReceta":false,
                                  "requiereRetencion":true,
                                  "fuente":"DIGEMID",
                                  "versionFuente":"2026.2",
                                  "vigenteDesde":"2026-01-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.denominacion").value("Venta libre sin receta"))
                .andExpect(jsonPath("$.requiereRetencion").value(true));

        mockMvc.perform(get("/api/v1/catalogo/condiciones-venta/{codigo}", "VENTA_LIBRE").with(soporteAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("VENTA_LIBRE"))
                .andExpect(jsonPath("$.denominacion").value("Venta libre sin receta"));

        mockMvc.perform(get("/api/v1/catalogo/condiciones-venta/{codigo}", "NO_EXISTE").with(soporteAdmin()))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/catalogo/condiciones-venta/{codigo}/estado", "VENTA_LIBRE")
                        .with(soporteAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVO\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/catalogo/condiciones-venta").with(soporteAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("VENTA_LIBRE"))
                .andExpect(jsonPath("$[0].estado").value("INACTIVO"));
    }
```

- [ ] **Step 2: Agregar la aserción GET-by-codigo a `managesFormaFarmaceuticaCreateUpdateStatusAndList`**

El método completo queda:

```java
    @Test
    void managesFormaFarmaceuticaCreateUpdateStatusAndList() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo/formas-farmaceuticas")
                        .with(soporteAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"TABLETA","denominacion":"Tableta","fuente":"DIGEMID"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("TABLETA"))
                .andExpect(jsonPath("$.denominacion").value("Tableta"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));

        mockMvc.perform(put("/api/v1/catalogo/formas-farmaceuticas/{codigo}", "TABLETA")
                        .with(soporteAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"TABLETA","denominacion":"Tableta recubierta","fuente":"DIGEMID"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.denominacion").value("Tableta recubierta"));

        mockMvc.perform(get("/api/v1/catalogo/formas-farmaceuticas/{codigo}", "TABLETA").with(soporteAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("TABLETA"))
                .andExpect(jsonPath("$.denominacion").value("Tableta recubierta"));

        mockMvc.perform(get("/api/v1/catalogo/formas-farmaceuticas/{codigo}", "NO_EXISTE").with(soporteAdmin()))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/catalogo/formas-farmaceuticas/{codigo}/estado", "TABLETA")
                        .with(soporteAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVO\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/catalogo/formas-farmaceuticas").with(soporteAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("TABLETA"))
                .andExpect(jsonPath("$[0].estado").value("INACTIVO"));
    }
```

- [ ] **Step 3: Agregar la aserción GET-by-codigo a `managesViaAdministracionCreateUpdateStatusAndList`**

El método completo queda:

```java
    @Test
    void managesViaAdministracionCreateUpdateStatusAndList() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo/vias-administracion")
                        .with(soporteAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"ORAL","denominacion":"Via oral","fuente":"DIGEMID"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("ORAL"))
                .andExpect(jsonPath("$.denominacion").value("Via oral"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));

        mockMvc.perform(put("/api/v1/catalogo/vias-administracion/{codigo}", "ORAL")
                        .with(soporteAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"ORAL","denominacion":"Via oral actualizada","fuente":"DIGEMID"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.denominacion").value("Via oral actualizada"));

        mockMvc.perform(get("/api/v1/catalogo/vias-administracion/{codigo}", "ORAL").with(soporteAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("ORAL"))
                .andExpect(jsonPath("$.denominacion").value("Via oral actualizada"));

        mockMvc.perform(get("/api/v1/catalogo/vias-administracion/{codigo}", "NO_EXISTE").with(soporteAdmin()))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/catalogo/vias-administracion/{codigo}/estado", "ORAL")
                        .with(soporteAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVO\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/catalogo/vias-administracion").with(soporteAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("ORAL"))
                .andExpect(jsonPath("$[0].estado").value("INACTIVO"));
    }
```

- [ ] **Step 4: Agregar la aserción GET-by-codigo a `managesUnidadMedidaCreateUpdateStatusAndList`**

El método completo queda:

```java
    @Test
    void managesUnidadMedidaCreateUpdateStatusAndList() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo/unidades-medida")
                        .with(soporteAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo":"MG",
                                  "denominacion":"Miligramo",
                                  "simbolo":"mg",
                                  "permiteDecimal":true,
                                  "fuente":"DIGEMID"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("MG"))
                .andExpect(jsonPath("$.simbolo").value("mg"))
                .andExpect(jsonPath("$.permiteDecimal").value(true))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));

        mockMvc.perform(put("/api/v1/catalogo/unidades-medida/{codigo}", "MG")
                        .with(soporteAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo":"MG",
                                  "denominacion":"Miligramo actualizado",
                                  "simbolo":"mg",
                                  "permiteDecimal":false,
                                  "fuente":"DIGEMID"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.denominacion").value("Miligramo actualizado"))
                .andExpect(jsonPath("$.permiteDecimal").value(false));

        mockMvc.perform(get("/api/v1/catalogo/unidades-medida/{codigo}", "MG").with(soporteAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("MG"))
                .andExpect(jsonPath("$.denominacion").value("Miligramo actualizado"))
                .andExpect(jsonPath("$.permiteDecimal").value(false));

        mockMvc.perform(get("/api/v1/catalogo/unidades-medida/{codigo}", "NO_EXISTE").with(soporteAdmin()))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/catalogo/unidades-medida/{codigo}/estado", "MG")
                        .with(soporteAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVO\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/catalogo/unidades-medida").with(soporteAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("MG"))
                .andExpect(jsonPath("$[0].estado").value("INACTIVO"));
    }
```

- [ ] **Step 5: Agregar la aserción GET-by-codigo a `managesClasificacionControladaCreateUpdateStatusAndList`**

El método completo queda:

```java
    @Test
    void managesClasificacionControladaCreateUpdateStatusAndList() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo/clasificaciones-controladas")
                        .with(soporteAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo":"LISTA_II",
                                  "denominacion":"Lista II",
                                  "normaFuente":"D.S. 023-2001-SA",
                                  "requiereRecetaEspecial":true,
                                  "retieneReceta":true,
                                  "vigenciaRecetaDias":30
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("LISTA_II"))
                .andExpect(jsonPath("$.requiereRecetaEspecial").value(true))
                .andExpect(jsonPath("$.vigenciaRecetaDias").value(30))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));

        // El PUT de este controller toma el codigo del BODY (no del path variable).
        mockMvc.perform(put("/api/v1/catalogo/clasificaciones-controladas/{codigo}", "LISTA_II")
                        .with(soporteAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo":"LISTA_II",
                                  "denominacion":"Lista II actualizada",
                                  "normaFuente":"D.S. 023-2001-SA",
                                  "requiereRecetaEspecial":true,
                                  "retieneReceta":false,
                                  "vigenciaRecetaDias":15
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.denominacion").value("Lista II actualizada"))
                .andExpect(jsonPath("$.retieneReceta").value(false))
                .andExpect(jsonPath("$.vigenciaRecetaDias").value(15));

        mockMvc.perform(get("/api/v1/catalogo/clasificaciones-controladas/{codigo}", "LISTA_II")
                        .with(soporteAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("LISTA_II"))
                .andExpect(jsonPath("$.denominacion").value("Lista II actualizada"))
                .andExpect(jsonPath("$.vigenciaRecetaDias").value(15));

        mockMvc.perform(get("/api/v1/catalogo/clasificaciones-controladas/{codigo}", "NO_EXISTE")
                        .with(soporteAdmin()))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/catalogo/clasificaciones-controladas/{codigo}/estado", "LISTA_II")
                        .with(soporteAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVO\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/catalogo/clasificaciones-controladas").with(soporteAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("LISTA_II"))
                .andExpect(jsonPath("$[0].estado").value("INACTIVO"));
    }
```

- [ ] **Step 6: Agregar la aserción GET-by-id a `managesPrincipioActivoCreateUpdateStatusListAndRejectsDuplicateDenomination`**

El método completo queda (se inserta el GET después del PUT y antes del PATCH de estado):

```java
    @Test
    void managesPrincipioActivoCreateUpdateStatusListAndRejectsDuplicateDenomination() throws Exception {
        var createResponse = mockMvc.perform(post("/api/v1/catalogo/principios-activos")
                        .with(principiosActivosAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigoFuente":"PA-001",
                                  "denominacion":"Paracetamol",
                                  "nombreNormalizado":"PARACETAMOL",
                                  "fuente":"DIGEMID"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.denominacion").value("Paracetamol"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andReturn().getResponse().getContentAsString();
        String principioActivoId = JsonPath.read(createResponse, "$.id");

        mockMvc.perform(put("/api/v1/catalogo/principios-activos/{principioActivoId}", principioActivoId)
                        .with(principiosActivosAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigoFuente":"PA-001",
                                  "denominacion":"Paracetamol 500mg",
                                  "nombreNormalizado":"PARACETAMOL 500MG",
                                  "fuente":"DIGEMID"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.denominacion").value("Paracetamol 500mg"));

        mockMvc.perform(get("/api/v1/catalogo/principios-activos/{principioActivoId}", principioActivoId)
                        .with(principiosActivosAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(principioActivoId))
                .andExpect(jsonPath("$.denominacion").value("Paracetamol 500mg"));

        mockMvc.perform(get("/api/v1/catalogo/principios-activos/{principioActivoId}", UUID.randomUUID())
                        .with(principiosActivosAdmin()))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/catalogo/principios-activos/{principioActivoId}/estado", principioActivoId)
                        .with(principiosActivosAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVO\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/catalogo/principios-activos")
                        .with(principiosActivosAdmin())
                        .param("texto", "Paracetamol"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(principioActivoId))
                .andExpect(jsonPath("$[0].estado").value("INACTIVO"));

        // La denominacion "Paracetamol 500mg" ya existe (indice unico uk_principio_activo_nombre):
        // el handler de creacion traduce la violacion de integridad a un ApplicationError de conflicto.
        mockMvc.perform(post("/api/v1/catalogo/principios-activos")
                        .with(principiosActivosAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigoFuente":"PA-002",
                                  "denominacion":"Paracetamol 500mg",
                                  "nombreNormalizado":"PARACETAMOL 500MG",
                                  "fuente":"DIGEMID"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAT_PRINCIPIO_ACTIVO_DUPLICADO"));
    }
```

Este método necesita el import `java.util.UUID` — confirmar que ya está importado en el archivo (no lo estaba en la versión original de `CatalogoSoporteApiIntegrationTest.java`; si falta, agregar `import java.util.UUID;` junto a los demás imports `java.*` al inicio del archivo).

- [ ] **Step 7: Ejecutar todo el archivo de test**

Run: `cd service-botica && .\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.catalogo.api.CatalogoSoporteApiIntegrationTest"`
Expected: BUILD SUCCESSFUL, 8 tests passed (los 8 ya existentes, con aserciones adicionales en 6 de ellos).

- [ ] **Step 8: Commit**

```bash
git add service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/api/CatalogoSoporteApiIntegrationTest.java
git commit -m "test(catalogo): cubrir GET /{codigo} de los 5 catalogos de soporte y GET /principios-activos/{id}"
```

---

## Tarea 19: Test de integración de `RubroComercial` (nuevo método en `CatalogoComercialApiIntegrationTest`)

Agrega un nuevo método `@Test managesRubroComercialLifecycleAndListing` a `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/api/CatalogoComercialApiIntegrationTest.java`, siguiendo el mismo patrón que `managesMarcaLifecycleAndListing`: crear → GET by id → PUT → GET by id (verifica cambio) → PATCH estado → GET lista (verifica filtro `esFarmaceutico` y `estado`). También actualiza el helper `gestor()`/`consultor()` para incluir las authorities de `RubroComercial`, y `resetCanonicalFixtures()` para limpiar la tabla `sch_catalogo.rubro_comercial` entre tests.

**Files:**
- Modify: `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/api/CatalogoComercialApiIntegrationTest.java`

**Interfaces:**
- Consumes: `POST/PUT/PATCH/GET /api/v1/catalogo/rubros-comerciales` (Tarea 12); authorities `catalogo.rubros-comerciales.gestionar`/`catalogo.rubros-comerciales.consultar` (Tarea 16, migración V022).

- [ ] **Step 1: Agregar la limpieza de `rubro_comercial` a `resetCanonicalFixtures`**

Editar `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/api/CatalogoComercialApiIntegrationTest.java`. El método `resetCanonicalFixtures` (privado, al final de la clase) pasa de:

```java
    private void resetCanonicalFixtures() {
        jdbcClient.sql("DELETE FROM sch_catalogo.sku_codigo_barra").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.sku_comercial").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.producto_principio_activo").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.producto_regulado").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.principio_activo").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.categoria_producto").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.marca").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.unidad_medida").update();
        jdbcClient.sql("DELETE FROM sch_admin.tenant").update();
    }
```

a:

```java
    private void resetCanonicalFixtures() {
        jdbcClient.sql("DELETE FROM sch_catalogo.sku_codigo_barra").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.sku_comercial").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.producto_principio_activo").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.producto_regulado").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.principio_activo").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.categoria_producto").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.marca").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.rubro_comercial").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.unidad_medida").update();
        jdbcClient.sql("DELETE FROM sch_admin.tenant").update();
    }
```

- [ ] **Step 2: Agregar las authorities de `RubroComercial` a `gestor()` y `consultor()`**

En el mismo archivo, el método `gestor()` pasa de:

```java
    private static SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor gestor() {
        return SecurityMockMvcRequestPostProcessors.user("admin").authorities(
                new SimpleGrantedAuthority("catalogo.categorias.gestionar"),
                new SimpleGrantedAuthority("catalogo.categorias.consultar"),
                new SimpleGrantedAuthority("catalogo.marcas.gestionar"),
                new SimpleGrantedAuthority("catalogo.marcas.consultar"),
                new SimpleGrantedAuthority("catalogo.productos-regulados.gestionar"),
                new SimpleGrantedAuthority("catalogo.productos-regulados.consultar"),
                new SimpleGrantedAuthority("catalogo.skus.gestionar"),
                new SimpleGrantedAuthority("catalogo.skus.consultar"));
    }
```

a:

```java
    private static SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor gestor() {
        return SecurityMockMvcRequestPostProcessors.user("admin").authorities(
                new SimpleGrantedAuthority("catalogo.categorias.gestionar"),
                new SimpleGrantedAuthority("catalogo.categorias.consultar"),
                new SimpleGrantedAuthority("catalogo.marcas.gestionar"),
                new SimpleGrantedAuthority("catalogo.marcas.consultar"),
                new SimpleGrantedAuthority("catalogo.productos-regulados.gestionar"),
                new SimpleGrantedAuthority("catalogo.productos-regulados.consultar"),
                new SimpleGrantedAuthority("catalogo.skus.gestionar"),
                new SimpleGrantedAuthority("catalogo.skus.consultar"),
                new SimpleGrantedAuthority("catalogo.rubros-comerciales.gestionar"),
                new SimpleGrantedAuthority("catalogo.rubros-comerciales.consultar"));
    }
```

Y `consultor()` pasa de:

```java
    private static SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor consultor() {
        return SecurityMockMvcRequestPostProcessors.user("consultor").authorities(
                new SimpleGrantedAuthority("catalogo.categorias.consultar"),
                new SimpleGrantedAuthority("catalogo.marcas.consultar"),
                new SimpleGrantedAuthority("catalogo.productos-regulados.consultar"),
                new SimpleGrantedAuthority("catalogo.skus.consultar"));
    }
```

a:

```java
    private static SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor consultor() {
        return SecurityMockMvcRequestPostProcessors.user("consultor").authorities(
                new SimpleGrantedAuthority("catalogo.categorias.consultar"),
                new SimpleGrantedAuthority("catalogo.marcas.consultar"),
                new SimpleGrantedAuthority("catalogo.productos-regulados.consultar"),
                new SimpleGrantedAuthority("catalogo.skus.consultar"),
                new SimpleGrantedAuthority("catalogo.rubros-comerciales.consultar"));
    }
```

- [ ] **Step 3: Agregar el método de test `managesRubroComercialLifecycleAndListing`**

Insertar el siguiente método nuevo justo después de `searchesAndPaginatesMarcas` (y antes de `managesProductoReguladoLifecycleAndPrincipiosActivos`):

```java
    @Test
    void managesRubroComercialLifecycleAndListing() throws Exception {
        var createResponse = mockMvc.perform(post("/api/v1/catalogo/rubros-comerciales")
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","codigo":"FARMA","nombre":"Farmaceutico",
                                 "descripcion":"Linea farmaceutica","esFarmaceutico":true,"orden":1}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("FARMA"))
                .andExpect(jsonPath("$.esFarmaceutico").value(true))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andReturn().getResponse().getContentAsString();
        String rubroId = JsonPath.read(createResponse, "$.id");

        mockMvc.perform(get("/api/v1/catalogo/rubros-comerciales/{rubroComercialId}", rubroId)
                        .with(consultor())
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(rubroId))
                .andExpect(jsonPath("$.codigo").value("FARMA"))
                .andExpect(jsonPath("$.orden").value(1));

        mockMvc.perform(get("/api/v1/catalogo/rubros-comerciales/{rubroComercialId}", UUID.randomUUID())
                        .with(consultor())
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/catalogo/rubros-comerciales/{rubroComercialId}", rubroId)
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","codigo":"FARMA","nombre":"Farmaceutico actualizado",
                                 "descripcion":"Linea farmaceutica actualizada","esFarmaceutico":true,"orden":2}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Farmaceutico actualizado"))
                .andExpect(jsonPath("$.orden").value(2));

        var noFarmaceuticoResponse = mockMvc.perform(post("/api/v1/catalogo/rubros-comerciales")
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","codigo":"BEBE","nombre":"Bebe",
                                 "descripcion":"Linea infantil","esFarmaceutico":false,"orden":3}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String bebeRubroId = JsonPath.read(noFarmaceuticoResponse, "$.id");

        mockMvc.perform(get("/api/v1/catalogo/rubros-comerciales")
                        .with(consultor())
                        .param("tenantId", TENANT_ID.toString())
                        .param("esFarmaceutico", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id=='%s')]".formatted(rubroId)).exists())
                .andExpect(jsonPath("$.items[?(@.id=='%s')]".formatted(bebeRubroId)).doesNotExist());

        mockMvc.perform(patch("/api/v1/catalogo/rubros-comerciales/{rubroComercialId}/estado", rubroId)
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","status":"INACTIVO"}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/catalogo/rubros-comerciales")
                        .with(consultor())
                        .param("tenantId", TENANT_ID.toString())
                        .param("estado", "INACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id=='%s')]".formatted(rubroId)).exists())
                .andExpect(jsonPath("$.items[?(@.id=='%s')]".formatted(bebeRubroId)).doesNotExist());
    }
```

- [ ] **Step 4: Agregar la verificación de denegación de acceso sin permiso**

En el método existente `deniesCatalogoAdministrationWithoutTheRequiredPermission`, agregar un bloque adicional al final del método (antes del cierre `}`):

```java
        mockMvc.perform(post("/api/v1/catalogo/rubros-comerciales")
                        .with(SecurityMockMvcRequestPostProcessors.user("viewer")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","codigo":"SIN_PERMISO","nombre":"Sin permiso",
                                 "esFarmaceutico":false,"orden":0}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isForbidden());
```

El método completo `deniesCatalogoAdministrationWithoutTheRequiredPermission` queda:

```java
    @Test
    void deniesCatalogoAdministrationWithoutTheRequiredPermission() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo/categorias")
                        .with(SecurityMockMvcRequestPostProcessors.user("viewer")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","codigo":"SIN_PERMISO","nombre":"Sin permiso","nivel":1,"orden":1}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/catalogo/skus")
                        .with(SecurityMockMvcRequestPostProcessors.user("viewer")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","tipoSku":"NO_REGULADO","codigoInterno":"SIN-PERMISO",
                                 "descripcionComercial":"Sin permiso","unidadVentaCodigo":"UND",
                                 "permiteVentaFraccion":false,"requiereLote":false,"requiereVencimiento":false,
                                 "afectoIgv":true,"stockMinimoDefault":0}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/catalogo/rubros-comerciales")
                        .with(SecurityMockMvcRequestPostProcessors.user("viewer")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","codigo":"SIN_PERMISO","nombre":"Sin permiso",
                                 "esFarmaceutico":false,"orden":0}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isForbidden());
    }
```

- [ ] **Step 5: Ejecutar todo el archivo de test**

Run: `cd service-botica && .\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.catalogo.api.CatalogoComercialApiIntegrationTest"`
Expected: BUILD SUCCESSFUL, 10 tests passed (los 9 anteriores más `managesRubroComercialLifecycleAndListing`).

- [ ] **Step 6: Commit**

```bash
git add service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/api/CatalogoComercialApiIntegrationTest.java
git commit -m "test(catalogo): agregar test de integracion de RubroComercial"
```

---

## Tarea 20: Test de integración de `TipoDocumentoIdentidad` (nuevo método en `CatalogoSoporteApiIntegrationTest`)

Agrega un nuevo método `@Test managesTipoDocumentoIdentidadLookupAndStatusChange` a `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/api/CatalogoSoporteApiIntegrationTest.java`. A diferencia de los demás catálogos de este archivo, `TipoDocumentoIdentidad` NO tiene `POST` (las 13 filas ya están sembradas globalmente por la migración `docs/cadena-farmacias-docs/database/migrations/V018__catalogo_tipo_documento_identidad_sunat.sql`, que se ejecuta como Flyway real también en el perfil `test` vía `classpath:migrations` — ver Global Constraints). El test usa directamente el código sembrado `'1'` (DNI): GET/{codigo} → GET lista → PATCH estado → GET/{codigo} (verifica el cambio). No se agrega este catálogo a `resetCanonicalFixtures()` porque sus filas son semilla global permanente, no fixtures de test — el `@Transactional` de la clase hace rollback de cualquier cambio (incluido el `PATCH` de estado) al final de cada test, por lo que no hace falta limpieza manual y no hay riesgo de contaminar otros tests de la clase.

**Files:**
- Modify: `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/api/CatalogoSoporteApiIntegrationTest.java`

**Interfaces:**
- Consumes: `GET /api/v1/catalogo/tipos-documento-identidad/{codigo}`, `GET /api/v1/catalogo/tipos-documento-identidad`, `PATCH /api/v1/catalogo/tipos-documento-identidad/{codigo}/estado` (Tarea 15); authorities `catalogo.tipos-documento-identidad.consultar`/`catalogo.tipos-documento-identidad.gestionar` (Tarea 16, migración V022); fila semilla `codigo='1', sigla='DNI', denominacion='Documento Nacional de Identidad', max=8, min=8, es_activo='1'` insertada por V018.

- [ ] **Step 1: Agregar un helper de autorización para `TipoDocumentoIdentidad`**

En `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/api/CatalogoSoporteApiIntegrationTest.java`, agregar un nuevo método privado estático junto a `soporteAdmin()` y `principiosActivosAdmin()` (al final de la clase, antes del cierre `}`):

```java
    private static SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor tipoDocumentoIdentidadAdmin() {
        return SecurityMockMvcRequestPostProcessors.user("catalogo-tipo-documento-identidad-admin").authorities(
                new SimpleGrantedAuthority("catalogo.tipos-documento-identidad.gestionar"),
                new SimpleGrantedAuthority("catalogo.tipos-documento-identidad.consultar"));
    }
```

- [ ] **Step 2: Escribir el test de lectura y cambio de estado (falla primero)**

Agregar el siguiente método `@Test` a la clase (antes del método `deniesCatalogoSoporteAdministrationWithoutTheRequiredPermission` existente):

```java
    @Test
    void managesTipoDocumentoIdentidadLookupAndStatusChange() throws Exception {
        mockMvc.perform(get("/api/v1/catalogo/tipos-documento-identidad/{codigo}", "1")
                        .with(tipoDocumentoIdentidadAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("1"))
                .andExpect(jsonPath("$.sigla").value("DNI"))
                .andExpect(jsonPath("$.denominacion").value("Documento Nacional de Identidad"))
                .andExpect(jsonPath("$.max").value(8))
                .andExpect(jsonPath("$.min").value(8))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));

        mockMvc.perform(get("/api/v1/catalogo/tipos-documento-identidad/{codigo}", "Z")
                        .with(tipoDocumentoIdentidadAdmin()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/catalogo/tipos-documento-identidad")
                        .with(tipoDocumentoIdentidadAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.codigo=='1')]").exists())
                .andExpect(jsonPath("$[?(@.codigo=='6')].sigla").value("RUC"));

        mockMvc.perform(patch("/api/v1/catalogo/tipos-documento-identidad/{codigo}/estado", "1")
                        .with(tipoDocumentoIdentidadAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVO\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/catalogo/tipos-documento-identidad/{codigo}", "1")
                        .with(tipoDocumentoIdentidadAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));

        mockMvc.perform(get("/api/v1/catalogo/tipos-documento-identidad")
                        .with(tipoDocumentoIdentidadAdmin())
                        .param("estado", "ACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.codigo=='1')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.codigo=='6')]").exists());
    }
```

- [ ] **Step 3: Ejecutar el test y verificar que falla por 404/inexistencia del endpoint**

Run: `cd service-botica && .\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.catalogo.api.CatalogoSoporteApiIntegrationTest.managesTipoDocumentoIdentidadLookupAndStatusChange"`
Expected: FAIL si las Tareas 13-16 (dominio, aplicación, infraestructura, API y migración de permisos de `TipoDocumentoIdentidad`) no se han ejecutado todavía. Si esas tareas ya se completaron, este test debe fallar únicamente por la ausencia del helper `tipoDocumentoIdentidadAdmin()`/el método de test antes de este Step — una vez agregados ambos en los Steps 1-2, continuar al Step 4.

- [ ] **Step 4: Agregar la verificación de denegación de acceso sin permiso**

En el método existente `deniesCatalogoSoporteAdministrationWithoutTheRequiredPermission`, agregar un bloque adicional al final del método (antes del cierre `}`):

```java
        mockMvc.perform(patch("/api/v1/catalogo/tipos-documento-identidad/{codigo}/estado", "1")
                        .with(SecurityMockMvcRequestPostProcessors.user("viewer"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVO\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/catalogo/tipos-documento-identidad")
                        .with(SecurityMockMvcRequestPostProcessors.user("viewer")))
                .andExpect(status().isForbidden());
```

El método completo `deniesCatalogoSoporteAdministrationWithoutTheRequiredPermission` queda:

```java
    @Test
    void deniesCatalogoSoporteAdministrationWithoutTheRequiredPermission() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo/condiciones-venta")
                        .with(SecurityMockMvcRequestPostProcessors.user("viewer"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo":"VENTA_LIBRE",
                                  "denominacion":"Venta libre",
                                  "requiereReceta":false,
                                  "requiereRetencion":false
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/catalogo/principios-activos")
                        .with(SecurityMockMvcRequestPostProcessors.user("viewer"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"denominacion":"Paracetamol"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/catalogo/condiciones-venta")
                        .with(SecurityMockMvcRequestPostProcessors.user("viewer")))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/catalogo/tipos-documento-identidad/{codigo}/estado", "1")
                        .with(SecurityMockMvcRequestPostProcessors.user("viewer"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVO\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/catalogo/tipos-documento-identidad")
                        .with(SecurityMockMvcRequestPostProcessors.user("viewer")))
                .andExpect(status().isForbidden());
    }
```

- [ ] **Step 5: Ejecutar todo el archivo de test**

Run: `cd service-botica && .\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.catalogo.api.CatalogoSoporteApiIntegrationTest"`
Expected: BUILD SUCCESSFUL, 9 tests passed (los 8 de la Tarea 18 más `managesTipoDocumentoIdentidadLookupAndStatusChange`).

- [ ] **Step 6: Ejecutar la suite completa de tests de integración del módulo catalogo**

Run: `cd service-botica && .\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.catalogo.*"`
Expected: BUILD SUCCESSFUL. Confirma que las 11 rutas de catálogo (9 GET-by-id agregados en las Tareas 1-8 más `PrincipioActivo` de la Tarea 3, RubroComercial de la Tarea 19, TipoDocumentoIdentidad de esta tarea) y la migración V022 (Tarea 16) funcionan juntas sin romper ningún test previamente verde.

- [ ] **Step 7: Ejecutar la verificación completa del backend (ArchUnit + Spring Modulith + todos los tests)**

Run: `cd service-botica && .\gradlew.bat check --warning-mode all`
Expected: BUILD SUCCESSFUL. Esto corre `architectureTest` (ArchUnit: ningún módulo importa paquetes internos de otro; `catalogo` sigue sin depender de módulos no declarados en `build.gradle`) y `ApplicationModules.verify()` (Spring Modulith: los nuevos beans de `RubroComercial`/`TipoDocumentoIdentidad` no introducen ciclos ni dependencias no declaradas), además de todos los tests unitarios e de integración del monorepo.

- [ ] **Step 8: Commit**

```bash
git add service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/api/CatalogoSoporteApiIntegrationTest.java
git commit -m "test(catalogo): agregar test de integracion de TipoDocumentoIdentidad"
```

---

## Self-Review

**1. Cobertura del spec:**

- **(A) GET by id en 8 catálogos existentes** (el prompt original estimaba "7"; la inspección directa confirmó 8: `CategoriaProducto`, `Marca`, `PrincipioActivo` con patrón UUID, y `CondicionVenta`, `FormaFarmaceutica`, `ViaAdministracion`, `UnidadMedida`, `ClasificacionControlada` con patrón código) → cubierto por las Tareas 1-8, una por catálogo, cada una agregando puerto, adapter, query/handler, endpoint de controller y wiring de configuración, con test unitario de handler TDD (rojo→verde) en cada una.
- **(B) `RubroComercial` completo** (dominio → aplicación → infraestructura JPA+JDBC → API con POST/PUT/PATCH-estado/GET-id/GET-lista-paginada con filtros `q`/`esFarmaceutico`/`estado`) → cubierto por las Tareas 9 (dominio), 10 (aplicación), 11 (infraestructura + wiring), 12 (API REST + `changeRubroComercialStatus` en `CatalogoControlUseCase`/`CatalogoControlService`). La tabla `sch_catalogo.rubro_comercial` (V003, ya existente) no tiene columna `estado`, solo `es_activo CHAR(1)` — las Tareas 9-12 derivan el estado ACTIVO/INACTIVO de `es_activo` consistentemente en dominio, lectura JDBC y el `PATCH` de cambio de estado.
- **(C) `TipoDocumentoIdentidad` completo** (catálogo global SUNAT, sin tenant, solo GET/{codigo}, GET lista sin paginar, PATCH estado — sin POST/DELETE) → cubierto por las Tareas 13 (dominio y aplicación, solo `restore` sin `create`), 14 (infraestructura + `changeTipoDocumentoIdentidadStatus`), 15 (API REST). Se investigó `docs/cadena-farmacias-docs/docs/03-requerimientos/` y no se encontró ninguna regla de negocio específica sobre `tipo_documento_identidad` más allá de los `COMMENT ON` de la propia migración V018 — la Tarea 13 lo documenta explícitamente en vez de inventar una regla inexistente.
- **(D) Migración de permisos** → Tarea 16, migración `V022__seed_catalogo_rubro_tipo_documento_permissions.sql` (numeración confirmada: V021 es la última migración SQL real en `service-botica/bootstrap-app/src/main/resources/db/migration/` al momento de escribir este plan), con authorities `catalogo.rubros-comerciales.{consultar,gestionar}` y `catalogo.tipos-documento-identidad.{consultar,gestionar}` siguiendo el patrón kebab-case existente. La Tarea 16 documenta explícitamente el riesgo de colisión de numeración con el test ya existente `MigrationV022Test.java` (que prueba V021) y da instrucciones para renumerar si otra migración V022 aparece primero en disco.
- **(D) 11 tests de integración** (9 catálogos existentes tocados por GET-by-id + 2 nuevos = 11, ver tabla de la sección "Resumen") → Tareas 17 y 18 agregan aserciones `GET /{id}` a los métodos de ciclo de vida YA EXISTENTES en `CatalogoComercialApiIntegrationTest.java` (CategoriaProducto, Marca — ProductoRegulado y Sku ya estaban cubiertos, no se tocan) y `CatalogoSoporteApiIntegrationTest.java` (CondicionVenta, FormaFarmaceutica, ViaAdministracion, UnidadMedida, ClasificacionControlada, PrincipioActivo — 6 métodos). Las Tareas 19 y 20 agregan un método de test nuevo completo por cada catálogo nuevo (RubroComercial, TipoDocumentoIdentidad), cada uno cubriendo como mínimo crear (si aplica)/GET-by-id/listar/cambiar-estado autenticando con el permiso correcto, tal como pide el prompt. Total de catálogos con cobertura de test tocada: 11.
- **Restricciones de arquitectura**: cada tarea respeta `domain/` sin Spring/JPA (Tareas 9, 13), `application/` con `Result<T, ApplicationError>` (todas las Tareas 1-20), escritura JPA en `persistence/write` + lectura JDBC en `persistence/read` (Tarea 11 agrega ambos lados para `RubroComercial`; Tarea 14 idem para `TipoDocumentoIdentidad`; los GET-by-id de las Tareas 1-8 reutilizan el patrón ya establecido de reconstrucción de agregado desde el `write port`, igual que `ConsultarSkuHandler`/`ConsultarProductoReguladoHandler`), `api/` como única superficie pública reutilizando `CatalogoApiMapper`/`CatalogoApplicationMapper` en vez de crear mappers nuevos (verificado en cada tarea).
- **No se modifica ningún script bajo `docs/cadena-farmacias-docs/database/migrations/`** en ninguna tarea — las tablas de `RubroComercial` y `TipoDocumentoIdentidad` ya existen ahí (V003, V018) y el plan solo las consume.

**2. Escaneo de placeholders:** revisado cada bloque de código de las 20 tareas — no hay `TODO`, `...`, "similar a la tarea N" sin código, ni comentarios de "agregar validación aquí" sin implementación real. Cada `Step` de tipo código incluye el archivo completo o el fragmento exacto a insertar con contexto suficiente (dónde insertarlo) para un subagente sin contexto previo.

**3. Consistencia de tipos/firmas entre tareas:**
- `CatalogoComercialPort.findMarcaById(UUID, UUID)` (Tarea 2) y `findCategoriaById(UUID, UUID)` (Tarea 1) tienen la misma firma `Optional<T> findXById(UUID tenantId, UUID xId)` que el ya existente `findSkuById(UUID, UUID)` — consistente.
- `CatalogoSoportePort.findXByCodigo(String)` (Tareas 4-8) y `findPrincipioActivoById(UUID)` (Tarea 3) devuelven `Optional<T>` reconstruido vía `T.restore(...)` con las mismas firmas de `restore` ya definidas en cada clase de dominio existente — verificado contra el código fuente real de `CondicionVenta`, `FormaFarmaceutica`, `ViaAdministracion`, `UnidadMedida`, `ClasificacionControlada`, `PrincipioActivo` (no se inventó ningún parámetro).
- `RubroComercial.restore(...)` (Tarea 9) y `TipoDocumentoIdentidad.restore(...)` (Tarea 13) siguen el mismo estilo de factory estático que `Marca.restore`/`CondicionVenta.restore` (id/campos/estado, sin `create` en el caso de `TipoDocumentoIdentidad` porque no hay caso de uso de creación).
- Cada `ConsultarXHandler` (Tareas 1-8, 12, 15) usa exactamente `CatalogoApplicationMapper::toResult` como referencia de método, coherente con `ConsultarSkuHandler`/`ConsultarProductoReguladoHandler` ya existentes — no se introduce un mapper paralelo.
- Los códigos de error (`CAT_MARCA_NO_ENCONTRADA`, `CAT_CATEGORIA_PRODUCTO_NO_ENCONTRADA`, etc.) en los handlers de consulta (Tareas 1-8) reutilizan el mismo prefijo `CAT_` y el mismo estilo `_NO_ENCONTRAD[O|A]` que los códigos ya usados en `CatalogoControlService` para las mismas entidades (ej. `CatalogoControlService.changeMarcaStatus` ya usa `CAT_MARCA_NO_ENCONTRADA` — Tarea 2 reutiliza el mismo código en el handler de consulta, no inventa uno nuevo).
- Todas las 20 tareas usan `@PreAuthorize("hasAuthority('catalogo.X.consultar')")` para GET y `.gestionar` para POST/PUT/PATCH, consistente con el 100% de los controllers ya existentes en el módulo.

No se encontraron incoherencias de tipos/firmas entre tareas al revisar con ojos frescos. El plan está completo y listo para ejecución.

