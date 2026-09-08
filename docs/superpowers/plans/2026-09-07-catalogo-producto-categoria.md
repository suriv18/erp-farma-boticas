# Módulo Catálogo: Producto + Categoría — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar el módulo `catalogo` de `service-botica` con dos agregados (`Categoria`, `Producto`), CRUD completo, persistencia real (JPA escritura + JDBC lectura), migración Flyway, y API REST con autorización — sentando la base para que el módulo Inventario (trabajo futuro) pueda referenciar productos reales en vez de datos hardcodeados.

**Architecture:** Clean Architecture + DDD + Ports & Adapters + CQRS dentro de `service-botica/modules/catalogo/`. `domain/` y `api/` (commands/queries) + `application/` (handlers) siguen el estilo CQRS-puro de `modules/organizacion`. `infrastructure/persistence/{read,write}` y `api/controller` siguen el estilo maduro de `modules/security`. Multi-tenant vía `tenantId` (UUID público de `sch_farmacia.tenant`) resuelto del claim `tid` del JWT en cada controller, igual que `LocalAuthController`/`LocalAuthJdbcAdapter`.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Data JPA, Spring JDBC (`JdbcClient`), PostgreSQL 18 + Flyway, JUnit 5, MockMvc, Spring Modulith 2.1.

## Global Constraints

- Backend: `cd service-botica && .\gradlew.bat check --warning-mode all` debe pasar (build + tests + ArchUnit + Spring Modulith verify) antes de dar por terminado el trabajo.
- El módulo `catalogo` ya existe como scaffold (`build.gradle`, `package-info.java` con `@ApplicationModule(id="catalogo", allowedDependencies={"organizacion::api"})`, `api/package-info.java` con `@NamedInterface("api")`), y ya está registrado en `settings.gradle` y referenciado en `bootstrap-app/build.gradle` — no crear estos archivos desde cero, solo verificarlos y añadir el resto.
- Migraciones nuevas van en `service-botica/bootstrap-app/src/main/resources/db/migration/`, numeradas después de la última existente (verificar el número libre en la Task 1 — al momento de escribir este plan la última es V021).
- El perfil de test de integración real (Testcontainers+Postgres) usa `@ActiveProfiles("test")` + `@Import(PostgresTestContainerConfiguration.class)` + `@SpringBootTest` (+ `@AutoConfigureMockMvc` para tests HTTP) — NO un perfil llamado "integration". Copiar exactamente las anotaciones de clase de `IamApiIntegrationTest.java`.
- Los agregados de dominio son inmutables: factories estáticas devuelven `Result<T, ErrorDetail>`; transiciones de estado (`activate()`/`deactivate()`) devuelven una nueva instancia, nunca mutan la existente.
- Los commands/queries llevan `tenantId: UUID` (el UUID público de `sch_farmacia.tenant`, viene del claim `tid` del JWT) explícito como campo — no existe un contexto de actor autenticado compartido entre módulos.
- Sin comentarios explicativos en el código salvo invariantes no obvias; Result pattern para errores esperables; nombres en español para el dominio de negocio (Categoria, Producto, TipoProducto...), inglés para tipos técnicos ya establecidos por el framework.
- No modificar `docs/cadena-farmacias-docs/database/migrations/*.sql` (registro histórico).

---

## Fase 1 — Migración de base de datos

### Task 1: Migración Flyway que crea `sch_catalogo.categoria` y `sch_catalogo.producto`

**Files:**
- Create: `service-botica/bootstrap-app/src/main/resources/db/migration/V022__catalogo_producto_categoria.sql`
- Test: `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/db/MigrationV022Test.java`

**Interfaces:**
- Produces: tablas `sch_catalogo.categoria`, `sch_catalogo.producto`; permisos `catalogo.categorias.gestionar`, `catalogo.categorias.consultar`, `catalogo.productos.gestionar`, `catalogo.productos.consultar` en `sch_seguridad.permiso`. Usadas por todas las tareas siguientes.

- [ ] **Step 1: Verificar el número de migración siguiente disponible**

Run: `ls service-botica/bootstrap-app/src/main/resources/db/migration/`
Expected: la migración más alta hoy es `V021__separar_identidad_membership.sql`. Usar `V022` como siguiente número. Si al ejecutar este paso ya existe un `V022` (por trabajo posterior no reflejado en este plan), usar el siguiente número libre y ajustar el nombre de archivo en los pasos restantes de esta tarea.

- [ ] **Step 2: Ver cómo `V018__seed_security_administration_permissions.sql` siembra permisos, para replicar el mismo patrón**

Run: `cat service-botica/bootstrap-app/src/main/resources/db/migration/V018__seed_security_administration_permissions.sql`
Anotar: el nombre exacto de la columna `modulo_id` en `sch_seguridad.permiso`, cómo se referencia `sch_seguridad.modulo_sistema`, y si module `SEGURIDAD` se inserta o ya existe. Este plan asume que hace falta insertar un módulo `CATALOGO` nuevo en `sch_seguridad.modulo_sistema` antes de poder insertar sus permisos (por la FK `modulo_id`).

- [ ] **Step 3: Escribir el test de integración que falla — verifica que las tablas y permisos nuevos existen**

Crear `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/db/MigrationV022Test.java`. Antes de escribir el cuerpo, copiar las anotaciones de clase exactas desde `service-botica/bootstrap-app/src/test/java/com/softprimesolutions/security/db/MigrationV021Test.java`:

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
    void createsCategoriaAndProductoTablesWithExpectedConstraints() {
        var jdbc = JdbcClient.create(dataSource);

        var categoriaExists = jdbc.sql("""
                        SELECT COUNT(*) FROM information_schema.tables
                         WHERE table_schema = 'sch_catalogo' AND table_name = 'categoria'
                        """)
                .query(Long.class).single();
        assertThat(categoriaExists).isEqualTo(1L);

        var productoExists = jdbc.sql("""
                        SELECT COUNT(*) FROM information_schema.tables
                         WHERE table_schema = 'sch_catalogo' AND table_name = 'producto'
                        """)
                .query(Long.class).single();
        assertThat(productoExists).isEqualTo(1L);

        var categoriaUniqueNombre = jdbc.sql("""
                        SELECT COUNT(*) FROM pg_constraint
                         WHERE conname = 'uk_cat_categoria_tenant_nombre'
                        """)
                .query(Long.class).single();
        assertThat(categoriaUniqueNombre).isEqualTo(1L);

        var productoFkCategoria = jdbc.sql("""
                        SELECT COUNT(*) FROM pg_constraint
                         WHERE conname = 'fk_cat_producto_categoria'
                        """)
                .query(Long.class).single();
        assertThat(productoFkCategoria).isEqualTo(1L);

        var productoMedicamentoCheck = jdbc.sql("""
                        SELECT COUNT(*) FROM pg_constraint
                         WHERE conname = 'ck_cat_producto_medicamento_requiere_datos'
                        """)
                .query(Long.class).single();
        assertThat(productoMedicamentoCheck).isEqualTo(1L);

        var permisosNuevos = jdbc.sql("""
                        SELECT COUNT(*) FROM sch_seguridad.permiso
                         WHERE codigo IN ('catalogo.categorias.gestionar', 'catalogo.categorias.consultar',
                                           'catalogo.productos.gestionar', 'catalogo.productos.consultar')
                        """)
                .query(Long.class).single();
        assertThat(permisosNuevos).isEqualTo(4L);
    }
}
```

Ajustar las anotaciones de clase (`@SpringBootTest`, `@ActiveProfiles`, y cualquier `@Import`/`@Transactional` que el Step 2 haya revelado en `MigrationV021Test.java`) para que coincidan exactamente antes de continuar.

- [ ] **Step 4: Ejecutar y verificar que el test falla**

Run: `cd service-botica && .\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.catalogo.db.MigrationV022Test"`
Expected: FAIL — las tablas `sch_catalogo.categoria`/`producto` no existen todavía.

- [ ] **Step 5: Escribir la migración `V022__catalogo_producto_categoria.sql`**

Crear `service-botica/bootstrap-app/src/main/resources/db/migration/V022__catalogo_producto_categoria.sql`:

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

