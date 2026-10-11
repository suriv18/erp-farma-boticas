# Compras, parte 1: listado de recepciones por orden (backend) — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Exponer `GET /api/v1/compras/recepciones?ordenCompraId=<uuid>&page&size` que devuelve las recepciones completas (con líneas y lote creado) de una orden, para que el frontend de compras muestre el historial de recepciones.

**Architecture:** Mismo patrón CQRS pragmático del módulo: una query (`ListarRecepcionesOrdenQuery`) en `application`, un método nuevo en `ConsultarRecepcionesUseCase`/`ConsultarRecepcionesHandler`, un método en `ComprasReadPort` implementado en `ComprasJdbcReadAdapter` (lectura JDBC, sin tocar escritura ni migraciones) y un endpoint en `RecepcionController`. Reutiliza `RecepcionResult`, `PaginaResult`, `Paginacion` y el mapper de páginas existentes.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Modulith 2.1, JdbcClient, JUnit 5, Mockito, AssertJ, MockMvc + Testcontainers (PostgreSQL), Gradle 9.5.1.

Spec: `docs/superpowers/specs/2026-10-06-frontend-compras-design.md` (sección "Backend: listado de recepciones por orden").

## Global Constraints

- Todo archivo **nuevo** del backend alcanza 100% de líneas y ramas (JaCoCo por clase en `check`); los archivos modificados no pueden bajar la cobertura que ya tenían.
- Sin comentarios en el código; sin duplicación; respetar Clean Architecture/CQRS: `api` → `application` (puertos) → `infrastructure`; el controlador no importa `infrastructure`.
- Permiso del endpoint: `compras.recepciones.consultar` (ya sembrado en `V032`); sin migración nueva.
- `ordenCompraId` es obligatorio; `page >= 0`, `size` entre 1 y 100 (`COM_PAGINACION_INVALIDA`, 400); orden por `fecha_recepcion DESC, id DESC`; un `ordenCompraId` inexistente o de otro tenant devuelve una página vacía con 200 (no revela existencia).
- Comandos desde `service-botica/` en PowerShell: `.\gradlew.bat ...`. Verificación final: `.\gradlew.bat check --warning-mode all`.
- Commits terminan **exactamente** con `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.

Rutas abreviadas: `MAIN` = `service-botica/modules/compras/src/main/java/com/softprimesolutions/compras`, `TEST` = `service-botica/modules/compras/src/test/java/com/softprimesolutions/compras`, `IT` = `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/compras/api`.

---

### Task 1: Caso de uso, puerto de lectura y adaptador JDBC

**Files:**
- Create: `MAIN/application/dto/query/ListarRecepcionesOrdenQuery.java`
- Modify: `MAIN/application/port/in/ConsultarRecepcionesUseCase.java`
- Modify: `MAIN/application/usecase/query/ConsultarRecepcionesHandler.java`
- Modify: `MAIN/application/port/out/ComprasReadPort.java`
- Modify: `MAIN/infrastructure/persistence/read/adapter/ComprasJdbcReadAdapter.java`
- Test: `TEST/application/usecase/query/QueryHandlersTest.java`, `TEST/infrastructure/persistence/read/adapter/ComprasJdbcReadAdapterTest.java`, `TEST/api/controller/ComprasControllersTest.java` (solo adaptar a la interfaz)

**Interfaces:**
- Produces:
  - `record ListarRecepcionesOrdenQuery(UUID tenantId, UUID ordenId, int page, int size)`
  - `ConsultarRecepcionesUseCase.listarPorOrden(ListarRecepcionesOrdenQuery): Result<PaginaResult<RecepcionResult>, ApplicationError>` (la interfaz deja de ser `@FunctionalInterface`)
  - `ComprasReadPort.findRecepcionesDeOrden(UUID tenantId, UUID ordenId, int page, int size): PaginaResult<RecepcionResult>`

- [ ] **Step 1: Escribir los tests que fallan**

En `TEST/application/usecase/query/QueryHandlersTest.java` agregar el import `import com.softprimesolutions.compras.application.dto.query.ListarRecepcionesOrdenQuery;` junto a los demás imports de `dto.query`, y este test al final de la clase (antes de la `}` final):

```java
    @Test
    void listsTheReceptionsOfAnOrderOrRejectsAnInvalidPage() {
        var pagina = new PaginaResult<>(List.of(recepcionResult()), 0, 20, 1);
        when(readPort.findRecepcionesDeOrden(TENANT, ORDEN, 0, 20)).thenReturn(pagina);

        assertThat(value(recepciones.listarPorOrden(new ListarRecepcionesOrdenQuery(TENANT, ORDEN, 0, 20))))
                .isSameAs(pagina);
        assertThat(error(recepciones.listarPorOrden(new ListarRecepcionesOrdenQuery(TENANT, ORDEN, -1, 20))).code())
                .isEqualTo("COM_PAGINACION_INVALIDA");
        assertThat(error(recepciones.listarPorOrden(new ListarRecepcionesOrdenQuery(TENANT, ORDEN, 0, 101))).code())
                .isEqualTo("COM_PAGINACION_INVALIDA");
        verify(readPort, never()).findRecepcionesDeOrden(TENANT, ORDEN, -1, 20);
    }
