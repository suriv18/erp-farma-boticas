# Módulo Catálogo (BC-CAT) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar el módulo `catalogo` de `service-botica` (bounded context BC-CAT — Catálogo Farmacéutico Regulatorio) con 9 entidades/agregados mapeados contra las tablas ya existentes de `sch_farmacia` (V003), CRUD completo, persistencia real (JPA escritura + JDBC lectura), y API REST con autorización.

**Architecture:** Clean Architecture + DDD + Ports & Adapters + CQRS, siguiendo exactamente el patrón de `modules/security`: `domain/model` + `domain/valueobject`, `application/dto/{command,query,result}` + `application/port/{in,out}` + `application/usecase/{command,query}`, `infrastructure/persistence/{read,write}`, `api/{controller,dto,mapper}`. Sin navegación JPA entre entidades (`@ManyToOne`/`@JoinColumn`/`@OneToMany` no se usan en ningún lado del proyecto) — todas las FKs son columnas `Long` planas, y las colecciones hijas (principios activos de un producto regulado, códigos de barra de un SKU) se persisten en su tabla propia vía `JdbcClient`, no vía JPA.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Data JPA, Spring JDBC (`JdbcClient`), PostgreSQL 18 + Flyway, JUnit 5, MockMvc, Spring Modulith 2.1.

## Global Constraints

- Backend: `cd service-botica && .\gradlew.bat check --warning-mode all` debe pasar (build + tests + ArchUnit + Spring Modulith verify) antes de dar por terminado el trabajo.
- **No se crea ninguna migración de esquema nueva.** Las 12 tablas de este dominio (`sch_farmacia.condicion_venta`, `forma_farmaceutica`, `via_administracion`, `unidad_medida`, `clasificacion_controlada`, `principio_activo`, `categoria_producto`, `marca`, `producto_regulado`, `producto_principio_activo`, `sku_comercial`, `sku_codigo_barra`) ya existen y corren como Flyway real vía `docs/cadena-farmacias-docs/database/migrations/V003__catalogo_farmaceutico.sql`. La única migración nueva de este plan es de **siembra de permisos** (Task 1), reutilizando el módulo `CATALOGO` ya sembrado en `V016__navegacion_dinamica_rbac.sql`.
- Migración nueva de permisos numerada V022 (verificar en Task 1 que sigue siendo el número libre — la última migración propia del proyecto al momento de escribir este plan es `V021__separar_identidad_membership.sql`).
- El módulo `catalogo` ya existe como scaffold (`build.gradle`, `package-info.java` con `@ApplicationModule(id="catalogo", allowedDependencies={"organizacion::api"})`, `api/package-info.java` con `@NamedInterface("api")`), y ya está registrado en `settings.gradle` y referenciado en `bootstrap-app/build.gradle` — no crear estos archivos desde cero.
- El perfil de test de integración real (Testcontainers+Postgres) usa `@ActiveProfiles("test")` + `@Import(PostgresTestContainerConfiguration.class)` + `@SpringBootTest` (+ `@AutoConfigureMockMvc` para tests HTTP) — NO un perfil llamado "integration". Copiar exactamente las anotaciones de clase de `IamApiIntegrationTest.java`/`MigrationV021Test.java`.
- Los agregados de dominio son inmutables: factories estáticas `create(...)` devuelven `Result<T, ErrorDetail>`; `restore(...)` reconstruye sin re-validar. Activar/desactivar **no pasa por el agregado** — es un `UPDATE` directo vía el puerto de escritura (patrón `SecurityControlService.changeXStatus`), devolviendo `Result<Unit, ApplicationError>`.
- **Sin navegación JPA entre entidades**: nunca usar `@ManyToOne`, `@JoinColumn`, `@OneToMany` ni `@ElementCollection`. Todas las FKs (incluidas las compuestas `(tenant_id, id)` y auto-referenciales) son columnas `Long` planas en la entidad JPA. Colecciones hijas (principios activos de un producto regulado vía `producto_principio_activo`; códigos de barra de un SKU vía `sku_codigo_barra`) se leen/escriben con `JdbcClient` directo en el adapter, con el patrón "DELETE + re-INSERT" para reemplazos completos (igual que `IamJpaWriteAdapter.replacePermissions`) o INSERT/DELETE puntual para altas/bajas individuales (agregar/eliminar un código de barra).
- **Multi-tenant selectivo**: `Marca`, `CategoriaProducto`, `SKUComercial` son tenant-scoped (columna `tenant_id`, FK a `sch_farmacia.tenant`). `CondicionVenta`, `FormaFarmaceutica`, `ViaAdministracion`, `UnidadMedida`, `ClasificacionControlada`, `PrincipioActivo`, `ProductoRegulado` son catálogos **globales** (sin `tenant_id` en la tabla real) — no llevan `tenantId` en sus commands/queries ni en sus adapters.
- `tenantId` en los commands/queries de entidades tenant-scoped es el UUID público de `sch_farmacia.tenant` (resuelto del claim `tid` del JWT en el controller vía `@RequestParam UUID tenantId`, igual que `RolController`/`UsuarioController`). Los adapters de persistencia lo resuelven al `id` interno (`BIGINT`) con el patrón `findTenantId(UUID): Optional<Long>` de `IamJpaWriteAdapter`.
- Sin comentarios explicativos en el código salvo invariantes no obvias; Result pattern para errores esperables; nombres en español para el dominio de negocio, inglés para tipos técnicos ya establecidos por el framework.
- No modificar `docs/cadena-farmacias-docs/database/migrations/*.sql` (registro histórico, incluye V003 y V016 — solo lectura de referencia).

---

## Fase 1 — Migración de permisos

### Task 1: Migración Flyway que siembra los 12 permisos de Catálogo

**Files:**
- Create: `service-botica/bootstrap-app/src/main/resources/db/migration/V022__seed_catalogo_permissions.sql`
- Test: `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/db/MigrationV022Test.java`

**Interfaces:**
- Produces: 12 filas en `sch_seguridad.permiso` con `modulo_id` apuntando al módulo `CATALOGO` (ya existente). Usados por Task 26 (autorización de controllers) y el test de integración final (Task 27).

- [ ] **Step 1: Verificar el número de migración siguiente disponible**

Run: `ls service-botica/bootstrap-app/src/main/resources/db/migration/`
Expected: la migración más alta hoy es `V021__separar_identidad_membership.sql`. Usar `V022`. Si ya existe un `V022` por trabajo posterior no reflejado en este plan, usar el siguiente número libre y ajustar el nombre de archivo en los pasos restantes de esta tarea.

- [ ] **Step 2: Confirmar que el módulo `CATALOGO` ya existe en `sch_seguridad.modulo_sistema`**

Run: `grep -n "CATALOGO" docs/cadena-farmacias-docs/database/migrations/V016__navegacion_dinamica_rbac.sql`
Expected: una línea `('CATALOGO','Catálogo','Productos regulatorios y SKU comerciales.',30),` dentro de un `INSERT INTO sch_seguridad.modulo_sistema`. Esto confirma que la migración de este plan NO debe volver a insertar el módulo, solo los permisos.

- [ ] **Step 3: Escribir el test de integración que falla**

Crear `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/db/MigrationV022Test.java`. Copiar las anotaciones de clase exactas desde `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/security/db/MigrationV021Test.java`:

Run: `grep -n "@ActiveProfiles\|@SpringBootTest\|@Import\|@Transactional\|@RecordApplicationEvents" service-botica/bootstrap-app/src/test/java/com/softprimesolutions/security/db/MigrationV021Test.java`

Luego crear el archivo:

```java
package com.softprimesolutions.catalogo.db;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MigrationV022Test {

    @Autowired
    private DataSource dataSource;

    @Test
    void seedsTwelveCatalogoPermissionsUnderTheExistingModule() {
        var jdbc = JdbcClient.create(dataSource);

        var permissionCount = jdbc.sql("""
                        SELECT COUNT(*) FROM sch_seguridad.permiso p
                         JOIN sch_seguridad.modulo_sistema m ON m.id = p.modulo_id
                        WHERE m.codigo = 'CATALOGO'
                          AND p.codigo IN (
                            'catalogo.soporte.consultar', 'catalogo.soporte.gestionar',
                            'catalogo.principios-activos.consultar', 'catalogo.principios-activos.gestionar',
                            'catalogo.marcas.consultar', 'catalogo.marcas.gestionar',
                            'catalogo.categorias.consultar', 'catalogo.categorias.gestionar',
                            'catalogo.productos-regulados.consultar', 'catalogo.productos-regulados.gestionar',
                            'catalogo.skus.consultar', 'catalogo.skus.gestionar'
                          )
                        """)
                .query(Long.class).single();
        assertThat(permissionCount).isEqualTo(12L);

        var moduleCount = jdbc.sql("""
                        SELECT COUNT(*) FROM sch_seguridad.modulo_sistema WHERE codigo = 'CATALOGO'
                        """)
                .query(Long.class).single();
        assertThat(moduleCount).isEqualTo(1L);
    }
}
```

Ajustar las anotaciones de clase según lo revelado en el Step 2 de `MigrationV021Test.java` si difieren de lo asumido aquí.

- [ ] **Step 4: Ejecutar y verificar que el test falla**

Run: `cd service-botica && .\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.catalogo.db.MigrationV022Test"`
Expected: FAIL — los 12 permisos no existen todavía.

- [ ] **Step 5: Escribir la migración `V022__seed_catalogo_permissions.sql`**

Crear `service-botica/bootstrap-app/src/main/resources/db/migration/V022__seed_catalogo_permissions.sql`, siguiendo exactamente el patrón de `V018__seed_security_administration_permissions.sql`:

```sql
INSERT INTO sch_seguridad.permiso
    (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
SELECT m.id, seed.codigo, seed.recurso, seed.accion, seed.nombre, seed.descripcion, seed.es_critico, 'ACTIVO'
FROM sch_seguridad.modulo_sistema m
CROSS JOIN (VALUES
    ('catalogo.soporte.consultar', 'CATALOGO_SOPORTE', 'CONSULTAR', 'Consultar catalogos de soporte',
     'Permite consultar condicion de venta, forma farmaceutica, via de administracion, unidad de medida y clasificacion controlada.', FALSE),
    ('catalogo.soporte.gestionar', 'CATALOGO_SOPORTE', 'GESTIONAR', 'Gestionar catalogos de soporte',
     'Permite crear, editar y cambiar el estado de los catalogos de soporte regulatorio.', TRUE),
    ('catalogo.principios-activos.consultar', 'PRINCIPIO_ACTIVO', 'CONSULTAR', 'Consultar principios activos',
     'Permite consultar el catalogo de principios activos.', FALSE),
    ('catalogo.principios-activos.gestionar', 'PRINCIPIO_ACTIVO', 'GESTIONAR', 'Gestionar principios activos',
     'Permite crear, editar y cambiar el estado de principios activos.', TRUE),
    ('catalogo.marcas.consultar', 'MARCA', 'CONSULTAR', 'Consultar marcas',
     'Permite consultar las marcas registradas por el tenant.', FALSE),
    ('catalogo.marcas.gestionar', 'MARCA', 'GESTIONAR', 'Gestionar marcas',
     'Permite crear, editar y cambiar el estado de marcas.', TRUE),
    ('catalogo.categorias.consultar', 'CATEGORIA_PRODUCTO', 'CONSULTAR', 'Consultar categorias',
     'Permite consultar las categorias de producto del tenant.', FALSE),
    ('catalogo.categorias.gestionar', 'CATEGORIA_PRODUCTO', 'GESTIONAR', 'Gestionar categorias',
     'Permite crear, editar y cambiar el estado de categorias de producto.', TRUE),
    ('catalogo.productos-regulados.consultar', 'PRODUCTO_REGULADO', 'CONSULTAR', 'Consultar productos regulados',
     'Permite consultar la ficha regulatoria de productos.', FALSE),
    ('catalogo.productos-regulados.gestionar', 'PRODUCTO_REGULADO', 'GESTIONAR', 'Gestionar productos regulados',
     'Permite crear, editar y cambiar el estado de la ficha regulatoria de productos.', TRUE),
    ('catalogo.skus.consultar', 'SKU_COMERCIAL', 'CONSULTAR', 'Consultar SKU comerciales',
     'Permite consultar los SKU comerciales del tenant.', FALSE),
    ('catalogo.skus.gestionar', 'SKU_COMERCIAL', 'GESTIONAR', 'Gestionar SKU comerciales',
     'Permite crear, editar, cambiar el estado y administrar codigos de barra de SKU comerciales.', TRUE)
) AS seed(codigo, recurso, accion, nombre, descripcion, es_critico)
WHERE m.codigo = 'CATALOGO'
ON CONFLICT (codigo) DO UPDATE SET
    modulo_id = EXCLUDED.modulo_id,
    recurso = EXCLUDED.recurso,
    accion = EXCLUDED.accion,
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    es_critico = EXCLUDED.es_critico,
    estado = 'ACTIVO';
```

- [ ] **Step 6: Ejecutar y verificar que el test pasa**

Run: `cd service-botica && .\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.catalogo.db.MigrationV022Test"`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add service-botica/bootstrap-app/src/main/resources/db/migration/V022__seed_catalogo_permissions.sql service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/db/MigrationV022Test.java
git commit -m "feat(catalogo): sembrar permisos de autorizacion del modulo Catalogo"
```

---

## Fase 2 — Dominio

### Task 2: Value Objects (`TenantId`, `PrincipioActivoId`, `MarcaId`, `CategoriaProductoId`, `ProductoReguladoId`, `SkuId`)

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/valueobject/TenantId.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/valueobject/PrincipioActivoId.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/valueobject/MarcaId.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/valueobject/CategoriaProductoId.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/valueobject/ProductoReguladoId.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/valueobject/SkuId.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/valueobject/ValueObjectsTest.java`

**Interfaces:**
- Produces: 6 records inmutables sobre `UUID`, cada uno rechaza `null`. Usados por todas las tareas de dominio y aplicación siguientes.