INSERT INTO sch_seguridad.modulo_sistema (codigo, nombre, orden)
VALUES ('CATALOGO', 'Catálogo', 20)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO sch_seguridad.permiso (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
SELECT id, 'catalogo.categorias.gestionar', 'CATEGORIA', 'GESTIONAR',
       'Gestionar categorías', 'Crear, actualizar y cambiar el estado de categorías de catálogo.', FALSE, 'ACTIVO'
  FROM sch_seguridad.modulo_sistema WHERE codigo = 'CATALOGO';

INSERT INTO sch_seguridad.permiso (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
SELECT id, 'catalogo.categorias.consultar', 'CATEGORIA', 'CONSULTAR',
       'Consultar categorías', 'Listar categorías de catálogo.', FALSE, 'ACTIVO'
  FROM sch_seguridad.modulo_sistema WHERE codigo = 'CATALOGO';

INSERT INTO sch_seguridad.permiso (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
SELECT id, 'catalogo.productos.gestionar', 'PRODUCTO', 'GESTIONAR',
       'Gestionar productos', 'Crear, actualizar y cambiar el estado de productos de catálogo.', FALSE, 'ACTIVO'
  FROM sch_seguridad.modulo_sistema WHERE codigo = 'CATALOGO';

INSERT INTO sch_seguridad.permiso (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
SELECT id, 'catalogo.productos.consultar', 'PRODUCTO', 'CONSULTAR',
       'Consultar productos', 'Consultar y listar productos de catálogo.', FALSE, 'ACTIVO'
  FROM sch_seguridad.modulo_sistema WHERE codigo = 'CATALOGO';
```

Antes de dar este paso por completo, revisar la salida real del Step 2: si el nombre de columnas de `sch_seguridad.permiso`/`modulo_sistema` difiere de lo asumido aquí (`modulo_id`, `codigo`, `recurso`, `accion`, `nombre`, `descripcion`, `es_critico`, `estado`), ajustar el SQL de arriba para que coincida exactamente con `V018`.

- [ ] **Step 6: Ejecutar y verificar que el test pasa**

Run: `cd service-botica && .\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.catalogo.db.MigrationV022Test"`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add service-botica/bootstrap-app/src/main/resources/db/migration/V022__catalogo_producto_categoria.sql service-botica/bootstrap-app/src/test/java/com/softprimesolutions/catalogo/db/MigrationV022Test.java
git commit -m "feat(catalogo): migrar esquema sch_catalogo con categoria y producto"
```

---

## Fase 2 — Dominio

### Task 2: Value Objects e IDs (`TenantId`, `CategoriaId`, `ProductoId`)

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/valueobject/TenantId.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/valueobject/CategoriaId.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/valueobject/ProductoId.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/valueobject/ValueObjectsTest.java`

**Interfaces:**
- Produces: `TenantId(UUID value)`, `CategoriaId(UUID value)`, `ProductoId(UUID value)` — records inmutables que rechazan `null`. Usados por Task 3 (agregados) y todas las tareas posteriores.

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
        assertEquals(uuid, new CategoriaId(uuid).value());
        assertEquals(uuid, new ProductoId(uuid).value());
    }

    @Test
    void rejectsANullUuid() {
        assertThrows(NullPointerException.class, () -> new TenantId(null));
        assertThrows(NullPointerException.class, () -> new CategoriaId(null));
        assertThrows(NullPointerException.class, () -> new ProductoId(null));
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.valueobject.ValueObjectsTest"`
Expected: FAIL — `TenantId`, `CategoriaId`, `ProductoId` no existen todavía (error de compilación).

- [ ] **Step 3: Crear los tres Value Objects**

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

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/valueobject/CategoriaId.java`:

```java
package com.softprimesolutions.catalogo.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record CategoriaId(UUID value) {

    public CategoriaId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/valueobject/ProductoId.java`:

```java
package com.softprimesolutions.catalogo.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record ProductoId(UUID value) {

    public ProductoId {
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
git commit -m "feat(catalogo): agregar value objects TenantId, CategoriaId, ProductoId"
```

---

### Task 3: Enums de dominio (`EstadoCategoria`, `EstadoProducto`, `TipoProducto`, `CondicionVenta`)

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoCategoria.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoProducto.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/TipoProducto.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/CondicionVenta.java`

**Interfaces:**
- Produces: `EstadoCategoria{ACTIVA,INACTIVA}`, `EstadoProducto{ACTIVO,INACTIVO}`, `TipoProducto{MEDICAMENTO,DISPOSITIVO_MEDICO,PRODUCTO_SANITARIO,SUPLEMENTO_ALIMENTO,ARTICULO_NO_SANITARIO}`, `CondicionVenta{SIN_RECETA,CON_RECETA,RECETA_RETENIDA}`. Usados por Task 4 (agregados) y en adelante.

No requiere test dedicado (enums puros sin lógica) — se validan indirectamente vía Task 4.

- [ ] **Step 1: Crear los cuatro enums**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoCategoria.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

public enum EstadoCategoria {
    ACTIVA,
    INACTIVA
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoProducto.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

public enum EstadoProducto {
    ACTIVO,
    INACTIVO
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/TipoProducto.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

public enum TipoProducto {
    MEDICAMENTO,
    DISPOSITIVO_MEDICO,
    PRODUCTO_SANITARIO,
    SUPLEMENTO_ALIMENTO,
    ARTICULO_NO_SANITARIO
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/CondicionVenta.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

public enum CondicionVenta {
    SIN_RECETA,
    CON_RECETA,
    RECETA_RETENIDA
}
```

- [ ] **Step 2: Compilar el módulo**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoCategoria.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/EstadoProducto.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/TipoProducto.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/CondicionVenta.java
git commit -m "feat(catalogo): agregar enums de dominio EstadoCategoria, EstadoProducto, TipoProducto, CondicionVenta"
```

---

### Task 4: Agregado `Categoria`

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/Categoria.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/CategoriaTest.java`

**Interfaces:**
- Consumes: `CategoriaId`, `TenantId` (Task 2), `EstadoCategoria` (Task 3), `Result`, `ErrorDetail`, `AggregateRoot` (shared-kernel).
- Produces: `Categoria.create(CategoriaId id, TenantId tenantId, String nombre, String descripcion, Instant createdAt): Result<Categoria, ErrorDetail>`, `Categoria.restore(CategoriaId, TenantId, String nombre, String descripcion, EstadoCategoria, Instant createdAt, Instant updatedAt): Categoria`, getters `id()`, `tenantId()`, `nombre()`, `descripcion()`, `estado()`, `createdAt()`, `updatedAt()`. Usado por Task 8 (`CrearCategoriaHandler`) y Task 10 (mapper JPA).

- [ ] **Step 1: Escribir el test que falla**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/CategoriaTest.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.domain.valueobject.CategoriaId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CategoriaTest {

    @Test
    void createsAndNormalizesAValidCategoria() {
        var result = Categoria.create(
                new CategoriaId(UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589")),
                new TenantId(UUID.fromString("a92adf67-70e7-4cc1-bb05-ff074df7fdf5")),
                "  Analgésicos  ",
                "  Medicamentos para el dolor  ",
                Instant.parse("2026-09-07T10:00:00Z"));

        assertTrue(result.isSuccess());
        var categoria = result.getOrElse(error -> null);
        assertEquals("Analgésicos", categoria.nombre());
        assertEquals("Medicamentos para el dolor", categoria.descripcion());
        assertEquals(EstadoCategoria.ACTIVA, categoria.estado());
    }

    @Test
    void rejectsANameThatIsTooShort() {
        var result = Categoria.create(
                new CategoriaId(UUID.randomUUID()),
                new TenantId(UUID.randomUUID()),
                "A",
                null,
                Instant.parse("2026-09-07T10:00:00Z"));

        assertTrue(result.isFailure());
        assertEquals("CAT_CATEGORIA_INVALIDA", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void requiresIdTenantAndCreatedAt() {
        var missingId = Categoria.create(
                null, new TenantId(UUID.randomUUID()), "Analgésicos", null,
                Instant.parse("2026-09-07T10:00:00Z"));
        assertTrue(missingId.isFailure());

        var missingTenant = Categoria.create(
                new CategoriaId(UUID.randomUUID()), null, "Analgésicos", null,
                Instant.parse("2026-09-07T10:00:00Z"));
        assertTrue(missingTenant.isFailure());

        var missingCreatedAt = Categoria.create(
                new CategoriaId(UUID.randomUUID()), new TenantId(UUID.randomUUID()),
                "Analgésicos", null, null);
        assertTrue(missingCreatedAt.isFailure());
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.CategoriaTest"`
Expected: FAIL — `Categoria` no existe todavía (error de compilación).

- [ ] **Step 3: Crear `Categoria`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/Categoria.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

import com.softprimesolutions.catalogo.domain.valueobject.CategoriaId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Instant;
import java.util.Map;

/** Agrupación temática de productos dentro del catálogo de un tenant. */
public final class Categoria extends AggregateRoot {

    private static final int NAME_MIN_LENGTH = 2;
    private static final int NAME_MAX_LENGTH = 100;
    private static final int DESCRIPTION_MAX_LENGTH = 500;

    private final CategoriaId id;
    private final TenantId tenantId;
    private final String nombre;
    private final String descripcion;
    private final EstadoCategoria estado;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Categoria(
            CategoriaId id,
            TenantId tenantId,
            String nombre,
            String descripcion,
            EstadoCategoria estado,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.estado = estado;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Result<Categoria, ErrorDetail> create(
            CategoriaId id, TenantId tenantId, String nombre, String descripcion, Instant createdAt) {
        if (id == null) return invalid("id", "La identidad de la categoría es obligatoria.");
        if (tenantId == null) return invalid("tenantId", "El tenant es obligatorio.");
        if (createdAt == null) return invalid("createdAt", "El instante de registro es obligatorio.");

        var normalizedName = normalizeSpaces(nombre);
        if (normalizedName == null || normalizedName.length() < NAME_MIN_LENGTH
                || normalizedName.length() > NAME_MAX_LENGTH) {
            return invalid("nombre", "El nombre debe tener entre 2 y 100 caracteres.");
        }

        var normalizedDescription = normalizeNullable(descripcion);
        if (normalizedDescription != null && normalizedDescription.length() > DESCRIPTION_MAX_LENGTH) {
            return invalid("descripcion", "La descripción no debe exceder 500 caracteres.");
        }

        return Result.success(new Categoria(
                id, tenantId, normalizedName, normalizedDescription, EstadoCategoria.ACTIVA, createdAt, null));
    }

    public static Categoria restore(
            CategoriaId id,
            TenantId tenantId,
            String nombre,
            String descripcion,
            EstadoCategoria estado,
            Instant createdAt,
            Instant updatedAt) {
        return new Categoria(id, tenantId, nombre, descripcion, estado, createdAt, updatedAt);
    }

    private static Result<Categoria, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("CAT_CATEGORIA_INVALIDA", message, Map.of("field", field)));
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

    public CategoriaId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public String nombre() { return nombre; }
    public String descripcion() { return descripcion; }
    public EstadoCategoria estado() { return estado; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.CategoriaTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/Categoria.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/CategoriaTest.java
git commit -m "feat(catalogo): agregar agregado de dominio Categoria"
```

---

### Task 5: Agregado `Producto`

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/Producto.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/ProductoTest.java`

**Interfaces:**
- Consumes: `ProductoId`, `TenantId`, `CategoriaId` (Task 2), `EstadoProducto`, `TipoProducto`, `CondicionVenta` (Task 3).
- Produces: `Producto.create(ProductoId id, TenantId tenantId, CategoriaId categoriaId, String nombre, TipoProducto tipo, String laboratorio, String unidadMedida, String presentacion, int unidadesPorPaquete, String codigoBarras, BigDecimal precioVenta, CondicionVenta condicionVenta, boolean esGenerico, boolean esGenericoEsencial, String grupoTerapeutico, String codigoDigemid, String principioActivo, String concentracion, boolean requiereLote, boolean requiereVencimiento, Instant createdAt): Result<Producto, ErrorDetail>`, `Producto.restore(...)` (mismos campos + `EstadoProducto` + `updatedAt`), getters para todos los campos. Usado por Task 9 (`CrearProductoHandler`) y Task 11 (mapper JPA).

- [ ] **Step 1: Escribir el test que falla**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/ProductoTest.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.domain.valueobject.CategoriaId;
import com.softprimesolutions.catalogo.domain.valueobject.ProductoId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductoTest {

    private static final ProductoId PRODUCTO_ID = new ProductoId(
            UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589"));
    private static final TenantId TENANT_ID = new TenantId(
            UUID.fromString("a92adf67-70e7-4cc1-bb05-ff074df7fdf5"));
    private static final CategoriaId CATEGORIA_ID = new CategoriaId(
            UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final Instant CREATED_AT = Instant.parse("2026-09-07T10:00:00Z");

    @Test
    void createsANonMedicamentoProductWithoutRegulatoryFields() {
        var result = Producto.create(
                PRODUCTO_ID, TENANT_ID, CATEGORIA_ID, "  Alcohol en gel 250ml  ",
                TipoProducto.PRODUCTO_SANITARIO, null, "Frasco", null, 1, null,
                new BigDecimal("12.50"), null, false, false, null, null, null, null,
                false, false, CREATED_AT);

        assertTrue(result.isSuccess());
        var producto = result.getOrElse(error -> null);
        assertEquals("Alcohol en gel 250ml", producto.nombre());
        assertEquals(EstadoProducto.ACTIVO, producto.estado());
        assertEquals(new BigDecimal("12.50"), producto.precioVenta());
    }

    @Test
    void requiresActiveIngredientAndSaleConditionForMedicamento() {
        var result = Producto.create(
                PRODUCTO_ID, TENANT_ID, CATEGORIA_ID, "Paracetamol 500mg",
                TipoProducto.MEDICAMENTO, "Laboratorio X", "Tableta", "Caja x 10", 10, null,
                new BigDecimal("5.00"), null, false, false, null, null, null, null,
                false, false, CREATED_AT);

        assertTrue(result.isFailure());
        assertEquals("CAT_PRODUCTO_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void succeedsForMedicamentoWithActiveIngredientAndSaleCondition() {
        var result = Producto.create(
                PRODUCTO_ID, TENANT_ID, CATEGORIA_ID, "Paracetamol 500mg",
                TipoProducto.MEDICAMENTO, "Laboratorio X", "Tableta", "Caja x 10", 10, null,
                new BigDecimal("5.00"), CondicionVenta.SIN_RECETA, true, true, "Analgésicos",
                "DIG-001", "Paracetamol", "500mg", true, true, CREATED_AT);

        assertTrue(result.isSuccess());
        var producto = result.getOrElse(error -> null);
        assertEquals(CondicionVenta.SIN_RECETA, producto.condicionVenta());
        assertEquals("Paracetamol", producto.principioActivo());
        assertTrue(producto.requiereLote());
        assertTrue(producto.requiereVencimiento());
    }

    @Test
    void rejectsANonPositiveSalePrice() {
        var result = Producto.create(
                PRODUCTO_ID, TENANT_ID, CATEGORIA_ID, "Alcohol en gel",
                TipoProducto.PRODUCTO_SANITARIO, null, "Frasco", null, 1, null,
                BigDecimal.ZERO, null, false, false, null, null, null, null,
                false, false, CREATED_AT);

        assertTrue(result.isFailure());
        assertEquals("CAT_PRODUCTO_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void requiresIdTenantCategoriaAndCreatedAt() {
        var missingCategoria = Producto.create(
                PRODUCTO_ID, TENANT_ID, null, "Alcohol en gel",
                TipoProducto.PRODUCTO_SANITARIO, null, "Frasco", null, 1, null,
                new BigDecimal("12.50"), null, false, false, null, null, null, null,
                false, false, CREATED_AT);
        assertTrue(missingCategoria.isFailure());
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.ProductoTest"`
Expected: FAIL — `Producto` no existe todavía (error de compilación).

- [ ] **Step 3: Crear `Producto`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/Producto.java`:

```java
package com.softprimesolutions.catalogo.domain.model;

import com.softprimesolutions.catalogo.domain.valueobject.CategoriaId;
import com.softprimesolutions.catalogo.domain.valueobject.ProductoId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Ficha maestra de un artículo comercializable por el tenant (sin stock ni ubicación). */
public final class Producto extends AggregateRoot {

    private static final int NAME_MIN_LENGTH = 2;
    private static final int NAME_MAX_LENGTH = 200;
    private static final int LABORATORY_MAX_LENGTH = 150;
    private static final int UNIT_MAX_LENGTH = 30;
    private static final int PRESENTATION_MAX_LENGTH = 150;
    private static final int BARCODE_MAX_LENGTH = 40;
    private static final int THERAPEUTIC_GROUP_MAX_LENGTH = 150;
    private static final int DIGEMID_CODE_MAX_LENGTH = 40;
    private static final int ACTIVE_INGREDIENT_MAX_LENGTH = 200;
    private static final int CONCENTRATION_MAX_LENGTH = 60;

    private final ProductoId id;
    private final TenantId tenantId;
    private final CategoriaId categoriaId;
    private final String nombre;
    private final TipoProducto tipo;
    private final String laboratorio;
    private final String unidadMedida;
    private final String presentacion;
    private final int unidadesPorPaquete;
    private final String codigoBarras;
    private final BigDecimal precioVenta;
    private final CondicionVenta condicionVenta;
    private final boolean esGenerico;
    private final boolean esGenericoEsencial;
    private final String grupoTerapeutico;
    private final String codigoDigemid;
    private final String principioActivo;
    private final String concentracion;
    private final boolean requiereLote;
    private final boolean requiereVencimiento;
    private final EstadoProducto estado;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Producto(
            ProductoId id, TenantId tenantId, CategoriaId categoriaId, String nombre, TipoProducto tipo,
            String laboratorio, String unidadMedida, String presentacion, int unidadesPorPaquete,
            String codigoBarras, BigDecimal precioVenta, CondicionVenta condicionVenta, boolean esGenerico,
            boolean esGenericoEsencial, String grupoTerapeutico, String codigoDigemid, String principioActivo,
            String concentracion, boolean requiereLote, boolean requiereVencimiento, EstadoProducto estado,
            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.categoriaId = categoriaId;
        this.nombre = nombre;
        this.tipo = tipo;
        this.laboratorio = laboratorio;
        this.unidadMedida = unidadMedida;
        this.presentacion = presentacion;
        this.unidadesPorPaquete = unidadesPorPaquete;
        this.codigoBarras = codigoBarras;
        this.precioVenta = precioVenta;
        this.condicionVenta = condicionVenta;
        this.esGenerico = esGenerico;
        this.esGenericoEsencial = esGenericoEsencial;
        this.grupoTerapeutico = grupoTerapeutico;
        this.codigoDigemid = codigoDigemid;
        this.principioActivo = principioActivo;
        this.concentracion = concentracion;
        this.requiereLote = requiereLote;
        this.requiereVencimiento = requiereVencimiento;
        this.estado = estado;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Result<Producto, ErrorDetail> create(
            ProductoId id, TenantId tenantId, CategoriaId categoriaId, String nombre, TipoProducto tipo,
            String laboratorio, String unidadMedida, String presentacion, int unidadesPorPaquete,
            String codigoBarras, BigDecimal precioVenta, CondicionVenta condicionVenta, boolean esGenerico,
            boolean esGenericoEsencial, String grupoTerapeutico, String codigoDigemid, String principioActivo,
            String concentracion, boolean requiereLote, boolean requiereVencimiento, Instant createdAt) {
        if (id == null) return invalid("id", "La identidad del producto es obligatoria.");
        if (tenantId == null) return invalid("tenantId", "El tenant es obligatorio.");
        if (categoriaId == null) return invalid("categoriaId", "La categoría es obligatoria.");
        if (tipo == null) return invalid("tipo", "El tipo de producto es obligatorio.");
        if (createdAt == null) return invalid("createdAt", "El instante de registro es obligatorio.");

        var normalizedName = normalizeSpaces(nombre);
        if (normalizedName == null || normalizedName.length() < NAME_MIN_LENGTH
                || normalizedName.length() > NAME_MAX_LENGTH) {
            return invalid("nombre", "El nombre debe tener entre 2 y 200 caracteres.");
        }

        var normalizedLaboratory = normalizeNullable(laboratorio);
        if (!withinLength(normalizedLaboratory, LABORATORY_MAX_LENGTH)) {
            return invalid("laboratorio", "El laboratorio no debe exceder 150 caracteres.");
        }

        var normalizedUnit = normalizeSpaces(unidadMedida);
        if (normalizedUnit == null || normalizedUnit.isEmpty() || normalizedUnit.length() > UNIT_MAX_LENGTH) {
            return invalid("unidadMedida", "La unidad de medida es obligatoria y no debe exceder 30 caracteres.");
        }

        var normalizedPresentation = normalizeNullable(presentacion);
        if (!withinLength(normalizedPresentation, PRESENTATION_MAX_LENGTH)) {
            return invalid("presentacion", "La presentación no debe exceder 150 caracteres.");
        }

        if (unidadesPorPaquete < 1) {
            return invalid("unidadesPorPaquete", "Las unidades por paquete deben ser al menos 1.");
        }

        var normalizedBarcode = normalizeNullable(codigoBarras);
        if (!withinLength(normalizedBarcode, BARCODE_MAX_LENGTH)) {
            return invalid("codigoBarras", "El código de barras no debe exceder 40 caracteres.");
        }

        if (precioVenta == null || precioVenta.signum() <= 0) {
            return invalid("precioVenta", "El precio de venta debe ser mayor que cero.");
        }

        var normalizedTherapeuticGroup = normalizeNullable(grupoTerapeutico);
        if (!withinLength(normalizedTherapeuticGroup, THERAPEUTIC_GROUP_MAX_LENGTH)) {
            return invalid("grupoTerapeutico", "El grupo terapéutico no debe exceder 150 caracteres.");
        }

        var normalizedDigemidCode = normalizeNullable(codigoDigemid);
        if (!withinLength(normalizedDigemidCode, DIGEMID_CODE_MAX_LENGTH)) {
            return invalid("codigoDigemid", "El código DIGEMID no debe exceder 40 caracteres.");
        }

        var normalizedActiveIngredient = normalizeNullable(principioActivo);
        if (!withinLength(normalizedActiveIngredient, ACTIVE_INGREDIENT_MAX_LENGTH)) {
            return invalid("principioActivo", "El principio activo no debe exceder 200 caracteres.");
        }

        var normalizedConcentration = normalizeNullable(concentracion);
        if (!withinLength(normalizedConcentration, CONCENTRATION_MAX_LENGTH)) {
            return invalid("concentracion", "La concentración no debe exceder 60 caracteres.");
        }

        if (tipo == TipoProducto.MEDICAMENTO
                && (normalizedActiveIngredient == null || condicionVenta == null)) {
            return invalid("tipo",
                    "Un medicamento requiere principio activo y condición de venta.");
        }

        return Result.success(new Producto(
                id, tenantId, categoriaId, normalizedName, tipo, normalizedLaboratory, normalizedUnit,
                normalizedPresentation, unidadesPorPaquete, normalizedBarcode, precioVenta, condicionVenta,
                esGenerico, esGenericoEsencial, normalizedTherapeuticGroup, normalizedDigemidCode,
                normalizedActiveIngredient, normalizedConcentration, requiereLote, requiereVencimiento,
                EstadoProducto.ACTIVO, createdAt, null));
    }

    public static Producto restore(
            ProductoId id, TenantId tenantId, CategoriaId categoriaId, String nombre, TipoProducto tipo,
            String laboratorio, String unidadMedida, String presentacion, int unidadesPorPaquete,
            String codigoBarras, BigDecimal precioVenta, CondicionVenta condicionVenta, boolean esGenerico,
            boolean esGenericoEsencial, String grupoTerapeutico, String codigoDigemid, String principioActivo,
            String concentracion, boolean requiereLote, boolean requiereVencimiento, EstadoProducto estado,
            Instant createdAt, Instant updatedAt) {
        return new Producto(
                id, tenantId, categoriaId, nombre, tipo, laboratorio, unidadMedida, presentacion,
                unidadesPorPaquete, codigoBarras, precioVenta, condicionVenta, esGenerico, esGenericoEsencial,
                grupoTerapeutico, codigoDigemid, principioActivo, concentracion, requiereLote,
                requiereVencimiento, estado, createdAt, updatedAt);
    }

    private static Result<Producto, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("CAT_PRODUCTO_INVALIDO", message, Map.of("field", field)));
    }

    private static boolean withinLength(String value, int maximum) {
        return value == null || value.length() <= maximum;
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

    public ProductoId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public CategoriaId categoriaId() { return categoriaId; }
    public String nombre() { return nombre; }
    public TipoProducto tipo() { return tipo; }
    public String laboratorio() { return laboratorio; }
    public String unidadMedida() { return unidadMedida; }
    public String presentacion() { return presentacion; }
    public int unidadesPorPaquete() { return unidadesPorPaquete; }
    public String codigoBarras() { return codigoBarras; }
    public BigDecimal precioVenta() { return precioVenta; }
    public CondicionVenta condicionVenta() { return condicionVenta; }
    public boolean esGenerico() { return esGenerico; }
    public boolean esGenericoEsencial() { return esGenericoEsencial; }
    public String grupoTerapeutico() { return grupoTerapeutico; }
    public String codigoDigemid() { return codigoDigemid; }
    public String principioActivo() { return principioActivo; }
    public String concentracion() { return concentracion; }
    public boolean requiereLote() { return requiereLote; }
    public boolean requiereVencimiento() { return requiereVencimiento; }
    public EstadoProducto estado() { return estado; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.domain.model.ProductoTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/domain/model/Producto.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/domain/model/ProductoTest.java
git commit -m "feat(catalogo): agregar agregado de dominio Producto"
```

---

## Fase 3 — Application: DTOs, puertos y casos de uso de Categoría

### Task 6: DTOs de aplicación (`command`, `query`, `result`) para Categoría y Producto

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/CrearCategoriaCommand.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/ActualizarCategoriaCommand.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/CrearProductoCommand.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/ActualizarProductoCommand.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ListarCategoriasQuery.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarProductoQuery.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ListarProductosQuery.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/CategoriaResult.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/ProductoResult.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/PaginaResult.java`

**Interfaces:**
- Consumes: `Command<R>`, `Query<R>` (shared-application).
- Produces: todos los records listados abajo. Usados por Task 7 (puertos), Task 8-9 (handlers), Task 14 (API mapper).

Son records puros sin lógica — no requieren test dedicado, se validan indirectamente vía los handlers (Task 8-9).

- [ ] **Step 1: Crear los DTOs de `command`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/CrearCategoriaCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.CategoriaResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.util.UUID;

public record CrearCategoriaCommand(UUID tenantId, String nombre, String descripcion)
        implements Command<CategoriaResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/ActualizarCategoriaCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.CategoriaResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.util.UUID;

public record ActualizarCategoriaCommand(UUID tenantId, UUID categoriaId, String nombre, String descripcion)
        implements Command<CategoriaResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/CrearProductoCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.ProductoResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.math.BigDecimal;
import java.util.UUID;

public record CrearProductoCommand(
        UUID tenantId,
        UUID categoriaId,
        String nombre,
        String tipo,
        String laboratorio,
        String unidadMedida,
        String presentacion,
        Integer unidadesPorPaquete,
        String codigoBarras,
        BigDecimal precioVenta,
        String condicionVenta,
        boolean esGenerico,
        boolean esGenericoEsencial,
        String grupoTerapeutico,
        String codigoDigemid,
        String principioActivo,
        String concentracion,
        boolean requiereLote,
        boolean requiereVencimiento) implements Command<ProductoResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/command/ActualizarProductoCommand.java`:

```java
package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.ProductoResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.math.BigDecimal;
import java.util.UUID;

public record ActualizarProductoCommand(
        UUID tenantId,
        UUID productoId,
        UUID categoriaId,
        String nombre,
        String tipo,
        String laboratorio,
        String unidadMedida,
        String presentacion,
        Integer unidadesPorPaquete,
        String codigoBarras,
        BigDecimal precioVenta,
        String condicionVenta,
        boolean esGenerico,
        boolean esGenericoEsencial,
        String grupoTerapeutico,
        String codigoDigemid,
        String principioActivo,
        String concentracion,
        boolean requiereLote,
        boolean requiereVencimiento) implements Command<ProductoResult> {
}
```

- [ ] **Step 2: Crear los DTOs de `query`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ListarCategoriasQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.CategoriaResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.List;
import java.util.UUID;

public record ListarCategoriasQuery(UUID tenantId, String estado) implements Query<List<CategoriaResult>> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ConsultarProductoQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.ProductoResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.UUID;

public record ConsultarProductoQuery(UUID tenantId, UUID productoId) implements Query<ProductoResult> {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/query/ListarProductosQuery.java`:

```java
package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.ProductoResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.UUID;

public record ListarProductosQuery(
        UUID tenantId,
        String texto,
        UUID categoriaId,
        String tipo,
        String estado,
        int page,
        int size) implements Query<PaginaResult<ProductoResult>> {
}
```

- [ ] **Step 3: Crear los DTOs de `result`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/CategoriaResult.java`:

```java
package com.softprimesolutions.catalogo.application.dto.result;

import java.time.Instant;
import java.util.UUID;

public record CategoriaResult(
        UUID id,
        UUID tenantId,
        String nombre,
        String descripcion,
        String estado,
        Instant createdAt,
        Instant updatedAt) {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/result/ProductoResult.java`:

```java
package com.softprimesolutions.catalogo.application.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductoResult(
        UUID id,
        UUID tenantId,
        UUID categoriaId,
        String nombre,
        String tipo,
        String laboratorio,
        String unidadMedida,
        String presentacion,
        int unidadesPorPaquete,
        String codigoBarras,
        BigDecimal precioVenta,
        String condicionVenta,
        boolean esGenerico,
        boolean esGenericoEsencial,
        String grupoTerapeutico,
        String codigoDigemid,
        String principioActivo,
        String concentracion,
        boolean requiereLote,
        boolean requiereVencimiento,
        String estado,
        Instant createdAt,
        Instant updatedAt) {
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

- [ ] **Step 4: Compilar el módulo**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/dto/
git commit -m "feat(catalogo): agregar DTOs de aplicacion command/query/result"
```

---

### Task 7: Puertos `application/port/{in,out}` para Categoría y Producto

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/CrearCategoriaUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ActualizarCategoriaUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ListarCategoriasUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/CrearProductoUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ActualizarProductoUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarProductoUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ListarProductosUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/CatalogoControlUseCase.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoWritePort.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoReadPort.java`

**Interfaces:**
- Consumes: DTOs de Task 6, `Categoria`/`Producto` (Task 4-5), `Result`, `Unit` (shared-kernel), `ApplicationError` (shared-application).
- Produces: todas las interfaces listadas — contratos que Task 8-9 (handlers) implementan e invocan, y que Task 10-11 (adapters JPA/JDBC) implementan.

No requiere test dedicado (interfaces puras) — se validan indirectamente vía Task 8-9 con fakes.

- [ ] **Step 1: Crear los puertos `port/in`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/CrearCategoriaUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.CrearCategoriaCommand;
import com.softprimesolutions.catalogo.application.dto.result.CategoriaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface CrearCategoriaUseCase {
    Result<CategoriaResult, ApplicationError> execute(CrearCategoriaCommand command);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ActualizarCategoriaUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarCategoriaCommand;
import com.softprimesolutions.catalogo.application.dto.result.CategoriaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ActualizarCategoriaUseCase {
    Result<CategoriaResult, ApplicationError> execute(ActualizarCategoriaCommand command);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ListarCategoriasUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ListarCategoriasQuery;
import com.softprimesolutions.catalogo.application.dto.result.CategoriaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;

@FunctionalInterface
public interface ListarCategoriasUseCase {
    Result<List<CategoriaResult>, ApplicationError> execute(ListarCategoriasQuery query);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/CrearProductoUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.CrearProductoCommand;
import com.softprimesolutions.catalogo.application.dto.result.ProductoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface CrearProductoUseCase {
    Result<ProductoResult, ApplicationError> execute(CrearProductoCommand command);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ActualizarProductoUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarProductoCommand;
import com.softprimesolutions.catalogo.application.dto.result.ProductoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ActualizarProductoUseCase {
    Result<ProductoResult, ApplicationError> execute(ActualizarProductoCommand command);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ConsultarProductoUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarProductoQuery;
import com.softprimesolutions.catalogo.application.dto.result.ProductoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ConsultarProductoUseCase {
    Result<ProductoResult, ApplicationError> execute(ConsultarProductoQuery query);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/ListarProductosUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ListarProductosQuery;
import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.ProductoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ListarProductosUseCase {
    Result<PaginaResult<ProductoResult>, ApplicationError> execute(ListarProductosQuery query);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/in/CatalogoControlUseCase.java`:

```java
package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.shared.kernel.result.Unit;
import java.util.UUID;

public interface CatalogoControlUseCase {
    Result<Unit, ApplicationError> changeCategoriaStatus(UUID tenantId, UUID categoriaId, String status);

    Result<Unit, ApplicationError> changeProductoStatus(UUID tenantId, UUID productoId, String status);
}
```

- [ ] **Step 2: Crear `CatalogoWritePort`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoWritePort.java`:

```java
package com.softprimesolutions.catalogo.application.port.out;

import com.softprimesolutions.catalogo.domain.model.Categoria;
import com.softprimesolutions.catalogo.domain.model.Producto;
import java.time.Instant;
import java.util.UUID;

public interface CatalogoWritePort {

    SaveCategoriaOutcome save(Categoria categoria);

    SaveProductoOutcome save(Producto producto);

    boolean categoriaExists(UUID tenantId, UUID categoriaId);

    boolean changeCategoriaStatus(UUID tenantId, UUID categoriaId, String status, Instant changedAt);

    boolean changeProductoStatus(UUID tenantId, UUID productoId, String status, Instant changedAt);

    enum SaveCategoriaOutcome {
        CREATED,
        UPDATED,
        TENANT_NOT_FOUND,
        DUPLICATE_NAME,
        NOT_FOUND
    }

    enum SaveProductoOutcome {
        CREATED,
        UPDATED,
        TENANT_NOT_FOUND,
        CATEGORIA_NOT_FOUND,
        DUPLICATE_BARCODE,
        NOT_FOUND
    }
}
```

- [ ] **Step 3: Crear `CatalogoReadPort`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/out/CatalogoReadPort.java`:

```java
package com.softprimesolutions.catalogo.application.port.out;

import com.softprimesolutions.catalogo.application.dto.result.CategoriaResult;
import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.ProductoResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogoReadPort {

    List<CategoriaResult> findCategorias(UUID tenantId, String estado);

    Optional<ProductoResult> findProducto(UUID tenantId, UUID productoId);

    PaginaResult<ProductoResult> findProductos(
            UUID tenantId, String texto, UUID categoriaId, String tipo, String estado, int page, int size);
}
```

- [ ] **Step 4: Compilar el módulo**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/port/
git commit -m "feat(catalogo): agregar puertos de entrada y salida de aplicacion"
```

---

### Task 8: Mapper de aplicación y handlers de Categoría (crear, actualizar, listar)

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/mapper/CatalogoApplicationMapper.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearCategoriaHandler.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarCategoriaHandler.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ListarCategoriasHandler.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CrearCategoriaHandlerTest.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarCategoriaHandlerTest.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ListarCategoriasHandlerTest.java`

**Interfaces:**
- Consumes: `Categoria` (Task 4), DTOs (Task 6), puertos (Task 7), `IdentifierGenerator`, `ClockPort` (shared-application).
- Produces: `CrearCategoriaHandler implements CrearCategoriaUseCase`, `ActualizarCategoriaHandler implements ActualizarCategoriaUseCase`, `ListarCategoriasHandler implements ListarCategoriasUseCase`, `CatalogoApplicationMapper.toResult(Categoria): CategoriaResult`. Usados por Task 14 (controller).

- [ ] **Step 1: Escribir el test que falla para `CrearCategoriaHandler`**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CrearCategoriaHandlerTest.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.command.CrearCategoriaCommand;
import com.softprimesolutions.catalogo.application.port.out.CatalogoWritePort;
import com.softprimesolutions.catalogo.domain.model.Categoria;
import com.softprimesolutions.catalogo.domain.model.Producto;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrearCategoriaHandlerTest {

    private static final UUID TENANT_ID = UUID.fromString("172e0f26-a765-46f3-841c-4a11407ccf5b");

    @Test
    void createsACategoriaSuccessfully() {
        var writePort = new FakeCatalogoWritePort();
        var handler = new CrearCategoriaHandler(writePort, new SequentialIds(), () -> Instant.parse("2026-09-07T10:00:00Z"));

        var result = handler.execute(new CrearCategoriaCommand(TENANT_ID, "Analgésicos", null));

        assertTrue(result.isSuccess());
        var created = result.getOrElse(error -> null);
        assertEquals("Analgésicos", created.nombre());
        assertEquals("ACTIVA", created.estado());
    }

    @Test
    void failsWithValidationErrorWhenNameIsTooShort() {
        var writePort = new FakeCatalogoWritePort();
        var handler = new CrearCategoriaHandler(writePort, new SequentialIds(), () -> Instant.parse("2026-09-07T10:00:00Z"));

        var result = handler.execute(new CrearCategoriaCommand(TENANT_ID, "A", null));

        assertTrue(result.isFailure());
        assertEquals("CAT_CATEGORIA_INVALIDA", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void failsWithConflictWhenNameAlreadyExists() {
        var writePort = new FakeCatalogoWritePort();
        writePort.categoriaOutcome = CatalogoWritePort.SaveCategoriaOutcome.DUPLICATE_NAME;
        var handler = new CrearCategoriaHandler(writePort, new SequentialIds(), () -> Instant.parse("2026-09-07T10:00:00Z"));

        var result = handler.execute(new CrearCategoriaCommand(TENANT_ID, "Analgésicos", null));

        assertTrue(result.isFailure());
        assertEquals("CAT_CATEGORIA_DUPLICADA", result.fold(value -> null, error -> error.code()));
    }

    private static final class SequentialIds implements com.softprimesolutions.shared.application.port.IdentifierGenerator {
        private long sequence;

        @Override
        public UUID next() {
            return new UUID(0, ++sequence);
        }
    }

    private static final class FakeCatalogoWritePort implements CatalogoWritePort {
        private SaveCategoriaOutcome categoriaOutcome = SaveCategoriaOutcome.CREATED;

        @Override
        public SaveCategoriaOutcome save(Categoria categoria) {
            return categoriaOutcome;
        }

        @Override
        public SaveProductoOutcome save(Producto producto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean categoriaExists(UUID tenantId, UUID categoriaId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeCategoriaStatus(UUID tenantId, UUID categoriaId, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeProductoStatus(UUID tenantId, UUID productoId, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.command.CrearCategoriaHandlerTest"`
Expected: FAIL — `CrearCategoriaHandler` no existe todavía (error de compilación).

- [ ] **Step 3: Crear `CatalogoApplicationMapper`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/mapper/CatalogoApplicationMapper.java`:

```java
package com.softprimesolutions.catalogo.application.mapper;

import com.softprimesolutions.catalogo.application.dto.result.CategoriaResult;
import com.softprimesolutions.catalogo.application.dto.result.ProductoResult;
import com.softprimesolutions.catalogo.domain.model.Categoria;
import com.softprimesolutions.catalogo.domain.model.Producto;

public final class CatalogoApplicationMapper {

    private CatalogoApplicationMapper() {
    }

    public static CategoriaResult toResult(Categoria categoria) {
        return new CategoriaResult(
                categoria.id().value(),
                categoria.tenantId().value(),
                categoria.nombre(),
                categoria.descripcion(),
                categoria.estado().name(),
                categoria.createdAt(),
                categoria.updatedAt());
    }

    public static ProductoResult toResult(Producto producto) {
        return new ProductoResult(
                producto.id().value(),
                producto.tenantId().value(),
                producto.categoriaId().value(),
                producto.nombre(),
                producto.tipo().name(),
                producto.laboratorio(),
                producto.unidadMedida(),
                producto.presentacion(),
                producto.unidadesPorPaquete(),
                producto.codigoBarras(),
                producto.precioVenta(),
                producto.condicionVenta() == null ? null : producto.condicionVenta().name(),
                producto.esGenerico(),
                producto.esGenericoEsencial(),
                producto.grupoTerapeutico(),
                producto.codigoDigemid(),
                producto.principioActivo(),
                producto.concentracion(),
                producto.requiereLote(),
                producto.requiereVencimiento(),
                producto.estado().name(),
                producto.createdAt(),
                producto.updatedAt());
    }
}
```

- [ ] **Step 4: Crear `CrearCategoriaHandler`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearCategoriaHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.CrearCategoriaCommand;
import com.softprimesolutions.catalogo.application.dto.result.CategoriaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.CrearCategoriaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoWritePort;
import com.softprimesolutions.catalogo.domain.model.Categoria;
import com.softprimesolutions.catalogo.domain.valueobject.CategoriaId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearCategoriaHandler implements CrearCategoriaUseCase {

    private final CatalogoWritePort writePort;
    private final IdentifierGenerator identifierGenerator;
    private final ClockPort clock;

    public CrearCategoriaHandler(CatalogoWritePort writePort, IdentifierGenerator identifierGenerator, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.identifierGenerator = Objects.requireNonNull(identifierGenerator, "identifierGenerator es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<CategoriaResult, ApplicationError> execute(CrearCategoriaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var categoria = Categoria.create(
                new CategoriaId(identifierGenerator.next()),
                command.tenantId() == null ? null : new TenantId(command.tenantId()),
                command.nombre(), command.descripcion(), clock.now());
        return categoria.fold(this::persist, this::validationFailure);
    }

    private Result<CategoriaResult, ApplicationError> persist(Categoria categoria) {
        var outcome = writePort.save(categoria);
        if (outcome == CatalogoWritePort.SaveCategoriaOutcome.TENANT_NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_TENANT_NO_ENCONTRADO", "El tenant indicado no existe.", ErrorCategory.NOT_FOUND));
        }
        if (outcome == CatalogoWritePort.SaveCategoriaOutcome.DUPLICATE_NAME) {
            return Result.failure(new StandardApplicationError(
                    "CAT_CATEGORIA_DUPLICADA", "Ya existe una categoría con el nombre indicado.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(CatalogoApplicationMapper.toResult(categoria));
    }

    private Result<CategoriaResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

- [ ] **Step 5: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.command.CrearCategoriaHandlerTest"`
Expected: PASS

- [ ] **Step 6: Escribir el test que falla para `ActualizarCategoriaHandler`**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarCategoriaHandlerTest.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarCategoriaCommand;
import com.softprimesolutions.catalogo.application.port.out.CatalogoWritePort;
import com.softprimesolutions.catalogo.domain.model.Categoria;
import com.softprimesolutions.catalogo.domain.model.Producto;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActualizarCategoriaHandlerTest {

    private static final UUID TENANT_ID = UUID.fromString("172e0f26-a765-46f3-841c-4a11407ccf5b");
    private static final UUID CATEGORIA_ID = UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589");

    @Test
    void updatesACategoriaSuccessfully() {
        var writePort = new FakeCatalogoWritePort();
        var handler = new ActualizarCategoriaHandler(writePort, () -> Instant.parse("2026-09-07T11:00:00Z"));

        var result = handler.execute(new ActualizarCategoriaCommand(
                TENANT_ID, CATEGORIA_ID, "Analgésicos y antipiréticos", "Descripción actualizada"));

        assertTrue(result.isSuccess());
        var updated = result.getOrElse(error -> null);
        assertEquals("Analgésicos y antipiréticos", updated.nombre());
    }

    @Test
    void failsWithNotFoundWhenCategoriaDoesNotExist() {
        var writePort = new FakeCatalogoWritePort();
        writePort.categoriaOutcome = CatalogoWritePort.SaveCategoriaOutcome.NOT_FOUND;
        var handler = new ActualizarCategoriaHandler(writePort, () -> Instant.parse("2026-09-07T11:00:00Z"));

        var result = handler.execute(new ActualizarCategoriaCommand(
                TENANT_ID, CATEGORIA_ID, "Analgésicos", null));

        assertTrue(result.isFailure());
        assertEquals("CAT_CATEGORIA_NO_ENCONTRADA", result.fold(value -> null, error -> error.code()));
    }

    private static final class FakeCatalogoWritePort implements CatalogoWritePort {
        private SaveCategoriaOutcome categoriaOutcome = SaveCategoriaOutcome.UPDATED;

        @Override
        public SaveCategoriaOutcome save(Categoria categoria) {
            return categoriaOutcome;
        }

        @Override
        public SaveProductoOutcome save(Producto producto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean categoriaExists(UUID tenantId, UUID categoriaId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeCategoriaStatus(UUID tenantId, UUID categoriaId, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeProductoStatus(UUID tenantId, UUID productoId, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }
    }
}
```

- [ ] **Step 7: Ejecutar y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.command.ActualizarCategoriaHandlerTest"`
Expected: FAIL — `ActualizarCategoriaHandler` no existe todavía.

- [ ] **Step 8: Crear `ActualizarCategoriaHandler`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarCategoriaHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarCategoriaCommand;
import com.softprimesolutions.catalogo.application.dto.result.CategoriaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ActualizarCategoriaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoWritePort;
import com.softprimesolutions.catalogo.domain.model.Categoria;
import com.softprimesolutions.catalogo.domain.valueobject.CategoriaId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarCategoriaHandler implements ActualizarCategoriaUseCase {

    private final CatalogoWritePort writePort;
    private final ClockPort clock;

    public ActualizarCategoriaHandler(CatalogoWritePort writePort, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<CategoriaResult, ApplicationError> execute(ActualizarCategoriaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var categoria = Categoria.create(
                new CategoriaId(command.categoriaId()),
                command.tenantId() == null ? null : new TenantId(command.tenantId()),
                command.nombre(), command.descripcion(), clock.now());
        return categoria.fold(this::persist, this::validationFailure);
    }

    private Result<CategoriaResult, ApplicationError> persist(Categoria categoria) {
        var outcome = writePort.save(categoria);
        if (outcome == CatalogoWritePort.SaveCategoriaOutcome.NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_CATEGORIA_NO_ENCONTRADA", "La categoría indicada no existe.", ErrorCategory.NOT_FOUND));
        }
        if (outcome == CatalogoWritePort.SaveCategoriaOutcome.DUPLICATE_NAME) {
            return Result.failure(new StandardApplicationError(
                    "CAT_CATEGORIA_DUPLICADA", "Ya existe una categoría con el nombre indicado.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(CatalogoApplicationMapper.toResult(categoria));
    }

    private Result<CategoriaResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

- [ ] **Step 9: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.command.ActualizarCategoriaHandlerTest"`
Expected: PASS

- [ ] **Step 10: Escribir el test que falla para `ListarCategoriasHandler`**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ListarCategoriasHandlerTest.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.query.ListarCategoriasQuery;
import com.softprimesolutions.catalogo.application.dto.result.CategoriaResult;
import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.ProductoResult;
import com.softprimesolutions.catalogo.application.port.out.CatalogoReadPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListarCategoriasHandlerTest {

    private static final UUID TENANT_ID = UUID.fromString("172e0f26-a765-46f3-841c-4a11407ccf5b");

    @Test
    void returnsCategoriasFromReadPort() {
        var categoria = new CategoriaResult(
                UUID.randomUUID(), TENANT_ID, "Analgésicos", null, "ACTIVA",
                Instant.parse("2026-09-07T10:00:00Z"), null);
        var readPort = new FakeCatalogoReadPort(List.of(categoria));
        var handler = new ListarCategoriasHandler(readPort);

        var result = handler.execute(new ListarCategoriasQuery(TENANT_ID, null));

        assertTrue(result.isSuccess());
        assertEquals(1, result.getOrElse(error -> null).size());
    }

    private static final class FakeCatalogoReadPort implements CatalogoReadPort {
        private final List<CategoriaResult> categorias;

        private FakeCatalogoReadPort(List<CategoriaResult> categorias) {
            this.categorias = categorias;
        }

        @Override
        public List<CategoriaResult> findCategorias(UUID tenantId, String estado) {
            return categorias;
        }

        @Override
        public Optional<ProductoResult> findProducto(UUID tenantId, UUID productoId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PaginaResult<ProductoResult> findProductos(
                UUID tenantId, String texto, UUID categoriaId, String tipo, String estado, int page, int size) {
            throw new UnsupportedOperationException();
        }
    }
}
```

- [ ] **Step 11: Ejecutar y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ListarCategoriasHandlerTest"`
Expected: FAIL — `ListarCategoriasHandler` no existe todavía.

- [ ] **Step 12: Crear `ListarCategoriasHandler`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ListarCategoriasHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ListarCategoriasQuery;
import com.softprimesolutions.catalogo.application.dto.result.CategoriaResult;
import com.softprimesolutions.catalogo.application.port.in.ListarCategoriasUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;
import java.util.Objects;

public final class ListarCategoriasHandler implements ListarCategoriasUseCase {

    private final CatalogoReadPort readPort;

    public ListarCategoriasHandler(CatalogoReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<List<CategoriaResult>, ApplicationError> execute(ListarCategoriasQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return Result.success(readPort.findCategorias(query.tenantId(), query.estado()));
    }
}
```

- [ ] **Step 13: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ListarCategoriasHandlerTest"`
Expected: PASS

- [ ] **Step 14: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/mapper/ service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearCategoriaHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarCategoriaHandler.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ListarCategoriasHandler.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/
git commit -m "feat(catalogo): agregar handlers de crear, actualizar y listar categorias"
```

---

### Task 9: Handlers de Producto (crear, actualizar, consultar, listar)

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearProductoHandler.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarProductoHandler.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarProductoHandler.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ListarProductosHandler.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CrearProductoHandlerTest.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarProductoHandlerTest.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarProductoHandlerTest.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ListarProductosHandlerTest.java`

**Interfaces:**
- Consumes: `Producto` (Task 5), DTOs (Task 6), puertos (Task 7), `CatalogoApplicationMapper` (Task 8).
- Produces: `CrearProductoHandler implements CrearProductoUseCase`, `ActualizarProductoHandler implements ActualizarProductoUseCase`, `ConsultarProductoHandler implements ConsultarProductoUseCase`, `ListarProductosHandler implements ListarProductosUseCase`. Usados por Task 14 (controller).

- [ ] **Step 1: Escribir el test que falla para `CrearProductoHandler`**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CrearProductoHandlerTest.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.command.CrearProductoCommand;
import com.softprimesolutions.catalogo.application.port.out.CatalogoWritePort;
import com.softprimesolutions.catalogo.domain.model.Categoria;
import com.softprimesolutions.catalogo.domain.model.Producto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrearProductoHandlerTest {

    private static final UUID TENANT_ID = UUID.fromString("172e0f26-a765-46f3-841c-4a11407ccf5b");
    private static final UUID CATEGORIA_ID = UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589");

    @Test
    void createsAProductSuccessfully() {
        var writePort = new FakeCatalogoWritePort();
        var handler = new CrearProductoHandler(writePort, new SequentialIds(), () -> Instant.parse("2026-09-07T10:00:00Z"));

        var result = handler.execute(new CrearProductoCommand(
                TENANT_ID, CATEGORIA_ID, "Alcohol en gel 250ml", "PRODUCTO_SANITARIO", null, "Frasco",
                null, 1, null, new BigDecimal("12.50"), null, false, false, null, null, null, null,
                false, false));

        assertTrue(result.isSuccess());
        var created = result.getOrElse(error -> null);
        assertEquals("Alcohol en gel 250ml", created.nombre());
        assertEquals("ACTIVO", created.estado());
    }

    @Test
    void failsWithValidationErrorWhenTypeIsInvalid() {
        var writePort = new FakeCatalogoWritePort();
        var handler = new CrearProductoHandler(writePort, new SequentialIds(), () -> Instant.parse("2026-09-07T10:00:00Z"));

        var result = handler.execute(new CrearProductoCommand(
                TENANT_ID, CATEGORIA_ID, "Producto X", "TIPO_INEXISTENTE", null, "Unidad",
                null, 1, null, new BigDecimal("10.00"), null, false, false, null, null, null, null,
                false, false));

        assertTrue(result.isFailure());
        assertEquals("CAT_PRODUCTO_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void failsWithNotFoundWhenCategoriaDoesNotExist() {
        var writePort = new FakeCatalogoWritePort();
        writePort.productoOutcome = CatalogoWritePort.SaveProductoOutcome.CATEGORIA_NOT_FOUND;
        var handler = new CrearProductoHandler(writePort, new SequentialIds(), () -> Instant.parse("2026-09-07T10:00:00Z"));

        var result = handler.execute(new CrearProductoCommand(
                TENANT_ID, CATEGORIA_ID, "Alcohol en gel", "PRODUCTO_SANITARIO", null, "Frasco",
                null, 1, null, new BigDecimal("12.50"), null, false, false, null, null, null, null,
                false, false));

        assertTrue(result.isFailure());
        assertEquals("CAT_CATEGORIA_NO_ENCONTRADA", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void failsWithConflictWhenBarcodeAlreadyExists() {
        var writePort = new FakeCatalogoWritePort();
        writePort.productoOutcome = CatalogoWritePort.SaveProductoOutcome.DUPLICATE_BARCODE;
        var handler = new CrearProductoHandler(writePort, new SequentialIds(), () -> Instant.parse("2026-09-07T10:00:00Z"));

        var result = handler.execute(new CrearProductoCommand(
                TENANT_ID, CATEGORIA_ID, "Alcohol en gel", "PRODUCTO_SANITARIO", null, "Frasco",
                null, 1, "7501234567890", new BigDecimal("12.50"), null, false, false, null, null, null, null,
                false, false));

        assertTrue(result.isFailure());
        assertEquals("CAT_PRODUCTO_CODIGO_BARRAS_DUPLICADO", result.fold(value -> null, error -> error.code()));
    }

    private static final class SequentialIds implements com.softprimesolutions.shared.application.port.IdentifierGenerator {
        private long sequence;

        @Override
        public UUID next() {
            return new UUID(0, ++sequence);
        }
    }

    private static final class FakeCatalogoWritePort implements CatalogoWritePort {
        private SaveProductoOutcome productoOutcome = SaveProductoOutcome.CREATED;

        @Override
        public SaveCategoriaOutcome save(Categoria categoria) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveProductoOutcome save(Producto producto) {
            return productoOutcome;
        }

        @Override
        public boolean categoriaExists(UUID tenantId, UUID categoriaId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeCategoriaStatus(UUID tenantId, UUID categoriaId, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeProductoStatus(UUID tenantId, UUID productoId, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.command.CrearProductoHandlerTest"`
Expected: FAIL — `CrearProductoHandler` no existe todavía.

- [ ] **Step 3: Crear `CrearProductoHandler`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CrearProductoHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.CrearProductoCommand;
import com.softprimesolutions.catalogo.application.dto.result.ProductoResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.CrearProductoUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoWritePort;
import com.softprimesolutions.catalogo.domain.model.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.Producto;
import com.softprimesolutions.catalogo.domain.model.TipoProducto;
import com.softprimesolutions.catalogo.domain.valueobject.CategoriaId;
import com.softprimesolutions.catalogo.domain.valueobject.ProductoId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class CrearProductoHandler implements CrearProductoUseCase {

    private final CatalogoWritePort writePort;
    private final IdentifierGenerator identifierGenerator;
    private final ClockPort clock;

    public CrearProductoHandler(CatalogoWritePort writePort, IdentifierGenerator identifierGenerator, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.identifierGenerator = Objects.requireNonNull(identifierGenerator, "identifierGenerator es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<ProductoResult, ApplicationError> execute(CrearProductoCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");

        final TipoProducto tipo;
        try {
            tipo = TipoProducto.valueOf(command.tipo() == null ? "" : command.tipo().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Result.failure(new StandardApplicationError(
                    "CAT_PRODUCTO_INVALIDO", "El tipo de producto no es válido.", ErrorCategory.VALIDATION,
                    Map.of("field", "tipo")));
        }

        CondicionVenta condicionVenta = null;
        if (command.condicionVenta() != null) {
            try {
                condicionVenta = CondicionVenta.valueOf(command.condicionVenta().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                return Result.failure(new StandardApplicationError(
                        "CAT_PRODUCTO_INVALIDO", "La condición de venta no es válida.", ErrorCategory.VALIDATION,
                        Map.of("field", "condicionVenta")));
            }
        }

        var producto = Producto.create(
                new ProductoId(identifierGenerator.next()),
                command.tenantId() == null ? null : new TenantId(command.tenantId()),
                command.categoriaId() == null ? null : new CategoriaId(command.categoriaId()),
                command.nombre(), tipo, command.laboratorio(), command.unidadMedida(), command.presentacion(),
                command.unidadesPorPaquete() == null ? 1 : command.unidadesPorPaquete(), command.codigoBarras(),
                command.precioVenta(), condicionVenta, command.esGenerico(), command.esGenericoEsencial(),
                command.grupoTerapeutico(), command.codigoDigemid(), command.principioActivo(),
                command.concentracion(), command.requiereLote(), command.requiereVencimiento(), clock.now());
        return producto.fold(this::persist, this::validationFailure);
    }

    private Result<ProductoResult, ApplicationError> persist(Producto producto) {
        var outcome = writePort.save(producto);
        if (outcome == CatalogoWritePort.SaveProductoOutcome.TENANT_NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_TENANT_NO_ENCONTRADO", "El tenant indicado no existe.", ErrorCategory.NOT_FOUND));
        }
        if (outcome == CatalogoWritePort.SaveProductoOutcome.CATEGORIA_NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_CATEGORIA_NO_ENCONTRADA", "La categoría indicada no existe.", ErrorCategory.NOT_FOUND));
        }
        if (outcome == CatalogoWritePort.SaveProductoOutcome.DUPLICATE_BARCODE) {
            return Result.failure(new StandardApplicationError(
                    "CAT_PRODUCTO_CODIGO_BARRAS_DUPLICADO", "Ya existe un producto con el código de barras indicado.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(CatalogoApplicationMapper.toResult(producto));
    }

    private Result<ProductoResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.command.CrearProductoHandlerTest"`
Expected: PASS

- [ ] **Step 5: Escribir el test que falla para `ActualizarProductoHandler`**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarProductoHandlerTest.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarProductoCommand;
import com.softprimesolutions.catalogo.application.port.out.CatalogoWritePort;
import com.softprimesolutions.catalogo.domain.model.Categoria;
import com.softprimesolutions.catalogo.domain.model.Producto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActualizarProductoHandlerTest {

    private static final UUID TENANT_ID = UUID.fromString("172e0f26-a765-46f3-841c-4a11407ccf5b");
    private static final UUID CATEGORIA_ID = UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589");
    private static final UUID PRODUCTO_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void updatesAProductSuccessfully() {
        var writePort = new FakeCatalogoWritePort();
        var handler = new ActualizarProductoHandler(writePort, () -> Instant.parse("2026-09-07T11:00:00Z"));

        var result = handler.execute(new ActualizarProductoCommand(
                TENANT_ID, PRODUCTO_ID, CATEGORIA_ID, "Alcohol en gel 500ml", "PRODUCTO_SANITARIO", null,
                "Frasco", null, 1, null, new BigDecimal("18.00"), null, false, false, null, null, null, null,
                false, false));

        assertTrue(result.isSuccess());
        var updated = result.getOrElse(error -> null);
        assertEquals("Alcohol en gel 500ml", updated.nombre());
    }

    @Test
    void failsWithNotFoundWhenProductDoesNotExist() {
        var writePort = new FakeCatalogoWritePort();
        writePort.productoOutcome = CatalogoWritePort.SaveProductoOutcome.NOT_FOUND;
        var handler = new ActualizarProductoHandler(writePort, () -> Instant.parse("2026-09-07T11:00:00Z"));

        var result = handler.execute(new ActualizarProductoCommand(
                TENANT_ID, PRODUCTO_ID, CATEGORIA_ID, "Alcohol en gel", "PRODUCTO_SANITARIO", null,
                "Frasco", null, 1, null, new BigDecimal("18.00"), null, false, false, null, null, null, null,
                false, false));

        assertTrue(result.isFailure());
        assertEquals("CAT_PRODUCTO_NO_ENCONTRADO", result.fold(value -> null, error -> error.code()));
    }

    private static final class FakeCatalogoWritePort implements CatalogoWritePort {
        private SaveProductoOutcome productoOutcome = SaveProductoOutcome.UPDATED;

        @Override
        public SaveCategoriaOutcome save(Categoria categoria) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveProductoOutcome save(Producto producto) {
            return productoOutcome;
        }

        @Override
        public boolean categoriaExists(UUID tenantId, UUID categoriaId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeCategoriaStatus(UUID tenantId, UUID categoriaId, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeProductoStatus(UUID tenantId, UUID productoId, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }
    }
}
```

- [ ] **Step 6: Ejecutar y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.command.ActualizarProductoHandlerTest"`
Expected: FAIL — `ActualizarProductoHandler` no existe todavía.

- [ ] **Step 7: Crear `ActualizarProductoHandler`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/ActualizarProductoHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarProductoCommand;
import com.softprimesolutions.catalogo.application.dto.result.ProductoResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ActualizarProductoUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoWritePort;
import com.softprimesolutions.catalogo.domain.model.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.Producto;
import com.softprimesolutions.catalogo.domain.model.TipoProducto;
import com.softprimesolutions.catalogo.domain.valueobject.CategoriaId;
import com.softprimesolutions.catalogo.domain.valueobject.ProductoId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class ActualizarProductoHandler implements ActualizarProductoUseCase {

    private final CatalogoWritePort writePort;
    private final ClockPort clock;

    public ActualizarProductoHandler(CatalogoWritePort writePort, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<ProductoResult, ApplicationError> execute(ActualizarProductoCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");

        final TipoProducto tipo;
        try {
            tipo = TipoProducto.valueOf(command.tipo() == null ? "" : command.tipo().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Result.failure(new StandardApplicationError(
                    "CAT_PRODUCTO_INVALIDO", "El tipo de producto no es válido.", ErrorCategory.VALIDATION,
                    Map.of("field", "tipo")));
        }

        CondicionVenta condicionVenta = null;
        if (command.condicionVenta() != null) {
            try {
                condicionVenta = CondicionVenta.valueOf(command.condicionVenta().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                return Result.failure(new StandardApplicationError(
                        "CAT_PRODUCTO_INVALIDO", "La condición de venta no es válida.", ErrorCategory.VALIDATION,
                        Map.of("field", "condicionVenta")));
            }
        }

        var producto = Producto.create(
                new ProductoId(command.productoId()),
                command.tenantId() == null ? null : new TenantId(command.tenantId()),
                command.categoriaId() == null ? null : new CategoriaId(command.categoriaId()),
                command.nombre(), tipo, command.laboratorio(), command.unidadMedida(), command.presentacion(),
                command.unidadesPorPaquete() == null ? 1 : command.unidadesPorPaquete(), command.codigoBarras(),
                command.precioVenta(), condicionVenta, command.esGenerico(), command.esGenericoEsencial(),
                command.grupoTerapeutico(), command.codigoDigemid(), command.principioActivo(),
                command.concentracion(), command.requiereLote(), command.requiereVencimiento(), clock.now());
        return producto.fold(this::persist, this::validationFailure);
    }

    private Result<ProductoResult, ApplicationError> persist(Producto producto) {
        var outcome = writePort.save(producto);
        if (outcome == CatalogoWritePort.SaveProductoOutcome.NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_PRODUCTO_NO_ENCONTRADO", "El producto indicado no existe.", ErrorCategory.NOT_FOUND));
        }
        if (outcome == CatalogoWritePort.SaveProductoOutcome.CATEGORIA_NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_CATEGORIA_NO_ENCONTRADA", "La categoría indicada no existe.", ErrorCategory.NOT_FOUND));
        }
        if (outcome == CatalogoWritePort.SaveProductoOutcome.DUPLICATE_BARCODE) {
            return Result.failure(new StandardApplicationError(
                    "CAT_PRODUCTO_CODIGO_BARRAS_DUPLICADO", "Ya existe un producto con el código de barras indicado.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(CatalogoApplicationMapper.toResult(producto));
    }

    private Result<ProductoResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
```

- [ ] **Step 8: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.command.ActualizarProductoHandlerTest"`
Expected: PASS

- [ ] **Step 9: Escribir el test que falla para `ConsultarProductoHandler`**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarProductoHandlerTest.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarProductoQuery;
import com.softprimesolutions.catalogo.application.dto.result.CategoriaResult;
import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.ProductoResult;
import com.softprimesolutions.catalogo.application.port.out.CatalogoReadPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarProductoHandlerTest {

    private static final UUID TENANT_ID = UUID.fromString("172e0f26-a765-46f3-841c-4a11407ccf5b");
    private static final UUID PRODUCTO_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void returnsTheProductWhenItExists() {
        var producto = sampleProducto();
        var readPort = new FakeCatalogoReadPort(Optional.of(producto));
        var handler = new ConsultarProductoHandler(readPort);

        var result = handler.execute(new ConsultarProductoQuery(TENANT_ID, PRODUCTO_ID));

        assertTrue(result.isSuccess());
        assertEquals("Alcohol en gel", result.getOrElse(error -> null).nombre());
    }

    @Test
    void failsWithNotFoundWhenProductDoesNotExist() {
        var readPort = new FakeCatalogoReadPort(Optional.empty());
        var handler = new ConsultarProductoHandler(readPort);

        var result = handler.execute(new ConsultarProductoQuery(TENANT_ID, PRODUCTO_ID));

        assertTrue(result.isFailure());
        assertEquals("CAT_PRODUCTO_NO_ENCONTRADO", result.fold(value -> null, error -> error.code()));
    }

    private static ProductoResult sampleProducto() {
        return new ProductoResult(
                PRODUCTO_ID, TENANT_ID, UUID.randomUUID(), "Alcohol en gel", "PRODUCTO_SANITARIO", null,
                "Frasco", null, 1, null, new BigDecimal("12.50"), null, false, false, null, null, null, null,
                false, false, "ACTIVO", Instant.parse("2026-09-07T10:00:00Z"), null);
    }

    private static final class FakeCatalogoReadPort implements CatalogoReadPort {
        private final Optional<ProductoResult> producto;

        private FakeCatalogoReadPort(Optional<ProductoResult> producto) {
            this.producto = producto;
        }

        @Override
        public List<CategoriaResult> findCategorias(UUID tenantId, String estado) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ProductoResult> findProducto(UUID tenantId, UUID productoId) {
            return producto;
        }

        @Override
        public PaginaResult<ProductoResult> findProductos(
                UUID tenantId, String texto, UUID categoriaId, String tipo, String estado, int page, int size) {
            throw new UnsupportedOperationException();
        }
    }
}
```

- [ ] **Step 10: Ejecutar y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ConsultarProductoHandlerTest"`
Expected: FAIL — `ConsultarProductoHandler` no existe todavía.

- [ ] **Step 11: Crear `ConsultarProductoHandler`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ConsultarProductoHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarProductoQuery;
import com.softprimesolutions.catalogo.application.dto.result.ProductoResult;
import com.softprimesolutions.catalogo.application.port.in.ConsultarProductoUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarProductoHandler implements ConsultarProductoUseCase {

    private final CatalogoReadPort readPort;

    public ConsultarProductoHandler(CatalogoReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<ProductoResult, ApplicationError> execute(ConsultarProductoQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return readPort.findProducto(query.tenantId(), query.productoId())
                .map(Result::<ProductoResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CAT_PRODUCTO_NO_ENCONTRADO", "El producto indicado no existe.", ErrorCategory.NOT_FOUND)));
    }
}
```

- [ ] **Step 12: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ConsultarProductoHandlerTest"`
Expected: PASS

- [ ] **Step 13: Escribir el test que falla para `ListarProductosHandler`**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/query/ListarProductosHandlerTest.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.query.ListarProductosQuery;
import com.softprimesolutions.catalogo.application.dto.result.CategoriaResult;
import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.ProductoResult;
import com.softprimesolutions.catalogo.application.port.out.CatalogoReadPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListarProductosHandlerTest {

    private static final UUID TENANT_ID = UUID.fromString("172e0f26-a765-46f3-841c-4a11407ccf5b");

    @Test
    void returnsAPageOfProductsFromReadPort() {
        var producto = new ProductoResult(
                UUID.randomUUID(), TENANT_ID, UUID.randomUUID(), "Alcohol en gel", "PRODUCTO_SANITARIO", null,
                "Frasco", null, 1, null, new BigDecimal("12.50"), null, false, false, null, null, null, null,
                false, false, "ACTIVO", Instant.parse("2026-09-07T10:00:00Z"), null);
        var page = new PaginaResult<>(List.of(producto), 0, 20, 1);
        var readPort = new FakeCatalogoReadPort(page);
        var handler = new ListarProductosHandler(readPort);

        var result = handler.execute(new ListarProductosQuery(TENANT_ID, null, null, null, null, 0, 20));

        assertTrue(result.isSuccess());
        assertEquals(1, result.getOrElse(error -> null).items().size());
    }

    @Test
    void failsWithValidationErrorWhenPaginationIsInvalid() {
        var readPort = new FakeCatalogoReadPort(new PaginaResult<>(List.of(), 0, 20, 0));
        var handler = new ListarProductosHandler(readPort);

        var result = handler.execute(new ListarProductosQuery(TENANT_ID, null, null, null, null, -1, 20));

        assertTrue(result.isFailure());
        assertEquals("CAT_PAGINACION_INVALIDA", result.fold(value -> null, error -> error.code()));
    }

    private static final class FakeCatalogoReadPort implements CatalogoReadPort {
        private final PaginaResult<ProductoResult> page;

        private FakeCatalogoReadPort(PaginaResult<ProductoResult> page) {
            this.page = page;
        }

        @Override
        public List<CategoriaResult> findCategorias(UUID tenantId, String estado) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ProductoResult> findProducto(UUID tenantId, UUID productoId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PaginaResult<ProductoResult> findProductos(
                UUID tenantId, String texto, UUID categoriaId, String tipo, String estado, int page, int size) {
            return this.page;
        }
    }
}
```

- [ ] **Step 14: Ejecutar y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ListarProductosHandlerTest"`
Expected: FAIL — `ListarProductosHandler` no existe todavía.

- [ ] **Step 15: Crear `ListarProductosHandler`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/query/ListarProductosHandler.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ListarProductosQuery;
import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.ProductoResult;
import com.softprimesolutions.catalogo.application.port.in.ListarProductosUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Map;
import java.util.Objects;

public final class ListarProductosHandler implements ListarProductosUseCase {

    private final CatalogoReadPort readPort;

    public ListarProductosHandler(CatalogoReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<PaginaResult<ProductoResult>, ApplicationError> execute(ListarProductosQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        if (query.page() < 0 || query.size() < 1 || query.size() > 100) {
            return Result.failure(new StandardApplicationError(
                    "CAT_PAGINACION_INVALIDA",
                    "page debe ser mayor o igual a 0 y size debe estar entre 1 y 100.",
                    ErrorCategory.VALIDATION,
                    Map.of("page", query.page(), "size", query.size())));
        }
        return Result.success(readPort.findProductos(
                query.tenantId(), query.texto(), query.categoriaId(), query.tipo(), query.estado(),
                query.page(), query.size()));
    }
}
```

- [ ] **Step 16: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.query.ListarProductosHandlerTest"`
Expected: PASS

- [ ] **Step 17: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/ service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/
git commit -m "feat(catalogo): agregar handlers de crear, actualizar, consultar y listar productos"
```

---

### Task 10: `CatalogoControlService` (activar/desactivar Categoria y Producto)

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlService.java`
- Test: `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlServiceTest.java`

**Interfaces:**
- Consumes: `CatalogoControlUseCase` (Task 7), `CatalogoWritePort` (Task 7), `ClockPort` (shared-application).
- Produces: `CatalogoControlService implements CatalogoControlUseCase`. Usado por Task 14 (controller).

- [ ] **Step 1: Escribir el test que falla**

Crear `service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlServiceTest.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.port.out.CatalogoWritePort;
import com.softprimesolutions.catalogo.domain.model.Categoria;
import com.softprimesolutions.catalogo.domain.model.Producto;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogoControlServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("172e0f26-a765-46f3-841c-4a11407ccf5b");
    private static final UUID CATEGORIA_ID = UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589");
    private static final UUID PRODUCTO_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void changesCategoriaStatusSuccessfully() {
        var writePort = new FakeCatalogoWritePort(true);
        var service = new CatalogoControlService(writePort, () -> Instant.parse("2026-09-07T12:00:00Z"));

        var result = service.changeCategoriaStatus(TENANT_ID, CATEGORIA_ID, "inactiva");

        assertTrue(result.isSuccess());
    }

    @Test
    void rejectsAnInvalidCategoriaStatus() {
        var writePort = new FakeCatalogoWritePort(true);
        var service = new CatalogoControlService(writePort, () -> Instant.parse("2026-09-07T12:00:00Z"));

        var result = service.changeCategoriaStatus(TENANT_ID, CATEGORIA_ID, "SUSPENDIDA");

        assertTrue(result.isFailure());
        assertEquals("CAT_ESTADO_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void reportsNotFoundWhenCategoriaDoesNotExist() {
        var writePort = new FakeCatalogoWritePort(false);
        var service = new CatalogoControlService(writePort, () -> Instant.parse("2026-09-07T12:00:00Z"));

        var result = service.changeCategoriaStatus(TENANT_ID, CATEGORIA_ID, "ACTIVA");

        assertTrue(result.isFailure());
        assertEquals("CAT_CATEGORIA_NO_ENCONTRADA", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void changesProductoStatusSuccessfully() {
        var writePort = new FakeCatalogoWritePort(true);
        var service = new CatalogoControlService(writePort, () -> Instant.parse("2026-09-07T12:00:00Z"));

        var result = service.changeProductoStatus(TENANT_ID, PRODUCTO_ID, "inactivo");

        assertTrue(result.isSuccess());
    }

    @Test
    void reportsNotFoundWhenProductoDoesNotExist() {
        var writePort = new FakeCatalogoWritePort(false);
        var service = new CatalogoControlService(writePort, () -> Instant.parse("2026-09-07T12:00:00Z"));

        var result = service.changeProductoStatus(TENANT_ID, PRODUCTO_ID, "ACTIVO");

        assertTrue(result.isFailure());
        assertEquals("CAT_PRODUCTO_NO_ENCONTRADO", result.fold(value -> null, error -> error.code()));
    }

    private static final class FakeCatalogoWritePort implements CatalogoWritePort {
        private final boolean found;

        private FakeCatalogoWritePort(boolean found) {
            this.found = found;
        }

        @Override
        public SaveCategoriaOutcome save(Categoria categoria) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveProductoOutcome save(Producto producto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean categoriaExists(UUID tenantId, UUID categoriaId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeCategoriaStatus(UUID tenantId, UUID categoriaId, String status, Instant changedAt) {
            return found;
        }

        @Override
        public boolean changeProductoStatus(UUID tenantId, UUID productoId, String status, Instant changedAt) {
            return found;
        }
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.command.CatalogoControlServiceTest"`
Expected: FAIL — `CatalogoControlService` no existe todavía.

- [ ] **Step 3: Crear `CatalogoControlService`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlService.java`:

```java
package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoWritePort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.shared.kernel.result.Unit;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class CatalogoControlService implements CatalogoControlUseCase {

    private static final Set<String> CATEGORIA_STATUSES = Set.of("ACTIVA", "INACTIVA");
    private static final Set<String> PRODUCTO_STATUSES = Set.of("ACTIVO", "INACTIVO");

    private final CatalogoWritePort writePort;
    private final ClockPort clock;

    public CatalogoControlService(CatalogoWritePort writePort, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<Unit, ApplicationError> changeCategoriaStatus(UUID tenantId, UUID categoriaId, String status) {
        var normalized = normalizeStatus(status);
        if (!CATEGORIA_STATUSES.contains(normalized)) return invalidStatus(CATEGORIA_STATUSES);
        return writePort.changeCategoriaStatus(tenantId, categoriaId, normalized, clock.now())
                ? Result.success(Unit.INSTANCE)
                : notFound("CAT_CATEGORIA_NO_ENCONTRADA", "La categoría no existe en el tenant indicado.");
    }

    @Override
    public Result<Unit, ApplicationError> changeProductoStatus(UUID tenantId, UUID productoId, String status) {
        var normalized = normalizeStatus(status);
        if (!PRODUCTO_STATUSES.contains(normalized)) return invalidStatus(PRODUCTO_STATUSES);
        return writePort.changeProductoStatus(tenantId, productoId, normalized, clock.now())
                ? Result.success(Unit.INSTANCE)
                : notFound("CAT_PRODUCTO_NO_ENCONTRADO", "El producto no existe en el tenant indicado.");
    }

    private static String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }

    private static Result<Unit, ApplicationError> invalidStatus(Set<String> allowed) {
        return Result.failure(new StandardApplicationError(
                "CAT_ESTADO_INVALIDO", "El estado indicado no es válido.",
                ErrorCategory.VALIDATION, Map.of("allowed", allowed)));
    }

    private static Result<Unit, ApplicationError> notFound(String code, String message) {
        return Result.failure(new StandardApplicationError(code, message, ErrorCategory.NOT_FOUND));
    }
}
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:test --tests "com.softprimesolutions.catalogo.application.usecase.command.CatalogoControlServiceTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlService.java service-botica/modules/catalogo/src/test/java/com/softprimesolutions/catalogo/application/usecase/command/CatalogoControlServiceTest.java
git commit -m "feat(catalogo): agregar CatalogoControlService para cambio de estado"
```

---

## Fase 4 — Persistencia (JPA escritura, JDBC lectura)

### Task 11: Entidades y repositorios JPA (`CategoriaJpaEntity`, `ProductoJpaEntity`)

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/CategoriaJpaEntity.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/ProductoJpaEntity.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/CategoriaJpaRepository.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/ProductoJpaRepository.java`

**Interfaces:**
- Produces: `CategoriaJpaEntity` (id, uuidPublico, tenantId, nombre, descripcion, estado, createdAt, updatedAt), `ProductoJpaEntity` (id, uuidPublico, tenantId, categoriaId, nombre, tipo, laboratorio, unidadMedida, presentacion, unidadesPorPaquete, codigoBarras, precioVenta, condicionVenta, esGenerico, esGenericoEsencial, grupoTerapeutico, codigoDigemid, principioActivo, concentracion, requiereLote, requiereVencimiento, estado, createdAt, updatedAt), `CategoriaJpaRepository.findByUuidPublico(UUID)`, `existsByTenantIdAndNombre(Long, String)`, `ProductoJpaRepository.findByUuidPublico(UUID)`, `existsByTenantIdAndCodigoBarras(Long, String)`. Usados por Task 12.

No hay test unitario dedicado para entidades JPA (son POJOs de mapeo sin lógica); se validan indirectamente vía Task 12 y el test de integración de Task 17.

- [ ] **Step 1: Crear `CategoriaJpaEntity.java`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/CategoriaJpaEntity.java`:

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
@Table(name = "categoria", schema = "sch_catalogo")
public class CategoriaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected CategoriaJpaEntity() {
    }

    public CategoriaJpaEntity(
            UUID uuidPublico, Long tenantId, String nombre, String descripcion, String estado,
            Instant createdAt, Instant updatedAt) {
        this.uuidPublico = uuidPublico;
        this.tenantId = tenantId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.estado = estado;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getTenantId() { return tenantId; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getEstado() { return estado; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

- [ ] **Step 2: Crear `ProductoJpaEntity.java`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/ProductoJpaEntity.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "producto", schema = "sch_catalogo")
public class ProductoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "categoria_id", nullable = false)
    private Long categoriaId;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(length = 150)
    private String laboratorio;

    @Column(name = "unidad_medida", nullable = false, length = 30)
    private String unidadMedida;

    @Column(length = 150)
    private String presentacion;

    @Column(name = "unidades_por_paquete", nullable = false)
    private int unidadesPorPaquete;

    @Column(name = "codigo_barras", length = 40)
    private String codigoBarras;

    @Column(name = "precio_venta", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioVenta;

    @Column(name = "condicion_venta", length = 20)
    private String condicionVenta;

    @Column(name = "es_generico", nullable = false)
    private boolean esGenerico;

    @Column(name = "es_generico_esencial", nullable = false)
    private boolean esGenericoEsencial;

    @Column(name = "grupo_terapeutico", length = 150)
    private String grupoTerapeutico;

    @Column(name = "codigo_digemid", length = 40)
    private String codigoDigemid;

    @Column(name = "principio_activo", length = 200)
    private String principioActivo;

    @Column(length = 60)
    private String concentracion;

    @Column(name = "requiere_lote", nullable = false)
    private boolean requiereLote;

    @Column(name = "requiere_vencimiento", nullable = false)
    private boolean requiereVencimiento;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected ProductoJpaEntity() {
    }

    public ProductoJpaEntity(
            UUID uuidPublico, Long tenantId, Long categoriaId, String nombre, String tipo, String laboratorio,
            String unidadMedida, String presentacion, int unidadesPorPaquete, String codigoBarras,
            BigDecimal precioVenta, String condicionVenta, boolean esGenerico, boolean esGenericoEsencial,
            String grupoTerapeutico, String codigoDigemid, String principioActivo, String concentracion,
            boolean requiereLote, boolean requiereVencimiento, String estado, Instant createdAt,
            Instant updatedAt) {
        this.uuidPublico = uuidPublico;
        this.tenantId = tenantId;
        this.categoriaId = categoriaId;
        this.nombre = nombre;
        this.tipo = tipo;
        this.laboratorio = laboratorio;
        this.unidadMedida = unidadMedida;
        this.presentacion = presentacion;
        this.unidadesPorPaquete = unidadesPorPaquete;
        this.codigoBarras = codigoBarras;
        this.precioVenta = precioVenta;
        this.condicionVenta = condicionVenta;
        this.esGenerico = esGenerico;
        this.esGenericoEsencial = esGenericoEsencial;
        this.grupoTerapeutico = grupoTerapeutico;
        this.codigoDigemid = codigoDigemid;
        this.principioActivo = principioActivo;
        this.concentracion = concentracion;
        this.requiereLote = requiereLote;
        this.requiereVencimiento = requiereVencimiento;
        this.estado = estado;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getTenantId() { return tenantId; }
    public Long getCategoriaId() { return categoriaId; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public String getLaboratorio() { return laboratorio; }
    public String getUnidadMedida() { return unidadMedida; }
    public String getPresentacion() { return presentacion; }
    public int getUnidadesPorPaquete() { return unidadesPorPaquete; }
    public String getCodigoBarras() { return codigoBarras; }
    public BigDecimal getPrecioVenta() { return precioVenta; }
    public String getCondicionVenta() { return condicionVenta; }
    public boolean isEsGenerico() { return esGenerico; }
    public boolean isEsGenericoEsencial() { return esGenericoEsencial; }
    public String getGrupoTerapeutico() { return grupoTerapeutico; }
    public String getCodigoDigemid() { return codigoDigemid; }
    public String getPrincipioActivo() { return principioActivo; }
    public String getConcentracion() { return concentracion; }
    public boolean isRequiereLote() { return requiereLote; }
    public boolean isRequiereVencimiento() { return requiereVencimiento; }
    public String getEstado() { return estado; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

- [ ] **Step 3: Crear los repositorios JPA**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/CategoriaJpaRepository.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.CategoriaJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaJpaRepository extends JpaRepository<CategoriaJpaEntity, Long> {
    Optional<CategoriaJpaEntity> findByUuidPublico(UUID uuidPublico);
    boolean existsByTenantIdAndNombre(Long tenantId, String nombre);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/ProductoJpaRepository.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.ProductoJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoJpaRepository extends JpaRepository<ProductoJpaEntity, Long> {
    Optional<ProductoJpaEntity> findByUuidPublico(UUID uuidPublico);
    boolean existsByTenantIdAndCodigoBarras(Long tenantId, String codigoBarras);
}
```

- [ ] **Step 4: Compilar el módulo**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/ service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/
git commit -m "feat(catalogo): agregar entidades y repositorios JPA de Categoria y Producto"
```

---

### Task 12: `CatalogoWriteMapper` y `CatalogoJpaWriteAdapter` (implementa `CatalogoWritePort` completo)

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/mapper/CatalogoWriteMapper.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoJpaWriteAdapter.java`

**Interfaces:**
- Consumes: `Categoria`, `Producto` (Task 4-5), `CategoriaJpaEntity`, `ProductoJpaEntity`, `CategoriaJpaRepository`, `ProductoJpaRepository` (Task 11), `CatalogoWritePort` (Task 7).
- Produces: `CatalogoJpaWriteAdapter implements CatalogoWritePort`, registrado como `@Repository`. Usado por Task 16 (wiring de Spring), verificado por Task 17 (test de integración).

No hay test unitario dedicado para el adapter (requiere BD real); se valida vía el test de integración de Task 17. El mapper tampoco requiere test dedicado (mapeo directo sin lógica).

- [ ] **Step 1: Crear `CatalogoWriteMapper`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/mapper/CatalogoWriteMapper.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.mapper;

import com.softprimesolutions.catalogo.domain.model.Categoria;
import com.softprimesolutions.catalogo.domain.model.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.EstadoCategoria;
import com.softprimesolutions.catalogo.domain.model.EstadoProducto;
import com.softprimesolutions.catalogo.domain.model.Producto;
import com.softprimesolutions.catalogo.domain.model.TipoProducto;
import com.softprimesolutions.catalogo.domain.valueobject.CategoriaId;
import com.softprimesolutions.catalogo.domain.valueobject.ProductoId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.CategoriaJpaEntity;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.ProductoJpaEntity;
import java.util.UUID;

public final class CatalogoWriteMapper {

    private CatalogoWriteMapper() {
    }

    public static CategoriaJpaEntity toEntity(Categoria categoria, Long tenantId) {
        return new CategoriaJpaEntity(
                categoria.id().value(), tenantId, categoria.nombre(), categoria.descripcion(),
                categoria.estado().name(), categoria.createdAt(), categoria.updatedAt());
    }

    public static Categoria toDomain(CategoriaJpaEntity entity, UUID tenantUuid) {
        return Categoria.restore(
                new CategoriaId(entity.getUuidPublico()), new TenantId(tenantUuid),
                entity.getNombre(), entity.getDescripcion(), EstadoCategoria.valueOf(entity.getEstado()),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static ProductoJpaEntity toEntity(Producto producto, Long tenantId, Long categoriaId) {
        return new ProductoJpaEntity(
                producto.id().value(), tenantId, categoriaId, producto.nombre(), producto.tipo().name(),
                producto.laboratorio(), producto.unidadMedida(), producto.presentacion(),
                producto.unidadesPorPaquete(), producto.codigoBarras(), producto.precioVenta(),
                producto.condicionVenta() == null ? null : producto.condicionVenta().name(),
                producto.esGenerico(), producto.esGenericoEsencial(), producto.grupoTerapeutico(),
                producto.codigoDigemid(), producto.principioActivo(), producto.concentracion(),
                producto.requiereLote(), producto.requiereVencimiento(), producto.estado().name(),
                producto.createdAt(), producto.updatedAt());
    }

    public static Producto toDomain(ProductoJpaEntity entity, UUID tenantUuid, UUID categoriaUuid) {
        return Producto.restore(
                new ProductoId(entity.getUuidPublico()), new TenantId(tenantUuid),
                new CategoriaId(categoriaUuid), entity.getNombre(), TipoProducto.valueOf(entity.getTipo()),
                entity.getLaboratorio(), entity.getUnidadMedida(), entity.getPresentacion(),
                entity.getUnidadesPorPaquete(), entity.getCodigoBarras(), entity.getPrecioVenta(),
                entity.getCondicionVenta() == null ? null : CondicionVenta.valueOf(entity.getCondicionVenta()),
                entity.isEsGenerico(), entity.isEsGenericoEsencial(), entity.getGrupoTerapeutico(),
                entity.getCodigoDigemid(), entity.getPrincipioActivo(), entity.getConcentracion(),
                entity.isRequiereLote(), entity.isRequiereVencimiento(), EstadoProducto.valueOf(entity.getEstado()),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
```

- [ ] **Step 2: Crear `CatalogoJpaWriteAdapter`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoJpaWriteAdapter.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.adapter;

import com.softprimesolutions.catalogo.application.port.out.CatalogoWritePort;
import com.softprimesolutions.catalogo.domain.model.Categoria;
import com.softprimesolutions.catalogo.domain.model.Producto;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.mapper.CatalogoWriteMapper;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.CategoriaJpaRepository;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.ProductoJpaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CatalogoJpaWriteAdapter implements CatalogoWritePort {

    private final CategoriaJpaRepository categoriaRepository;
    private final ProductoJpaRepository productoRepository;
    private final JdbcClient jdbcClient;

    public CatalogoJpaWriteAdapter(
            CategoriaJpaRepository categoriaRepository,
            ProductoJpaRepository productoRepository,
            JdbcClient jdbcClient) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional
    public SaveCategoriaOutcome save(Categoria categoria) {
        var tenantId = findTenantId(categoria.tenantId().value());
        if (tenantId.isEmpty()) return SaveCategoriaOutcome.TENANT_NOT_FOUND;

        var existing = categoriaRepository.findByUuidPublico(categoria.id().value());
        if (existing.isEmpty()) {
            if (categoriaRepository.existsByTenantIdAndNombre(tenantId.get(), categoria.nombre())) {
                return SaveCategoriaOutcome.DUPLICATE_NAME;
            }
            try {
                categoriaRepository.saveAndFlush(CatalogoWriteMapper.toEntity(categoria, tenantId.get()));
                return SaveCategoriaOutcome.CREATED;
            } catch (DataIntegrityViolationException exception) {
                return SaveCategoriaOutcome.DUPLICATE_NAME;
            }
        }

        var entity = existing.get();
        if (!entity.getNombre().equals(categoria.nombre())
                && categoriaRepository.existsByTenantIdAndNombre(tenantId.get(), categoria.nombre())) {
            return SaveCategoriaOutcome.DUPLICATE_NAME;
        }
        jdbcClient.sql("""
                        UPDATE sch_catalogo.categoria
                           SET nombre = :nombre, descripcion = :descripcion, updated_at = :updatedAt
                         WHERE uuid_publico = :categoriaId
                        """)
                .param("nombre", categoria.nombre())
                .param("descripcion", categoria.descripcion())
                .param("updatedAt", categoria.updatedAt())
                .param("categoriaId", categoria.id().value())
                .update();
        return SaveCategoriaOutcome.UPDATED;
    }

    @Override
    @Transactional
    public SaveProductoOutcome save(Producto producto) {
        var tenantId = findTenantId(producto.tenantId().value());
        if (tenantId.isEmpty()) return SaveProductoOutcome.TENANT_NOT_FOUND;

        var categoriaInternalId = findCategoriaInternalId(producto.categoriaId().value());
        if (categoriaInternalId.isEmpty()) return SaveProductoOutcome.CATEGORIA_NOT_FOUND;

        var existing = productoRepository.findByUuidPublico(producto.id().value());
        if (existing.isEmpty()) {
            if (producto.codigoBarras() != null
                    && productoRepository.existsByTenantIdAndCodigoBarras(tenantId.get(), producto.codigoBarras())) {
                return SaveProductoOutcome.DUPLICATE_BARCODE;
            }
            try {
                productoRepository.saveAndFlush(
                        CatalogoWriteMapper.toEntity(producto, tenantId.get(), categoriaInternalId.get()));
                return SaveProductoOutcome.CREATED;
            } catch (DataIntegrityViolationException exception) {
                return SaveProductoOutcome.DUPLICATE_BARCODE;
            }
        }

        var entity = existing.get();
        var barcodeChanged = producto.codigoBarras() != null && !producto.codigoBarras().equals(entity.getCodigoBarras());
        if (barcodeChanged
                && productoRepository.existsByTenantIdAndCodigoBarras(tenantId.get(), producto.codigoBarras())) {
            return SaveProductoOutcome.DUPLICATE_BARCODE;
        }
        try {
            jdbcClient.sql("""
                            UPDATE sch_catalogo.producto
                               SET categoria_id = :categoriaId, nombre = :nombre, tipo = :tipo,
                                   laboratorio = :laboratorio, unidad_medida = :unidadMedida,
                                   presentacion = :presentacion, unidades_por_paquete = :unidadesPorPaquete,
                                   codigo_barras = :codigoBarras, precio_venta = :precioVenta,
                                   condicion_venta = :condicionVenta, es_generico = :esGenerico,
                                   es_generico_esencial = :esGenericoEsencial,
                                   grupo_terapeutico = :grupoTerapeutico, codigo_digemid = :codigoDigemid,
                                   principio_activo = :principioActivo, concentracion = :concentracion,
                                   requiere_lote = :requiereLote, requiere_vencimiento = :requiereVencimiento,
                                   updated_at = :updatedAt
                             WHERE uuid_publico = :productoId
                            """)
                    .param("categoriaId", categoriaInternalId.get())
                    .param("nombre", producto.nombre())
                    .param("tipo", producto.tipo().name())
                    .param("laboratorio", producto.laboratorio())
                    .param("unidadMedida", producto.unidadMedida())
                    .param("presentacion", producto.presentacion())
                    .param("unidadesPorPaquete", producto.unidadesPorPaquete())
                    .param("codigoBarras", producto.codigoBarras())
                    .param("precioVenta", producto.precioVenta())
                    .param("condicionVenta", producto.condicionVenta() == null ? null : producto.condicionVenta().name())
                    .param("esGenerico", producto.esGenerico())
                    .param("esGenericoEsencial", producto.esGenericoEsencial())
                    .param("grupoTerapeutico", producto.grupoTerapeutico())
                    .param("codigoDigemid", producto.codigoDigemid())
                    .param("principioActivo", producto.principioActivo())
                    .param("concentracion", producto.concentracion())
                    .param("requiereLote", producto.requiereLote())
                    .param("requiereVencimiento", producto.requiereVencimiento())
                    .param("updatedAt", producto.updatedAt())
                    .param("productoId", producto.id().value())
                    .update();
            return SaveProductoOutcome.UPDATED;
        } catch (DataIntegrityViolationException exception) {
            return SaveProductoOutcome.DUPLICATE_BARCODE;
        }
    }

    @Override
    public boolean categoriaExists(UUID tenantId, UUID categoriaId) {
        var tenantInternalId = findTenantId(tenantId);
        if (tenantInternalId.isEmpty()) return false;
        return jdbcClient.sql("""
                        SELECT COUNT(*) FROM sch_catalogo.categoria
                         WHERE tenant_id = :tenantId AND uuid_publico = :categoriaId
                        """)
                .param("tenantId", tenantInternalId.get())
                .param("categoriaId", categoriaId)
                .query(Long.class).single() > 0;
    }

    @Override
    @Transactional
    public boolean changeCategoriaStatus(UUID tenantId, UUID categoriaId, String status, Instant changedAt) {
        var tenantInternalId = findTenantId(tenantId);
        if (tenantInternalId.isEmpty()) return false;
        return jdbcClient.sql("""
                        UPDATE sch_catalogo.categoria
                           SET estado = :status, updated_at = :changedAt
                         WHERE tenant_id = :tenantId AND uuid_publico = :categoriaId
                        """)
                .param("status", status)
                .param("changedAt", changedAt)
                .param("tenantId", tenantInternalId.get())
                .param("categoriaId", categoriaId)
                .update() == 1;
    }

    @Override
    @Transactional
    public boolean changeProductoStatus(UUID tenantId, UUID productoId, String status, Instant changedAt) {
        var tenantInternalId = findTenantId(tenantId);
        if (tenantInternalId.isEmpty()) return false;
        return jdbcClient.sql("""
                        UPDATE sch_catalogo.producto
                           SET estado = :status, updated_at = :changedAt
                         WHERE tenant_id = :tenantId AND uuid_publico = :productoId
                        """)
                .param("status", status)
                .param("changedAt", changedAt)
                .param("tenantId", tenantInternalId.get())
                .param("productoId", productoId)
                .update() == 1;
    }

    private Optional<Long> findTenantId(UUID tenantUuid) {
        if (tenantUuid == null) return Optional.empty();
        return jdbcClient.sql("SELECT id FROM sch_farmacia.tenant WHERE uuid_publico = :tenantUuid")
                .param("tenantUuid", tenantUuid)
                .query(Long.class)
                .optional();
    }

    private Optional<Long> findCategoriaInternalId(UUID categoriaUuid) {
        if (categoriaUuid == null) return Optional.empty();
        return jdbcClient.sql("SELECT id FROM sch_catalogo.categoria WHERE uuid_publico = :categoriaUuid")
                .param("categoriaUuid", categoriaUuid)
                .query(Long.class)
                .optional();
    }
}
```

- [ ] **Step 3: Compilar el módulo**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL. Nota: este paso no ejecuta el adapter contra BD real (eso ocurre en Task 17); solo confirma que el código compila con las firmas correctas del puerto.

- [ ] **Step 4: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/mapper/ service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/
git commit -m "feat(catalogo): agregar CatalogoJpaWriteAdapter y su mapper"
```

---

### Task 13: Read side — `CatalogoJdbcReadRepository`, `CatalogoReadMapper`, `CatalogoJdbcReadAdapter`

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/read/projection/CategoriaProjection.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/read/projection/ProductoProjection.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/read/repository/CatalogoJdbcReadRepository.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/read/mapper/CatalogoReadMapper.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/read/adapter/CatalogoJdbcReadAdapter.java`

**Interfaces:**
- Consumes: `CatalogoReadPort` (Task 7), `CategoriaResult`, `ProductoResult`, `PaginaResult` (Task 6).
- Produces: `CatalogoJdbcReadAdapter implements CatalogoReadPort`, registrado como `@Repository`. Usado por Task 16 (wiring), verificado por Task 17.

No hay test unitario dedicado (requiere BD real); se valida vía el test de integración de Task 17.

- [ ] **Step 1: Crear las projections**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/read/projection/CategoriaProjection.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.read.projection;

import java.time.Instant;
import java.util.UUID;

public record CategoriaProjection(
        UUID id,
        UUID tenantId,
        String nombre,
        String descripcion,
        String estado,
        Instant createdAt,
        Instant updatedAt) {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/read/projection/ProductoProjection.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.read.projection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductoProjection(
        UUID id,
        UUID tenantId,
        UUID categoriaId,
        String nombre,
        String tipo,
        String laboratorio,
        String unidadMedida,
        String presentacion,
        int unidadesPorPaquete,
        String codigoBarras,
        BigDecimal precioVenta,
        String condicionVenta,
        boolean esGenerico,
        boolean esGenericoEsencial,
        String grupoTerapeutico,
        String codigoDigemid,
        String principioActivo,
        String concentracion,
        boolean requiereLote,
        boolean requiereVencimiento,
        String estado,
        Instant createdAt,
        Instant updatedAt) {
}
```

- [ ] **Step 2: Crear `CatalogoJdbcReadRepository`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/read/repository/CatalogoJdbcReadRepository.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.read.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.read.projection.CategoriaProjection;
import com.softprimesolutions.catalogo.infrastructure.persistence.read.projection.ProductoProjection;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogoJdbcReadRepository {

    private final JdbcClient jdbcClient;

    public CatalogoJdbcReadRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<CategoriaProjection> findCategorias(UUID tenantId, String estado) {
        var filter = normalizeStatus(estado);
        return jdbcClient.sql("""
                        SELECT c.uuid_publico, t.uuid_publico AS tenant_uuid, c.nombre, c.descripcion,
                               c.estado, c.created_at, c.updated_at
                          FROM sch_catalogo.categoria c
                          JOIN sch_farmacia.tenant t ON t.id = c.tenant_id
                         WHERE t.uuid_publico = :tenantId
                           AND (:estado = '' OR c.estado = :estado)
                         ORDER BY c.nombre, c.id
                        """)
                .param("tenantId", tenantId)
                .param("estado", filter)
                .query((rs, rowNumber) -> new CategoriaProjection(
                        rs.getObject("uuid_publico", UUID.class),
                        rs.getObject("tenant_uuid", UUID.class),
                        rs.getString("nombre"),
                        rs.getString("descripcion"),
                        rs.getString("estado"),
                        toInstant(rs.getObject("created_at", OffsetDateTime.class)),
                        toInstant(rs.getObject("updated_at", OffsetDateTime.class))))
                .list();
    }

    public Optional<ProductoProjection> findProducto(UUID tenantId, UUID productoId) {
        return jdbcClient.sql(PRODUCTO_SELECT + """
                         WHERE t.uuid_publico = :tenantId AND p.uuid_publico = :productoId
                        """)
                .param("tenantId", tenantId)
                .param("productoId", productoId)
                .query(this::mapProducto)
                .optional();
    }

    public List<ProductoProjection> findProductos(
            UUID tenantId, String texto, UUID categoriaId, String tipo, String estado, int offset, int limit) {
        var filter = normalizeSearch(texto);
        return jdbcClient.sql(PRODUCTO_SELECT + PRODUCTO_FILTER
                        + " ORDER BY p.nombre, p.id LIMIT :limit OFFSET :offset")
                .param("tenantId", tenantId)
                .param("search", filter)
                .param("pattern", '%' + filter + '%')
                .param("categoriaId", categoriaId)
                .param("tipo", tipo == null ? "" : tipo)
                .param("estado", estado == null ? "" : estado)
                .param("limit", limit)
                .param("offset", offset)
                .query(this::mapProducto)
                .list();
    }

    public long countProductos(UUID tenantId, String texto, UUID categoriaId, String tipo, String estado) {
        var filter = normalizeSearch(texto);
        return jdbcClient.sql("SELECT COUNT(*) " + PRODUCTO_FROM + PRODUCTO_FILTER)
                .param("tenantId", tenantId)
                .param("search", filter)
                .param("pattern", '%' + filter + '%')
                .param("categoriaId", categoriaId)
                .param("tipo", tipo == null ? "" : tipo)
                .param("estado", estado == null ? "" : estado)
                .query(Long.class)
                .single();
    }

    private ProductoProjection mapProducto(java.sql.ResultSet rs, int rowNumber) throws java.sql.SQLException {
        return new ProductoProjection(
                rs.getObject("uuid_publico", UUID.class),
                rs.getObject("tenant_uuid", UUID.class),
                rs.getObject("categoria_uuid", UUID.class),
                rs.getString("nombre"),
                rs.getString("tipo"),
                rs.getString("laboratorio"),
                rs.getString("unidad_medida"),
                rs.getString("presentacion"),
                rs.getInt("unidades_por_paquete"),
                rs.getString("codigo_barras"),
                rs.getBigDecimal("precio_venta"),
                rs.getString("condicion_venta"),
                rs.getBoolean("es_generico"),
                rs.getBoolean("es_generico_esencial"),
                rs.getString("grupo_terapeutico"),
                rs.getString("codigo_digemid"),
                rs.getString("principio_activo"),
                rs.getString("concentracion"),
                rs.getBoolean("requiere_lote"),
                rs.getBoolean("requiere_vencimiento"),
                rs.getString("estado"),
                toInstant(rs.getObject("created_at", OffsetDateTime.class)),
                toInstant(rs.getObject("updated_at", OffsetDateTime.class)));
    }

    private static final String PRODUCTO_FROM = """
            FROM sch_catalogo.producto p
            JOIN sch_farmacia.tenant t ON t.id = p.tenant_id
            JOIN sch_catalogo.categoria c ON c.id = p.categoria_id
            """;

    private static final String PRODUCTO_SELECT = "SELECT p.uuid_publico, t.uuid_publico AS tenant_uuid, "
            + "c.uuid_publico AS categoria_uuid, p.nombre, p.tipo, p.laboratorio, p.unidad_medida, "
            + "p.presentacion, p.unidades_por_paquete, p.codigo_barras, p.precio_venta, p.condicion_venta, "
            + "p.es_generico, p.es_generico_esencial, p.grupo_terapeutico, p.codigo_digemid, "
            + "p.principio_activo, p.concentracion, p.requiere_lote, p.requiere_vencimiento, p.estado, "
            + "p.created_at, p.updated_at " + PRODUCTO_FROM;

    private static final String PRODUCTO_FILTER = """
             WHERE t.uuid_publico = :tenantId
               AND (:search = '' OR LOWER(p.nombre) LIKE :pattern OR LOWER(COALESCE(p.codigo_barras, '')) LIKE :pattern)
               AND (:categoriaId IS NULL OR c.uuid_publico = :categoriaId)
               AND (:tipo = '' OR p.tipo = :tipo)
               AND (:estado = '' OR p.estado = :estado)
            """;

    private static String normalizeSearch(String search) {
        return search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }

    private static java.time.Instant toInstant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }
}
```

- [ ] **Step 3: Crear `CatalogoReadMapper`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/read/mapper/CatalogoReadMapper.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.read.mapper;

import com.softprimesolutions.catalogo.application.dto.result.CategoriaResult;
import com.softprimesolutions.catalogo.application.dto.result.ProductoResult;
import com.softprimesolutions.catalogo.infrastructure.persistence.read.projection.CategoriaProjection;
import com.softprimesolutions.catalogo.infrastructure.persistence.read.projection.ProductoProjection;

public final class CatalogoReadMapper {

    private CatalogoReadMapper() {
    }

    public static CategoriaResult toResult(CategoriaProjection projection) {
        return new CategoriaResult(
                projection.id(), projection.tenantId(), projection.nombre(), projection.descripcion(),
                projection.estado(), projection.createdAt(), projection.updatedAt());
    }

    public static ProductoResult toResult(ProductoProjection projection) {
        return new ProductoResult(
                projection.id(), projection.tenantId(), projection.categoriaId(), projection.nombre(),
                projection.tipo(), projection.laboratorio(), projection.unidadMedida(), projection.presentacion(),
                projection.unidadesPorPaquete(), projection.codigoBarras(), projection.precioVenta(),
                projection.condicionVenta(), projection.esGenerico(), projection.esGenericoEsencial(),
                projection.grupoTerapeutico(), projection.codigoDigemid(), projection.principioActivo(),
                projection.concentracion(), projection.requiereLote(), projection.requiereVencimiento(),
                projection.estado(), projection.createdAt(), projection.updatedAt());
    }
}
```

- [ ] **Step 4: Crear `CatalogoJdbcReadAdapter`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/read/adapter/CatalogoJdbcReadAdapter.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.read.adapter;

import com.softprimesolutions.catalogo.application.dto.result.CategoriaResult;
import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.ProductoResult;
import com.softprimesolutions.catalogo.application.port.out.CatalogoReadPort;
import com.softprimesolutions.catalogo.infrastructure.persistence.read.mapper.CatalogoReadMapper;
import com.softprimesolutions.catalogo.infrastructure.persistence.read.repository.CatalogoJdbcReadRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CatalogoJdbcReadAdapter implements CatalogoReadPort {

    private final CatalogoJdbcReadRepository repository;

    public CatalogoJdbcReadAdapter(CatalogoJdbcReadRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResult> findCategorias(UUID tenantId, String estado) {
        return repository.findCategorias(tenantId, estado).stream().map(CatalogoReadMapper::toResult).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductoResult> findProducto(UUID tenantId, UUID productoId) {
        return repository.findProducto(tenantId, productoId).map(CatalogoReadMapper::toResult);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResult<ProductoResult> findProductos(
            UUID tenantId, String texto, UUID categoriaId, String tipo, String estado, int page, int size) {
        var items = repository.findProductos(tenantId, texto, categoriaId, tipo, estado, page * size, size)
                .stream().map(CatalogoReadMapper::toResult).toList();
        return new PaginaResult<>(items, page, size, repository.countProductos(tenantId, texto, categoriaId, tipo, estado));
    }
}
```

- [ ] **Step 5: Compilar el módulo**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/read/
git commit -m "feat(catalogo): agregar read side JDBC de Categoria y Producto"
```

---

## Fase 5 — API REST

### Task 14: DTOs HTTP y `CatalogoApiMapper`

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/request/CrearCategoriaRequest.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/request/ActualizarCategoriaRequest.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/request/CambiarEstadoRequest.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/request/CrearProductoRequest.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/request/ActualizarProductoRequest.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/response/CategoriaResponse.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/response/ProductoResponse.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/response/PaginaResponse.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/mapper/CatalogoApiMapper.java`

**Interfaces:**
- Consumes: DTOs de aplicación (Task 6).
- Produces: todos los DTOs HTTP y `CatalogoApiMapper` (métodos estáticos `toCommand`/`toQuery`/`toResponse`). Usado por Task 15 (controllers).

Son records/clases de mapeo puro — no requieren test dedicado, se validan indirectamente vía Task 17 (test de integración HTTP).

- [ ] **Step 1: Crear los DTOs de `request`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/request/CrearCategoriaRequest.java`:

```java
package com.softprimesolutions.catalogo.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CrearCategoriaRequest(
        @NotNull UUID tenantId,
        @NotBlank @Size(min = 2, max = 100) String nombre,
        @Size(max = 500) String descripcion) {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/request/ActualizarCategoriaRequest.java`:

```java
package com.softprimesolutions.catalogo.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ActualizarCategoriaRequest(
        @NotNull UUID tenantId,
        @NotBlank @Size(min = 2, max = 100) String nombre,
        @Size(max = 500) String descripcion) {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/request/CambiarEstadoRequest.java`:

```java
package com.softprimesolutions.catalogo.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CambiarEstadoRequest(@NotNull UUID tenantId, @NotBlank String status) {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/request/CrearProductoRequest.java`:

```java
package com.softprimesolutions.catalogo.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CrearProductoRequest(
        @NotNull UUID tenantId,
        @NotNull UUID categoriaId,
        @NotBlank @Size(min = 2, max = 200) String nombre,
        @NotBlank String tipo,
        @Size(max = 150) String laboratorio,
        @NotBlank @Size(max = 30) String unidadMedida,
        @Size(max = 150) String presentacion,
        Integer unidadesPorPaquete,
        @Size(max = 40) String codigoBarras,
        @NotNull @DecimalMin(value = "0.01") BigDecimal precioVenta,
        String condicionVenta,
        boolean esGenerico,
        boolean esGenericoEsencial,
        @Size(max = 150) String grupoTerapeutico,
        @Size(max = 40) String codigoDigemid,
        @Size(max = 200) String principioActivo,
        @Size(max = 60) String concentracion,
        boolean requiereLote,
        boolean requiereVencimiento) {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/request/ActualizarProductoRequest.java`:

```java
package com.softprimesolutions.catalogo.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record ActualizarProductoRequest(
        @NotNull UUID tenantId,
        @NotNull UUID categoriaId,
        @NotBlank @Size(min = 2, max = 200) String nombre,
        @NotBlank String tipo,
        @Size(max = 150) String laboratorio,
        @NotBlank @Size(max = 30) String unidadMedida,
        @Size(max = 150) String presentacion,
        Integer unidadesPorPaquete,
        @Size(max = 40) String codigoBarras,
        @NotNull @DecimalMin(value = "0.01") BigDecimal precioVenta,
        String condicionVenta,
        boolean esGenerico,
        boolean esGenericoEsencial,
        @Size(max = 150) String grupoTerapeutico,
        @Size(max = 40) String codigoDigemid,
        @Size(max = 200) String principioActivo,
        @Size(max = 60) String concentracion,
        boolean requiereLote,
        boolean requiereVencimiento) {
}
```

- [ ] **Step 2: Crear los DTOs de `response`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/response/CategoriaResponse.java`:

```java
package com.softprimesolutions.catalogo.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CategoriaResponse(
        UUID id,
        UUID tenantId,
        String nombre,
        String descripcion,
        String estado,
        Instant createdAt,
        Instant updatedAt) {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/response/ProductoResponse.java`:

```java
package com.softprimesolutions.catalogo.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductoResponse(
        UUID id,
        UUID tenantId,
        UUID categoriaId,
        String nombre,
        String tipo,
        String laboratorio,
        String unidadMedida,
        String presentacion,
        int unidadesPorPaquete,
        String codigoBarras,
        BigDecimal precioVenta,
        String condicionVenta,
        boolean esGenerico,
        boolean esGenericoEsencial,
        String grupoTerapeutico,
        String codigoDigemid,
        String principioActivo,
        String concentracion,
        boolean requiereLote,
        boolean requiereVencimiento,
        String estado,
        Instant createdAt,
        Instant updatedAt) {
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/response/PaginaResponse.java`:

```java
package com.softprimesolutions.catalogo.api.dto.response;

import java.util.List;

public record PaginaResponse<T>(List<T> items, int page, int size, long totalElements) {

    public PaginaResponse {
        items = List.copyOf(items);
    }
}
```

- [ ] **Step 3: Crear `CatalogoApiMapper`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/mapper/CatalogoApiMapper.java`:

```java
package com.softprimesolutions.catalogo.api.mapper;

import com.softprimesolutions.catalogo.api.dto.request.ActualizarCategoriaRequest;
import com.softprimesolutions.catalogo.api.dto.request.ActualizarProductoRequest;
import com.softprimesolutions.catalogo.api.dto.request.CrearCategoriaRequest;
import com.softprimesolutions.catalogo.api.dto.request.CrearProductoRequest;
import com.softprimesolutions.catalogo.api.dto.response.CategoriaResponse;
import com.softprimesolutions.catalogo.api.dto.response.PaginaResponse;
import com.softprimesolutions.catalogo.api.dto.response.ProductoResponse;
import com.softprimesolutions.catalogo.application.dto.command.ActualizarCategoriaCommand;
import com.softprimesolutions.catalogo.application.dto.command.ActualizarProductoCommand;
import com.softprimesolutions.catalogo.application.dto.command.CrearCategoriaCommand;
import com.softprimesolutions.catalogo.application.dto.command.CrearProductoCommand;
import com.softprimesolutions.catalogo.application.dto.result.CategoriaResult;
import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.ProductoResult;
import java.util.UUID;

public final class CatalogoApiMapper {

    private CatalogoApiMapper() {
    }

    public static CrearCategoriaCommand toCommand(CrearCategoriaRequest request) {
        return new CrearCategoriaCommand(request.tenantId(), request.nombre(), request.descripcion());
    }

    public static ActualizarCategoriaCommand toCommand(UUID categoriaId, ActualizarCategoriaRequest request) {
        return new ActualizarCategoriaCommand(
                request.tenantId(), categoriaId, request.nombre(), request.descripcion());
    }

    public static CrearProductoCommand toCommand(CrearProductoRequest request) {
        return new CrearProductoCommand(
                request.tenantId(), request.categoriaId(), request.nombre(), request.tipo(),
                request.laboratorio(), request.unidadMedida(), request.presentacion(),
                request.unidadesPorPaquete(), request.codigoBarras(), request.precioVenta(),
                request.condicionVenta(), request.esGenerico(), request.esGenericoEsencial(),
                request.grupoTerapeutico(), request.codigoDigemid(), request.principioActivo(),
                request.concentracion(), request.requiereLote(), request.requiereVencimiento());
    }

    public static ActualizarProductoCommand toCommand(UUID productoId, ActualizarProductoRequest request) {
        return new ActualizarProductoCommand(
                request.tenantId(), productoId, request.categoriaId(), request.nombre(), request.tipo(),
                request.laboratorio(), request.unidadMedida(), request.presentacion(),
                request.unidadesPorPaquete(), request.codigoBarras(), request.precioVenta(),
                request.condicionVenta(), request.esGenerico(), request.esGenericoEsencial(),
                request.grupoTerapeutico(), request.codigoDigemid(), request.principioActivo(),
                request.concentracion(), request.requiereLote(), request.requiereVencimiento());
    }

    public static CategoriaResponse toResponse(CategoriaResult result) {
        return new CategoriaResponse(
                result.id(), result.tenantId(), result.nombre(), result.descripcion(), result.estado(),
                result.createdAt(), result.updatedAt());
    }

    public static ProductoResponse toResponse(ProductoResult result) {
        return new ProductoResponse(
                result.id(), result.tenantId(), result.categoriaId(), result.nombre(), result.tipo(),
                result.laboratorio(), result.unidadMedida(), result.presentacion(), result.unidadesPorPaquete(),
                result.codigoBarras(), result.precioVenta(), result.condicionVenta(), result.esGenerico(),
                result.esGenericoEsencial(), result.grupoTerapeutico(), result.codigoDigemid(),
                result.principioActivo(), result.concentracion(), result.requiereLote(),
                result.requiereVencimiento(), result.estado(), result.createdAt(), result.updatedAt());
    }

    public static PaginaResponse<ProductoResponse> toProductoPage(PaginaResult<ProductoResult> result) {
        return new PaginaResponse<>(
                result.items().stream().map(CatalogoApiMapper::toResponse).toList(),
                result.page(), result.size(), result.totalElements());
    }
}
```

- [ ] **Step 4: Compilar el módulo**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/dto/ service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/mapper/
git commit -m "feat(catalogo): agregar DTOs HTTP y CatalogoApiMapper"
```

---

### Task 15: Controllers REST (`CategoriaController`, `ProductoController`)

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/CatalogoControllerSupport.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/CategoriaController.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/ProductoController.java`

**Interfaces:**
- Consumes: puertos `in` (Task 7), `CatalogoApiMapper` (Task 14), `ApplicationErrorHttpMapper` (shared-web).
- Produces: los tres endpoints REST descritos en el spec. Usados por Task 17 (test de integración).

No hay test unitario dedicado para controllers (MockMvc con contexto Spring completo se prueba en Task 17); estas clases no tienen lógica propia más allá de delegar a los puertos `in`.

- [ ] **Step 1: Crear `CatalogoControllerSupport`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/CatalogoControllerSupport.java`:

```java
package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.web.error.ApplicationErrorHttpMapper;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

final class CatalogoControllerSupport {

    private CatalogoControllerSupport() {
    }

    static ResponseEntity<ProblemDetail> problem(ApplicationError error) {
        var problem = ApplicationErrorHttpMapper.toProblemDetail(error);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }
}
```

- [ ] **Step 2: Crear `CategoriaController`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/CategoriaController.java`:

```java
package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.ActualizarCategoriaRequest;
import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoRequest;
import com.softprimesolutions.catalogo.api.dto.request.CrearCategoriaRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.query.ListarCategoriasQuery;
import com.softprimesolutions.catalogo.application.port.in.ActualizarCategoriaUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearCategoriaUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarCategoriasUseCase;
import jakarta.validation.Valid;
import java.net.URI;
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
public class CategoriaController {

    private final CrearCategoriaUseCase createCategoria;
    private final ActualizarCategoriaUseCase updateCategoria;
    private final ListarCategoriasUseCase listCategorias;
    private final CatalogoControlUseCase control;

    public CategoriaController(
            CrearCategoriaUseCase createCategoria,
            ActualizarCategoriaUseCase updateCategoria,
            ListarCategoriasUseCase listCategorias,
            CatalogoControlUseCase control) {
        this.createCategoria = createCategoria;
        this.updateCategoria = updateCategoria;
        this.listCategorias = listCategorias;
        this.control = control;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalogo.categorias.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody CrearCategoriaRequest request) {
        return createCategoria.execute(CatalogoApiMapper.toCommand(request)).fold(
                result -> ResponseEntity.created(URI.create("/api/v1/catalogo/categorias/" + result.id()))
                        .body(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PutMapping("/{categoriaId}")
    @PreAuthorize("hasAuthority('catalogo.categorias.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable UUID categoriaId, @Valid @RequestBody ActualizarCategoriaRequest request) {
        return updateCategoria.execute(CatalogoApiMapper.toCommand(categoriaId, request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{categoriaId}/estado")
    @PreAuthorize("hasAuthority('catalogo.categorias.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable UUID categoriaId, @Valid @RequestBody CambiarEstadoRequest request) {
        return control.changeCategoriaStatus(request.tenantId(), categoriaId, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(),
                CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.categorias.consultar')")
    public ResponseEntity<?> list(
            @RequestParam UUID tenantId, @RequestParam(required = false) String estado) {
        return listCategorias.execute(new ListarCategoriasQuery(tenantId, estado)).fold(
                result -> ResponseEntity.ok(result.stream().map(CatalogoApiMapper::toResponse).toList()),
                CatalogoControllerSupport::problem);
    }
}
```

- [ ] **Step 3: Crear `ProductoController`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/ProductoController.java`:

```java
package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.ActualizarProductoRequest;
import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoRequest;
import com.softprimesolutions.catalogo.api.dto.request.CrearProductoRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.query.ConsultarProductoQuery;
import com.softprimesolutions.catalogo.application.dto.query.ListarProductosQuery;
import com.softprimesolutions.catalogo.application.port.in.ActualizarProductoUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.ConsultarProductoUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearProductoUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarProductosUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
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
@RequestMapping("/api/v1/catalogo/productos")
public class ProductoController {

    private final CrearProductoUseCase createProducto;
    private final ActualizarProductoUseCase updateProducto;
    private final ConsultarProductoUseCase getProducto;
    private final ListarProductosUseCase listProductos;
    private final CatalogoControlUseCase control;

    public ProductoController(
            CrearProductoUseCase createProducto,
            ActualizarProductoUseCase updateProducto,
            ConsultarProductoUseCase getProducto,
            ListarProductosUseCase listProductos,
            CatalogoControlUseCase control) {
        this.createProducto = createProducto;
        this.updateProducto = updateProducto;
        this.getProducto = getProducto;
        this.listProductos = listProductos;
        this.control = control;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalogo.productos.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody CrearProductoRequest request) {
        return createProducto.execute(CatalogoApiMapper.toCommand(request)).fold(
                result -> ResponseEntity.created(URI.create("/api/v1/catalogo/productos/" + result.id()))
                        .body(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PutMapping("/{productoId}")
    @PreAuthorize("hasAuthority('catalogo.productos.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable UUID productoId, @Valid @RequestBody ActualizarProductoRequest request) {
        return updateProducto.execute(CatalogoApiMapper.toCommand(productoId, request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{productoId}/estado")
    @PreAuthorize("hasAuthority('catalogo.productos.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable UUID productoId, @Valid @RequestBody CambiarEstadoRequest request) {
        return control.changeProductoStatus(request.tenantId(), productoId, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(),
                CatalogoControllerSupport::problem);
    }

    @GetMapping("/{productoId}")
    @PreAuthorize("hasAuthority('catalogo.productos.consultar')")
    public ResponseEntity<?> get(@PathVariable UUID productoId, @RequestParam UUID tenantId) {
        return getProducto.execute(new ConsultarProductoQuery(tenantId, productoId)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.productos.consultar')")
    public ResponseEntity<?> list(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return listProductos.execute(new ListarProductosQuery(tenantId, q, categoriaId, tipo, estado, page, size))
                .fold(
                        result -> ResponseEntity.ok(CatalogoApiMapper.toProductoPage(result)),
                        CatalogoControllerSupport::problem);
    }
}
```

- [ ] **Step 4: Compilar el módulo**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/api/controller/
git commit -m "feat(catalogo): agregar CategoriaController y ProductoController"
```

---