```

En `TEST/infrastructure/persistence/read/adapter/ComprasJdbcReadAdapterTest.java` agregar al final de la clase (el import de `Rows`, `ORDEN`, `TENANT`, `RECEPCION` ya existe):

```java
    @Test
    void listsTheReceptionsOfAnOrderWithTheirLinesAndTheTotal() {
        jdbc.rows("ORDER BY r.fecha_recepcion DESC", Rows.recepcion())
                .rows("ORDER BY rl.numero_linea", Rows.recepcionLinea())
                .scalar("SELECT COUNT(*) ", 5L);

        var pagina = adapter.findRecepcionesDeOrden(TENANT, ORDEN, 1, 2);

        assertThat(pagina.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(RECEPCION);
            assertThat(item.ordenCompraId()).isEqualTo(ORDEN);
            assertThat(item.lineas()).singleElement()
                    .satisfies(linea -> assertThat(linea.numeroLote()).isEqualTo("LOTE-1"));
        });
        assertThat(pagina.page()).isEqualTo(1);
        assertThat(pagina.size()).isEqualTo(2);
        assertThat(pagina.totalElements()).isEqualTo(5);
        assertThat(jdbc.statementContaining("ORDER BY r.fecha_recepcion DESC").params())
                .containsEntry("tenantId", TENANT).containsEntry("ordenId", ORDEN)
                .containsEntry("limit", 2).containsEntry("offset", 2);
        assertThat(jdbc.statementContaining("SELECT COUNT(*) ").params())
                .containsEntry("tenantId", TENANT).containsEntry("ordenId", ORDEN);
    }

    @Test
    void listsNoReceptionsForAnOrderWithoutThem() {
        jdbc.scalar("SELECT COUNT(*) ", 0L);

        var pagina = adapter.findRecepcionesDeOrden(TENANT, ORDEN, 0, 20);

        assertThat(pagina.items()).isEmpty();
        assertThat(pagina.totalElements()).isZero();
    }
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `.\gradlew.bat :modules:compras:test --tests "com.softprimesolutions.compras.application.usecase.query.QueryHandlersTest" --tests "com.softprimesolutions.compras.infrastructure.persistence.read.adapter.ComprasJdbcReadAdapterTest"`
Expected: FAIL de compilación (`ListarRecepcionesOrdenQuery`, `listarPorOrden` y `findRecepcionesDeOrden` no existen).

- [ ] **Step 3: Implementar**

`MAIN/application/dto/query/ListarRecepcionesOrdenQuery.java`:

```java
package com.softprimesolutions.compras.application.dto.query;

import java.util.UUID;

public record ListarRecepcionesOrdenQuery(UUID tenantId, UUID ordenId, int page, int size) {
}
```

`MAIN/application/port/in/ConsultarRecepcionesUseCase.java` (reemplazar el archivo completo):

```java
package com.softprimesolutions.compras.application.port.in;

import com.softprimesolutions.compras.application.dto.query.ListarRecepcionesOrdenQuery;
import com.softprimesolutions.compras.application.dto.query.ObtenerRecepcionQuery;
import com.softprimesolutions.compras.application.dto.result.PaginaResult;
import com.softprimesolutions.compras.application.dto.result.RecepcionResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

public interface ConsultarRecepcionesUseCase {

    Result<RecepcionResult, ApplicationError> obtener(ObtenerRecepcionQuery query);

    Result<PaginaResult<RecepcionResult>, ApplicationError> listarPorOrden(ListarRecepcionesOrdenQuery query);
}
```

