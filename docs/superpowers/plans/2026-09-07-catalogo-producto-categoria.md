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
