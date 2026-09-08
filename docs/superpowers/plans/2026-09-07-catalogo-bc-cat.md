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

### Task 8: Agregado `SKUComercial` (con `CodigoBarraSku`, `TipoSku`, `EstadoComercialSku`)

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/TipoSku.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoComercialSku.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/CodigoBarraSku.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/SKUComercial.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/SKUComercialTest.java`

**Interfaces:**
- Consumes: `SkuId`, `TenantId`, `ProductoReguladoId`, `CategoriaProductoId`, `MarcaId` (Task 2).
- Produces:
  - `CodigoBarraSku(String codigoBarra, String tipoCodigo, boolean esPrincipal, LocalDate vigenteDesde, LocalDate vigenteHasta, EstadoCatalogoSoporte estado)` — record simple; reutiliza `EstadoCatalogoSoporte` de `domain.model.soporte` (ACTIVO/INACTIVO, misma forma que la tabla real).
  - `SKUComercial.create(SkuId id, TenantId tenantId, ProductoReguladoId productoReguladoId, CategoriaProductoId categoriaId, MarcaId marcaId, TipoSku tipoSku, String codigoInterno, String descripcionComercial, String nombreCorto, String presentacionComercial, String unidadVentaCodigo, BigDecimal contenido, String unidadContenidoCodigo, BigDecimal pesoGramos, BigDecimal altoCm, BigDecimal anchoCm, BigDecimal largoCm, boolean permiteVentaFraccion, BigDecimal factorFraccion, boolean requiereLote, boolean requiereVencimiento, boolean afectoIgv, BigDecimal stockMinimoDefault, BigDecimal stockMaximoDefault, String imagenUri, String createdBy, Instant createdAt): Result<SKUComercial, ErrorDetail>`.
  - `restore(...)` con los mismos 25 campos + `List<CodigoBarraSku> codigosBarra` + `EstadoComercialSku estado` + `String updatedBy` + `Instant updatedAt`.
  - `conCodigoBarra(CodigoBarraSku codigo): SKUComercial`, `sinCodigoBarra(String codigoBarra): SKUComercial`, `conCodigoBarraPrincipal(String codigoBarra): SKUComercial` (marca ese código como único `esPrincipal=true`, el resto pasa a `false`) — todos devuelven nueva instancia.
  - Getters para los 25 campos + `codigosBarra()` + `estado()` + `createdBy()`/`updatedBy()`/`createdAt()`/`updatedAt()`.
- Usado por Task 16 (handlers), Task 22 (mapper JPA).

- [ ] **Step 1: Escribir el test que falla**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/SKUComercialTest.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.domain.model.soporte.EstadoCatalogoSoporte;
import com.softprimesolutions.catalogo.domain.valueobject.CategoriaProductoId;
import com.softprimesolutions.catalogo.domain.valueobject.MarcaId;
import com.softprimesolutions.catalogo.domain.valueobject.ProductoReguladoId;
import com.softprimesolutions.catalogo.domain.valueobject.SkuId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SKUComercialTest {

    private static final SkuId SKU_ID = new SkuId(UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589"));
    private static final TenantId TENANT_ID = new TenantId(UUID.fromString("a92adf67-70e7-4cc1-bb05-ff074df7fdf5"));
    private static final Instant CREATED_AT = Instant.parse("2026-09-07T10:00:00Z");

    @Test
    void createsANoRegulatedSkuWithoutProductoRegulado() {
        var result = SKUComercial.create(
                SKU_ID, TENANT_ID, null, null, null, TipoSku.NO_REGULADO, "SKU-001",
                "Alcohol en gel 250ml", null, null, null, null, null, null, null, null, null,
                false, null, true, true, true, BigDecimal.ZERO, null, null, "test", CREATED_AT);

        assertTrue(result.isSuccess());
        var sku = result.getOrElse(error -> null);
        assertEquals("Alcohol en gel 250ml", sku.descripcionComercial());
        assertEquals(EstadoComercialSku.ACTIVO, sku.estado());
    }

    @Test
    void rejectsARegulatedSkuWithoutProductoRegulado() {
        var result = SKUComercial.create(
                SKU_ID, TENANT_ID, null, null, null, TipoSku.REGULADO, "SKU-002",
                "Paracetamol 500mg", null, null, null, null, null, null, null, null, null,
                false, null, true, true, true, BigDecimal.ZERO, null, null, "test", CREATED_AT);

        assertTrue(result.isFailure());
        assertEquals("CAT_SKU_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void acceptsARegulatedSkuWithProductoRegulado() {
        var productoReguladoId = new ProductoReguladoId(UUID.randomUUID());
        var result = SKUComercial.create(
                SKU_ID, TENANT_ID, productoReguladoId, null, null, TipoSku.REGULADO, "SKU-003",
                "Paracetamol 500mg", null, null, null, null, null, null, null, null, null,
                false, null, true, true, true, BigDecimal.ZERO, null, null, "test", CREATED_AT);

        assertTrue(result.isSuccess());
    }

    @Test
    void rejectsAFactorFraccionWhenVentaFraccionIsDisabled() {
        var result = SKUComercial.create(
                SKU_ID, TENANT_ID, null, null, null, TipoSku.NO_REGULADO, "SKU-004",
                "Producto fraccionable", null, null, null, null, null, null, null, null, null,
                false, new BigDecimal("0.5"), true, true, true, BigDecimal.ZERO, null, null, "test", CREATED_AT);

        assertTrue(result.isFailure());
        assertEquals("CAT_SKU_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsStockMaximoBelowStockMinimo() {
        var result = SKUComercial.create(
                SKU_ID, TENANT_ID, null, null, null, TipoSku.NO_REGULADO, "SKU-005",
                "Producto con stock", null, null, null, null, null, null, null, null, null,
                false, null, true, true, true, new BigDecimal("10"), new BigDecimal("5"), null, "test", CREATED_AT);

        assertTrue(result.isFailure());
        assertEquals("CAT_SKU_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void managesBarcodesAsAnImmutableCollection() {
        var sku = SKUComercial.create(
                        SKU_ID, TENANT_ID, null, null, null, TipoSku.NO_REGULADO, "SKU-006",
                        "Alcohol en gel", null, null, null, null, null, null, null, null, null,
                        false, null, true, true, true, BigDecimal.ZERO, null, null, "test", CREATED_AT)
                .getOrElse(error -> null);

        var codigoUno = new CodigoBarraSku("7501234567890", "EAN13", true, null, null, EstadoCatalogoSoporte.ACTIVO);
        var codigoDos = new CodigoBarraSku("7501234567891", "EAN13", false, null, null, EstadoCatalogoSoporte.ACTIVO);

        var conCodigos = sku.conCodigoBarra(codigoUno).conCodigoBarra(codigoDos);
        assertEquals(2, conCodigos.codigosBarra().size());

        var conPrincipalCambiado = conCodigos.conCodigoBarraPrincipal("7501234567891");
        var principal = conPrincipalCambiado.codigosBarra().stream()
                .filter(CodigoBarraSku::esPrincipal).findFirst().orElseThrow();
        assertEquals("7501234567891", principal.codigoBarra());

        var sinCodigoUno = conPrincipalCambiado.sinCodigoBarra("7501234567890");
        assertEquals(1, sinCodigoUno.codigosBarra().size());
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.SKUComercialTest"`
Expected: FAIL — `SKUComercial`/`CodigoBarraSku`/`TipoSku`/`EstadoComercialSku` no existen todavía.

- [ ] **Step 3: Crear `TipoSku` y `EstadoComercialSku`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/TipoSku.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

public enum TipoSku {
    REGULADO,
    NO_REGULADO
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoComercialSku.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

public enum EstadoComercialSku {
    ACTIVO,
    INACTIVO,
    BLOQUEADO,
    DESCONTINUADO
}
```

- [ ] **Step 4: Crear `CodigoBarraSku`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/CodigoBarraSku.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

import com.softprimesolutions.catalogo.domain.model.soporte.EstadoCatalogoSoporte;
import java.time.LocalDate;
import java.util.Objects;

/** Código de barras asociado a un SKU comercial; un SKU puede tener varios, máximo uno principal activo. */
public record CodigoBarraSku(
        String codigoBarra,
        String tipoCodigo,
        boolean esPrincipal,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        EstadoCatalogoSoporte estado) {

    public CodigoBarraSku {
        Objects.requireNonNull(codigoBarra, "codigoBarra es obligatorio");
        Objects.requireNonNull(estado, "estado es obligatorio");
    }
}
```

- [ ] **Step 5: Crear `SKUComercial`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/SKUComercial.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

import com.softprimesolutions.catalogo.domain.model.soporte.EstadoCatalogoSoporte;
import com.softprimesolutions.catalogo.domain.valueobject.CategoriaProductoId;
import com.softprimesolutions.catalogo.domain.valueobject.MarcaId;
import com.softprimesolutions.catalogo.domain.valueobject.ProductoReguladoId;
import com.softprimesolutions.catalogo.domain.valueobject.SkuId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Unidad/presentación comercial que se vende en retail, opcionalmente ligada a un producto regulado. */
public final class SKUComercial extends AggregateRoot {

    private static final int CODIGO_INTERNO_MIN_LENGTH = 2;
    private static final int CODIGO_INTERNO_MAX_LENGTH = 60;
    private static final int DESCRIPCION_COMERCIAL_MIN_LENGTH = 2;
    private static final int DESCRIPCION_COMERCIAL_MAX_LENGTH = 500;
    private static final int NOMBRE_CORTO_MAX_LENGTH = 200;
    private static final int PRESENTACION_COMERCIAL_MAX_LENGTH = 300;
    private static final int CODIGO_REFERENCIA_MAX_LENGTH = 30;
    private static final int IMAGEN_URI_MAX_LENGTH = 2000;