`MAIN/application/port/out/ComprasReadPort.java`: agregar al final de la interfaz (antes de la `}`):

```java

    PaginaResult<RecepcionResult> findRecepcionesDeOrden(UUID tenantId, UUID ordenId, int page, int size);
```

`MAIN/application/usecase/query/ConsultarRecepcionesHandler.java`: agregar los imports `com.softprimesolutions.compras.application.dto.query.ListarRecepcionesOrdenQuery` y `com.softprimesolutions.compras.application.dto.result.PaginaResult`, y este método después de `obtener`:

```java

    @Override
    public Result<PaginaResult<RecepcionResult>, ApplicationError> listarPorOrden(ListarRecepcionesOrdenQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return Paginacion.invalida(query.page(), query.size())
                .<Result<PaginaResult<RecepcionResult>, ApplicationError>>map(Result::failure)
                .orElseGet(() -> Result.success(readPort.findRecepcionesDeOrden(
                        query.tenantId(), query.ordenId(), query.page(), query.size())));
    }
```

`MAIN/infrastructure/persistence/read/adapter/ComprasJdbcReadAdapter.java`: reemplazar la constante `RECEPCION` por estas tres (mismo texto SQL, partido en columnas, origen y filtro):

```java
    private static final String RECEPCION_COLUMNAS = """
            SELECT r.uuid_publico, r.numero, o.uuid_publico AS orden_uuid, pr.uuid_publico AS proveedor_uuid,
                   es.uuid_publico AS establecimiento_uuid, a.uuid_publico AS almacen_uuid,
                   r.documento_proveedor_tipo, r.documento_proveedor_serie, r.documento_proveedor_numero,
                   r.guia_remision_remitente, r.guia_remision_transportista, r.fecha_recepcion,
                   r.temperatura_recepcion_c, r.humedad_relativa_pct, r.estado, r.observacion
            """;
    private static final String RECEPCION_FROM = """
              FROM sch_abastecimiento.recepcion_compra r
              JOIN sch_admin.tenant t ON t.id = r.tenant_id
              LEFT JOIN sch_abastecimiento.orden_compra o ON o.id = r.orden_compra_id AND o.tenant_id = r.tenant_id
              JOIN sch_abastecimiento.proveedor pr ON pr.id = r.proveedor_id AND pr.tenant_id = r.tenant_id
              JOIN sch_organizacion.establecimiento_farmaceutico es
                ON es.id = r.establecimiento_id AND es.tenant_id = r.tenant_id
              JOIN sch_organizacion.almacen a ON a.id = r.almacen_id AND a.tenant_id = r.tenant_id
             WHERE t.uuid_publico = :tenantId AND r.es_activo = '1'
            """;
    private static final String RECEPCION = RECEPCION_COLUMNAS + RECEPCION_FROM
            + " AND r.uuid_publico = :recepcionId";
    private static final String RECEPCIONES_DE_ORDEN = RECEPCION_COLUMNAS + RECEPCION_FROM
            + " AND o.uuid_publico = :ordenId ORDER BY r.fecha_recepcion DESC, r.id DESC LIMIT :limit OFFSET :offset";
    private static final String RECEPCIONES_DE_ORDEN_TOTAL = "SELECT COUNT(*) " + RECEPCION_FROM
            + " AND o.uuid_publico = :ordenId";
```

Y agregar este método justo después de `findRecepcion`:

```java

    @Override
    @Transactional(readOnly = true)
    public PaginaResult<RecepcionResult> findRecepcionesDeOrden(UUID tenantId, UUID ordenId, int page, int size) {
        var items = jdbcClient.sql(RECEPCIONES_DE_ORDEN)
                .param("tenantId", tenantId)
                .param("ordenId", ordenId)
                .param("limit", size)
                .param("offset", page * size)
                .query((rs, rowNumber) -> mapRecepcion(rs))
                .list()
                .stream()
                .map(recepcion -> recepcion.conLineas(lineas(tenantId, recepcion.id())))
                .toList();
        var total = jdbcClient.sql(RECEPCIONES_DE_ORDEN_TOTAL)
                .param("tenantId", tenantId)
                .param("ordenId", ordenId)
                .query(Long.class)
                .single();
        return new PaginaResult<>(items, page, size, total);
    }
```

- [ ] **Step 4: Adaptar los tests del controlador a la interfaz con dos métodos**

