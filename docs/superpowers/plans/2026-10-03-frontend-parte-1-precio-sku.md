# Frontend, parte 1: precio de referencia y búsqueda de SKU para el POS (backend `catalogo`) — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Agregar el precio de venta de referencia al SKU y enriquecer el listado de SKUs (unidad de venta, venta por fracción, precio de referencia) y su búsqueda (por código de barras) para que el POS pueda buscar, precargar el precio y validar la cantidad.

**Architecture:** Cambio transversal y mecánico en `catalogo` siguiendo el patrón existente: columna nueva (`V037`), componente nuevo en el dominio `SKUComercial`, en comandos, resultados, DTOs, entidad JPA y adapters (escritura JDBC/JPA y lectura JDBC). El listado `SkuResumen` gana tres campos y su filtro de texto también acepta un código de barras exacto. No cambia ningún contrato de otros módulos.

**Tech Stack:** Java 25, Spring Boot 4.1, JPA + JdbcClient, Flyway, JUnit 5 + AssertJ/Mockito, Testcontainers (PostgreSQL), Gradle 9.5.1.

Spec: `docs/superpowers/specs/2026-10-03-frontend-caja-pos-ventas-design.md`. Siguientes planes: `2026-10-03-frontend-parte-2-base-y-caja.md`, `...parte-3-ventas.md`, `...parte-4-pos.md`.

## Global Constraints

- Todo archivo fuente **nuevo** debe tener 100% de cobertura de líneas y ramas; los modificados mantienen el 100% en las líneas que cambian (gate JaCoCo por clase en `catalogo`).
- Sin comentarios explicativos en el código; preferir lambdas; prohibido código duplicado.
- `precio_venta_referencia`: `NUMERIC(18,4)` nullable, `>= 0`, máximo 1000000000, hasta 4 decimales; es un dato de referencia (el POS lo precarga y el cajero puede editarlo); `ventas` sigue recibiendo el precio que envía el POS.
- **Regla de ubicación del componente nuevo:** en todo constructor, `create`, `restore`, record o llamada posicional, `precioVentaReferencia` va **inmediatamente después de `stockMaximoDefault` y antes de `imagenUri`**. En JSON y DTOs de request/response el campo se llama `precioVentaReferencia`.
- `SkuResumen` / `SkuResumenResponse` agregan **al final** tres campos: `String unidadVentaCodigo`, `boolean permiteVentaFraccion`, `BigDecimal precioVentaReferencia`.
- La búsqueda de texto del listado (`q`) sigue buscando en descripción y código interno y suma una coincidencia **exacta** (sin distinguir mayúsculas) con cualquier código de barras del SKU.
- Los tests existentes que construyen estos tipos posicionalmente se actualizan insertando `null` (precio) en la posición indicada; no se modifican sus aserciones.
- Comandos desde `service-botica/` en PowerShell: `.\gradlew.bat :modules:catalogo:test --tests "<clase>"`.
- Commits terminan **exactamente** con `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- Si un test del plan no compila por inferencia de javac, usar un witness de tipo explícito y anotarlo en el reporte.

Rutas abreviadas: `MAIN` = `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo`, `TEST` = `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo`, `MIG` = `service-botica/bootstrap-app/src/main/resources/db/migration`, `BTEST` = `service-botica/bootstrap-app/src/test/java/com/softprimesolutions`.

---

### Task 1: Migración V037

**Files:**
- Create: `MIG/V037__sku_precio_venta_referencia.sql`
- Test: `BTEST/catalogo/db/MigrationV037Test.java`

**Interfaces:**
- Produces: columna `sch_catalogo.sku_comercial.precio_venta_referencia NUMERIC(18,4)` nullable con la restricción `ck_sku_precio_venta_referencia`.

- [ ] **Step 1: Escribir el test que falla**

Antes de escribirlo, abrir `BTEST/catalogo/db/MigrationV022Test.java` y copiar su estructura (anotaciones `@ActiveProfiles("test")`, `@Import(PostgresTestContainerConfiguration.class)`, `@SpringBootTest`, inyección de `JdbcClient`). Crear `BTEST/catalogo/db/MigrationV037Test.java`:

```java
package com.softprimesolutions.catalogo.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.testsupport.PostgresTestContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@SpringBootTest
class MigrationV037Test {

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void addsTheNullableReferenceSalePriceColumnWithTheExpectedPrecision() {
        var columna = jdbcClient.sql("""
                        SELECT is_nullable, numeric_precision, numeric_scale
                          FROM information_schema.columns
                         WHERE table_schema = 'sch_catalogo' AND table_name = 'sku_comercial'
                           AND column_name = 'precio_venta_referencia'
                        """).query().singleRow();

        assertThat(columna).containsEntry("is_nullable", "YES");
        assertThat(((Number) columna.get("numeric_precision")).intValue()).isEqualTo(18);
        assertThat(((Number) columna.get("numeric_scale")).intValue()).isEqualTo(4);
    }