    private final SkuId id;
    private final TenantId tenantId;
    private final ProductoReguladoId productoReguladoId;
    private final CategoriaProductoId categoriaId;
    private final MarcaId marcaId;
    private final TipoSku tipoSku;
    private final String codigoInterno;
    private final String descripcionComercial;
    private final String nombreCorto;
    private final String presentacionComercial;
    private final String unidadVentaCodigo;
    private final BigDecimal contenido;
    private final String unidadContenidoCodigo;
    private final BigDecimal pesoGramos;
    private final BigDecimal altoCm;
    private final BigDecimal anchoCm;
    private final BigDecimal largoCm;
    private final boolean permiteVentaFraccion;
    private final BigDecimal factorFraccion;
    private final boolean requiereLote;
    private final boolean requiereVencimiento;
    private final boolean afectoIgv;
    private final BigDecimal stockMinimoDefault;
    private final BigDecimal stockMaximoDefault;
    private final String imagenUri;
    private final List<CodigoBarraSku> codigosBarra;
    private final EstadoComercialSku estado;
    private final String createdBy;
    private final Instant createdAt;
    private final String updatedBy;
    private final Instant updatedAt;

    private SKUComercial(
            SkuId id, TenantId tenantId, ProductoReguladoId productoReguladoId, CategoriaProductoId categoriaId,
            MarcaId marcaId, TipoSku tipoSku, String codigoInterno, String descripcionComercial,
            String nombreCorto, String presentacionComercial, String unidadVentaCodigo, BigDecimal contenido,
            String unidadContenidoCodigo, BigDecimal pesoGramos, BigDecimal altoCm, BigDecimal anchoCm,
            BigDecimal largoCm, boolean permiteVentaFraccion, BigDecimal factorFraccion, boolean requiereLote,
            boolean requiereVencimiento, boolean afectoIgv, BigDecimal stockMinimoDefault,
            BigDecimal stockMaximoDefault, String imagenUri, List<CodigoBarraSku> codigosBarra,
            EstadoComercialSku estado, String createdBy, Instant createdAt, String updatedBy, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.productoReguladoId = productoReguladoId;
        this.categoriaId = categoriaId;
        this.marcaId = marcaId;
        this.tipoSku = tipoSku;
        this.codigoInterno = codigoInterno;
        this.descripcionComercial = descripcionComercial;
        this.nombreCorto = nombreCorto;
        this.presentacionComercial = presentacionComercial;
        this.unidadVentaCodigo = unidadVentaCodigo;
        this.contenido = contenido;
        this.unidadContenidoCodigo = unidadContenidoCodigo;
        this.pesoGramos = pesoGramos;
        this.altoCm = altoCm;
        this.anchoCm = anchoCm;
        this.largoCm = largoCm;
        this.permiteVentaFraccion = permiteVentaFraccion;
        this.factorFraccion = factorFraccion;
        this.requiereLote = requiereLote;
        this.requiereVencimiento = requiereVencimiento;
        this.afectoIgv = afectoIgv;
        this.stockMinimoDefault = stockMinimoDefault;
        this.stockMaximoDefault = stockMaximoDefault;
        this.imagenUri = imagenUri;
        this.codigosBarra = List.copyOf(codigosBarra);
        this.estado = estado;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public static Result<SKUComercial, ErrorDetail> create(
            SkuId id, TenantId tenantId, ProductoReguladoId productoReguladoId, CategoriaProductoId categoriaId,
            MarcaId marcaId, TipoSku tipoSku, String codigoInterno, String descripcionComercial,
            String nombreCorto, String presentacionComercial, String unidadVentaCodigo, BigDecimal contenido,
            String unidadContenidoCodigo, BigDecimal pesoGramos, BigDecimal altoCm, BigDecimal anchoCm,
            BigDecimal largoCm, boolean permiteVentaFraccion, BigDecimal factorFraccion, boolean requiereLote,
            boolean requiereVencimiento, boolean afectoIgv, BigDecimal stockMinimoDefault,
            BigDecimal stockMaximoDefault, String imagenUri, String createdBy, Instant createdAt) {
        if (id == null) return invalid("id", "La identidad del SKU es obligatoria.");
        if (tenantId == null) return invalid("tenantId", "El tenant es obligatorio.");
        if (tipoSku == null) return invalid("tipoSku", "El tipo de SKU es obligatorio.");
        if (tipoSku == TipoSku.REGULADO && productoReguladoId == null) {
            return invalid("productoReguladoId", "Un SKU regulado requiere un producto regulado asociado.");
        }
        if (createdBy == null || createdBy.isBlank()) {
            return invalid("createdBy", "El creador del registro es obligatorio.");
        }
        if (createdAt == null) return invalid("createdAt", "El instante de registro es obligatorio.");

        var normalizedCodigoInterno = normalizeSpaces(codigoInterno);
        if (normalizedCodigoInterno == null || normalizedCodigoInterno.length() < CODIGO_INTERNO_MIN_LENGTH
                || normalizedCodigoInterno.length() > CODIGO_INTERNO_MAX_LENGTH) {
            return invalid("codigoInterno", "El código interno debe tener entre 2 y 60 caracteres.");
        }

        var normalizedDescripcionComercial = normalizeSpaces(descripcionComercial);
        if (normalizedDescripcionComercial == null
                || normalizedDescripcionComercial.length() < DESCRIPCION_COMERCIAL_MIN_LENGTH
                || normalizedDescripcionComercial.length() > DESCRIPCION_COMERCIAL_MAX_LENGTH) {
            return invalid("descripcionComercial", "La descripción comercial debe tener entre 2 y 500 caracteres.");
        }

        var normalizedNombreCorto = normalizeNullable(nombreCorto);
        if (!withinLength(normalizedNombreCorto, NOMBRE_CORTO_MAX_LENGTH)) {
            return invalid("nombreCorto", "El nombre corto no debe exceder 200 caracteres.");
        }

        var normalizedPresentacionComercial = normalizeNullable(presentacionComercial);
        if (!withinLength(normalizedPresentacionComercial, PRESENTACION_COMERCIAL_MAX_LENGTH)) {
            return invalid("presentacionComercial", "La presentación comercial no debe exceder 300 caracteres.");
        }

        var normalizedUnidadVentaCodigo = normalizeUpper(unidadVentaCodigo);
        if (!withinLength(normalizedUnidadVentaCodigo, CODIGO_REFERENCIA_MAX_LENGTH)) {
            return invalid("unidadVentaCodigo", "El código de unidad de venta no debe exceder 30 caracteres.");
        }

        if (contenido != null && contenido.signum() <= 0) {
            return invalid("contenido", "El contenido debe ser mayor que cero si se informa.");
        }

        var normalizedUnidadContenidoCodigo = normalizeUpper(unidadContenidoCodigo);
        if (!withinLength(normalizedUnidadContenidoCodigo, CODIGO_REFERENCIA_MAX_LENGTH)) {
            return invalid("unidadContenidoCodigo", "El código de unidad de contenido no debe exceder 30 caracteres.");
        }

        if (pesoGramos != null && pesoGramos.signum() <= 0) {
            return invalid("pesoGramos", "El peso debe ser mayor que cero si se informa.");
        }
        if (altoCm != null && altoCm.signum() <= 0) {
            return invalid("altoCm", "El alto debe ser mayor que cero si se informa.");
        }
        if (anchoCm != null && anchoCm.signum() <= 0) {
            return invalid("anchoCm", "El ancho debe ser mayor que cero si se informa.");
        }
        if (largoCm != null && largoCm.signum() <= 0) {
            return invalid("largoCm", "El largo debe ser mayor que cero si se informa.");
        }

        if (permiteVentaFraccion && (factorFraccion == null || factorFraccion.signum() <= 0)) {
            return invalid("factorFraccion", "El factor de fracción es obligatorio y positivo cuando se permite venta por fracción.");
        }
        if (!permiteVentaFraccion && factorFraccion != null) {
            return invalid("factorFraccion", "El factor de fracción debe ser nulo cuando no se permite venta por fracción.");
        }

        if (stockMinimoDefault == null || stockMinimoDefault.signum() < 0) {
            return invalid("stockMinimoDefault", "El stock mínimo por defecto debe ser mayor o igual a cero.");
        }
        if (stockMaximoDefault != null && stockMaximoDefault.compareTo(stockMinimoDefault) < 0) {
            return invalid("stockMaximoDefault", "El stock máximo por defecto no puede ser menor que el mínimo.");
        }

        var normalizedImagenUri = normalizeNullable(imagenUri);
        if (!withinLength(normalizedImagenUri, IMAGEN_URI_MAX_LENGTH)) {
            return invalid("imagenUri", "La URI de imagen no debe exceder 2000 caracteres.");
        }

        return Result.success(new SKUComercial(
                id, tenantId, productoReguladoId, categoriaId, marcaId, tipoSku, normalizedCodigoInterno,
                normalizedDescripcionComercial, normalizedNombreCorto, normalizedPresentacionComercial,
                normalizedUnidadVentaCodigo, contenido, normalizedUnidadContenidoCodigo, pesoGramos, altoCm,
                anchoCm, largoCm, permiteVentaFraccion, factorFraccion, requiereLote, requiereVencimiento,
                afectoIgv, stockMinimoDefault, stockMaximoDefault, normalizedImagenUri, List.of(),
                EstadoComercialSku.ACTIVO, createdBy.trim(), createdAt, null, null));
    }

    public static SKUComercial restore(
            SkuId id, TenantId tenantId, ProductoReguladoId productoReguladoId, CategoriaProductoId categoriaId,
            MarcaId marcaId, TipoSku tipoSku, String codigoInterno, String descripcionComercial,
            String nombreCorto, String presentacionComercial, String unidadVentaCodigo, BigDecimal contenido,
            String unidadContenidoCodigo, BigDecimal pesoGramos, BigDecimal altoCm, BigDecimal anchoCm,
            BigDecimal largoCm, boolean permiteVentaFraccion, BigDecimal factorFraccion, boolean requiereLote,
            boolean requiereVencimiento, boolean afectoIgv, BigDecimal stockMinimoDefault,
            BigDecimal stockMaximoDefault, String imagenUri, List<CodigoBarraSku> codigosBarra,
            EstadoComercialSku estado, String createdBy, Instant createdAt, String updatedBy,
            Instant updatedAt) {
        return new SKUComercial(
                id, tenantId, productoReguladoId, categoriaId, marcaId, tipoSku, codigoInterno,
                descripcionComercial, nombreCorto, presentacionComercial, unidadVentaCodigo, contenido,
                unidadContenidoCodigo, pesoGramos, altoCm, anchoCm, largoCm, permiteVentaFraccion,
                factorFraccion, requiereLote, requiereVencimiento, afectoIgv, stockMinimoDefault,
                stockMaximoDefault, imagenUri, codigosBarra, estado, createdBy, createdAt, updatedBy, updatedAt);
    }

    public SKUComercial conCodigoBarra(CodigoBarraSku codigo) {
        var nuevaLista = new ArrayList<>(codigosBarra);
        nuevaLista.removeIf(existing -> existing.codigoBarra().equals(codigo.codigoBarra()));
        nuevaLista.add(codigo);
        return copyWithCodigosBarra(nuevaLista);
    }

    public SKUComercial sinCodigoBarra(String codigoBarra) {
        var nuevaLista = new ArrayList<>(codigosBarra);
        nuevaLista.removeIf(existing -> existing.codigoBarra().equals(codigoBarra));
        return copyWithCodigosBarra(nuevaLista);
    }

    public SKUComercial conCodigoBarraPrincipal(String codigoBarra) {
        var nuevaLista = codigosBarra.stream()
                .map(existing -> new CodigoBarraSku(
                        existing.codigoBarra(), existing.tipoCodigo(),
                        existing.codigoBarra().equals(codigoBarra), existing.vigenteDesde(),
                        existing.vigenteHasta(), existing.estado()))
                .toList();
        return copyWithCodigosBarra(new ArrayList<>(nuevaLista));
    }

    private SKUComercial copyWithCodigosBarra(List<CodigoBarraSku> nuevaLista) {
        return new SKUComercial(
                id, tenantId, productoReguladoId, categoriaId, marcaId, tipoSku, codigoInterno,
                descripcionComercial, nombreCorto, presentacionComercial, unidadVentaCodigo, contenido,
                unidadContenidoCodigo, pesoGramos, altoCm, anchoCm, largoCm, permiteVentaFraccion,
                factorFraccion, requiereLote, requiereVencimiento, afectoIgv, stockMinimoDefault,
                stockMaximoDefault, imagenUri, nuevaLista, estado, createdBy, createdAt, updatedBy, updatedAt);
    }

    private static Result<SKUComercial, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("CAT_SKU_INVALIDO", message, Map.of("field", field)));
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