`ConsultarRecepcionesUseCase` deja de ser funcional, así que las lambdas `query -> ...` de `TEST/api/controller/ComprasControllersTest.java` ya no compilan. En ese archivo:

1. En el test de registro de recepción (el que hace `var controller = new RecepcionController(command -> { received.set(command); return ok(recepcionResult()); }, query -> ok(recepcionResult()));`) cambiar el segundo argumento `query -> ok(recepcionResult())` por `mock(ConsultarRecepcionesUseCase.class)`.
2. Reemplazar `getsAReceptionById` y `translatesTheReceptionFailuresToProblemDetails` por:

```java
    @Test
    void getsAReceptionById() {
        var query = mock(ConsultarRecepcionesUseCase.class);
        when(query.obtener(new ObtenerRecepcionQuery(TENANT, RECEPCION))).thenReturn(ok(recepcionResult()));
        var controller = new RecepcionController(command -> conflict(), query);

        var response = controller.getById(JWT, RECEPCION);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOfSatisfying(RecepcionResponse.class,
                body -> assertThat(body.id()).isEqualTo(RECEPCION));
    }

    @Test
    void translatesTheReceptionFailuresToProblemDetails() {
        var query = mock(ConsultarRecepcionesUseCase.class);
        when(query.obtener(any())).thenReturn(conflict());
        var controller = new RecepcionController(command -> conflict(), query);

        assertConflict(controller.register(JWT, null, recepcionRequest()));
        assertConflict(controller.getById(JWT, UUID.randomUUID()));
    }
```

3. Agregar los imports que falten: `com.softprimesolutions.compras.application.port.in.ConsultarRecepcionesUseCase`, `static org.mockito.ArgumentMatchers.any`, `static org.mockito.Mockito.when` (y `mock` si no está). Quitar el `AtomicReference<ObtenerRecepcionQuery>` que quede sin uso.

- [ ] **Step 5: Ejecutar y verificar que pasan**

Run: `.\gradlew.bat :modules:compras:test`
Expected: PASS (toda la suite del módulo).

- [ ] **Step 6: Commit**

```bash
git add service-botica/modules/compras
git commit -m "feat(compras): consulta de recepciones por orden en aplicacion y lectura

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Endpoint REST y mapper

**Files:**
- Modify: `MAIN/api/mapper/ComprasApiMapper.java`
- Modify: `MAIN/api/controller/RecepcionController.java`
- Test: `TEST/api/mapper/ComprasApiMapperTest.java`, `TEST/api/controller/ComprasControllersTest.java`

**Interfaces:**
- Consumes: `ConsultarRecepcionesUseCase.listarPorOrden`, `ListarRecepcionesOrdenQuery` (Task 1).
- Produces: `ComprasApiMapper.toRecepcionPage(PaginaResult<RecepcionResult>): PaginaResponse<RecepcionResponse>`; `RecepcionController.listByOrden(Jwt, UUID ordenCompraId, int page, int size): ResponseEntity<?>` mapeado a `GET /api/v1/compras/recepciones`.

- [ ] **Step 1: Escribir los tests que fallan**

En `TEST/api/mapper/ComprasApiMapperTest.java`, importar `static com.softprimesolutions.compras.ComprasResultFixtures.recepcionResult` si no está y agregar:

```java
    @Test
    void mapsThePageOfReceptions() {
        var pagina = ComprasApiMapper.toRecepcionPage(new PaginaResult<>(List.of(recepcionResult()), 1, 5, 6));

        assertThat(pagina.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(RECEPCION);
            assertThat(item.lineas()).hasSize(1);
        });
        assertThat(pagina.page()).isEqualTo(1);
        assertThat(pagina.size()).isEqualTo(5);
        assertThat(pagina.totalElements()).isEqualTo(6);
    }