- [ ] **Step 1: Escribir el test que falla**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/valueobject/ValueObjectsTest.java`:

```java
package com.softprimesolutions.catalogo.domain.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ValueObjectsTest {

    @Test
    void wrapsAndExposesTheUnderlyingUuid() {
        var uuid = UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589");

        assertEquals(uuid, new TenantId(uuid).value());
        assertEquals(uuid, new PrincipioActivoId(uuid).value());
        assertEquals(uuid, new MarcaId(uuid).value());
        assertEquals(uuid, new CategoriaProductoId(uuid).value());
        assertEquals(uuid, new ProductoReguladoId(uuid).value());
        assertEquals(uuid, new SkuId(uuid).value());
    }

    @Test
    void rejectsANullUuid() {
        assertThrows(NullPointerException.class, () -> new TenantId(null));
        assertThrows(NullPointerException.class, () -> new PrincipioActivoId(null));
        assertThrows(NullPointerException.class, () -> new MarcaId(null));
        assertThrows(NullPointerException.class, () -> new CategoriaProductoId(null));
        assertThrows(NullPointerException.class, () -> new ProductoReguladoId(null));
        assertThrows(NullPointerException.class, () -> new SkuId(null));
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.valueobject.ValueObjectsTest"`
Expected: FAIL — ninguno de los 6 tipos existe todavía (error de compilación).

- [ ] **Step 3: Crear los 6 Value Objects**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/valueobject/TenantId.java`:

```java
package com.softprimesolutions.catalogo.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/** Identificador público del tenant; la PK BIGINT permanece en persistencia. */
public record TenantId(UUID value) {

    public TenantId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/valueobject/PrincipioActivoId.java`:

```java
package com.softprimesolutions.catalogo.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record PrincipioActivoId(UUID value) {

    public PrincipioActivoId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/valueobject/MarcaId.java`:

```java
package com.softprimesolutions.catalogo.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record MarcaId(UUID value) {

    public MarcaId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/valueobject/CategoriaProductoId.java`:

```java
package com.softprimesolutions.catalogo.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record CategoriaProductoId(UUID value) {

    public CategoriaProductoId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/valueobject/ProductoReguladoId.java`:

```java
package com.softprimesolutions.catalogo.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record ProductoReguladoId(UUID value) {

    public ProductoReguladoId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/valueobject/SkuId.java`:

```java
package com.softprimesolutions.catalogo.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record SkuId(UUID value) {

    public SkuId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.valueobject.ValueObjectsTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/valueobject/ service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/valueobject/
git commit -m "feat(catalogo): agregar value objects del modulo Catalogo"
```

---

### Task 3: Los 5 catálogos de soporte (`CondicionVenta`, `FormaFarmaceutica`, `ViaAdministracion`, `UnidadMedida`, `ClasificacionControlada`)

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/soporte/EstadoCatalogoSoporte.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/soporte/CondicionVenta.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/soporte/FormaFarmaceutica.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/soporte/ViaAdministracion.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/soporte/UnidadMedida.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/soporte/ClasificacionControlada.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/soporte/CondicionVentaTest.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/soporte/FormaFarmaceuticaTest.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/soporte/ViaAdministracionTest.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/soporte/UnidadMedidaTest.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/soporte/ClasificacionControladaTest.java`

**Interfaces:**
- Produces:
  - `CondicionVenta.create(String codigo, String denominacion, boolean requiereReceta, boolean requiereRetencion, String fuente, String versionFuente, LocalDate vigenteDesde, LocalDate vigenteHasta): Result<CondicionVenta, ErrorDetail>`, `restore(String codigo, String denominacion, boolean requiereReceta, boolean requiereRetencion, String fuente, String versionFuente, LocalDate vigenteDesde, LocalDate vigenteHasta, EstadoCatalogoSoporte estado): CondicionVenta`.
  - `FormaFarmaceutica.create(String codigo, String denominacion, String fuente): Result<FormaFarmaceutica, ErrorDetail>`, `restore(String codigo, String denominacion, String fuente, EstadoCatalogoSoporte estado): FormaFarmaceutica`.
  - `ViaAdministracion.create(String codigo, String denominacion, String fuente): Result<ViaAdministracion, ErrorDetail>`, `restore(...)` igual forma.
  - `UnidadMedida.create(String codigo, String denominacion, String simbolo, boolean permiteDecimal, String fuente): Result<UnidadMedida, ErrorDetail>`, `restore(String codigo, String denominacion, String simbolo, boolean permiteDecimal, String fuente, EstadoCatalogoSoporte estado): UnidadMedida`.
  - `ClasificacionControlada.create(String codigo, String denominacion, String normaFuente, boolean requiereRecetaEspecial, boolean retieneReceta, Integer vigenciaRecetaDias): Result<ClasificacionControlada, ErrorDetail>`, `restore(String codigo, String denominacion, String normaFuente, boolean requiereRecetaEspecial, boolean retieneReceta, Integer vigenciaRecetaDias, EstadoCatalogoSoporte estado): ClasificacionControlada`.
  - Todos exponen getters para cada campo, más `codigo()` y `estado()`.
- Usado por Task 12 (handlers), Task 18 (mapper JPA).

- [ ] **Step 1: Escribir el test que falla para `CondicionVenta`**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/soporte/CondicionVentaTest.java`:

```java
package com.softprimesolutions.catalogo.domain.model.soporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CondicionVentaTest {

    @Test
    void createsAndNormalizesAValidCondicionVenta() {
        var result = CondicionVenta.create(
                "  sin-receta  ", "  Sin receta médica  ", false, false,
                "DIGEMID", "1.0", null, null);

        assertTrue(result.isSuccess());
        var condicion = result.getOrElse(error -> null);
        assertEquals("SIN-RECETA", condicion.codigo());
        assertEquals("Sin receta médica", condicion.denominacion());
        assertEquals(EstadoCatalogoSoporte.ACTIVO, condicion.estado());
    }

    @Test
    void rejectsAMissingCodigo() {
        var result = CondicionVenta.create(null, "Sin receta médica", false, false, null, null, null, null);

        assertTrue(result.isFailure());
        assertEquals("CAT_CONDICION_VENTA_INVALIDA", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsADenominationThatIsTooShort() {
        var result = CondicionVenta.create("SIN-RECETA", "S", false, false, null, null, null, null);

        assertTrue(result.isFailure());
        assertEquals("CAT_CONDICION_VENTA_INVALIDA", result.fold(value -> null, error -> error.code()));
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.soporte.CondicionVentaTest"`
Expected: FAIL — `CondicionVenta`/`EstadoCatalogoSoporte` no existen todavía.

- [ ] **Step 3: Crear `EstadoCatalogoSoporte`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/soporte/EstadoCatalogoSoporte.java`:

```java
package com.softprimesolutions.catalogo.domain.model.soporte;

public enum EstadoCatalogoSoporte {
    ACTIVO,
    INACTIVO
}
```

- [ ] **Step 4: Crear `CondicionVenta`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/soporte/CondicionVenta.java`:

```java
package com.softprimesolutions.catalogo.domain.model.soporte;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

/** Condición regulatoria de venta de un producto (ej. sin receta, con receta, receta retenida). */
public final class CondicionVenta {

    private static final int CODIGO_MAX_LENGTH = 30;
    private static final int DENOMINACION_MIN_LENGTH = 2;
    private static final int DENOMINACION_MAX_LENGTH = 200;
    private static final int FUENTE_MAX_LENGTH = 300;
    private static final int VERSION_FUENTE_MAX_LENGTH = 100;

    private final String codigo;
    private final String denominacion;
    private final boolean requiereReceta;
    private final boolean requiereRetencion;
    private final String fuente;
    private final String versionFuente;
    private final LocalDate vigenteDesde;
    private final LocalDate vigenteHasta;
    private final EstadoCatalogoSoporte estado;

    private CondicionVenta(
            String codigo, String denominacion, boolean requiereReceta, boolean requiereRetencion,
            String fuente, String versionFuente, LocalDate vigenteDesde, LocalDate vigenteHasta,
            EstadoCatalogoSoporte estado) {
        this.codigo = codigo;
        this.denominacion = denominacion;
        this.requiereReceta = requiereReceta;
        this.requiereRetencion = requiereRetencion;
        this.fuente = fuente;
        this.versionFuente = versionFuente;
        this.vigenteDesde = vigenteDesde;
        this.vigenteHasta = vigenteHasta;
        this.estado = estado;
    }

    public static Result<CondicionVenta, ErrorDetail> create(
            String codigo, String denominacion, boolean requiereReceta, boolean requiereRetencion,
            String fuente, String versionFuente, LocalDate vigenteDesde, LocalDate vigenteHasta) {
        var normalizedCodigo = normalizeUpper(codigo);
        if (normalizedCodigo == null || normalizedCodigo.length() > CODIGO_MAX_LENGTH) {
            return invalid("codigo", "El código es obligatorio y no debe exceder 30 caracteres.");
        }

        var normalizedDenominacion = normalizeSpaces(denominacion);
        if (normalizedDenominacion == null || normalizedDenominacion.length() < DENOMINACION_MIN_LENGTH
                || normalizedDenominacion.length() > DENOMINACION_MAX_LENGTH) {
            return invalid("denominacion", "La denominación debe tener entre 2 y 200 caracteres.");
        }

        var normalizedFuente = normalizeNullable(fuente);
        if (!withinLength(normalizedFuente, FUENTE_MAX_LENGTH)) {
            return invalid("fuente", "La fuente no debe exceder 300 caracteres.");
        }

        var normalizedVersionFuente = normalizeNullable(versionFuente);
        if (!withinLength(normalizedVersionFuente, VERSION_FUENTE_MAX_LENGTH)) {
            return invalid("versionFuente", "La versión de fuente no debe exceder 100 caracteres.");
        }

        if (vigenteDesde != null && vigenteHasta != null && vigenteHasta.isBefore(vigenteDesde)) {
            return invalid("vigenteHasta", "La vigencia hasta no puede ser anterior a la vigencia desde.");
        }

        return Result.success(new CondicionVenta(
                normalizedCodigo, normalizedDenominacion, requiereReceta, requiereRetencion,
                normalizedFuente, normalizedVersionFuente, vigenteDesde, vigenteHasta,
                EstadoCatalogoSoporte.ACTIVO));
    }

    public static CondicionVenta restore(
            String codigo, String denominacion, boolean requiereReceta, boolean requiereRetencion,
            String fuente, String versionFuente, LocalDate vigenteDesde, LocalDate vigenteHasta,
            EstadoCatalogoSoporte estado) {
        return new CondicionVenta(
                codigo, denominacion, requiereReceta, requiereRetencion, fuente, versionFuente,
                vigenteDesde, vigenteHasta, estado);
    }

    private static Result<CondicionVenta, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("CAT_CONDICION_VENTA_INVALIDA", message, Map.of("field", field)));
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

    private static String normalizeUpper(String value) {
        var normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static boolean withinLength(String value, int maximum) {
        return value == null || value.length() <= maximum;
    }

    public String codigo() { return codigo; }
    public String denominacion() { return denominacion; }
    public boolean requiereReceta() { return requiereReceta; }
    public boolean requiereRetencion() { return requiereRetencion; }
    public String fuente() { return fuente; }
    public String versionFuente() { return versionFuente; }
    public LocalDate vigenteDesde() { return vigenteDesde; }
    public LocalDate vigenteHasta() { return vigenteHasta; }
    public EstadoCatalogoSoporte estado() { return estado; }
}
```

- [ ] **Step 5: Ejecutar y verificar que `CondicionVentaTest` pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.soporte.CondicionVentaTest"`
Expected: PASS

- [ ] **Step 6: Escribir el test que falla para `FormaFarmaceutica`**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/soporte/FormaFarmaceuticaTest.java`:

```java
package com.softprimesolutions.catalogo.domain.model.soporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FormaFarmaceuticaTest {

    @Test
    void createsAndNormalizesAValidFormaFarmaceutica() {
        var result = FormaFarmaceutica.create("  tableta  ", "  Tableta  ", "DIGEMID");

        assertTrue(result.isSuccess());
        var forma = result.getOrElse(error -> null);
        assertEquals("TABLETA", forma.codigo());
        assertEquals("Tableta", forma.denominacion());
        assertEquals(EstadoCatalogoSoporte.ACTIVO, forma.estado());
    }

    @Test
    void rejectsAMissingDenominacion() {
        var result = FormaFarmaceutica.create("TABLETA", " ", null);

        assertTrue(result.isFailure());
        assertEquals("CAT_FORMA_FARMACEUTICA_INVALIDA", result.fold(value -> null, error -> error.code()));
    }
}
```

- [ ] **Step 7: Ejecutar y verificar que falla, luego crear `FormaFarmaceutica`**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceuticaTest"`
Expected: FAIL.

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/soporte/FormaFarmaceutica.java`:

```java
package com.softprimesolutions.catalogo.domain.model.soporte;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Locale;
import java.util.Map;

/** Forma en que se presenta un producto farmacéutico (ej. tableta, jarabe, crema). */
public final class FormaFarmaceutica {

    private static final int CODIGO_MAX_LENGTH = 30;
    private static final int DENOMINACION_MIN_LENGTH = 2;
    private static final int DENOMINACION_MAX_LENGTH = 200;
    private static final int FUENTE_MAX_LENGTH = 300;

    private final String codigo;
    private final String denominacion;
    private final String fuente;
    private final EstadoCatalogoSoporte estado;

    private FormaFarmaceutica(String codigo, String denominacion, String fuente, EstadoCatalogoSoporte estado) {
        this.codigo = codigo;
        this.denominacion = denominacion;
        this.fuente = fuente;
        this.estado = estado;
    }

    public static Result<FormaFarmaceutica, ErrorDetail> create(String codigo, String denominacion, String fuente) {
        var normalizedCodigo = normalizeUpper(codigo);
        if (normalizedCodigo == null || normalizedCodigo.length() > CODIGO_MAX_LENGTH) {
            return invalid("codigo", "El código es obligatorio y no debe exceder 30 caracteres.");
        }

        var normalizedDenominacion = normalizeSpaces(denominacion);
        if (normalizedDenominacion == null || normalizedDenominacion.length() < DENOMINACION_MIN_LENGTH
                || normalizedDenominacion.length() > DENOMINACION_MAX_LENGTH) {
            return invalid("denominacion", "La denominación debe tener entre 2 y 200 caracteres.");
        }

        var normalizedFuente = normalizeNullable(fuente);
        if (!withinLength(normalizedFuente, FUENTE_MAX_LENGTH)) {
            return invalid("fuente", "La fuente no debe exceder 300 caracteres.");
        }

        return Result.success(new FormaFarmaceutica(
                normalizedCodigo, normalizedDenominacion, normalizedFuente, EstadoCatalogoSoporte.ACTIVO));
    }

    public static FormaFarmaceutica restore(
            String codigo, String denominacion, String fuente, EstadoCatalogoSoporte estado) {
        return new FormaFarmaceutica(codigo, denominacion, fuente, estado);
    }

    private static Result<FormaFarmaceutica, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("CAT_FORMA_FARMACEUTICA_INVALIDA", message, Map.of("field", field)));
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

    private static String normalizeUpper(String value) {
        var normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static boolean withinLength(String value, int maximum) {
        return value == null || value.length() <= maximum;
    }

    public String codigo() { return codigo; }
    public String denominacion() { return denominacion; }
    public String fuente() { return fuente; }
    public EstadoCatalogoSoporte estado() { return estado; }
}
```

- [ ] **Step 8: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceuticaTest"`
Expected: PASS

- [ ] **Step 9: Escribir el test que falla para `ViaAdministracion`**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/soporte/ViaAdministracionTest.java`:

```java
package com.softprimesolutions.catalogo.domain.model.soporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ViaAdministracionTest {

    @Test
    void createsAndNormalizesAValidViaAdministracion() {
        var result = ViaAdministracion.create("  oral  ", "  Vía oral  ", "DIGEMID");

        assertTrue(result.isSuccess());
        var via = result.getOrElse(error -> null);
        assertEquals("ORAL", via.codigo());
        assertEquals("Vía oral", via.denominacion());
    }

    @Test
    void rejectsAMissingCodigo() {
        var result = ViaAdministracion.create(null, "Vía oral", null);

        assertTrue(result.isFailure());
        assertEquals("CAT_VIA_ADMINISTRACION_INVALIDA", result.fold(value -> null, error -> error.code()));
    }
}
```

- [ ] **Step 10: Ejecutar y verificar que falla, luego crear `ViaAdministracion`**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracionTest"`
Expected: FAIL.

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/soporte/ViaAdministracion.java` (misma forma que `FormaFarmaceutica`, solo cambia nombre de clase y código de error):

```java
package com.softprimesolutions.catalogo.domain.model.soporte;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Locale;
import java.util.Map;

/** Vía por la que se administra un producto farmacéutico (ej. oral, tópica, inyectable). */
public final class ViaAdministracion {

    private static final int CODIGO_MAX_LENGTH = 30;
    private static final int DENOMINACION_MIN_LENGTH = 2;
    private static final int DENOMINACION_MAX_LENGTH = 200;
    private static final int FUENTE_MAX_LENGTH = 300;

    private final String codigo;
    private final String denominacion;
    private final String fuente;
    private final EstadoCatalogoSoporte estado;

    private ViaAdministracion(String codigo, String denominacion, String fuente, EstadoCatalogoSoporte estado) {
        this.codigo = codigo;
        this.denominacion = denominacion;
        this.fuente = fuente;
        this.estado = estado;
    }

    public static Result<ViaAdministracion, ErrorDetail> create(String codigo, String denominacion, String fuente) {
        var normalizedCodigo = normalizeUpper(codigo);
        if (normalizedCodigo == null || normalizedCodigo.length() > CODIGO_MAX_LENGTH) {
            return invalid("codigo", "El código es obligatorio y no debe exceder 30 caracteres.");
        }

        var normalizedDenominacion = normalizeSpaces(denominacion);
        if (normalizedDenominacion == null || normalizedDenominacion.length() < DENOMINACION_MIN_LENGTH
                || normalizedDenominacion.length() > DENOMINACION_MAX_LENGTH) {
            return invalid("denominacion", "La denominación debe tener entre 2 y 200 caracteres.");
        }

        var normalizedFuente = normalizeNullable(fuente);
        if (!withinLength(normalizedFuente, FUENTE_MAX_LENGTH)) {
            return invalid("fuente", "La fuente no debe exceder 300 caracteres.");
        }

        return Result.success(new ViaAdministracion(
                normalizedCodigo, normalizedDenominacion, normalizedFuente, EstadoCatalogoSoporte.ACTIVO));
    }

    public static ViaAdministracion restore(
            String codigo, String denominacion, String fuente, EstadoCatalogoSoporte estado) {
        return new ViaAdministracion(codigo, denominacion, fuente, estado);
    }

    private static Result<ViaAdministracion, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("CAT_VIA_ADMINISTRACION_INVALIDA", message, Map.of("field", field)));
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

    private static String normalizeUpper(String value) {
        var normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static boolean withinLength(String value, int maximum) {
        return value == null || value.length() <= maximum;
    }

    public String codigo() { return codigo; }
    public String denominacion() { return denominacion; }
    public String fuente() { return fuente; }
    public EstadoCatalogoSoporte estado() { return estado; }
}
```

- [ ] **Step 11: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracionTest"`
Expected: PASS

- [ ] **Step 12: Escribir el test que falla para `UnidadMedida`**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/soporte/UnidadMedidaTest.java`:

```java
package com.softprimesolutions.catalogo.domain.model.soporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UnidadMedidaTest {

    @Test
    void createsAndNormalizesAValidUnidadMedida() {
        var result = UnidadMedida.create("  mg  ", "  Miligramo  ", "mg", false, "DIGEMID");

        assertTrue(result.isSuccess());
        var unidad = result.getOrElse(error -> null);
        assertEquals("MG", unidad.codigo());
        assertEquals("Miligramo", unidad.denominacion());
        assertEquals("mg", unidad.simbolo());
        assertTrue(!unidad.permiteDecimal());
    }

    @Test
    void rejectsAMissingDenominacion() {
        var result = UnidadMedida.create("MG", null, null, false, null);

        assertTrue(result.isFailure());
        assertEquals("CAT_UNIDAD_MEDIDA_INVALIDA", result.fold(value -> null, error -> error.code()));
    }
}
```

- [ ] **Step 13: Ejecutar y verificar que falla, luego crear `UnidadMedida`**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedidaTest"`
Expected: FAIL.

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/soporte/UnidadMedida.java`:

```java
package com.softprimesolutions.catalogo.domain.model.soporte;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Locale;
import java.util.Map;

/** Unidad de medida usada para cantidades y concentraciones (ej. mg, ml, comprimidos). */
public final class UnidadMedida {

    private static final int CODIGO_MAX_LENGTH = 30;
    private static final int DENOMINACION_MIN_LENGTH = 2;
    private static final int DENOMINACION_MAX_LENGTH = 150;
    private static final int SIMBOLO_MAX_LENGTH = 30;
    private static final int FUENTE_MAX_LENGTH = 300;

    private final String codigo;
    private final String denominacion;
    private final String simbolo;
    private final boolean permiteDecimal;
    private final String fuente;
    private final EstadoCatalogoSoporte estado;

    private UnidadMedida(
            String codigo, String denominacion, String simbolo, boolean permiteDecimal, String fuente,
            EstadoCatalogoSoporte estado) {
        this.codigo = codigo;
        this.denominacion = denominacion;
        this.simbolo = simbolo;
        this.permiteDecimal = permiteDecimal;
        this.fuente = fuente;
        this.estado = estado;
    }

    public static Result<UnidadMedida, ErrorDetail> create(
            String codigo, String denominacion, String simbolo, boolean permiteDecimal, String fuente) {
        var normalizedCodigo = normalizeUpper(codigo);
        if (normalizedCodigo == null || normalizedCodigo.length() > CODIGO_MAX_LENGTH) {
            return invalid("codigo", "El código es obligatorio y no debe exceder 30 caracteres.");
        }

        var normalizedDenominacion = normalizeSpaces(denominacion);
        if (normalizedDenominacion == null || normalizedDenominacion.length() < DENOMINACION_MIN_LENGTH
                || normalizedDenominacion.length() > DENOMINACION_MAX_LENGTH) {
            return invalid("denominacion", "La denominación debe tener entre 2 y 150 caracteres.");
        }

        var normalizedSimbolo = normalizeNullable(simbolo);
        if (!withinLength(normalizedSimbolo, SIMBOLO_MAX_LENGTH)) {
            return invalid("simbolo", "El símbolo no debe exceder 30 caracteres.");
        }

        var normalizedFuente = normalizeNullable(fuente);
        if (!withinLength(normalizedFuente, FUENTE_MAX_LENGTH)) {
            return invalid("fuente", "La fuente no debe exceder 300 caracteres.");
        }

        return Result.success(new UnidadMedida(
                normalizedCodigo, normalizedDenominacion, normalizedSimbolo, permiteDecimal, normalizedFuente,
                EstadoCatalogoSoporte.ACTIVO));
    }

    public static UnidadMedida restore(
            String codigo, String denominacion, String simbolo, boolean permiteDecimal, String fuente,
            EstadoCatalogoSoporte estado) {
        return new UnidadMedida(codigo, denominacion, simbolo, permiteDecimal, fuente, estado);
    }

    private static Result<UnidadMedida, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("CAT_UNIDAD_MEDIDA_INVALIDA", message, Map.of("field", field)));
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

    private static String normalizeUpper(String value) {
        var normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static boolean withinLength(String value, int maximum) {
        return value == null || value.length() <= maximum;
    }

    public String codigo() { return codigo; }
    public String denominacion() { return denominacion; }
    public String simbolo() { return simbolo; }
    public boolean permiteDecimal() { return permiteDecimal; }
    public String fuente() { return fuente; }
    public EstadoCatalogoSoporte estado() { return estado; }
}
```

- [ ] **Step 14: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedidaTest"`
Expected: PASS

- [ ] **Step 15: Escribir el test que falla para `ClasificacionControlada`**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/soporte/ClasificacionControladaTest.java`:

```java
package com.softprimesolutions.catalogo.domain.model.soporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClasificacionControladaTest {

    @Test
    void createsAndNormalizesAValidClasificacionControlada() {
        var result = ClasificacionControlada.create(
                "  lista-i  ", "  Lista I - Estupefacientes  ", "DL-22095", true, true, 30);

        assertTrue(result.isSuccess());
        var clasificacion = result.getOrElse(error -> null);
        assertEquals("LISTA-I", clasificacion.codigo());
        assertEquals(30, clasificacion.vigenciaRecetaDias());
    }

    @Test
    void rejectsANonPositiveVigenciaRecetaDias() {
        var result = ClasificacionControlada.create("LISTA-I", "Lista I", null, true, true, 0);

        assertTrue(result.isFailure());
        assertEquals("CAT_CLASIFICACION_CONTROLADA_INVALIDA", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void allowsANullVigenciaRecetaDias() {
        var result = ClasificacionControlada.create("LISTA-I", "Lista I", null, true, true, null);

        assertTrue(result.isSuccess());
    }
}
```

- [ ] **Step 16: Ejecutar y verificar que falla, luego crear `ClasificacionControlada`**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControladaTest"`
Expected: FAIL.

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/soporte/ClasificacionControlada.java`:

```java
package com.softprimesolutions.catalogo.domain.model.soporte;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Locale;
import java.util.Map;

/** Clasificación de sustancias/productos controlados (ej. listas de estupefacientes/psicotrópicos). */
public final class ClasificacionControlada {

    private static final int CODIGO_MAX_LENGTH = 40;
    private static final int DENOMINACION_MIN_LENGTH = 2;
    private static final int DENOMINACION_MAX_LENGTH = 200;
    private static final int NORMA_FUENTE_MAX_LENGTH = 300;

    private final String codigo;
    private final String denominacion;
    private final String normaFuente;
    private final boolean requiereRecetaEspecial;
    private final boolean retieneReceta;
    private final Integer vigenciaRecetaDias;
    private final EstadoCatalogoSoporte estado;

    private ClasificacionControlada(
            String codigo, String denominacion, String normaFuente, boolean requiereRecetaEspecial,
            boolean retieneReceta, Integer vigenciaRecetaDias, EstadoCatalogoSoporte estado) {
        this.codigo = codigo;
        this.denominacion = denominacion;
        this.normaFuente = normaFuente;
        this.requiereRecetaEspecial = requiereRecetaEspecial;
        this.retieneReceta = retieneReceta;
        this.vigenciaRecetaDias = vigenciaRecetaDias;
        this.estado = estado;
    }

    public static Result<ClasificacionControlada, ErrorDetail> create(
            String codigo, String denominacion, String normaFuente, boolean requiereRecetaEspecial,
            boolean retieneReceta, Integer vigenciaRecetaDias) {
        var normalizedCodigo = normalizeUpper(codigo);
        if (normalizedCodigo == null || normalizedCodigo.length() > CODIGO_MAX_LENGTH) {
            return invalid("codigo", "El código es obligatorio y no debe exceder 40 caracteres.");
        }

        var normalizedDenominacion = normalizeSpaces(denominacion);
        if (normalizedDenominacion == null || normalizedDenominacion.length() < DENOMINACION_MIN_LENGTH
                || normalizedDenominacion.length() > DENOMINACION_MAX_LENGTH) {
            return invalid("denominacion", "La denominación debe tener entre 2 y 200 caracteres.");
        }

        var normalizedNormaFuente = normalizeNullable(normaFuente);
        if (!withinLength(normalizedNormaFuente, NORMA_FUENTE_MAX_LENGTH)) {
            return invalid("normaFuente", "La norma fuente no debe exceder 300 caracteres.");
        }

        if (vigenciaRecetaDias != null && vigenciaRecetaDias <= 0) {
            return invalid("vigenciaRecetaDias", "La vigencia de receta en días debe ser mayor que cero.");
        }

        return Result.success(new ClasificacionControlada(
                normalizedCodigo, normalizedDenominacion, normalizedNormaFuente, requiereRecetaEspecial,
                retieneReceta, vigenciaRecetaDias, EstadoCatalogoSoporte.ACTIVO));
    }

    public static ClasificacionControlada restore(
            String codigo, String denominacion, String normaFuente, boolean requiereRecetaEspecial,
            boolean retieneReceta, Integer vigenciaRecetaDias, EstadoCatalogoSoporte estado) {
        return new ClasificacionControlada(
                codigo, denominacion, normaFuente, requiereRecetaEspecial, retieneReceta, vigenciaRecetaDias,
                estado);
    }

    private static Result<ClasificacionControlada, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail(
                "CAT_CLASIFICACION_CONTROLADA_INVALIDA", message, Map.of("field", field)));
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

    private static String normalizeUpper(String value) {
        var normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static boolean withinLength(String value, int maximum) {
        return value == null || value.length() <= maximum;
    }

    public String codigo() { return codigo; }
    public String denominacion() { return denominacion; }
    public String normaFuente() { return normaFuente; }
    public boolean requiereRecetaEspecial() { return requiereRecetaEspecial; }
    public boolean retieneReceta() { return retieneReceta; }
    public Integer vigenciaRecetaDias() { return vigenciaRecetaDias; }
    public EstadoCatalogoSoporte estado() { return estado; }
}
```

- [ ] **Step 17: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControladaTest"`
Expected: PASS

- [ ] **Step 18: Ejecutar todos los tests de soporte juntos y commit**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.soporte.*"`
Expected: PASS (5 clases de test, todas verdes).

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/soporte/ service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/soporte/
git commit -m "feat(catalogo): agregar los 5 catalogos de soporte regulatorio"
```

---

### Task 4: Agregado `PrincipioActivo`

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoPrincipioActivo.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/PrincipioActivo.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/PrincipioActivoTest.java`

**Interfaces:**
- Consumes: `PrincipioActivoId` (Task 2).
- Produces: `PrincipioActivo.create(PrincipioActivoId id, String codigoFuente, String denominacion, String nombreNormalizado, String fuente): Result<PrincipioActivo, ErrorDetail>`, `restore(PrincipioActivoId id, String codigoFuente, String denominacion, String nombreNormalizado, String fuente, EstadoPrincipioActivo estado): PrincipioActivo`, getters `id()`, `codigoFuente()`, `denominacion()`, `nombreNormalizado()`, `fuente()`, `estado()`. Usado por Task 13 (handlers), Task 19 (mapper JPA).

- [ ] **Step 1: Escribir el test que falla**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/PrincipioActivoTest.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.domain.valueobject.PrincipioActivoId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PrincipioActivoTest {

    @Test
    void createsAndNormalizesAValidPrincipioActivo() {
        var result = PrincipioActivo.create(
                new PrincipioActivoId(UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589")),
                "  PA-001  ", "  Paracetamol  ", "  paracetamol  ", "DIGEMID");

        assertTrue(result.isSuccess());
        var principio = result.getOrElse(error -> null);
        assertEquals("Paracetamol", principio.denominacion());
        assertEquals(EstadoPrincipioActivo.ACTIVO, principio.estado());
    }

    @Test
    void rejectsAMissingId() {
        var result = PrincipioActivo.create(null, null, "Paracetamol", null, null);
        assertTrue(result.isFailure());
    }

    @Test
    void rejectsADenominationThatIsTooShort() {
        var result = PrincipioActivo.create(
                new PrincipioActivoId(UUID.randomUUID()), null, "P", null, null);

        assertTrue(result.isFailure());
        assertEquals("CAT_PRINCIPIO_ACTIVO_INVALIDO", result.fold(value -> null, error -> error.code()));
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.PrincipioActivoTest"`
Expected: FAIL.

- [ ] **Step 3: Crear `EstadoPrincipioActivo`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoPrincipioActivo.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

public enum EstadoPrincipioActivo {
    ACTIVO,
    INACTIVO
}
```

- [ ] **Step 4: Crear `PrincipioActivo`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/PrincipioActivo.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

import com.softprimesolutions.catalogo.domain.valueobject.PrincipioActivoId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Map;

/** Sustancia farmacológicamente activa, referenciada por uno o más productos regulados. */
public final class PrincipioActivo extends AggregateRoot {

    private static final int CODIGO_FUENTE_MAX_LENGTH = 80;
    private static final int DENOMINACION_MIN_LENGTH = 2;
    private static final int DENOMINACION_MAX_LENGTH = 300;
    private static final int NOMBRE_NORMALIZADO_MAX_LENGTH = 300;
    private static final int FUENTE_MAX_LENGTH = 300;

    private final PrincipioActivoId id;
    private final String codigoFuente;
    private final String denominacion;
    private final String nombreNormalizado;
    private final String fuente;
    private final EstadoPrincipioActivo estado;

    private PrincipioActivo(
            PrincipioActivoId id, String codigoFuente, String denominacion, String nombreNormalizado,
            String fuente, EstadoPrincipioActivo estado) {
        this.id = id;
        this.codigoFuente = codigoFuente;
        this.denominacion = denominacion;
        this.nombreNormalizado = nombreNormalizado;
        this.fuente = fuente;
        this.estado = estado;
    }

    public static Result<PrincipioActivo, ErrorDetail> create(
            PrincipioActivoId id, String codigoFuente, String denominacion, String nombreNormalizado,
            String fuente) {
        if (id == null) return invalid("id", "La identidad del principio activo es obligatoria.");

        var normalizedCodigoFuente = normalizeNullable(codigoFuente);
        if (!withinLength(normalizedCodigoFuente, CODIGO_FUENTE_MAX_LENGTH)) {
            return invalid("codigoFuente", "El código fuente no debe exceder 80 caracteres.");
        }

        var normalizedDenominacion = normalizeSpaces(denominacion);
        if (normalizedDenominacion == null || normalizedDenominacion.length() < DENOMINACION_MIN_LENGTH
                || normalizedDenominacion.length() > DENOMINACION_MAX_LENGTH) {
            return invalid("denominacion", "La denominación debe tener entre 2 y 300 caracteres.");
        }

        var normalizedNombreNormalizado = normalizeNullable(nombreNormalizado);
        if (!withinLength(normalizedNombreNormalizado, NOMBRE_NORMALIZADO_MAX_LENGTH)) {
            return invalid("nombreNormalizado", "El nombre normalizado no debe exceder 300 caracteres.");
        }

        var normalizedFuente = normalizeNullable(fuente);
        if (!withinLength(normalizedFuente, FUENTE_MAX_LENGTH)) {
            return invalid("fuente", "La fuente no debe exceder 300 caracteres.");
        }

        return Result.success(new PrincipioActivo(
                id, normalizedCodigoFuente, normalizedDenominacion, normalizedNombreNormalizado,
                normalizedFuente, EstadoPrincipioActivo.ACTIVO));
    }

    public static PrincipioActivo restore(
            PrincipioActivoId id, String codigoFuente, String denominacion, String nombreNormalizado,
            String fuente, EstadoPrincipioActivo estado) {
        return new PrincipioActivo(id, codigoFuente, denominacion, nombreNormalizado, fuente, estado);
    }

    private static Result<PrincipioActivo, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("CAT_PRINCIPIO_ACTIVO_INVALIDO", message, Map.of("field", field)));
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

    public PrincipioActivoId id() { return id; }
    public String codigoFuente() { return codigoFuente; }
    public String denominacion() { return denominacion; }
    public String nombreNormalizado() { return nombreNormalizado; }
    public String fuente() { return fuente; }
    public EstadoPrincipioActivo estado() { return estado; }
}
```

- [ ] **Step 5: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.PrincipioActivoTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoPrincipioActivo.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/PrincipioActivo.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/PrincipioActivoTest.java
git commit -m "feat(catalogo): agregar agregado de dominio PrincipioActivo"
```

---

### Task 5: Agregado `Marca`

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoMarca.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/Marca.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/MarcaTest.java`

**Interfaces:**
- Consumes: `MarcaId`, `TenantId` (Task 2).
- Produces: `Marca.create(MarcaId id, TenantId tenantId, String codigo, String nombre, String descripcion): Result<Marca, ErrorDetail>`, `restore(MarcaId id, TenantId tenantId, String codigo, String nombre, String descripcion, EstadoMarca estado): Marca`, getters `id()`, `tenantId()`, `codigo()`, `nombre()`, `descripcion()`, `estado()`. Usado por Task 14 (handlers), Task 20 (mapper JPA).

- [ ] **Step 1: Escribir el test que falla**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/MarcaTest.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.domain.valueobject.MarcaId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MarcaTest {

    @Test
    void createsAndNormalizesAValidMarca() {
        var result = Marca.create(
                new MarcaId(UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589")),
                new TenantId(UUID.fromString("a92adf67-70e7-4cc1-bb05-ff074df7fdf5")),
                "  bayer  ", "  Bayer  ", null);

        assertTrue(result.isSuccess());
        var marca = result.getOrElse(error -> null);
        assertEquals("Bayer", marca.nombre());
        assertEquals(EstadoMarca.ACTIVO, marca.estado());
    }

    @Test
    void requiresIdTenantAndCodigo() {
        var missingTenant = Marca.create(
                new MarcaId(UUID.randomUUID()), null, "BAYER", "Bayer", null);
        assertTrue(missingTenant.isFailure());
        assertEquals("CAT_MARCA_INVALIDA", missingTenant.fold(value -> null, error -> error.code()));
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.MarcaTest"`
Expected: FAIL.

- [ ] **Step 3: Crear `EstadoMarca`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoMarca.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

public enum EstadoMarca {
    ACTIVO,
    INACTIVO
}
```

- [ ] **Step 4: Crear `Marca`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/Marca.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

import com.softprimesolutions.catalogo.domain.valueobject.MarcaId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Map;

/** Marca comercial de un producto, propia de un tenant. */
public final class Marca extends AggregateRoot {

    private static final int CODIGO_MIN_LENGTH = 2;
    private static final int CODIGO_MAX_LENGTH = 50;
    private static final int NOMBRE_MIN_LENGTH = 2;
    private static final int NOMBRE_MAX_LENGTH = 180;
    private static final int DESCRIPCION_MAX_LENGTH = 500;

    private final MarcaId id;
    private final TenantId tenantId;
    private final String codigo;
    private final String nombre;
    private final String descripcion;
    private final EstadoMarca estado;

    private Marca(
            MarcaId id, TenantId tenantId, String codigo, String nombre, String descripcion,
            EstadoMarca estado) {
        this.id = id;
        this.tenantId = tenantId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    public static Result<Marca, ErrorDetail> create(
            MarcaId id, TenantId tenantId, String codigo, String nombre, String descripcion) {
        if (id == null) return invalid("id", "La identidad de la marca es obligatoria.");
        if (tenantId == null) return invalid("tenantId", "El tenant es obligatorio.");

        var normalizedCodigo = normalizeSpaces(codigo);
        if (normalizedCodigo == null || normalizedCodigo.length() < CODIGO_MIN_LENGTH
                || normalizedCodigo.length() > CODIGO_MAX_LENGTH) {
            return invalid("codigo", "El código debe tener entre 2 y 50 caracteres.");
        }

        var normalizedNombre = normalizeSpaces(nombre);
        if (normalizedNombre == null || normalizedNombre.length() < NOMBRE_MIN_LENGTH
                || normalizedNombre.length() > NOMBRE_MAX_LENGTH) {
            return invalid("nombre", "El nombre debe tener entre 2 y 180 caracteres.");
        }

        var normalizedDescripcion = normalizeNullable(descripcion);
        if (!withinLength(normalizedDescripcion, DESCRIPCION_MAX_LENGTH)) {
            return invalid("descripcion", "La descripción no debe exceder 500 caracteres.");
        }

        return Result.success(new Marca(
                id, tenantId, normalizedCodigo, normalizedNombre, normalizedDescripcion, EstadoMarca.ACTIVO));
    }

    public static Marca restore(
            MarcaId id, TenantId tenantId, String codigo, String nombre, String descripcion,
            EstadoMarca estado) {
        return new Marca(id, tenantId, codigo, nombre, descripcion, estado);
    }

    private static Result<Marca, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("CAT_MARCA_INVALIDA", message, Map.of("field", field)));
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

    public MarcaId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public String codigo() { return codigo; }
    public String nombre() { return nombre; }
    public String descripcion() { return descripcion; }
    public EstadoMarca estado() { return estado; }
}
```

- [ ] **Step 5: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.MarcaTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoMarca.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/Marca.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/MarcaTest.java
git commit -m "feat(catalogo): agregar agregado de dominio Marca"
```

---

### Task 6: Agregado `CategoriaProducto`

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoCategoriaProducto.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/CategoriaProducto.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/CategoriaProductoTest.java`

**Interfaces:**
- Consumes: `CategoriaProductoId`, `TenantId` (Task 2).
- Produces: `CategoriaProducto.create(CategoriaProductoId id, TenantId tenantId, CategoriaProductoId categoriaPadreId, String codigo, String nombre, String descripcion, int nivel, int orden): Result<CategoriaProducto, ErrorDetail>`, `restore(CategoriaProductoId id, TenantId tenantId, CategoriaProductoId categoriaPadreId, String codigo, String nombre, String descripcion, int nivel, int orden, EstadoCategoriaProducto estado): CategoriaProducto`, getters `id()`, `tenantId()`, `categoriaPadreId()`, `codigo()`, `nombre()`, `descripcion()`, `nivel()`, `orden()`, `estado()`. Usado por Task 14 (handlers), Task 20 (mapper JPA).

- [ ] **Step 1: Escribir el test que falla**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/CategoriaProductoTest.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.domain.valueobject.CategoriaProductoId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CategoriaProductoTest {

    @Test
    void createsARootCategoriaSuccessfully() {
        var result = CategoriaProducto.create(
                new CategoriaProductoId(UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589")),
                new TenantId(UUID.fromString("a92adf67-70e7-4cc1-bb05-ff074df7fdf5")),
                null, "  analgesicos  ", "  Analgésicos  ", null, 1, 0);

        assertTrue(result.isSuccess());
        var categoria = result.getOrElse(error -> null);
        assertEquals("Analgésicos", categoria.nombre());
        assertEquals(1, categoria.nivel());
    }

    @Test
    void createsAChildCategoriaWithAParent() {
        var padreId = new CategoriaProductoId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        var result = CategoriaProducto.create(
                new CategoriaProductoId(UUID.randomUUID()),
                new TenantId(UUID.randomUUID()),
                padreId, "antiinflamatorios", "Antiinflamatorios", null, 2, 1);

        assertTrue(result.isSuccess());
        assertEquals(padreId, result.getOrElse(error -> null).categoriaPadreId());
    }

    @Test
    void rejectsANivelBelowOne() {
        var result = CategoriaProducto.create(
                new CategoriaProductoId(UUID.randomUUID()), new TenantId(UUID.randomUUID()),
                null, "analgesicos", "Analgésicos", null, 0, 0);

        assertTrue(result.isFailure());
        assertEquals("CAT_CATEGORIA_PRODUCTO_INVALIDA", result.fold(value -> null, error -> error.code()));
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.CategoriaProductoTest"`
Expected: FAIL.

- [ ] **Step 3: Crear `EstadoCategoriaProducto`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoCategoriaProducto.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

public enum EstadoCategoriaProducto {
    ACTIVO,
    INACTIVO
}
```

- [ ] **Step 4: Crear `CategoriaProducto`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/CategoriaProducto.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

import com.softprimesolutions.catalogo.domain.valueobject.CategoriaProductoId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Map;

/** Categoría jerárquica de producto, propia de un tenant. */
public final class CategoriaProducto extends AggregateRoot {

    private static final int CODIGO_MIN_LENGTH = 2;
    private static final int CODIGO_MAX_LENGTH = 50;
    private static final int NOMBRE_MIN_LENGTH = 2;
    private static final int NOMBRE_MAX_LENGTH = 180;
    private static final int DESCRIPCION_MAX_LENGTH = 500;

    private final CategoriaProductoId id;
    private final TenantId tenantId;
    private final CategoriaProductoId categoriaPadreId;
    private final String codigo;
    private final String nombre;
    private final String descripcion;
    private final int nivel;
    private final int orden;
    private final EstadoCategoriaProducto estado;

    private CategoriaProducto(
            CategoriaProductoId id, TenantId tenantId, CategoriaProductoId categoriaPadreId, String codigo,
            String nombre, String descripcion, int nivel, int orden, EstadoCategoriaProducto estado) {
        this.id = id;
        this.tenantId = tenantId;
        this.categoriaPadreId = categoriaPadreId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.nivel = nivel;
        this.orden = orden;
        this.estado = estado;
    }

    public static Result<CategoriaProducto, ErrorDetail> create(
            CategoriaProductoId id, TenantId tenantId, CategoriaProductoId categoriaPadreId, String codigo,
            String nombre, String descripcion, int nivel, int orden) {
        if (id == null) return invalid("id", "La identidad de la categoría es obligatoria.");
        if (tenantId == null) return invalid("tenantId", "El tenant es obligatorio.");

        var normalizedCodigo = normalizeSpaces(codigo);
        if (normalizedCodigo == null || normalizedCodigo.length() < CODIGO_MIN_LENGTH
                || normalizedCodigo.length() > CODIGO_MAX_LENGTH) {
            return invalid("codigo", "El código debe tener entre 2 y 50 caracteres.");
        }

        var normalizedNombre = normalizeSpaces(nombre);
        if (normalizedNombre == null || normalizedNombre.length() < NOMBRE_MIN_LENGTH
                || normalizedNombre.length() > NOMBRE_MAX_LENGTH) {
            return invalid("nombre", "El nombre debe tener entre 2 y 180 caracteres.");
        }

        var normalizedDescripcion = normalizeNullable(descripcion);
        if (!withinLength(normalizedDescripcion, DESCRIPCION_MAX_LENGTH)) {
            return invalid("descripcion", "La descripción no debe exceder 500 caracteres.");
        }

        if (nivel < 1) return invalid("nivel", "El nivel debe ser mayor o igual a 1.");
        if (orden < 0) return invalid("orden", "El orden debe ser mayor o igual a 0.");

        return Result.success(new CategoriaProducto(
                id, tenantId, categoriaPadreId, normalizedCodigo, normalizedNombre, normalizedDescripcion,
                nivel, orden, EstadoCategoriaProducto.ACTIVO));
    }

    public static CategoriaProducto restore(
            CategoriaProductoId id, TenantId tenantId, CategoriaProductoId categoriaPadreId, String codigo,
            String nombre, String descripcion, int nivel, int orden, EstadoCategoriaProducto estado) {
        return new CategoriaProducto(
                id, tenantId, categoriaPadreId, codigo, nombre, descripcion, nivel, orden, estado);
    }

    private static Result<CategoriaProducto, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("CAT_CATEGORIA_PRODUCTO_INVALIDA", message, Map.of("field", field)));
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

    public CategoriaProductoId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public CategoriaProductoId categoriaPadreId() { return categoriaPadreId; }
    public String codigo() { return codigo; }
    public String nombre() { return nombre; }
    public String descripcion() { return descripcion; }
    public int nivel() { return nivel; }
    public int orden() { return orden; }
    public EstadoCategoriaProducto estado() { return estado; }
}
```

- [ ] **Step 5: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.CategoriaProductoTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoCategoriaProducto.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/CategoriaProducto.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/CategoriaProductoTest.java
git commit -m "feat(catalogo): agregar agregado de dominio CategoriaProducto"
```

---

### Task 7: Agregado `ProductoRegulado` (con `PrincipioActivoAsociado`)

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoRegulatorio.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/PrincipioActivoAsociado.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/ProductoRegulado.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/ProductoReguladoTest.java`

**Interfaces:**
- Consumes: `ProductoReguladoId`, `PrincipioActivoId` (Task 2).
- Produces:
  - `PrincipioActivoAsociado(PrincipioActivoId principioActivoId, String concentracionTexto, BigDecimal cantidad, String unidadMedidaCodigo, boolean esPrincipal, short orden)` — record simple, sin factory con validación propia (se valida en el handler que lo asocia, Task 15).
  - `ProductoRegulado.create(ProductoReguladoId id, String tipoProducto, String rubroCodigo, String tipoRegistro, String numeroRegistro, String denominacion, String concentracionTexto, String presentacionRegulatoria, String formaFarmaceuticaCodigo, String viaAdministracionCodigo, String unidadMedidaCodigo, String condicionVentaCodigo, String clasificacionAtc, String clasificacionControladaCodigo, String tipoLiberacion, String origenFabricacion, String paisOrigen, String subpartidaNacional, String titularRegistro, String fabricante, String importador, String establecimientoExpendio, LocalDate vigenteDesde, LocalDate vigenteHasta, String fuente, String versionFuente, Instant createdAt): Result<ProductoRegulado, ErrorDetail>`.
  - `restore(...)` con los mismos 26 campos + `List<PrincipioActivoAsociado> principiosActivos` + `EstadoRegulatorio estado` + `Instant updatedAt`.
  - `conPrincipioActivoAsociado(PrincipioActivoAsociado asociado): ProductoRegulado` y `sinPrincipioActivoAsociado(PrincipioActivoId principioActivoId): ProductoRegulado` — métodos que devuelven una nueva instancia con la lista de principios activos modificada (el agregado sigue siendo inmutable).
  - Getters para los 26 campos + `principiosActivos()` (lista inmutable) + `estado()` + `createdAt()`/`updatedAt()`.
- Usado por Task 15 (handlers), Task 21 (mapper JPA).

- [ ] **Step 1: Escribir el test que falla**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/ProductoReguladoTest.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.domain.valueobject.PrincipioActivoId;
import com.softprimesolutions.catalogo.domain.valueobject.ProductoReguladoId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductoReguladoTest {

    private static final ProductoReguladoId PRODUCTO_REGULADO_ID = new ProductoReguladoId(
            UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589"));
    private static final Instant CREATED_AT = Instant.parse("2026-09-07T10:00:00Z");

    @Test
    void createsAValidProductoReguladoWithoutActiveIngredients() {
        var result = ProductoRegulado.create(
                PRODUCTO_REGULADO_ID, "MEDICAMENTO", null, "RS", "RS-12345",
                "Paracetamol 500mg", "500mg", "Caja x 10 tabletas", "TABLETA", "ORAL", "MG",
                "SIN-RECETA", null, null, null, null, null, null, "Laboratorio X", "Laboratorio X",
                null, null, null, null, null, null, CREATED_AT);

        assertTrue(result.isSuccess());
        var producto = result.getOrElse(error -> null);
        assertEquals("Paracetamol 500mg", producto.denominacion());
        assertEquals(EstadoRegulatorio.VIGENTE, producto.estado());
        assertTrue(producto.principiosActivos().isEmpty());
    }

    @Test
    void rejectsRegistroWithOnlyOneOfTipoAndNumero() {
        var result = ProductoRegulado.create(
                PRODUCTO_REGULADO_ID, "MEDICAMENTO", null, "RS", null,
                "Paracetamol 500mg", null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, CREATED_AT);

        assertTrue(result.isFailure());
        assertEquals("CAT_PRODUCTO_REGULADO_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsVigenciaHastaBeforeVigenciaDesde() {
        var result = ProductoRegulado.create(
                PRODUCTO_REGULADO_ID, "MEDICAMENTO", null, null, null,
                "Paracetamol 500mg", null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null,
                LocalDate.parse("2026-12-31"), LocalDate.parse("2026-01-01"), null, null, CREATED_AT);

        assertTrue(result.isFailure());
        assertEquals("CAT_PRODUCTO_REGULADO_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void addsAndRemovesAssociatedActiveIngredients() {
        var producto = ProductoRegulado.create(
                        PRODUCTO_REGULADO_ID, "MEDICAMENTO", null, null, null,
                        "Paracetamol 500mg", null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, CREATED_AT)
                .getOrElse(error -> null);

        var principioActivoId = new PrincipioActivoId(UUID.randomUUID());
        var asociado = new PrincipioActivoAsociado(principioActivoId, "500mg", new BigDecimal("500"), "MG", true, (short) 1);

        var withAsociado = producto.conPrincipioActivoAsociado(asociado);
        assertEquals(1, withAsociado.principiosActivos().size());

        var withoutAsociado = withAsociado.sinPrincipioActivoAsociado(principioActivoId);
        assertTrue(withoutAsociado.principiosActivos().isEmpty());
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.ProductoReguladoTest"`
Expected: FAIL — `ProductoRegulado`/`PrincipioActivoAsociado`/`EstadoRegulatorio` no existen todavía.

- [ ] **Step 3: Crear `EstadoRegulatorio`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoRegulatorio.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

public enum EstadoRegulatorio {
    VIGENTE,
    VENCIDO,
    SUSPENDIDO,
    CANCELADO,
    POR_VALIDAR
}
```

- [ ] **Step 4: Crear `PrincipioActivoAsociado`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/PrincipioActivoAsociado.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

import com.softprimesolutions.catalogo.domain.valueobject.PrincipioActivoId;
import java.math.BigDecimal;
import java.util.Objects;

/** Relación entre un producto regulado y uno de sus principios activos, con su concentración. */
public record PrincipioActivoAsociado(
        PrincipioActivoId principioActivoId,
        String concentracionTexto,
        BigDecimal cantidad,
        String unidadMedidaCodigo,
        boolean esPrincipal,
        short orden) {

    public PrincipioActivoAsociado {
        Objects.requireNonNull(principioActivoId, "principioActivoId es obligatorio");
    }
}
```

- [ ] **Step 5: Crear `ProductoRegulado`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/ProductoRegulado.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

import com.softprimesolutions.catalogo.domain.valueobject.PrincipioActivoId;
import com.softprimesolutions.catalogo.domain.valueobject.ProductoReguladoId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Ficha regulatoria global de un producto farmacéutico (registro sanitario, composición, condición de venta). */
public final class ProductoRegulado extends AggregateRoot {

    private static final int TIPO_PRODUCTO_MIN_LENGTH = 2;
    private static final int TIPO_PRODUCTO_MAX_LENGTH = 40;
    private static final int RUBRO_CODIGO_MAX_LENGTH = 50;
    private static final int TIPO_REGISTRO_MAX_LENGTH = 40;
    private static final int NUMERO_REGISTRO_MAX_LENGTH = 100;
    private static final int DENOMINACION_MIN_LENGTH = 2;
    private static final int DENOMINACION_MAX_LENGTH = 500;
    private static final int CONCENTRACION_TEXTO_MAX_LENGTH = 300;
    private static final int PRESENTACION_REGULATORIA_MAX_LENGTH = 500;
    private static final int CODIGO_REFERENCIA_MAX_LENGTH = 40;
    private static final int CLASIFICACION_ATC_MAX_LENGTH = 30;
    private static final int TIPO_LIBERACION_MAX_LENGTH = 40;
    private static final int ORIGEN_FABRICACION_MAX_LENGTH = 40;
    private static final int PAIS_ORIGEN_MAX_LENGTH = 100;
    private static final int SUBPARTIDA_NACIONAL_MAX_LENGTH = 30;
    private static final int PARTE_INTERESADA_MAX_LENGTH = 300;
    private static final int ESTABLECIMIENTO_EXPENDIO_MAX_LENGTH = 200;
    private static final int FUENTE_MAX_LENGTH = 300;
    private static final int VERSION_FUENTE_MAX_LENGTH = 100;

    private final ProductoReguladoId id;
    private final String tipoProducto;
    private final String rubroCodigo;
    private final String tipoRegistro;
    private final String numeroRegistro;
    private final String denominacion;
    private final String concentracionTexto;
    private final String presentacionRegulatoria;
    private final String formaFarmaceuticaCodigo;
    private final String viaAdministracionCodigo;
    private final String unidadMedidaCodigo;
    private final String condicionVentaCodigo;
    private final String clasificacionAtc;
    private final String clasificacionControladaCodigo;
    private final String tipoLiberacion;
    private final String origenFabricacion;
    private final String paisOrigen;
    private final String subpartidaNacional;
    private final String titularRegistro;
    private final String fabricante;
    private final String importador;
    private final String establecimientoExpendio;
    private final LocalDate vigenteDesde;
    private final LocalDate vigenteHasta;
    private final String fuente;
    private final String versionFuente;
    private final List<PrincipioActivoAsociado> principiosActivos;
    private final EstadoRegulatorio estado;
    private final Instant createdAt;
    private final Instant updatedAt;

    private ProductoRegulado(
            ProductoReguladoId id, String tipoProducto, String rubroCodigo, String tipoRegistro,
            String numeroRegistro, String denominacion, String concentracionTexto,
            String presentacionRegulatoria, String formaFarmaceuticaCodigo, String viaAdministracionCodigo,
            String unidadMedidaCodigo, String condicionVentaCodigo, String clasificacionAtc,
            String clasificacionControladaCodigo, String tipoLiberacion, String origenFabricacion,
            String paisOrigen, String subpartidaNacional, String titularRegistro, String fabricante,
            String importador, String establecimientoExpendio, LocalDate vigenteDesde, LocalDate vigenteHasta,
            String fuente, String versionFuente, List<PrincipioActivoAsociado> principiosActivos,
            EstadoRegulatorio estado, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tipoProducto = tipoProducto;
        this.rubroCodigo = rubroCodigo;
        this.tipoRegistro = tipoRegistro;
        this.numeroRegistro = numeroRegistro;
        this.denominacion = denominacion;
        this.concentracionTexto = concentracionTexto;
        this.presentacionRegulatoria = presentacionRegulatoria;
        this.formaFarmaceuticaCodigo = formaFarmaceuticaCodigo;
        this.viaAdministracionCodigo = viaAdministracionCodigo;
        this.unidadMedidaCodigo = unidadMedidaCodigo;
        this.condicionVentaCodigo = condicionVentaCodigo;
        this.clasificacionAtc = clasificacionAtc;
        this.clasificacionControladaCodigo = clasificacionControladaCodigo;
        this.tipoLiberacion = tipoLiberacion;
        this.origenFabricacion = origenFabricacion;
        this.paisOrigen = paisOrigen;
        this.subpartidaNacional = subpartidaNacional;
        this.titularRegistro = titularRegistro;
        this.fabricante = fabricante;
        this.importador = importador;
        this.establecimientoExpendio = establecimientoExpendio;
        this.vigenteDesde = vigenteDesde;
        this.vigenteHasta = vigenteHasta;
        this.fuente = fuente;
        this.versionFuente = versionFuente;
        this.principiosActivos = List.copyOf(principiosActivos);
        this.estado = estado;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Result<ProductoRegulado, ErrorDetail> create(
            ProductoReguladoId id, String tipoProducto, String rubroCodigo, String tipoRegistro,
            String numeroRegistro, String denominacion, String concentracionTexto,
            String presentacionRegulatoria, String formaFarmaceuticaCodigo, String viaAdministracionCodigo,
            String unidadMedidaCodigo, String condicionVentaCodigo, String clasificacionAtc,
            String clasificacionControladaCodigo, String tipoLiberacion, String origenFabricacion,
            String paisOrigen, String subpartidaNacional, String titularRegistro, String fabricante,
            String importador, String establecimientoExpendio, LocalDate vigenteDesde, LocalDate vigenteHasta,
            String fuente, String versionFuente, Instant createdAt) {
        if (id == null) return invalid("id", "La identidad del producto regulado es obligatoria.");
        if (createdAt == null) return invalid("createdAt", "El instante de registro es obligatorio.");

        var normalizedTipoProducto = normalizeSpaces(tipoProducto);
        if (normalizedTipoProducto == null || normalizedTipoProducto.length() < TIPO_PRODUCTO_MIN_LENGTH
                || normalizedTipoProducto.length() > TIPO_PRODUCTO_MAX_LENGTH) {
            return invalid("tipoProducto", "El tipo de producto debe tener entre 2 y 40 caracteres.");
        }

        var normalizedRubroCodigo = normalizeNullable(rubroCodigo);
        if (!withinLength(normalizedRubroCodigo, RUBRO_CODIGO_MAX_LENGTH)) {
            return invalid("rubroCodigo", "El rubro no debe exceder 50 caracteres.");
        }

        var normalizedTipoRegistro = normalizeNullable(tipoRegistro);
        if (!withinLength(normalizedTipoRegistro, TIPO_REGISTRO_MAX_LENGTH)) {
            return invalid("tipoRegistro", "El tipo de registro no debe exceder 40 caracteres.");
        }

        var normalizedNumeroRegistro = normalizeNullable(numeroRegistro);
        if (!withinLength(normalizedNumeroRegistro, NUMERO_REGISTRO_MAX_LENGTH)) {
            return invalid("numeroRegistro", "El número de registro no debe exceder 100 caracteres.");
        }

        if ((normalizedTipoRegistro == null) != (normalizedNumeroRegistro == null)) {
            return invalid("tipoRegistro", "El tipo y número de registro deben informarse juntos.");
        }

        var normalizedDenominacion = normalizeSpaces(denominacion);
        if (normalizedDenominacion == null || normalizedDenominacion.length() < DENOMINACION_MIN_LENGTH
                || normalizedDenominacion.length() > DENOMINACION_MAX_LENGTH) {
            return invalid("denominacion", "La denominación debe tener entre 2 y 500 caracteres.");
        }

        var normalizedConcentracionTexto = normalizeNullable(concentracionTexto);
        if (!withinLength(normalizedConcentracionTexto, CONCENTRACION_TEXTO_MAX_LENGTH)) {
            return invalid("concentracionTexto", "La concentración no debe exceder 300 caracteres.");
        }

        var normalizedPresentacionRegulatoria = normalizeNullable(presentacionRegulatoria);
        if (!withinLength(normalizedPresentacionRegulatoria, PRESENTACION_REGULATORIA_MAX_LENGTH)) {
            return invalid("presentacionRegulatoria", "La presentación regulatoria no debe exceder 500 caracteres.");
        }

        var normalizedFormaFarmaceuticaCodigo = normalizeUpper(formaFarmaceuticaCodigo);
        if (!withinLength(normalizedFormaFarmaceuticaCodigo, CODIGO_REFERENCIA_MAX_LENGTH)) {
            return invalid("formaFarmaceuticaCodigo", "El código de forma farmacéutica no debe exceder 40 caracteres.");
        }

        var normalizedViaAdministracionCodigo = normalizeUpper(viaAdministracionCodigo);
        if (!withinLength(normalizedViaAdministracionCodigo, CODIGO_REFERENCIA_MAX_LENGTH)) {
            return invalid("viaAdministracionCodigo", "El código de vía de administración no debe exceder 40 caracteres.");
        }

        var normalizedUnidadMedidaCodigo = normalizeUpper(unidadMedidaCodigo);
        if (!withinLength(normalizedUnidadMedidaCodigo, CODIGO_REFERENCIA_MAX_LENGTH)) {
            return invalid("unidadMedidaCodigo", "El código de unidad de medida no debe exceder 40 caracteres.");
        }

        var normalizedCondicionVentaCodigo = normalizeUpper(condicionVentaCodigo);
        if (!withinLength(normalizedCondicionVentaCodigo, CODIGO_REFERENCIA_MAX_LENGTH)) {
            return invalid("condicionVentaCodigo", "El código de condición de venta no debe exceder 40 caracteres.");
        }

        var normalizedClasificacionAtc = normalizeUpper(clasificacionAtc);
        if (!withinLength(normalizedClasificacionAtc, CLASIFICACION_ATC_MAX_LENGTH)) {
            return invalid("clasificacionAtc", "La clasificación ATC no debe exceder 30 caracteres.");
        }

        var normalizedClasificacionControladaCodigo = normalizeUpper(clasificacionControladaCodigo);
        if (!withinLength(normalizedClasificacionControladaCodigo, CODIGO_REFERENCIA_MAX_LENGTH)) {
            return invalid("clasificacionControladaCodigo", "El código de clasificación controlada no debe exceder 40 caracteres.");
        }

        var normalizedTipoLiberacion = normalizeNullable(tipoLiberacion);
        if (!withinLength(normalizedTipoLiberacion, TIPO_LIBERACION_MAX_LENGTH)) {
            return invalid("tipoLiberacion", "El tipo de liberación no debe exceder 40 caracteres.");
        }

        var normalizedOrigenFabricacion = normalizeNullable(origenFabricacion);
        if (!withinLength(normalizedOrigenFabricacion, ORIGEN_FABRICACION_MAX_LENGTH)) {
            return invalid("origenFabricacion", "El origen de fabricación no debe exceder 40 caracteres.");
        }

        var normalizedPaisOrigen = normalizeNullable(paisOrigen);
        if (!withinLength(normalizedPaisOrigen, PAIS_ORIGEN_MAX_LENGTH)) {
            return invalid("paisOrigen", "El país de origen no debe exceder 100 caracteres.");
        }

        var normalizedSubpartidaNacional = normalizeNullable(subpartidaNacional);
        if (!withinLength(normalizedSubpartidaNacional, SUBPARTIDA_NACIONAL_MAX_LENGTH)) {
            return invalid("subpartidaNacional", "La subpartida nacional no debe exceder 30 caracteres.");
        }

        var normalizedTitularRegistro = normalizeNullable(titularRegistro);
        if (!withinLength(normalizedTitularRegistro, PARTE_INTERESADA_MAX_LENGTH)) {
            return invalid("titularRegistro", "El titular de registro no debe exceder 300 caracteres.");
        }

        var normalizedFabricante = normalizeNullable(fabricante);
        if (!withinLength(normalizedFabricante, PARTE_INTERESADA_MAX_LENGTH)) {
            return invalid("fabricante", "El fabricante no debe exceder 300 caracteres.");
        }

        var normalizedImportador = normalizeNullable(importador);
        if (!withinLength(normalizedImportador, PARTE_INTERESADA_MAX_LENGTH)) {
            return invalid("importador", "El importador no debe exceder 300 caracteres.");
        }

        var normalizedEstablecimientoExpendio = normalizeNullable(establecimientoExpendio);
        if (!withinLength(normalizedEstablecimientoExpendio, ESTABLECIMIENTO_EXPENDIO_MAX_LENGTH)) {
            return invalid("establecimientoExpendio", "El establecimiento de expendio no debe exceder 200 caracteres.");
        }

        if (vigenteDesde != null && vigenteHasta != null && vigenteHasta.isBefore(vigenteDesde)) {
            return invalid("vigenteHasta", "La vigencia hasta no puede ser anterior a la vigencia desde.");
        }

        var normalizedFuente = normalizeNullable(fuente);
        if (!withinLength(normalizedFuente, FUENTE_MAX_LENGTH)) {
            return invalid("fuente", "La fuente no debe exceder 300 caracteres.");
        }

        var normalizedVersionFuente = normalizeNullable(versionFuente);
        if (!withinLength(normalizedVersionFuente, VERSION_FUENTE_MAX_LENGTH)) {
            return invalid("versionFuente", "La versión de fuente no debe exceder 100 caracteres.");
        }

        return Result.success(new ProductoRegulado(
                id, normalizedTipoProducto, normalizedRubroCodigo, normalizedTipoRegistro,
                normalizedNumeroRegistro, normalizedDenominacion, normalizedConcentracionTexto,
                normalizedPresentacionRegulatoria, normalizedFormaFarmaceuticaCodigo,
                normalizedViaAdministracionCodigo, normalizedUnidadMedidaCodigo, normalizedCondicionVentaCodigo,
                normalizedClasificacionAtc, normalizedClasificacionControladaCodigo, normalizedTipoLiberacion,
                normalizedOrigenFabricacion, normalizedPaisOrigen, normalizedSubpartidaNacional,
                normalizedTitularRegistro, normalizedFabricante, normalizedImportador,
                normalizedEstablecimientoExpendio, vigenteDesde, vigenteHasta, normalizedFuente,
                normalizedVersionFuente, List.of(), EstadoRegulatorio.VIGENTE, createdAt, null));
    }

    public static ProductoRegulado restore(
            ProductoReguladoId id, String tipoProducto, String rubroCodigo, String tipoRegistro,
            String numeroRegistro, String denominacion, String concentracionTexto,
            String presentacionRegulatoria, String formaFarmaceuticaCodigo, String viaAdministracionCodigo,
            String unidadMedidaCodigo, String condicionVentaCodigo, String clasificacionAtc,
            String clasificacionControladaCodigo, String tipoLiberacion, String origenFabricacion,
            String paisOrigen, String subpartidaNacional, String titularRegistro, String fabricante,
            String importador, String establecimientoExpendio, LocalDate vigenteDesde, LocalDate vigenteHasta,
            String fuente, String versionFuente, List<PrincipioActivoAsociado> principiosActivos,
            EstadoRegulatorio estado, Instant createdAt, Instant updatedAt) {
        return new ProductoRegulado(
                id, tipoProducto, rubroCodigo, tipoRegistro, numeroRegistro, denominacion, concentracionTexto,
                presentacionRegulatoria, formaFarmaceuticaCodigo, viaAdministracionCodigo, unidadMedidaCodigo,
                condicionVentaCodigo, clasificacionAtc, clasificacionControladaCodigo, tipoLiberacion,
                origenFabricacion, paisOrigen, subpartidaNacional, titularRegistro, fabricante, importador,
                establecimientoExpendio, vigenteDesde, vigenteHasta, fuente, versionFuente, principiosActivos,
                estado, createdAt, updatedAt);
    }

    public ProductoRegulado conPrincipioActivoAsociado(PrincipioActivoAsociado asociado) {
        var nuevaLista = new ArrayList<>(principiosActivos);
        nuevaLista.removeIf(existing -> existing.principioActivoId().equals(asociado.principioActivoId()));
        nuevaLista.add(asociado);
        return new ProductoRegulado(
                id, tipoProducto, rubroCodigo, tipoRegistro, numeroRegistro, denominacion, concentracionTexto,
                presentacionRegulatoria, formaFarmaceuticaCodigo, viaAdministracionCodigo, unidadMedidaCodigo,
                condicionVentaCodigo, clasificacionAtc, clasificacionControladaCodigo, tipoLiberacion,
                origenFabricacion, paisOrigen, subpartidaNacional, titularRegistro, fabricante, importador,
                establecimientoExpendio, vigenteDesde, vigenteHasta, fuente, versionFuente, nuevaLista,
                estado, createdAt, updatedAt);
    }

    public ProductoRegulado sinPrincipioActivoAsociado(PrincipioActivoId principioActivoId) {
        var nuevaLista = new ArrayList<>(principiosActivos);
        nuevaLista.removeIf(existing -> existing.principioActivoId().equals(principioActivoId));
        return new ProductoRegulado(
                id, tipoProducto, rubroCodigo, tipoRegistro, numeroRegistro, denominacion, concentracionTexto,
                presentacionRegulatoria, formaFarmaceuticaCodigo, viaAdministracionCodigo, unidadMedidaCodigo,
                condicionVentaCodigo, clasificacionAtc, clasificacionControladaCodigo, tipoLiberacion,
                origenFabricacion, paisOrigen, subpartidaNacional, titularRegistro, fabricante, importador,
                establecimientoExpendio, vigenteDesde, vigenteHasta, fuente, versionFuente, nuevaLista,
                estado, createdAt, updatedAt);
    }

    private static Result<ProductoRegulado, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("CAT_PRODUCTO_REGULADO_INVALIDO", message, Map.of("field", field)));
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

    private static String normalizeUpper(String value) {
        var normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toUpperCase(java.util.Locale.ROOT);
    }

    private static boolean withinLength(String value, int maximum) {
        return value == null || value.length() <= maximum;
    }

    public ProductoReguladoId id() { return id; }
    public String tipoProducto() { return tipoProducto; }
    public String rubroCodigo() { return rubroCodigo; }
    public String tipoRegistro() { return tipoRegistro; }
    public String numeroRegistro() { return numeroRegistro; }
    public String denominacion() { return denominacion; }
    public String concentracionTexto() { return concentracionTexto; }
    public String presentacionRegulatoria() { return presentacionRegulatoria; }
    public String formaFarmaceuticaCodigo() { return formaFarmaceuticaCodigo; }
    public String viaAdministracionCodigo() { return viaAdministracionCodigo; }
    public String unidadMedidaCodigo() { return unidadMedidaCodigo; }
    public String condicionVentaCodigo() { return condicionVentaCodigo; }
    public String clasificacionAtc() { return clasificacionAtc; }
    public String clasificacionControladaCodigo() { return clasificacionControladaCodigo; }
    public String tipoLiberacion() { return tipoLiberacion; }
    public String origenFabricacion() { return origenFabricacion; }
    public String paisOrigen() { return paisOrigen; }
    public String subpartidaNacional() { return subpartidaNacional; }
    public String titularRegistro() { return titularRegistro; }
    public String fabricante() { return fabricante; }
    public String importador() { return importador; }
    public String establecimientoExpendio() { return establecimientoExpendio; }
    public LocalDate vigenteDesde() { return vigenteDesde; }
    public LocalDate vigenteHasta() { return vigenteHasta; }
    public String fuente() { return fuente; }
    public String versionFuente() { return versionFuente; }
    public List<PrincipioActivoAsociado> principiosActivos() { return principiosActivos; }
    public EstadoRegulatorio estado() { return estado; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
```

- [ ] **Step 6: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.ProductoReguladoTest"`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoRegulatorio.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/PrincipioActivoAsociado.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/ProductoRegulado.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/ProductoReguladoTest.java
git commit -m "feat(catalogo): agregar agregado de dominio ProductoRegulado"
```

---