    @Test
    void forbidsNegativeReferencePrices() {
        assertThat(jdbcClient.sql("""
                        SELECT pg_get_constraintdef(oid) FROM pg_constraint
                         WHERE conname = 'ck_sku_precio_venta_referencia'
                        """).query(String.class).single())
                .contains("precio_venta_referencia IS NULL").contains(">= (0)::numeric");
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `.\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.catalogo.db.MigrationV037Test"`
Expected: FAIL (columna y restricción inexistentes).

- [ ] **Step 3: Implementar**

Verificar antes que `V036` es la última migración de `MIG`. Crear `MIG/V037__sku_precio_venta_referencia.sql`:

```sql
ALTER TABLE sch_catalogo.sku_comercial
    ADD COLUMN precio_venta_referencia NUMERIC(18, 4),
    ADD CONSTRAINT ck_sku_precio_venta_referencia
        CHECK (precio_venta_referencia IS NULL OR precio_venta_referencia >= 0);
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `.\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.catalogo.db.MigrationV037Test" --tests "com.softprimesolutions.catalogo.db.MigrationV022Test"`
Expected: PASS. Si el texto de `pg_get_constraintdef` difiere (p. ej. `(precio_venta_referencia >= (0)::numeric)`), ajustar solo las subcadenas esperadas del test, no la restricción.

- [ ] **Step 5: Commit**

```bash
git add service-botica/bootstrap-app
git commit -m "feat(catalogo): migracion V037 con precio de venta de referencia del SKU

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Precio de referencia en dominio, aplicación, API y persistencia

**Files (todas bajo `MAIN` salvo indicación):**
- Modify: `domain/model/SKUComercial.java`
- Modify: `application/dto/command/CrearSkuCommand.java`, `ActualizarSkuCommand.java`, `application/dto/result/SkuResult.java`
- Modify: `application/usecase/command/CrearSkuHandler.java`, `ActualizarSkuHandler.java`, `application/mapper/CatalogoApplicationMapper.java`
- Modify: `api/dto/request/SkuRequest.java`, `api/dto/response/SkuResponse.java`, `api/mapper/CatalogoApiMapper.java`
- Modify: `infrastructure/persistence/write/entity/SkuComercialJpaEntity.java`, `infrastructure/persistence/write/mapper/CatalogoComercialWriteMapper.java`, `infrastructure/persistence/write/adapter/CatalogoComercialJpaWriteAdapter.java`
- Test: `TEST/domain/model/SKUComercialTest.java` y todos los tests existentes que construyan estos tipos (ver Step 3)

**Interfaces:**
- Produces: `SKUComercial#precioVentaReferencia(): BigDecimal`; el componente `precioVentaReferencia` en `CrearSkuCommand`, `ActualizarSkuCommand`, `SkuResult`, `SkuRequest`, `SkuResponse` y en `SKUComercial.create/restore`; `SkuComercialJpaEntity#getPrecioVentaReferencia()`; error de dominio `CAT_SKU_INVALIDO` con `field = "precioVentaReferencia"`.

- [ ] **Step 1: Escribir los tests que fallan**

En `TEST/domain/model/SKUComercialTest.java`, **primero** agregar un helper que evite repetir los 28 argumentos y úsalo en los tests nuevos (los tests existentes se actualizan en el Step 3 solo insertando `null`):

```java
    private static com.softprimesolutions.shared.kernel.result.Result<SKUComercial, com.softprimesolutions.shared.kernel.error.ErrorDetail> creaConPrecio(BigDecimal precio) {
        return SKUComercial.create(
                SKU_ID, TENANT_ID, null, null, null, TipoSku.NO_REGULADO, "SKU-PRECIO",
                "Producto con precio", null, null, "UND", null, null, null, null, null, null,
                false, null, null, true, true, true, BigDecimal.ZERO, null, precio, null, "test", CREATED_AT);
    }
```
(usar imports normales de `Result` y `ErrorDetail` en lugar de nombres calificados si el archivo ya los importa).

Agregar los tests:

```java
    @Test
    void acceptsAMissingOrValidReferencePrice() {
        assertTrue(creaConPrecio(null).isSuccess());
        assertEquals(null, creaConPrecio(null).getOrElse(error -> null).precioVentaReferencia());
        assertEquals(new BigDecimal("12.5000"),
                creaConPrecio(new BigDecimal("12.5000")).getOrElse(error -> null).precioVentaReferencia());
        assertTrue(creaConPrecio(BigDecimal.ZERO).isSuccess());
        assertTrue(creaConPrecio(new BigDecimal("1000000000")).isSuccess());
    }

    @Test
    void rejectsNegativeTooPreciseOrTooLargeReferencePrices() {
        for (var invalido : new BigDecimal[] {
            new BigDecimal("-0.01"), new BigDecimal("1.00001"), new BigDecimal("1000000000.01")}) {
            var result = creaConPrecio(invalido);

            assertTrue(result.isFailure());
            assertEquals("CAT_SKU_INVALIDO", result.fold(value -> null, error -> error.code()));
            assertEquals("precioVentaReferencia", result.fold(value -> null, error -> error.metadata().get("field")));
        }
    }
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `.\gradlew.bat :modules:catalogo:test --tests "*SKUComercialTest"`
Expected: FAIL de compilación (`create` sin el parámetro nuevo y `precioVentaReferencia()` inexistente).

- [ ] **Step 3: Implementar**

**3.1 Dominio** (`SKUComercial.java`):
- Constante: `private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("1000000000");`
- Campo `private final BigDecimal precioVentaReferencia;` justo después de `stockMaximoDefault`; parámetro en el constructor privado, en `create(...)` y en `restore(...)` (regla de ubicación); asignación en el constructor; pasarlo en las cuatro invocaciones internas del constructor (`create`, `restore`, `conActualizacion`, `copyWithCodigosBarra`) en la misma posición.
- Accesor: `public BigDecimal precioVentaReferencia() { return precioVentaReferencia; }` junto a los demás.
- Validación en `create`, justo después del bloque de `stockMaximoDefault` y antes del de `imagenUri`:

```java
        if (precioVentaReferencia != null && (precioVentaReferencia.signum() < 0
                || precioVentaReferencia.compareTo(PRECIO_MAXIMO) > 0
                || precioVentaReferencia.stripTrailingZeros().scale() > 4)) {
            return invalid("precioVentaReferencia",
                    "El precio de venta de referencia debe estar entre 0 y 1000000000 con hasta 4 decimales.");
        }
```

**3.2 Aplicación:** agregar `BigDecimal precioVentaReferencia` (regla de ubicación) a `CrearSkuCommand`, `ActualizarSkuCommand` y `SkuResult`; en `CrearSkuHandler` y `ActualizarSkuHandler` pasar `command.precioVentaReferencia()` en la llamada a `SKUComercial.create(...)`, después de `command.stockMaximoDefault()`; en `CatalogoApplicationMapper.toResult(SKUComercial)` pasar `sku.precioVentaReferencia()` después de `sku.stockMaximoDefault()`.

**3.3 API:** agregar `BigDecimal precioVentaReferencia` a `SkuRequest` (con `@DecimalMin("0.00")`, importar `jakarta.validation.constraints.DecimalMin`) y a `SkuResponse` (regla de ubicación); en `CatalogoApiMapper`, `toCreateCommand`/`toUpdateCommand` pasan `request.precioVentaReferencia()` y `toResponse(SkuResult)` pasa `result.precioVentaReferencia()`, siempre después del argumento de `stockMaximoDefault`.

**3.4 Persistencia:**
- `SkuComercialJpaEntity`: campo `@Column(name = "precio_venta_referencia") private BigDecimal precioVentaReferencia;` después de `stockMaximoDefault`; parámetro del constructor público y asignación en la misma posición; `public BigDecimal getPrecioVentaReferencia() { return precioVentaReferencia; }`.
- `CatalogoComercialWriteMapper.toEntity(SKUComercial, ...)`: pasar `sku.precioVentaReferencia()` después de `sku.stockMaximoDefault()`.
- `CatalogoComercialJpaWriteAdapter`: en el `UPDATE` JDBC agregar `precio_venta_referencia = :precioVentaReferencia,` después de `stock_maximo_default = :stockMaximoDefault,` y el parámetro `.param("precioVentaReferencia", sku.precioVentaReferencia())` después de `.param("stockMaximoDefault", ...)`; en la llamada `SKUComercial.restore(...)` (≈ línea 283) pasar `entity.getPrecioVentaReferencia()` después de `entity.getStockMaximoDefault()`.

**3.5 Actualizar los tests existentes** (solo insertar `null` en la posición nueva; sin cambiar aserciones). Localizar todos los puntos con:

```
grep -rn "SKUComercial.create(\|SKUComercial.restore(\|new CrearSkuCommand(\|new ActualizarSkuCommand(\|new SkuResult(\|new SkuRequest(\|new SkuResponse(\|new SkuComercialJpaEntity(" service-botica
```
y corregir cada uno hasta que `.\gradlew.bat :modules:catalogo:compileTestJava :bootstrap-app:compileTestJava` compile.

**3.6 Tests nuevos de propagación** (agregar a las clases de tests existentes del patrón de cada capa, con las fixtures que ya usen): (a) mapper de aplicación: `toResult` conserva `precioVentaReferencia`; (b) mapper de API: el request llega al comando y el resultado a la respuesta; (c) handlers `CrearSkuHandler`/`ActualizarSkuHandler`: un comando con precio negativo devuelve `CAT_SKU_INVALIDO`; (d) mapper de escritura y adapter JPA: la entidad y el UPDATE llevan el precio (si el adapter se prueba con `JdbcClientStub`/mocks, afirmar el parámetro `precioVentaReferencia`).

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `.\gradlew.bat :modules:catalogo:check`
Expected: BUILD SUCCESSFUL (tests, JaCoCo 100% por clase y ArchUnit).

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/catalogo service-botica/bootstrap-app
git commit -m "feat(catalogo): precio de venta de referencia en el SKU

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Listado de SKUs enriquecido y búsqueda por código de barras

**Files:**
- Modify: `MAIN/application/dto/result/SkuResumen.java`, `MAIN/api/dto/response/SkuResumenResponse.java`
- Modify: `MAIN/api/mapper/CatalogoApiMapper.java` (`toSkuPage`)
- Modify: `MAIN/infrastructure/persistence/read/repository/CatalogoJdbcReadRepository.java` (`findSkus`, `SKU_FILTER`)
- Test: tests existentes de `CatalogoJdbcReadRepository`/read adapter y del mapper de API (ubicarlos con `grep -rln "SkuResumen" service-botica/modules/catalogo/src/test`)

**Interfaces:**
- Produces: `SkuResumen(UUID id, String codigoInterno, String descripcionComercial, String tipoSku, String estado, String unidadVentaCodigo, boolean permiteVentaFraccion, BigDecimal precioVentaReferencia)`; igual para `SkuResumenResponse`; el filtro `q` del listado coincide también con un código de barras exacto.

- [ ] **Step 1: Escribir los tests que fallan**

En el test del repositorio de lectura que ya prueba `findSkus` (usa un stub/mocks de `JdbcClient`; copiar su estilo), agregar:

```java
    @Test
    void listsTheSalesDataOfEachSkuAndSearchesByExactBarcode() {
        stub.rows("s.precio_venta_referencia", skuRow());

        var skus = repository.findSkus(TENANT, "7750001234567", null, null, null, null, 0, 20);

        assertThat(skus).hasSize(1);
        var sku = skus.getFirst();
        assertThat(sku.unidadVentaCodigo()).isEqualTo("UND");
        assertThat(sku.permiteVentaFraccion()).isTrue();
        assertThat(sku.precioVentaReferencia()).isEqualByComparingTo("12.5000");
        var statement = stub.statementContaining("s.precio_venta_referencia");
        assertThat(statement.sql()).contains("sch_catalogo.sku_codigo_barra b").contains("LOWER(b.codigo_barra) = :texto");
        assertThat(statement.params()).containsEntry("texto", "7750001234567");
    }

    @Test
    void aSkuWithoutReferencePriceYieldsANullPrice() {
        var row = skuRow();
        row.put("precio_venta_referencia", null);
        stub.rows("s.precio_venta_referencia", row);

        assertThat(repository.findSkus(TENANT, "", null, null, null, null, 0, 20).getFirst().precioVentaReferencia())
                .isNull();
    }
```
con el helper `skuRow()` que devuelve un `Map` con las columnas `uuid_publico`, `codigo_interno`, `descripcion_comercial`, `tipo_sku`, `estado_comercial`, `unidad_venta_codigo` (`"UND"`), `permite_venta_fraccion` (`true`), `precio_venta_referencia` (`new BigDecimal("12.5000")`). Adaptar los nombres `stub`, `repository`, `TENANT` y el helper de filas al estilo real del test existente (si el repositorio se prueba con otro doble, usar su equivalente y conservar las aserciones).

Agregar al test del mapper de API (`toSkuPage`):

```java
    @Test
    void mapsTheSalesDataOfASkuSummaryIntoItsResponse() {
        var id = UUID.randomUUID();
        var pagina = new PaginaResult<>(
                List.of(new SkuResumen(id, "SKU-1", "Producto", "NO_REGULADO", "ACTIVO", "UND", true,
                        new BigDecimal("12.5000"))),
                0, 20, 1L);

        var respuesta = CatalogoApiMapper.toSkuPage(pagina).items().getFirst();

        assertThat(respuesta.unidadVentaCodigo()).isEqualTo("UND");
        assertThat(respuesta.permiteVentaFraccion()).isTrue();
        assertThat(respuesta.precioVentaReferencia()).isEqualByComparingTo("12.5000");
    }
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `.\gradlew.bat :modules:catalogo:test`
Expected: FAIL de compilación (`SkuResumen` sin los campos nuevos).

- [ ] **Step 3: Implementar**

`MAIN/application/dto/result/SkuResumen.java`:

```java
package com.softprimesolutions.catalogo.application.dto.result;

import java.math.BigDecimal;
import java.util.UUID;

public record SkuResumen(
        UUID id,
        String codigoInterno,
        String descripcionComercial,
        String tipoSku,
        String estado,
        String unidadVentaCodigo,
        boolean permiteVentaFraccion,
        BigDecimal precioVentaReferencia) {
}
```

`MAIN/api/dto/response/SkuResumenResponse.java`:

```java
package com.softprimesolutions.catalogo.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record SkuResumenResponse(
        UUID id,
        String codigoInterno,
        String descripcionComercial,
        String tipoSku,
        String estado,
        String unidadVentaCodigo,
        boolean permiteVentaFraccion,
        BigDecimal precioVentaReferencia) {
}
```

`CatalogoApiMapper.toSkuPage`: reemplazar la construcción por

```java
                        .map(resumen -> new SkuResumenResponse(
                                resumen.id(), resumen.codigoInterno(), resumen.descripcionComercial(),
                                resumen.tipoSku(), resumen.estado(), resumen.unidadVentaCodigo(),
                                resumen.permiteVentaFraccion(), resumen.precioVentaReferencia()))
```

`CatalogoJdbcReadRepository`: en `SKU_FILTER` reemplazar la condición de texto por

```java
               AND (:texto = '' OR LOWER(s.descripcion_comercial) LIKE :pattern
                    OR LOWER(s.codigo_interno) LIKE :pattern
                    OR EXISTS (SELECT 1 FROM sch_catalogo.sku_codigo_barra b
                                WHERE b.tenant_id = s.tenant_id AND b.sku_id = s.id
                                  AND LOWER(b.codigo_barra) = :texto))
```
y en `findSkus` reemplazar la lista de columnas y el mapeo:

```java
        return jdbcClient.sql("SELECT s.uuid_publico, s.codigo_interno, s.descripcion_comercial, s.tipo_sku, "
                        + "s.estado_comercial, s.unidad_venta_codigo, s.permite_venta_fraccion, "
                        + "s.precio_venta_referencia " + SKU_FROM + SKU_FILTER
                        + " ORDER BY s.descripcion_comercial LIMIT :limit OFFSET :offset")
```
```java
                .query((rs, rowNumber) -> new SkuResumen(
                        rs.getObject("uuid_publico", UUID.class), rs.getString("codigo_interno"),
                        rs.getString("descripcion_comercial"), rs.getString("tipo_sku"),
                        rs.getString("estado_comercial"), rs.getString("unidad_venta_codigo"),
                        rs.getBoolean("permite_venta_fraccion"), rs.getBigDecimal("precio_venta_referencia")))
```
(`countSkus` usa el mismo `SKU_FILTER`, por lo que cuenta igual con el filtro nuevo). Actualizar cualquier otro `new SkuResumen(` / `new SkuResumenResponse(` del código y de los tests (`grep -rn "new SkuResumen(\|new SkuResumenResponse(" service-botica`) para que compilen.

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `.\gradlew.bat :modules:catalogo:check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/catalogo
git commit -m "feat(catalogo): listado de SKUs con datos de venta y busqueda por codigo de barras

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Integración HTTP contra PostgreSQL y verificación completa

**Files:**
- Modify: `BTEST/catalogo/api/CatalogoComercialApiIntegrationTest.java`

**Interfaces:**
- Consumes: endpoints `/api/v1/catalogo/skus` (POST, PUT, GET, GET lista) y `/api/v1/catalogo/skus/{id}/codigos-barra`; campos `gestor`, `consultor`, `mockMvc` de la clase.

- [ ] **Step 1: Escribir los tests**

Agregar a `CatalogoComercialApiIntegrationTest`:

```java
    @Test
    void storesTheReferencePriceAndExposesItInDetailAndListing() throws Exception {
        var creado = mockMvc.perform(post("/api/v1/catalogo/skus")
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoSku":"NO_REGULADO","codigoInterno":"SKU-PRECIO",
                                 "descripcionComercial":"Producto con precio","unidadVentaCodigo":"UND",
                                 "permiteVentaFraccion":false,"requiereLote":false,"requiereVencimiento":false,
                                 "afectoIgv":true,"stockMinimoDefault":0,"precioVentaReferencia":12.5}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.precioVentaReferencia").value(12.5))
                .andReturn().getResponse().getContentAsString();
        String skuId = JsonPath.read(creado, "$.id");

        mockMvc.perform(get("/api/v1/catalogo/skus/{skuId}", skuId).header("Authorization", consultor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioVentaReferencia").value(12.5));
        mockMvc.perform(get("/api/v1/catalogo/skus").header("Authorization", consultor)
                        .param("q", "SKU-PRECIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id=='%s')].precioVentaReferencia".formatted(skuId)).value(12.5))
                .andExpect(jsonPath("$.items[?(@.id=='%s')].unidadVentaCodigo".formatted(skuId)).value("UND"))
                .andExpect(jsonPath("$.items[?(@.id=='%s')].permiteVentaFraccion".formatted(skuId)).value(false));

        mockMvc.perform(put("/api/v1/catalogo/skus/{skuId}", skuId)
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoSku":"NO_REGULADO","codigoInterno":"SKU-PRECIO",
                                 "descripcionComercial":"Producto con precio","unidadVentaCodigo":"UND",
                                 "permiteVentaFraccion":false,"requiereLote":false,"requiereVencimiento":false,
                                 "afectoIgv":true,"stockMinimoDefault":0,"precioVentaReferencia":15.75}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioVentaReferencia").value(15.75));
        mockMvc.perform(get("/api/v1/catalogo/skus/{skuId}", skuId).header("Authorization", consultor))
                .andExpect(jsonPath("$.precioVentaReferencia").value(15.75));
    }

    @Test
    void aSkuWithoutReferencePriceKeepsItNullAndInvalidPricesAreRejected() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo/skus")
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoSku":"NO_REGULADO","codigoInterno":"SKU-SIN-PRECIO",
                                 "descripcionComercial":"Producto sin precio","unidadVentaCodigo":"UND",
                                 "permiteVentaFraccion":false,"requiereLote":false,"requiereVencimiento":false,
                                 "afectoIgv":true,"stockMinimoDefault":0}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.precioVentaReferencia").doesNotExist());
        for (var invalido : new String[] {"-1", "1.00001"}) {
            mockMvc.perform(post("/api/v1/catalogo/skus")
                            .header("Authorization", gestor).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"tipoSku":"NO_REGULADO","codigoInterno":"SKU-PRECIO-MALO",
                                     "descripcionComercial":"Producto con precio invalido","unidadVentaCodigo":"UND",
                                     "permiteVentaFraccion":false,"requiereLote":false,"requiereVencimiento":false,
                                     "afectoIgv":true,"stockMinimoDefault":0,"precioVentaReferencia":%s}
                                    """.formatted(invalido)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void findsASkuByItsExactBarcodeThroughTheTextSearch() throws Exception {
        var creado = mockMvc.perform(post("/api/v1/catalogo/skus")
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoSku":"NO_REGULADO","codigoInterno":"SKU-ESCANEO",
                                 "descripcionComercial":"Producto escaneable","unidadVentaCodigo":"UND",
                                 "permiteVentaFraccion":false,"requiereLote":false,"requiereVencimiento":false,
                                 "afectoIgv":true,"stockMinimoDefault":0}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String skuId = JsonPath.read(creado, "$.id");
        mockMvc.perform(post("/api/v1/catalogo/skus/{skuId}/codigos-barra", skuId)
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoBarra\":\"7750009998887\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/catalogo/skus").header("Authorization", consultor)
                        .param("q", "7750009998887"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id=='%s')]".formatted(skuId)).exists());
        mockMvc.perform(get("/api/v1/catalogo/skus").header("Authorization", consultor)
                        .param("q", "775000999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id=='%s')]".formatted(skuId)).doesNotExist());
    }
```

- [ ] **Step 2: Ejecutar los tests de integración**

Run: `.\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.catalogo.api.CatalogoComercialApiIntegrationTest"` (requiere Docker).
Expected: PASS. Si `-1` no devuelve 400 sino 422 u otro código, ajustar a lo que haga el manejador global para `@DecimalMin` o `CAT_SKU_INVALIDO`, sin quitar la comprobación de rechazo. Si el filtro JsonPath del listado devuelve el valor como lista de un elemento, es el comportamiento esperado de Spring y no requiere cambios.

- [ ] **Step 3: Verificación completa**

Run: `.\gradlew.bat check --warning-mode all`
Expected: BUILD SUCCESSFUL (compilación, tests, ArchUnit, Spring Modulith `verify()` y JaCoCo al 100% por clase).

- [ ] **Step 4: Commit**

```bash
git add service-botica/bootstrap-app
git commit -m "test(catalogo): precio de referencia y busqueda por codigo de barras contra PostgreSQL

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage (sección de backend):** columna `V037` nullable con `CHECK >= 0` (Task 1); campo en dominio, request, response y listado (Tasks 2–3); tests con cobertura 100% (todas las tareas). **Desviación del spec (necesaria):** el spec sólo pedía el precio, pero el POS también necesita, del listado, `unidadVentaCodigo` y `permiteVentaFraccion` para validar cantidades y la búsqueda por código de barras para el escaneo, que el filtro `q` actual no soporta; ambos se agregan en la Task 3 y se documentan en el spec del frontend al ejecutar el plan 4.

**Riesgos conocidos a vigilar al ejecutar:** (a) el cambio de `SKUComercial.create/restore` rompe llamadas posicionales en muchos tests: se resuelve con `grep` + compilación como indica la Task 2 Step 3.5; (b) el Task 3 asume que `findSkus` se prueba con un doble de `JdbcClient`: si el test real usa otra técnica, mantener las aserciones y adaptar el montaje; (c) `LOWER(b.codigo_barra) = :texto` usa el texto ya normalizado (recortado y en minúsculas), por lo que sólo coincide con códigos exactos.

**Consistencia de tipos:** el componente `precioVentaReferencia` ocupa la misma posición en `SKUComercial`, `CrearSkuCommand`, `ActualizarSkuCommand`, `SkuResult`, `SkuRequest`, `SkuResponse`, la entidad JPA y los mappers (Task 2); `SkuResumen` y `SkuResumenResponse` agregan los mismos tres campos al final y en el mismo orden (Task 3).