    public SkuId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public ProductoReguladoId productoReguladoId() { return productoReguladoId; }
    public CategoriaProductoId categoriaId() { return categoriaId; }
    public MarcaId marcaId() { return marcaId; }
    public TipoSku tipoSku() { return tipoSku; }
    public String codigoInterno() { return codigoInterno; }
    public String descripcionComercial() { return descripcionComercial; }
    public String nombreCorto() { return nombreCorto; }
    public String presentacionComercial() { return presentacionComercial; }
    public String unidadVentaCodigo() { return unidadVentaCodigo; }
    public BigDecimal contenido() { return contenido; }
    public String unidadContenidoCodigo() { return unidadContenidoCodigo; }
    public BigDecimal pesoGramos() { return pesoGramos; }
    public BigDecimal altoCm() { return altoCm; }
    public BigDecimal anchoCm() { return anchoCm; }
    public BigDecimal largoCm() { return largoCm; }
    public boolean permiteVentaFraccion() { return permiteVentaFraccion; }
    public BigDecimal factorFraccion() { return factorFraccion; }
    public boolean requiereLote() { return requiereLote; }
    public boolean requiereVencimiento() { return requiereVencimiento; }
    public boolean afectoIgv() { return afectoIgv; }
    public BigDecimal stockMinimoDefault() { return stockMinimoDefault; }
    public BigDecimal stockMaximoDefault() { return stockMaximoDefault; }
    public String imagenUri() { return imagenUri; }
    public List<CodigoBarraSku> codigosBarra() { return codigosBarra; }
    public EstadoComercialSku estado() { return estado; }
    public String createdBy() { return createdBy; }
    public Instant createdAt() { return createdAt; }
    public String updatedBy() { return updatedBy; }
    public Instant updatedAt() { return updatedAt; }
}
```

- [ ] **Step 6: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.SKUComercialTest"`
Expected: PASS

- [ ] **Step 7: Ejecutar todos los tests de dominio juntos**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.*"`
Expected: PASS (todas las clases de dominio creadas hasta ahora, verdes).

- [ ] **Step 8: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/TipoSku.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoComercialSku.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/CodigoBarraSku.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/SKUComercial.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/SKUComercialTest.java
git commit -m "feat(catalogo): agregar agregado de dominio SKUComercial"
```

---

## Fase 3 — Application: DTOs, puertos y casos de uso

A partir de aquí, dado el número de entidades (9), las tareas agrupan varias entidades afines en vez de una tarea por entidad — mismo rigor TDD y código completo, menos fragmentación.

### Task 9: DTOs de aplicación (`command`, `query`, `result`) para las 9 entidades

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/` — un record por comando (ver lista abajo).
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/` — un record por query.
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/` — un record por resultado + `PaginaResult<T>` genérico.

**Interfaces:**
- Consumes: `Command<R>`, `Query<R>` (shared-application).
- Produces: todos los records listados abajo. Son DTOs puros sin lógica — no requieren test dedicado, se validan indirectamente vía las Tasks 11-16 (handlers).

- [ ] **Step 1: Crear los `result` (se necesitan primero porque los `command`/`query` los referencian como tipo de retorno)**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/CondicionVentaResult.java`:

```java
package com.softprimesolutions.catalogo.application.dto.result;

import java.time.LocalDate;

public record CondicionVentaResult(
        String codigo, String denominacion, boolean requiereReceta, boolean requiereRetencion,
        String fuente, String versionFuente, LocalDate vigenteDesde, LocalDate vigenteHasta, String estado) {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/FormaFarmaceuticaResult.java`:

```java
package com.softprimesolutions.catalogo.application.dto.result;

public record FormaFarmaceuticaResult(String codigo, String denominacion, String fuente, String estado) {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/ViaAdministracionResult.java`:

```java
package com.softprimesolutions.catalogo.application.dto.result;

public record ViaAdministracionResult(String codigo, String denominacion, String fuente, String estado) {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/UnidadMedidaResult.java`:

```java
package com.softprimesolutions.catalogo.application.dto.result;

public record UnidadMedidaResult(
        String codigo, String denominacion, String simbolo, boolean permiteDecimal, String fuente, String estado) {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/ClasificacionControladaResult.java`:

```java
package com.softprimesolutions.catalogo.application.dto.result;

public record ClasificacionControladaResult(
        String codigo, String denominacion, String normaFuente, boolean requiereRecetaEspecial,
        boolean retieneReceta, Integer vigenciaRecetaDias, String estado) {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/PrincipioActivoResult.java`:

```java
package com.softprimesolutions.catalogo.application.dto.result;

import java.util.UUID;

public record PrincipioActivoResult(
        UUID id, String codigoFuente, String denominacion, String nombreNormalizado, String fuente, String estado) {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/MarcaResult.java`:

```java
package com.softprimesolutions.catalogo.application.dto.result;

import java.util.UUID;

public record MarcaResult(
        UUID id, UUID tenantId, String codigo, String nombre, String descripcion, String estado) {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/CategoriaProductoResult.java`:

```java
package com.softprimesolutions.catalogo.application.dto.result;

import java.util.UUID;

public record CategoriaProductoResult(
        UUID id, UUID tenantId, UUID categoriaPadreId, String codigo, String nombre, String descripcion,
        int nivel, int orden, String estado) {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/PrincipioActivoAsociadoResult.java`:

```java
package com.softprimesolutions.catalogo.application.dto.result;

import java.math.BigDecimal;
import java.util.UUID;

public record PrincipioActivoAsociadoResult(
        UUID principioActivoId, String concentracionTexto, BigDecimal cantidad, String unidadMedidaCodigo,
        boolean esPrincipal, short orden) {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/ProductoReguladoResult.java`:

```java
package com.softprimesolutions.catalogo.application.dto.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProductoReguladoResult(
        UUID id,
        String tipoProducto,
        String rubroCodigo,
        String tipoRegistro,
        String numeroRegistro,
        String denominacion,
        String concentracionTexto,
        String presentacionRegulatoria,
        String formaFarmaceuticaCodigo,
        String viaAdministracionCodigo,
        String unidadMedidaCodigo,
        String condicionVentaCodigo,
        String clasificacionAtc,
        String clasificacionControladaCodigo,
        String tipoLiberacion,
        String origenFabricacion,
        String paisOrigen,
        String subpartidaNacional,
        String titularRegistro,
        String fabricante,
        String importador,
        String establecimientoExpendio,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        String fuente,
        String versionFuente,
        List<PrincipioActivoAsociadoResult> principiosActivos,
        String estadoRegulatorio,
        Instant createdAt,
        Instant updatedAt) {

    public ProductoReguladoResult {
        principiosActivos = List.copyOf(principiosActivos);
    }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/ProductoReguladoResumen.java`:

```java
package com.softprimesolutions.catalogo.application.dto.result;

import java.util.UUID;

public record ProductoReguladoResumen(
        UUID id, String denominacion, String condicionVentaCodigo, String estadoRegulatorio) {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/CodigoBarraSkuResult.java`:

```java
package com.softprimesolutions.catalogo.application.dto.result;

import java.time.LocalDate;

public record CodigoBarraSkuResult(
        String codigoBarra, String tipoCodigo, boolean esPrincipal, LocalDate vigenteDesde,
        LocalDate vigenteHasta, String estado) {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/SkuResult.java`:

```java
package com.softprimesolutions.catalogo.application.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SkuResult(
        UUID id,
        UUID tenantId,
        UUID productoReguladoId,
        UUID categoriaId,
        UUID marcaId,
        String tipoSku,
        String codigoInterno,
        String descripcionComercial,
        String nombreCorto,
        String presentacionComercial,
        String unidadVentaCodigo,
        BigDecimal contenido,
        String unidadContenidoCodigo,
        BigDecimal pesoGramos,
        BigDecimal altoCm,
        BigDecimal anchoCm,
        BigDecimal largoCm,
        boolean permiteVentaFraccion,
        BigDecimal factorFraccion,
        boolean requiereLote,
        boolean requiereVencimiento,
        boolean afectoIgv,
        BigDecimal stockMinimoDefault,
        BigDecimal stockMaximoDefault,
        String imagenUri,
        List<CodigoBarraSkuResult> codigosBarra,
        String estado,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt) {

    public SkuResult {
        codigosBarra = List.copyOf(codigosBarra);
    }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/SkuResumen.java`:

```java
package com.softprimesolutions.catalogo.application.dto.result;

import java.util.UUID;

public record SkuResumen(
        UUID id, String codigoInterno, String descripcionComercial, String tipoSku, String estado) {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/PaginaResult.java`:

```java
package com.softprimesolutions.catalogo.application.dto.result;

import java.util.List;

public record PaginaResult<T>(List<T> items, int page, int size, long totalElements) {

    public PaginaResult {
        items = List.copyOf(items);
    }
}
```

- [ ] **Step 2: Crear los `command` (5 catálogos de soporte + PrincipioActivo)**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/CrearCondicionVentaCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.time.LocalDate;

public record CrearCondicionVentaCommand(
        String codigo, String denominacion, boolean requiereReceta, boolean requiereRetencion,
        String fuente, String versionFuente, LocalDate vigenteDesde, LocalDate vigenteHasta)
        implements Command<CondicionVentaResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/ActualizarCondicionVentaCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.time.LocalDate;

public record ActualizarCondicionVentaCommand(
        String codigo, String denominacion, boolean requiereReceta, boolean requiereRetencion,
        String fuente, String versionFuente, LocalDate vigenteDesde, LocalDate vigenteHasta)
        implements Command<CondicionVentaResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/CrearFormaFarmaceuticaCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.shared.application.cqrs.Command;

public record CrearFormaFarmaceuticaCommand(String codigo, String denominacion, String fuente)
        implements Command<FormaFarmaceuticaResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/ActualizarFormaFarmaceuticaCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.shared.application.cqrs.Command;

public record ActualizarFormaFarmaceuticaCommand(String codigo, String denominacion, String fuente)
        implements Command<FormaFarmaceuticaResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/CrearViaAdministracionCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.shared.application.cqrs.Command;

public record CrearViaAdministracionCommand(String codigo, String denominacion, String fuente)
        implements Command<ViaAdministracionResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/ActualizarViaAdministracionCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.shared.application.cqrs.Command;

public record ActualizarViaAdministracionCommand(String codigo, String denominacion, String fuente)
        implements Command<ViaAdministracionResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/CrearUnidadMedidaCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.shared.application.cqrs.Command;

public record CrearUnidadMedidaCommand(
        String codigo, String denominacion, String simbolo, boolean permiteDecimal, String fuente)
        implements Command<UnidadMedidaResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/ActualizarUnidadMedidaCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.shared.application.cqrs.Command;

public record ActualizarUnidadMedidaCommand(
        String codigo, String denominacion, String simbolo, boolean permiteDecimal, String fuente)
        implements Command<UnidadMedidaResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/CrearClasificacionControladaCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.shared.application.cqrs.Command;

public record CrearClasificacionControladaCommand(
        String codigo, String denominacion, String normaFuente, boolean requiereRecetaEspecial,
        boolean retieneReceta, Integer vigenciaRecetaDias) implements Command<ClasificacionControladaResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/ActualizarClasificacionControladaCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.shared.application.cqrs.Command;

public record ActualizarClasificacionControladaCommand(
        String codigo, String denominacion, String normaFuente, boolean requiereRecetaEspecial,
        boolean retieneReceta, Integer vigenciaRecetaDias) implements Command<ClasificacionControladaResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/CrearPrincipioActivoCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.shared.application.cqrs.Command;

public record CrearPrincipioActivoCommand(
        String codigoFuente, String denominacion, String nombreNormalizado, String fuente)
        implements Command<PrincipioActivoResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/ActualizarPrincipioActivoCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.util.UUID;

public record ActualizarPrincipioActivoCommand(
        UUID principioActivoId, String codigoFuente, String denominacion, String nombreNormalizado, String fuente)
        implements Command<PrincipioActivoResult> {
}
```

- [ ] **Step 3: Crear los `command` de Marca y CategoriaProducto**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/CrearMarcaCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.util.UUID;

public record CrearMarcaCommand(UUID tenantId, String codigo, String nombre, String descripcion)
        implements Command<MarcaResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/ActualizarMarcaCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.util.UUID;

public record ActualizarMarcaCommand(UUID tenantId, UUID marcaId, String codigo, String nombre, String descripcion)
        implements Command<MarcaResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/CrearCategoriaProductoCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.util.UUID;

public record CrearCategoriaProductoCommand(
        UUID tenantId, UUID categoriaPadreId, String codigo, String nombre, String descripcion,
        int nivel, int orden) implements Command<CategoriaProductoResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/ActualizarCategoriaProductoCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.util.UUID;

public record ActualizarCategoriaProductoCommand(
        UUID tenantId, UUID categoriaId, UUID categoriaPadreId, String codigo, String nombre,
        String descripcion, int nivel, int orden) implements Command<CategoriaProductoResult> {
}
```

- [ ] **Step 4: Crear los `command`/`query` de ProductoRegulado**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/CrearProductoReguladoCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.time.LocalDate;

public record CrearProductoReguladoCommand(
        String tipoProducto,
        String rubroCodigo,
        String tipoRegistro,
        String numeroRegistro,
        String denominacion,
        String concentracionTexto,
        String presentacionRegulatoria,
        String formaFarmaceuticaCodigo,
        String viaAdministracionCodigo,
        String unidadMedidaCodigo,
        String condicionVentaCodigo,
        String clasificacionAtc,
        String clasificacionControladaCodigo,
        String tipoLiberacion,
        String origenFabricacion,
        String paisOrigen,
        String subpartidaNacional,
        String titularRegistro,
        String fabricante,
        String importador,
        String establecimientoExpendio,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        String fuente,
        String versionFuente) implements Command<ProductoReguladoResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/ActualizarProductoReguladoCommand.java` (mismos campos que `CrearProductoReguladoCommand` + `productoReguladoId` al inicio):

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.time.LocalDate;
import java.util.UUID;

public record ActualizarProductoReguladoCommand(
        UUID productoReguladoId,
        String tipoProducto,
        String rubroCodigo,
        String tipoRegistro,
        String numeroRegistro,
        String denominacion,
        String concentracionTexto,
        String presentacionRegulatoria,
        String formaFarmaceuticaCodigo,
        String viaAdministracionCodigo,
        String unidadMedidaCodigo,
        String condicionVentaCodigo,
        String clasificacionAtc,
        String clasificacionControladaCodigo,
        String tipoLiberacion,
        String origenFabricacion,
        String paisOrigen,
        String subpartidaNacional,
        String titularRegistro,
        String fabricante,
        String importador,
        String establecimientoExpendio,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        String fuente,
        String versionFuente) implements Command<ProductoReguladoResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/AsociarPrincipioActivoCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.math.BigDecimal;
import java.util.UUID;

public record AsociarPrincipioActivoCommand(
        UUID productoReguladoId, UUID principioActivoId, String concentracionTexto, BigDecimal cantidad,
        String unidadMedidaCodigo, boolean esPrincipal, short orden) implements Command<ProductoReguladoResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/DesasociarPrincipioActivoCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.util.UUID;

public record DesasociarPrincipioActivoCommand(UUID productoReguladoId, UUID principioActivoId)
        implements Command<ProductoReguladoResult> {
}
```

- [ ] **Step 5: Crear los `command` de SKUComercial**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/CrearSkuCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.SkuResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.math.BigDecimal;
import java.util.UUID;

public record CrearSkuCommand(
        UUID tenantId,
        UUID productoReguladoId,
        UUID categoriaId,
        UUID marcaId,
        String tipoSku,
        String codigoInterno,
        String descripcionComercial,
        String nombreCorto,
        String presentacionComercial,
        String unidadVentaCodigo,
        BigDecimal contenido,
        String unidadContenidoCodigo,
        BigDecimal pesoGramos,
        BigDecimal altoCm,
        BigDecimal anchoCm,
        BigDecimal largoCm,
        boolean permiteVentaFraccion,
        BigDecimal factorFraccion,
        boolean requiereLote,
        boolean requiereVencimiento,
        boolean afectoIgv,
        BigDecimal stockMinimoDefault,
        BigDecimal stockMaximoDefault,
        String imagenUri,
        String createdBy) implements Command<SkuResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/ActualizarSkuCommand.java` (mismos campos + `skuId`, sin `createdBy`, con `updatedBy`):

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.SkuResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.math.BigDecimal;
import java.util.UUID;

public record ActualizarSkuCommand(
        UUID tenantId,
        UUID skuId,
        UUID productoReguladoId,
        UUID categoriaId,
        UUID marcaId,
        String tipoSku,
        String codigoInterno,
        String descripcionComercial,
        String nombreCorto,
        String presentacionComercial,
        String unidadVentaCodigo,
        BigDecimal contenido,
        String unidadContenidoCodigo,
        BigDecimal pesoGramos,
        BigDecimal altoCm,
        BigDecimal anchoCm,
        BigDecimal largoCm,
        boolean permiteVentaFraccion,
        BigDecimal factorFraccion,
        boolean requiereLote,
        boolean requiereVencimiento,
        boolean afectoIgv,
        BigDecimal stockMinimoDefault,
        BigDecimal stockMaximoDefault,
        String imagenUri,
        String updatedBy) implements Command<SkuResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/AgregarCodigoBarraCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.SkuResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.time.LocalDate;
import java.util.UUID;

public record AgregarCodigoBarraCommand(
        UUID tenantId, UUID skuId, String codigoBarra, String tipoCodigo, LocalDate vigenteDesde,
        LocalDate vigenteHasta) implements Command<SkuResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/EliminarCodigoBarraCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.SkuResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.util.UUID;

public record EliminarCodigoBarraCommand(UUID tenantId, UUID skuId, String codigoBarra)
        implements Command<SkuResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/MarcarCodigoBarraPrincipalCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.SkuResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.util.UUID;

public record MarcarCodigoBarraPrincipalCommand(UUID tenantId, UUID skuId, String codigoBarra)
        implements Command<SkuResult> {
}
```

- [ ] **Step 6: Crear los `query`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ListarCondicionesVentaQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.List;

public record ListarCondicionesVentaQuery(String estado) implements Query<List<CondicionVentaResult>> {
}
```

Crear, siguiendo exactamente la misma forma (un campo `String estado`, retorno `List<XResult>`), los siguientes 4 queries:
- `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ListarFormasFarmaceuticasQuery.java` → `Query<List<FormaFarmaceuticaResult>>`
- `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ListarViasAdministracionQuery.java` → `Query<List<ViaAdministracionResult>>`
- `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ListarUnidadesMedidaQuery.java` → `Query<List<UnidadMedidaResult>>`
- `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ListarClasificacionesControladasQuery.java` → `Query<List<ClasificacionControladaResult>>`

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ListarPrincipioActivoQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.List;

public record ListarPrincipioActivoQuery(String texto, String estado) implements Query<List<PrincipioActivoResult>> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ListarMarcasQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.List;
import java.util.UUID;

public record ListarMarcasQuery(UUID tenantId, String estado) implements Query<List<MarcaResult>> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ListarCategoriasProductoQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.List;
import java.util.UUID;

public record ListarCategoriasProductoQuery(UUID tenantId, UUID categoriaPadreId, String estado)
        implements Query<List<CategoriaProductoResult>> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarProductoReguladoQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.UUID;

public record ConsultarProductoReguladoQuery(UUID productoReguladoId) implements Query<ProductoReguladoResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ListarProductosReguladosQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResumen;
import com.softprimesolutions.shared.application.cqrs.Query;

public record ListarProductosReguladosQuery(
        String texto, String condicionVentaCodigo, String estadoRegulatorio, int page, int size)
        implements Query<PaginaResult<ProductoReguladoResumen>> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarSkuQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.SkuResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.UUID;

public record ConsultarSkuQuery(UUID tenantId, UUID skuId) implements Query<SkuResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ListarSkusQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.SkuResumen;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.UUID;

public record ListarSkusQuery(
        UUID tenantId, String texto, UUID categoriaId, UUID marcaId, String tipoSku, String estado,
        int page, int size) implements Query<PaginaResult<SkuResumen>> {
}
```

- [ ] **Step 7: Compilar el módulo**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/
git commit -m "feat(catalogo): agregar DTOs de aplicacion command/query/result"
```

---

### Task 10: Puertos `application/port/{in,out}` completos

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoSoportePort.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoComercialPort.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/ProductoReguladoPort.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoReadPort.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/` — una interfaz por caso de uso (lista completa abajo).

**Interfaces:**
- Consumes: DTOs (Task 9), agregados de dominio (Task 3-8).
- Produces: todas las interfaces listadas — contratos que las Tasks 11-16 (handlers) implementan y las Tasks 18-21 (adapters) implementan del lado `out`.

No requiere test dedicado (interfaces puras).

- [ ] **Step 1: Crear `CatalogoSoportePort`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoSoportePort.java`:

```java
package com.softprimesolutions.catalogo.application.port.out;

import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import java.time.Instant;
import java.util.UUID;

public interface CatalogoSoportePort {

    SaveOutcome save(CondicionVenta condicionVenta);

    SaveOutcome save(FormaFarmaceutica formaFarmaceutica);

    SaveOutcome save(ViaAdministracion viaAdministracion);

    SaveOutcome save(UnidadMedida unidadMedida);

    SaveOutcome save(ClasificacionControlada clasificacionControlada);

    SavePrincipioActivoOutcome save(PrincipioActivo principioActivo);

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

    enum SavePrincipioActivoOutcome { CREATED, UPDATED, NOT_FOUND }
}
```

- [ ] **Step 2: Crear `CatalogoComercialPort`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoComercialPort.java`:

```java
package com.softprimesolutions.catalogo.application.port.out;

import com.softprimesolutions.catalogo.domain.model.CategoriaProducto;
import com.softprimesolutions.catalogo.domain.model.Marca;
import com.softprimesolutions.catalogo.domain.model.SKUComercial;
import java.time.Instant;
import java.util.UUID;

public interface CatalogoComercialPort {

    SaveMarcaOutcome save(Marca marca);

    SaveCategoriaOutcome save(CategoriaProducto categoria);

    SaveSkuOutcome save(SKUComercial sku);

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

- [ ] **Step 3: Crear `ProductoReguladoPort`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/ProductoReguladoPort.java`:

```java
package com.softprimesolutions.catalogo.application.port.out;

import com.softprimesolutions.catalogo.domain.model.ProductoRegulado;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ProductoReguladoPort {

    SaveOutcome save(ProductoRegulado productoRegulado);

    Optional<ProductoRegulado> findById(UUID productoReguladoId);

    boolean changeStatus(UUID productoReguladoId, String status, Instant changedAt);

    enum SaveOutcome {
        CREATED, UPDATED, NOT_FOUND, FORMA_FARMACEUTICA_NOT_FOUND, VIA_ADMINISTRACION_NOT_FOUND,
        UNIDAD_MEDIDA_NOT_FOUND, CONDICION_VENTA_NOT_FOUND, CLASIFICACION_CONTROLADA_NOT_FOUND,
        PRINCIPIO_ACTIVO_NOT_FOUND
    }
}
```

- [ ] **Step 4: Crear `CatalogoReadPort`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoReadPort.java`:

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

    List<MarcaResult> findMarcas(UUID tenantId, String estado);

    List<CategoriaProductoResult> findCategoriasProducto(UUID tenantId, UUID categoriaPadreId, String estado);

    PaginaResult<ProductoReguladoResumen> findProductosRegulados(
            String texto, String condicionVentaCodigo, String estadoRegulatorio, int page, int size);

    PaginaResult<SkuResumen> findSkus(
            UUID tenantId, String texto, UUID categoriaId, UUID marcaId, String tipoSku, String estado,
            int page, int size);
}
```

- [ ] **Step 5: Crear los puertos `in` de los 5 catálogos de soporte + PrincipioActivo**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/CrearCondicionVentaUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.CrearCondicionVentaCommand;
import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface CrearCondicionVentaUseCase {
    Result<CondicionVentaResult, ApplicationError> execute(CrearCondicionVentaCommand command);
}
```

Crear cada uno de los siguientes archivos con exactamente esta forma (paquete `com.softprimesolutions.catalogo.application.port.in`, `@FunctionalInterface`, único método `execute`), sustituyendo nombre de interfaz/command/result según la tabla:

| Archivo | Command | Result |
|---|---|---|
| `ActualizarCondicionVentaUseCase.java` | `ActualizarCondicionVentaCommand` | `CondicionVentaResult` |
| `CrearFormaFarmaceuticaUseCase.java` | `CrearFormaFarmaceuticaCommand` | `FormaFarmaceuticaResult` |
| `ActualizarFormaFarmaceuticaUseCase.java` | `ActualizarFormaFarmaceuticaCommand` | `FormaFarmaceuticaResult` |
| `CrearViaAdministracionUseCase.java` | `CrearViaAdministracionCommand` | `ViaAdministracionResult` |
| `ActualizarViaAdministracionUseCase.java` | `ActualizarViaAdministracionCommand` | `ViaAdministracionResult` |
| `CrearUnidadMedidaUseCase.java` | `CrearUnidadMedidaCommand` | `UnidadMedidaResult` |
| `ActualizarUnidadMedidaUseCase.java` | `ActualizarUnidadMedidaCommand` | `UnidadMedidaResult` |
| `CrearClasificacionControladaUseCase.java` | `CrearClasificacionControladaCommand` | `ClasificacionControladaResult` |
| `ActualizarClasificacionControladaUseCase.java` | `ActualizarClasificacionControladaCommand` | `ClasificacionControladaResult` |
| `CrearPrincipioActivoUseCase.java` | `CrearPrincipioActivoCommand` | `PrincipioActivoResult` |
| `ActualizarPrincipioActivoUseCase.java` | `ActualizarPrincipioActivoCommand` | `PrincipioActivoResult` |

Plantilla exacta (ejemplo instanciado para `ActualizarCondicionVentaUseCase.java`, repetir cambiando solo los 3 nombres de la fila correspondiente):

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarCondicionVentaCommand;
import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ActualizarCondicionVentaUseCase {
    Result<CondicionVentaResult, ApplicationError> execute(ActualizarCondicionVentaCommand command);
}
```

Ahora los puertos `in` de consulta, misma forma pero con `Query` en vez de `Command` y `List<XResult>` como tipo de éxito:

| Archivo | Query | Tipo de éxito |
|---|---|---|
| `ListarCondicionesVentaUseCase.java` | `ListarCondicionesVentaQuery` | `List<CondicionVentaResult>` |
| `ListarFormasFarmaceuticasUseCase.java` | `ListarFormasFarmaceuticasQuery` | `List<FormaFarmaceuticaResult>` |
| `ListarViasAdministracionUseCase.java` | `ListarViasAdministracionQuery` | `List<ViaAdministracionResult>` |
| `ListarUnidadesMedidaUseCase.java` | `ListarUnidadesMedidaQuery` | `List<UnidadMedidaResult>` |
| `ListarClasificacionesControladasUseCase.java` | `ListarClasificacionesControladasQuery` | `List<ClasificacionControladaResult>` |
| `ListarPrincipioActivoUseCase.java` | `ListarPrincipioActivoQuery` | `List<PrincipioActivoResult>` |

Plantilla exacta (ejemplo instanciado para `ListarCondicionesVentaUseCase.java`):

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ListarCondicionesVentaQuery;
import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;

@FunctionalInterface
public interface ListarCondicionesVentaUseCase {
    Result<List<CondicionVentaResult>, ApplicationError> execute(ListarCondicionesVentaQuery query);
}
```

- [ ] **Step 6: Crear los puertos `in` de Marca, CategoriaProducto, ProductoRegulado, SKUComercial**

Comandos (misma plantilla del Step 5, sustituyendo nombres):

| Archivo | Command | Result |
|---|---|---|
| `CrearMarcaUseCase.java` | `CrearMarcaCommand` | `MarcaResult` |
| `ActualizarMarcaUseCase.java` | `ActualizarMarcaCommand` | `MarcaResult` |
| `CrearCategoriaProductoUseCase.java` | `CrearCategoriaProductoCommand` | `CategoriaProductoResult` |
| `ActualizarCategoriaProductoUseCase.java` | `ActualizarCategoriaProductoCommand` | `CategoriaProductoResult` |
| `CrearProductoReguladoUseCase.java` | `CrearProductoReguladoCommand` | `ProductoReguladoResult` |
| `ActualizarProductoReguladoUseCase.java` | `ActualizarProductoReguladoCommand` | `ProductoReguladoResult` |
| `AsociarPrincipioActivoUseCase.java` | `AsociarPrincipioActivoCommand` | `ProductoReguladoResult` |
| `DesasociarPrincipioActivoUseCase.java` | `DesasociarPrincipioActivoCommand` | `ProductoReguladoResult` |
| `CrearSkuUseCase.java` | `CrearSkuCommand` | `SkuResult` |
| `ActualizarSkuUseCase.java` | `ActualizarSkuCommand` | `SkuResult` |
| `AgregarCodigoBarraUseCase.java` | `AgregarCodigoBarraCommand` | `SkuResult` |
| `EliminarCodigoBarraUseCase.java` | `EliminarCodigoBarraCommand` | `SkuResult` |
| `MarcarCodigoBarraPrincipalUseCase.java` | `MarcarCodigoBarraPrincipalCommand` | `SkuResult` |

Cada uno con la misma forma que la plantilla del Step 5 (ejemplo instanciado para `CrearMarcaUseCase.java`):

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.CrearMarcaCommand;
import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface CrearMarcaUseCase {
    Result<MarcaResult, ApplicationError> execute(CrearMarcaCommand command);
}
```

Consultas (misma plantilla del Step 5 con `Query`):

| Archivo | Query | Tipo de éxito |
|---|---|---|
| `ListarMarcasUseCase.java` | `ListarMarcasQuery` | `List<MarcaResult>` |
| `ListarCategoriasProductoUseCase.java` | `ListarCategoriasProductoQuery` | `List<CategoriaProductoResult>` |
| `ConsultarProductoReguladoUseCase.java` | `ConsultarProductoReguladoQuery` | `ProductoReguladoResult` (sin `List<>`) |
| `ListarProductosReguladosUseCase.java` | `ListarProductosReguladosQuery` | `PaginaResult<ProductoReguladoResumen>` (sin `List<>`) |
| `ConsultarSkuUseCase.java` | `ConsultarSkuQuery` | `SkuResult` (sin `List<>`) |
| `ListarSkusUseCase.java` | `ListarSkusQuery` | `PaginaResult<SkuResumen>` (sin `List<>`) |

Ejemplo instanciado para `ConsultarProductoReguladoUseCase.java` (tipo de éxito simple, sin `List`):

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarProductoReguladoQuery;
import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ConsultarProductoReguladoUseCase {
    Result<ProductoReguladoResult, ApplicationError> execute(ConsultarProductoReguladoQuery query);
}
```

Ejemplo instanciado para `ListarProductosReguladosUseCase.java` (tipo de éxito `PaginaResult<T>`, sin `List` externo):

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ListarProductosReguladosQuery;
import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResumen;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ListarProductosReguladosUseCase {
    Result<PaginaResult<ProductoReguladoResumen>, ApplicationError> execute(ListarProductosReguladosQuery query);
}
```

`ListarMarcasUseCase.java` y `ListarCategoriasProductoUseCase.java` siguen la plantilla `List<XResult>` del Step 5 (ejemplo `ListarCondicionesVentaUseCase.java`). `ConsultarSkuUseCase.java` sigue la plantilla de `ConsultarProductoReguladoUseCase.java`. `ListarSkusUseCase.java` sigue la plantilla de `ListarProductosReguladosUseCase.java`.

- [ ] **Step 7: Crear `CatalogoControlUseCase` (transversal, activar/desactivar las 9 entidades)**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/CatalogoControlUseCase.java`:

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
}
```

- [ ] **Step 8: Compilar el módulo**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL. Nota: los puertos `in` creados según el patrón descrito en el Step 5/6 (no repetidos literalmente en este documento) deben crearse todos como archivos reales, uno por interfaz, en `application/port/in/`, siguiendo exactamente el molde de `CrearCondicionVentaUseCase.java`.

- [ ] **Step 9: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/
git commit -m "feat(catalogo): agregar puertos de entrada y salida de aplicacion"
```

---

### Task 11: Mapper de aplicación + handlers de escritura de los 5 catálogos de soporte + PrincipioActivo

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/mapper/CatalogoApplicationMapper.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearCondicionVentaHandler.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarCondicionVentaHandler.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearFormaFarmaceuticaHandler.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarFormaFarmaceuticaHandler.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearViaAdministracionHandler.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarViaAdministracionHandler.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearUnidadMedidaHandler.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarUnidadMedidaHandler.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearClasificacionControladaHandler.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarClasificacionControladaHandler.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearPrincipioActivoHandler.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarPrincipioActivoHandler.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CrearCondicionVentaHandlerTest.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CrearPrincipioActivoHandlerTest.java`

**Interfaces:**
- Consumes: agregados de dominio (Task 3, 4), DTOs (Task 9), `CatalogoSoportePort` (Task 10).
- Produces: `CatalogoApplicationMapper.toResult(X): XResult` para las 6 entidades, y los 12 handlers listados, cada uno implementando su `UseCase` correspondiente (Task 10). Usados por Task 23 (controllers).

Este plan escribe TDD completo solo para 2 handlers representativos (`CrearCondicionVentaHandler`, `CrearPrincipioActivoHandler` — uno con PK String, otro con PK UUID) para no repetir 12 veces el mismo ciclo RED/GREEN; los 10 handlers restantes se crean directamente con su código completo (mecánicos, mismo patrón, sin fakes nuevos que inventar) y se verifican todos juntos al final de la tarea.

- [ ] **Step 1: Escribir el test que falla para `CrearCondicionVentaHandler`**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CrearCondicionVentaHandlerTest.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.command.CrearCondicionVentaCommand;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrearCondicionVentaHandlerTest {

    @Test
    void createsACondicionVentaSuccessfully() {
        var writePort = new FakeCatalogoSoportePort();
        var handler = new CrearCondicionVentaHandler(writePort);

        var result = handler.execute(new CrearCondicionVentaCommand(
                "SIN-RECETA", "Sin receta médica", false, false, null, null, null, null));

        assertTrue(result.isSuccess());
        assertEquals("SIN-RECETA", result.getOrElse(error -> null).codigo());
    }

    @Test
    void failsWithConflictWhenCodigoAlreadyExists() {
        var writePort = new FakeCatalogoSoportePort();
        writePort.outcome = CatalogoSoportePort.SaveOutcome.DUPLICATE_CODIGO;
        var handler = new CrearCondicionVentaHandler(writePort);

        var result = handler.execute(new CrearCondicionVentaCommand(
                "SIN-RECETA", "Sin receta médica", false, false, null, null, null, null));

        assertTrue(result.isFailure());
        assertEquals("CAT_CONDICION_VENTA_DUPLICADA", result.fold(value -> null, error -> error.code()));
    }

    private static final class FakeCatalogoSoportePort implements CatalogoSoportePort {
        private SaveOutcome outcome = SaveOutcome.CREATED;

        @Override
        public SaveOutcome save(CondicionVenta condicionVenta) {
            return outcome;
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

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.command.CrearCondicionVentaHandlerTest"`
Expected: FAIL — `CrearCondicionVentaHandler` no existe todavía.

- [ ] **Step 3: Crear `CatalogoApplicationMapper` (los 6 métodos `toResult` de esta tarea)**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/mapper/CatalogoApplicationMapper.java`:

```java
package com.softprimesolutions.catalogo.application.mapper;

import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;

public final class CatalogoApplicationMapper {

    private CatalogoApplicationMapper() {
    }

    public static CondicionVentaResult toResult(CondicionVenta condicionVenta) {
        return new CondicionVentaResult(
                condicionVenta.codigo(), condicionVenta.denominacion(), condicionVenta.requiereReceta(),
                condicionVenta.requiereRetencion(), condicionVenta.fuente(), condicionVenta.versionFuente(),
                condicionVenta.vigenteDesde(), condicionVenta.vigenteHasta(), condicionVenta.estado().name());
    }

    public static FormaFarmaceuticaResult toResult(FormaFarmaceutica formaFarmaceutica) {
        return new FormaFarmaceuticaResult(
                formaFarmaceutica.codigo(), formaFarmaceutica.denominacion(), formaFarmaceutica.fuente(),
                formaFarmaceutica.estado().name());
    }

    public static ViaAdministracionResult toResult(ViaAdministracion viaAdministracion) {
        return new ViaAdministracionResult(
                viaAdministracion.codigo(), viaAdministracion.denominacion(), viaAdministracion.fuente(),
                viaAdministracion.estado().name());
    }

    public static UnidadMedidaResult toResult(UnidadMedida unidadMedida) {
        return new UnidadMedidaResult(
                unidadMedida.codigo(), unidadMedida.denominacion(), unidadMedida.simbolo(),
                unidadMedida.permiteDecimal(), unidadMedida.fuente(), unidadMedida.estado().name());
    }

    public static ClasificacionControladaResult toResult(ClasificacionControlada clasificacionControlada) {
        return new ClasificacionControladaResult(
                clasificacionControlada.codigo(), clasificacionControlada.denominacion(),
                clasificacionControlada.normaFuente(), clasificacionControlada.requiereRecetaEspecial(),
                clasificacionControlada.retieneReceta(), clasificacionControlada.vigenciaRecetaDias(),
                clasificacionControlada.estado().name());
    }

    public static PrincipioActivoResult toResult(PrincipioActivo principioActivo) {
        return new PrincipioActivoResult(
                principioActivo.id().value(), principioActivo.codigoFuente(), principioActivo.denominacion(),
                principioActivo.nombreNormalizado(), principioActivo.fuente(), principioActivo.estado().name());
    }
}
```

- [ ] **Step 4: Crear `CrearCondicionVentaHandler`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearCondicionVentaHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.CrearCondicionVentaCommand;
import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.CrearCondicionVentaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearCondicionVentaHandler implements CrearCondicionVentaUseCase {

    private final CatalogoSoportePort writePort;

    public CrearCondicionVentaHandler(CatalogoSoportePort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<CondicionVentaResult, ApplicationError> execute(CrearCondicionVentaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var condicionVenta = CondicionVenta.create(
                command.codigo(), command.denominacion(), command.requiereReceta(), command.requiereRetencion(),
                command.fuente(), command.versionFuente(), command.vigenteDesde(), command.vigenteHasta());
        return condicionVenta.fold(this::persist, this::validationFailure);
    }

    private Result<CondicionVentaResult, ApplicationError> persist(CondicionVenta condicionVenta) {
        var outcome = writePort.save(condicionVenta);
        if (outcome == CatalogoSoportePort.SaveOutcome.DUPLICATE_CODIGO) {
            return Result.failure(new StandardApplicationError(
                    "CAT_CONDICION_VENTA_DUPLICADA", "Ya existe una condición de venta con el código indicado.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(CatalogoApplicationMapper.toResult(condicionVenta));
    }

    private Result<CondicionVentaResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

- [ ] **Step 5: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.command.CrearCondicionVentaHandlerTest"`
Expected: PASS

- [ ] **Step 6: Crear `ActualizarCondicionVentaHandler`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarCondicionVentaHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarCondicionVentaCommand;
import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ActualizarCondicionVentaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarCondicionVentaHandler implements ActualizarCondicionVentaUseCase {

    private final CatalogoSoportePort writePort;

    public ActualizarCondicionVentaHandler(CatalogoSoportePort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<CondicionVentaResult, ApplicationError> execute(ActualizarCondicionVentaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var condicionVenta = CondicionVenta.create(
                command.codigo(), command.denominacion(), command.requiereReceta(), command.requiereRetencion(),
                command.fuente(), command.versionFuente(), command.vigenteDesde(), command.vigenteHasta());
        return condicionVenta.fold(this::persist, this::validationFailure);
    }

    private Result<CondicionVentaResult, ApplicationError> persist(CondicionVenta condicionVenta) {
        var outcome = writePort.save(condicionVenta);
        if (outcome == CatalogoSoportePort.SaveOutcome.NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_CONDICION_VENTA_NO_ENCONTRADA", "La condición de venta indicada no existe.",
                    ErrorCategory.NOT_FOUND));
        }
        return Result.success(CatalogoApplicationMapper.toResult(condicionVenta));
    }

    private Result<CondicionVentaResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

- [ ] **Step 7: Crear los 8 handlers restantes de los catálogos de soporte (misma forma exacta que Step 4/6, cambiando el tipo de agregado y el código de error)**

Cada handler sigue exactamente la forma de `CrearCondicionVentaHandler`/`ActualizarCondicionVentaHandler`, cambiando: el nombre de la clase, la interfaz `UseCase` implementada, el tipo de comando, el tipo de dominio (`FormaFarmaceutica`/`ViaAdministracion`/`UnidadMedida`/`ClasificacionControlada`), la llamada al factory `create(...)` con los campos propios de cada entidad (ver Task 3 para las firmas exactas), y el código de error `CAT_<ENTIDAD>_DUPLICADA`/`CAT_<ENTIDAD>_NO_ENCONTRADA`.

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearFormaFarmaceuticaHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.CrearFormaFarmaceuticaCommand;
import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.CrearFormaFarmaceuticaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearFormaFarmaceuticaHandler implements CrearFormaFarmaceuticaUseCase {

    private final CatalogoSoportePort writePort;

    public CrearFormaFarmaceuticaHandler(CatalogoSoportePort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<FormaFarmaceuticaResult, ApplicationError> execute(CrearFormaFarmaceuticaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var forma = FormaFarmaceutica.create(command.codigo(), command.denominacion(), command.fuente());
        return forma.fold(this::persist, this::validationFailure);
    }

    private Result<FormaFarmaceuticaResult, ApplicationError> persist(FormaFarmaceutica forma) {
        var outcome = writePort.save(forma);
        if (outcome == CatalogoSoportePort.SaveOutcome.DUPLICATE_CODIGO) {
            return Result.failure(new StandardApplicationError(
                    "CAT_FORMA_FARMACEUTICA_DUPLICADA", "Ya existe una forma farmacéutica con el código indicado.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(CatalogoApplicationMapper.toResult(forma));
    }

    private Result<FormaFarmaceuticaResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarFormaFarmaceuticaHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarFormaFarmaceuticaCommand;
import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ActualizarFormaFarmaceuticaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarFormaFarmaceuticaHandler implements ActualizarFormaFarmaceuticaUseCase {

    private final CatalogoSoportePort writePort;

    public ActualizarFormaFarmaceuticaHandler(CatalogoSoportePort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<FormaFarmaceuticaResult, ApplicationError> execute(ActualizarFormaFarmaceuticaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var forma = FormaFarmaceutica.create(command.codigo(), command.denominacion(), command.fuente());
        return forma.fold(this::persist, this::validationFailure);
    }

    private Result<FormaFarmaceuticaResult, ApplicationError> persist(FormaFarmaceutica forma) {
        var outcome = writePort.save(forma);
        if (outcome == CatalogoSoportePort.SaveOutcome.NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_FORMA_FARMACEUTICA_NO_ENCONTRADA", "La forma farmacéutica indicada no existe.",
                    ErrorCategory.NOT_FOUND));
        }
        return Result.success(CatalogoApplicationMapper.toResult(forma));
    }

    private Result<FormaFarmaceuticaResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearViaAdministracionHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.CrearViaAdministracionCommand;
import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.CrearViaAdministracionUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearViaAdministracionHandler implements CrearViaAdministracionUseCase {

    private final CatalogoSoportePort writePort;

    public CrearViaAdministracionHandler(CatalogoSoportePort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<ViaAdministracionResult, ApplicationError> execute(CrearViaAdministracionCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var via = ViaAdministracion.create(command.codigo(), command.denominacion(), command.fuente());
        return via.fold(this::persist, this::validationFailure);
    }

    private Result<ViaAdministracionResult, ApplicationError> persist(ViaAdministracion via) {
        var outcome = writePort.save(via);
        if (outcome == CatalogoSoportePort.SaveOutcome.DUPLICATE_CODIGO) {
            return Result.failure(new StandardApplicationError(
                    "CAT_VIA_ADMINISTRACION_DUPLICADA", "Ya existe una vía de administración con el código indicado.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(CatalogoApplicationMapper.toResult(via));
    }

    private Result<ViaAdministracionResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarViaAdministracionHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarViaAdministracionCommand;
import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ActualizarViaAdministracionUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarViaAdministracionHandler implements ActualizarViaAdministracionUseCase {

    private final CatalogoSoportePort writePort;

    public ActualizarViaAdministracionHandler(CatalogoSoportePort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<ViaAdministracionResult, ApplicationError> execute(ActualizarViaAdministracionCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var via = ViaAdministracion.create(command.codigo(), command.denominacion(), command.fuente());
        return via.fold(this::persist, this::validationFailure);
    }

    private Result<ViaAdministracionResult, ApplicationError> persist(ViaAdministracion via) {
        var outcome = writePort.save(via);
        if (outcome == CatalogoSoportePort.SaveOutcome.NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_VIA_ADMINISTRACION_NO_ENCONTRADA", "La vía de administración indicada no existe.",
                    ErrorCategory.NOT_FOUND));
        }
        return Result.success(CatalogoApplicationMapper.toResult(via));
    }

    private Result<ViaAdministracionResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearUnidadMedidaHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.CrearUnidadMedidaCommand;
import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.CrearUnidadMedidaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearUnidadMedidaHandler implements CrearUnidadMedidaUseCase {

    private final CatalogoSoportePort writePort;

    public CrearUnidadMedidaHandler(CatalogoSoportePort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<UnidadMedidaResult, ApplicationError> execute(CrearUnidadMedidaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var unidad = UnidadMedida.create(
                command.codigo(), command.denominacion(), command.simbolo(), command.permiteDecimal(),
                command.fuente());
        return unidad.fold(this::persist, this::validationFailure);
    }

    private Result<UnidadMedidaResult, ApplicationError> persist(UnidadMedida unidad) {
        var outcome = writePort.save(unidad);
        if (outcome == CatalogoSoportePort.SaveOutcome.DUPLICATE_CODIGO) {
            return Result.failure(new StandardApplicationError(
                    "CAT_UNIDAD_MEDIDA_DUPLICADA", "Ya existe una unidad de medida con el código indicado.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(CatalogoApplicationMapper.toResult(unidad));
    }

    private Result<UnidadMedidaResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarUnidadMedidaHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarUnidadMedidaCommand;
import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ActualizarUnidadMedidaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarUnidadMedidaHandler implements ActualizarUnidadMedidaUseCase {

    private final CatalogoSoportePort writePort;

    public ActualizarUnidadMedidaHandler(CatalogoSoportePort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<UnidadMedidaResult, ApplicationError> execute(ActualizarUnidadMedidaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var unidad = UnidadMedida.create(
                command.codigo(), command.denominacion(), command.simbolo(), command.permiteDecimal(),
                command.fuente());
        return unidad.fold(this::persist, this::validationFailure);
    }

    private Result<UnidadMedidaResult, ApplicationError> persist(UnidadMedida unidad) {
        var outcome = writePort.save(unidad);
        if (outcome == CatalogoSoportePort.SaveOutcome.NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_UNIDAD_MEDIDA_NO_ENCONTRADA", "La unidad de medida indicada no existe.",
                    ErrorCategory.NOT_FOUND));
        }
        return Result.success(CatalogoApplicationMapper.toResult(unidad));
    }

    private Result<UnidadMedidaResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearClasificacionControladaHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.CrearClasificacionControladaCommand;
import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.CrearClasificacionControladaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearClasificacionControladaHandler implements CrearClasificacionControladaUseCase {

    private final CatalogoSoportePort writePort;

    public CrearClasificacionControladaHandler(CatalogoSoportePort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<ClasificacionControladaResult, ApplicationError> execute(
            CrearClasificacionControladaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var clasificacion = ClasificacionControlada.create(
                command.codigo(), command.denominacion(), command.normaFuente(),
                command.requiereRecetaEspecial(), command.retieneReceta(), command.vigenciaRecetaDias());
        return clasificacion.fold(this::persist, this::validationFailure);
    }

    private Result<ClasificacionControladaResult, ApplicationError> persist(ClasificacionControlada clasificacion) {
        var outcome = writePort.save(clasificacion);
        if (outcome == CatalogoSoportePort.SaveOutcome.DUPLICATE_CODIGO) {
            return Result.failure(new StandardApplicationError(
                    "CAT_CLASIFICACION_CONTROLADA_DUPLICADA",
                    "Ya existe una clasificación controlada con el código indicado.", ErrorCategory.CONFLICT));
        }
        return Result.success(CatalogoApplicationMapper.toResult(clasificacion));
    }

    private Result<ClasificacionControladaResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarClasificacionControladaHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarClasificacionControladaCommand;
import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ActualizarClasificacionControladaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarClasificacionControladaHandler implements ActualizarClasificacionControladaUseCase {

    private final CatalogoSoportePort writePort;

    public ActualizarClasificacionControladaHandler(CatalogoSoportePort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<ClasificacionControladaResult, ApplicationError> execute(
            ActualizarClasificacionControladaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var clasificacion = ClasificacionControlada.create(
                command.codigo(), command.denominacion(), command.normaFuente(),
                command.requiereRecetaEspecial(), command.retieneReceta(), command.vigenciaRecetaDias());
        return clasificacion.fold(this::persist, this::validationFailure);
    }

    private Result<ClasificacionControladaResult, ApplicationError> persist(ClasificacionControlada clasificacion) {
        var outcome = writePort.save(clasificacion);
        if (outcome == CatalogoSoportePort.SaveOutcome.NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_CLASIFICACION_CONTROLADA_NO_ENCONTRADA",
                    "La clasificación controlada indicada no existe.", ErrorCategory.NOT_FOUND));
        }
        return Result.success(CatalogoApplicationMapper.toResult(clasificacion));
    }

    private Result<ClasificacionControladaResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

- [ ] **Step 8: Ejecutar y verificar que los 5 handlers de `CrearX`/`ActualizarX` de soporte compilan y no rompen nada**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL (los handlers de `PrincipioActivo`, `Marca`, `CategoriaProducto`, `ProductoRegulado`, `SKUComercial` referenciados por sus `UseCase` en `CatalogoModuleConfiguration` todavía no existen — eso es esperado hasta Tasks 12-14; en este punto solo se compila `:modules:catalogo` de forma aislada, sin wiring de Spring, así que no hay error).

- [ ] **Step 9: Escribir el test que falla para `CrearPrincipioActivoHandler`**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CrearPrincipioActivoHandlerTest.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.command.CrearPrincipioActivoCommand;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrearPrincipioActivoHandlerTest {

    @Test
    void createsAPrincipioActivoSuccessfully() {
        var writePort = new FakeCatalogoSoportePort();
        var handler = new CrearPrincipioActivoHandler(writePort, () -> UUID.fromString(
                "98a1587e-27ef-4077-befd-6f5af4901589"));

        var result = handler.execute(new CrearPrincipioActivoCommand(null, "Paracetamol", null, null));

        assertTrue(result.isSuccess());
        assertEquals("Paracetamol", result.getOrElse(error -> null).denominacion());
    }

    private static final class FakeCatalogoSoportePort implements CatalogoSoportePort {
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
        public SavePrincipioActivoOutcome save(PrincipioActivo principioActivo) {
            return SavePrincipioActivoOutcome.CREATED;
        }

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
        public boolean changeCondicionVentaStatus(String codigo, String status, Instant changedAt) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeFormaFarmaceuticaStatus(String codigo, String status, Instant changedAt) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeViaAdministracionStatus(String codigo, String status, Instant changedAt) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeUnidadMedidaStatus(String codigo, String status, Instant changedAt) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeClasificacionControladaStatus(String codigo, String status, Instant changedAt) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changePrincipioActivoStatus(UUID principioActivoId, String status, Instant changedAt) { throw new UnsupportedOperationException(); }
    }
}
```

- [ ] **Step 10: Ejecutar y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.command.CrearPrincipioActivoHandlerTest"`
Expected: FAIL — `CrearPrincipioActivoHandler` no existe todavía.

- [ ] **Step 11: Crear `CrearPrincipioActivoHandler`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearPrincipioActivoHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.CrearPrincipioActivoCommand;
import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.CrearPrincipioActivoUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.valueobject.PrincipioActivoId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearPrincipioActivoHandler implements CrearPrincipioActivoUseCase {

    private final CatalogoSoportePort writePort;
    private final IdentifierGenerator identifierGenerator;

    public CrearPrincipioActivoHandler(CatalogoSoportePort writePort, IdentifierGenerator identifierGenerator) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.identifierGenerator = Objects.requireNonNull(identifierGenerator, "identifierGenerator es obligatorio");
    }

    @Override
    public Result<PrincipioActivoResult, ApplicationError> execute(CrearPrincipioActivoCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var principioActivo = PrincipioActivo.create(
                new PrincipioActivoId(identifierGenerator.next()), command.codigoFuente(), command.denominacion(),
                command.nombreNormalizado(), command.fuente());
        return principioActivo.fold(this::persist, this::validationFailure);
    }

    private Result<PrincipioActivoResult, ApplicationError> persist(PrincipioActivo principioActivo) {
        writePort.save(principioActivo);
        return Result.success(CatalogoApplicationMapper.toResult(principioActivo));
    }

    private Result<PrincipioActivoResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

- [ ] **Step 12: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.command.CrearPrincipioActivoHandlerTest"`
Expected: PASS

- [ ] **Step 13: Crear `ActualizarPrincipioActivoHandler`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarPrincipioActivoHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarPrincipioActivoCommand;
import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ActualizarPrincipioActivoUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.valueobject.PrincipioActivoId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarPrincipioActivoHandler implements ActualizarPrincipioActivoUseCase {

    private final CatalogoSoportePort writePort;

    public ActualizarPrincipioActivoHandler(CatalogoSoportePort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<PrincipioActivoResult, ApplicationError> execute(ActualizarPrincipioActivoCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var principioActivo = PrincipioActivo.create(
                new PrincipioActivoId(command.principioActivoId()), command.codigoFuente(), command.denominacion(),
                command.nombreNormalizado(), command.fuente());
        return principioActivo.fold(this::persist, this::validationFailure);
    }

    private Result<PrincipioActivoResult, ApplicationError> persist(PrincipioActivo principioActivo) {
        var outcome = writePort.save(principioActivo);
        if (outcome == CatalogoSoportePort.SavePrincipioActivoOutcome.NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_PRINCIPIO_ACTIVO_NO_ENCONTRADO", "El principio activo indicado no existe.",
                    ErrorCategory.NOT_FOUND));
        }
        return Result.success(CatalogoApplicationMapper.toResult(principioActivo));
    }

    private Result<PrincipioActivoResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

- [ ] **Step 14: Ejecutar todos los tests de la tarea juntos**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.command.Crear*HandlerTest"`
Expected: PASS

- [ ] **Step 15: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/mapper/ service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearCondicionVentaHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarCondicionVentaHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearFormaFarmaceuticaHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarFormaFarmaceuticaHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearViaAdministracionHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarViaAdministracionHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearUnidadMedidaHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarUnidadMedidaHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearClasificacionControladaHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarClasificacionControladaHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearPrincipioActivoHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarPrincipioActivoHandler.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CrearCondicionVentaHandlerTest.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CrearPrincipioActivoHandlerTest.java
git commit -m "feat(catalogo): agregar handlers de escritura de catalogos de soporte y principio activo"
```

---