```
(`RECEPCION` y `PaginaResult`/`List` ya están importados en ese archivo; si `RECEPCION` no lo está, importar `static com.softprimesolutions.compras.ComprasFixtures.RECEPCION`.)

En `TEST/api/controller/ComprasControllersTest.java` agregar este test y reemplazar `translatesTheReceptionFailuresToProblemDetails` (dejado por la Task 1) por la versión extendida:

```java
    @Test
    void listsTheReceptionsOfAnOrderForTheTenantOfTheToken() {
        var query = mock(ConsultarRecepcionesUseCase.class);
        when(query.listarPorOrden(new ListarRecepcionesOrdenQuery(TENANT, ORDEN, 1, 5)))
                .thenReturn(ok(new PaginaResult<>(List.of(recepcionResult()), 1, 5, 6)));
        var controller = new RecepcionController(command -> conflict(), query);

        var response = controller.listByOrden(JWT, ORDEN, 1, 5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOfSatisfying(PaginaResponse.class, body -> {
            assertThat(body.items()).hasSize(1);
            assertThat(body.totalElements()).isEqualTo(6);
        });
    }

    @Test
    void translatesTheReceptionFailuresToProblemDetails() {
        var query = mock(ConsultarRecepcionesUseCase.class);
        when(query.obtener(any())).thenReturn(conflict());
        when(query.listarPorOrden(any())).thenReturn(conflict());
        var controller = new RecepcionController(command -> conflict(), query);

        assertConflict(controller.register(JWT, null, recepcionRequest()));
        assertConflict(controller.getById(JWT, UUID.randomUUID()));
        assertConflict(controller.listByOrden(JWT, ORDEN, 0, 20));
    }
```

Agregar los imports que falten en ese archivo: `com.softprimesolutions.compras.application.dto.query.ListarRecepcionesOrdenQuery`, `com.softprimesolutions.compras.application.dto.result.PaginaResult`, `com.softprimesolutions.compras.api.dto.response.PaginaResponse`, `static com.softprimesolutions.compras.ComprasFixtures.ORDEN` y `java.util.List` si no estuvieran.

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `.\gradlew.bat :modules:compras:test --tests "com.softprimesolutions.compras.api.*"`
Expected: FAIL de compilación (`toRecepcionPage` y `listByOrden` no existen).

- [ ] **Step 3: Implementar**

`MAIN/api/mapper/ComprasApiMapper.java`: agregar junto a `toOrdenPage`:

```java

    public static PaginaResponse<RecepcionResponse> toRecepcionPage(PaginaResult<RecepcionResult> page) {
        return toPage(page, ComprasApiMapper::toResponse);
    }
```

`MAIN/api/controller/RecepcionController.java`: agregar los imports `com.softprimesolutions.compras.application.dto.query.ListarRecepcionesOrdenQuery` y `org.springframework.web.bind.annotation.RequestParam`, y este método al final de la clase:

```java

    @GetMapping
    @PreAuthorize("hasAuthority('compras.recepciones.consultar')")
    public ResponseEntity<?> listByOrden(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID ordenCompraId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var query = new ListarRecepcionesOrdenQuery(
                ComprasControllerSupport.tenantOf(jwt), ordenCompraId, page, size);
        return queryRecepciones.listarPorOrden(query).fold(
                result -> ResponseEntity.ok(ComprasApiMapper.toRecepcionPage(result)),
                ComprasControllerSupport::problem);
    }
```

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `.\gradlew.bat :modules:compras:test`
Expected: PASS (toda la suite del módulo, incluidas las clases de la Task 1).

- [ ] **Step 5: Verificar la cobertura del módulo**

Run: `.\gradlew.bat :modules:compras:check`
Expected: BUILD SUCCESSFUL (JaCoCo por clase en verde; si `ComprasJdbcReadAdapter`, `ConsultarRecepcionesHandler`, `RecepcionController` o `ComprasApiMapper` bajan de 100%, agregar el caso faltante).

- [ ] **Step 6: Commit**

```bash
git add service-botica/modules/compras
git commit -m "feat(compras): listar recepciones de una orden

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Test de integración contra PostgreSQL y verificación completa

**Files:**
- Test: `IT/ComprasApiIntegrationTest.java`

**Interfaces:**
- Consumes: `GET /api/v1/compras/recepciones` (Task 2) y los helpers privados existentes del test: `proveedor(ruc, razonSocial)`, `ordenEmitida(proveedorId, cantidad, tolerancia)`, `recepcion(ordenId, almacen, lote, vencimiento, recibida, rechazada, clave)`, `bearer()`, `noPermissions`, `almacenId`.

- [ ] **Step 1: Escribir el test que falla**

Agregar este test en `IT/ComprasApiIntegrationTest.java` justo antes de `deniesAccessWithoutTheRequiredPermissionOrToken`:

```java
    @Test
    void listsTheReceptionsOfAnOrderNewestFirstWithTheirLinesAndPaginates() throws Exception {
        var proveedorId = proveedor("20100070970", "Laboratorios Peru SAC");
        var ordenId = ordenEmitida(proveedorId, "10", "0");
        var otraOrdenId = ordenEmitida(proveedorId, "10", "0");
        var vencimiento = LocalDate.now().plusYears(1);
        var primera = recepcion(ordenId, almacenId, "LP-020", vencimiento, "4", "0", null)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var segunda = recepcion(ordenId, almacenId, "LP-021", vencimiento, "3", "0", null)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        recepcion(otraOrdenId, almacenId, "LP-022", vencimiento, "2", "0", null).andExpect(status().isCreated());

        mockMvc.perform(get(COM + "/recepciones").header("Authorization", bearer())
                        .param("ordenCompraId", ordenId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].id").value((String) JsonPath.read(segunda, "$.id")))
                .andExpect(jsonPath("$.items[1].id").value((String) JsonPath.read(primera, "$.id")))
                .andExpect(jsonPath("$.items[0].lineas[0].numeroLote").value("LP-021"))
                .andExpect(jsonPath("$.items[0].lineas[0].loteId").isNotEmpty())
                .andExpect(jsonPath("$.items[1].lineas[0].cantidadAceptada").value(4));
        mockMvc.perform(get(COM + "/recepciones").header("Authorization", bearer())
                        .param("ordenCompraId", ordenId.toString()).param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value((String) JsonPath.read(primera, "$.id")));
    }

    @Test
    void listingReceptionsValidatesTheParametersAndTheirScope() throws Exception {
        var ordenId = ordenEmitida(proveedor("20100070970", "Laboratorios Peru SAC"), "10", "0");

        mockMvc.perform(get(COM + "/recepciones").header("Authorization", bearer()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(COM + "/recepciones").header("Authorization", bearer())
                        .param("ordenCompraId", ordenId.toString()).param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COM_PAGINACION_INVALIDA"));
        mockMvc.perform(get(COM + "/recepciones").header("Authorization", bearer())
                        .param("ordenCompraId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.items.length()").value(0));
        mockMvc.perform(get(COM + "/recepciones").header("Authorization", noPermissions)
                        .param("ordenCompraId", ordenId.toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(COM + "/recepciones").param("ordenCompraId", ordenId.toString()))
                .andExpect(status().isUnauthorized());
    }
```

- [ ] **Step 2: Ejecutar el test de integración**

Run: `.\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.compras.api.ComprasApiIntegrationTest"`
Expected: PASS (los dos tests nuevos y los existentes; requiere Docker para Testcontainers). Si un `jsonPath(...).value(...)` falla por tipo (`Integer` vs `Long`), ajustar solo ese valor esperado.

- [ ] **Step 3: Verificación completa**

Run: `.\gradlew.bat check --warning-mode all`
Expected: BUILD SUCCESSFUL (tests, JaCoCo por clase, ArchUnit y Spring Modulith `verify`).

- [ ] **Step 4: Commit**

```bash
git add service-botica/bootstrap-app/src/test/java/com/softprimesolutions/compras/api/ComprasApiIntegrationTest.java
git commit -m "test(compras): integracion del listado de recepciones por orden

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

## Self-Review

**Cobertura del spec (sección backend):** endpoint `GET /recepciones?ordenCompraId&page&size` con `ordenCompraId` obligatorio y recepciones completas con líneas y lote, orden por fecha descendente, permiso `compras.recepciones.consultar` (Task 2 y Task 3); capas query/handler, `ComprasReadPort`, adaptador JDBC y controlador sin migración nueva (Task 1 y 2); pruebas unitarias del handler, del adaptador, del mapper y del controlador más integración con dos recepciones, orden, paginación, parámetro obligatorio y permiso (Tasks 1–3); cobertura 100% en archivos nuevos (verificada en Task 2 Step 5 y Task 3 Step 3).

**Escaneo de placeholders:** todo paso de código incluye el código; los ajustes dependientes del repo (imports faltantes, tipo numérico de `jsonPath`) están acotados a un caso concreto. No hay "TBD".

**Consistencia de tipos:** `ListarRecepcionesOrdenQuery(tenantId, ordenId, page, size)`, `findRecepcionesDeOrden(tenantId, ordenId, page, size)`, `listarPorOrden(query)`, `toRecepcionPage(page)` y `listByOrden(jwt, ordenCompraId, page, size)` se usan con las mismas firmas en las tres tareas.
