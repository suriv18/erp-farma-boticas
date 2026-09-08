# Módulo Catálogo (BC-CAT) — Persistencia y API REST Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Continuación de:** `docs/superpowers/plans/2026-09-07-catalogo-bc-cat.md` (Tasks 1-16 — migración de permisos, dominio completo, capa de aplicación completa con puertos y handlers). Este documento asume que esas 16 tareas ya están implementadas y comiteadas, y continúa la numeración desde la Task 17.

**Goal:** Completar el módulo `catalogo` con persistencia real (JPA para escritura, JDBC para lectura, contra las tablas ya existentes de `sch_farmacia` vía V003) y API REST con autorización, más el test de integración end-to-end que verifica todo el módulo funcionando junto.

**Architecture:** Los adapters de escritura (`CatalogoSoporteJpaWriteAdapter`, `CatalogoComercialJpaWriteAdapter`, `ProductoReguladoJpaWriteAdapter`) implementan los 3 puertos `out` de escritura ya definidos (Task 10 del documento anterior). El read side (`CatalogoJdbcReadAdapter`) implementa el único `CatalogoReadPort`. Controllers REST (`CategoriaController`... hasta `SkuController`, 6 en total) inyectan los puertos `in` ya definidos. `CatalogoModuleConfiguration` conecta los handlers con Spring.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Data JPA, Spring JDBC (`JdbcClient`), PostgreSQL 18 + Flyway, JUnit 5, MockMvc, Spring Modulith 2.1.

## Global Constraints

- Backend: `cd service-botica && .\gradlew.bat check --warning-mode all` debe pasar (build + tests + ArchUnit + Spring Modulith verify) antes de dar por terminado el trabajo — este comando es el Step final de la Task 25 (última de este documento).
- **Sin navegación JPA entre entidades**: nunca `@ManyToOne`, `@JoinColumn`, `@OneToMany`, `@ElementCollection`. Todas las FKs (incluidas las compuestas `(tenant_id, id)` y auto-referenciales como `categoria_padre_id`) son columnas `Long` planas en la entidad JPA — confirmado contra `AsignacionRolJpaEntity` en `modules/security`, que modela 4 FKs (`empresaId`, `establecimientoId`, `almacenId`, `terminalId`) así, sin ninguna anotación de relación.
- **Colecciones hijas en tabla separada** (principios activos de `ProductoRegulado` vía `producto_principio_activo`; códigos de barra de `SKUComercial` vía `sku_codigo_barra`): se leen/escriben con `JdbcClient` directo en el adapter, patrón "DELETE + re-INSERT" para reemplazo completo de la colección — confirmado contra `IamJpaWriteAdapter.replacePermissions` en `modules/security`, que hace exactamente esto para `rol_permiso`.
- Las 12 tablas reales (`sch_farmacia.condicion_venta`, `forma_farmaceutica`, `via_administracion`, `unidad_medida`, `clasificacion_controlada`, `principio_activo`, `categoria_producto`, `marca`, `producto_regulado`, `producto_principio_activo`, `sku_comercial`, `sku_codigo_barra`) ya existen — no se crea ninguna migración de esquema en este documento (la única migración del módulo, de permisos, ya se hizo en la Task 1 del documento anterior).
- `Marca`, `CategoriaProducto`, `SKUComercial` son tenant-scoped (columna `tenant_id` BIGINT interno, resuelto desde el UUID público de `sch_farmacia.tenant` con el patrón `findTenantId(UUID): Optional<Long>` de `IamJpaWriteAdapter`). `CondicionVenta`, `FormaFarmaceutica`, `ViaAdministracion`, `UnidadMedida`, `ClasificacionControlada`, `PrincipioActivo`, `ProductoRegulado` son catálogos globales — sin resolución de tenant en sus adapters.
- El perfil de test de integración real (Testcontainers+Postgres) usa `@ActiveProfiles("test")` + `@Import(PostgresTestContainerConfiguration.class)` + `@SpringBootTest` (+ `@AutoConfigureMockMvc` para tests HTTP) — copiar exactamente las anotaciones de clase de `IamApiIntegrationTest.java`.
- Autorización HTTP con `@PreAuthorize("hasAuthority(...)")`, usando exactamente los 12 códigos de permiso sembrados en la Task 1 del documento anterior: `catalogo.soporte.{consultar,gestionar}`, `catalogo.principios-activos.{consultar,gestionar}`, `catalogo.marcas.{consultar,gestionar}`, `catalogo.categorias.{consultar,gestionar}`, `catalogo.productos-regulados.{consultar,gestionar}`, `catalogo.skus.{consultar,gestionar}`.
- Endpoints de cambio de estado usan `PATCH .../estado` con body `{"tenantId":..., "status":"..."}` (solo para entidades tenant-scoped) o `{"status":"..."}` (entidades globales) — mismo patrón HTTP que `RolController`/`UsuarioController` en `security`, no dos endpoints separados de activar/desactivar.
- Sin comentarios explicativos en el código salvo invariantes no obvias; Result pattern para errores esperables; nombres en español para el dominio de negocio, inglés para tipos técnicos ya establecidos por el framework.
- No modificar `docs/cadena-farmacias-docs/database/migrations/*.sql` (registro histórico, incluye V003 — solo lectura de referencia para nombres exactos de columna).

---

## Fase 4 — Persistencia (JPA escritura, JDBC lectura)

### Task 17: Entidades y repositorios JPA para las 12 tablas

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/CondicionVentaJpaEntity.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/FormaFarmaceuticaJpaEntity.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/ViaAdministracionJpaEntity.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/UnidadMedidaJpaEntity.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/ClasificacionControladaJpaEntity.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/PrincipioActivoJpaEntity.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/MarcaJpaEntity.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/CategoriaProductoJpaEntity.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/ProductoReguladoJpaEntity.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/ProductoPrincipioActivoJpaEntity.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/SkuComercialJpaEntity.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/SkuCodigoBarraJpaEntity.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/CondicionVentaJpaRepository.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/FormaFarmaceuticaJpaRepository.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/ViaAdministracionJpaRepository.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/UnidadMedidaJpaRepository.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/ClasificacionControladaJpaRepository.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/PrincipioActivoJpaRepository.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/MarcaJpaRepository.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/CategoriaProductoJpaRepository.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/ProductoReguladoJpaRepository.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/ProductoPrincipioActivoJpaRepository.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/SkuComercialJpaRepository.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/SkuCodigoBarraJpaRepository.java`

**Interfaces:**
- Produces: 12 entidades JPA mapeando exactamente las columnas reales de V003 (ver Global Constraints), y 12 repositorios Spring Data con los métodos `findByX`/`existsByX` que necesitarán los adapters de las Tasks 18-20. No hay test unitario dedicado (POJOs de mapeo sin lógica); se validan indirectamente en la Task 25.

- [ ] **Step 1: Crear las 5 entidades JPA de catálogos de soporte (PK `codigo` String)**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/CondicionVentaJpaEntity.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "condicion_venta", schema = "sch_farmacia")
public class CondicionVentaJpaEntity {

    @Id
    @Column(length = 30)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String denominacion;

    @Column(name = "requiere_receta", nullable = false)
    private boolean requiereReceta;

    @Column(name = "requiere_retencion", nullable = false)
    private boolean requiereRetencion;

    @Column(length = 300)
    private String fuente;

    @Column(name = "version_fuente", length = 100)
    private String versionFuente;

    @Column(name = "vigente_desde")
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    @Column(nullable = false, length = 20)
    private String estado;

    protected CondicionVentaJpaEntity() {
    }

    public CondicionVentaJpaEntity(
            String codigo, String denominacion, boolean requiereReceta, boolean requiereRetencion,
            String fuente, String versionFuente, LocalDate vigenteDesde, LocalDate vigenteHasta,
            String estado) {
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

    public String getCodigo() { return codigo; }
    public String getDenominacion() { return denominacion; }
    public boolean isRequiereReceta() { return requiereReceta; }
    public boolean isRequiereRetencion() { return requiereRetencion; }
    public String getFuente() { return fuente; }
    public String getVersionFuente() { return versionFuente; }
    public LocalDate getVigenteDesde() { return vigenteDesde; }
    public LocalDate getVigenteHasta() { return vigenteHasta; }
    public String getEstado() { return estado; }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/FormaFarmaceuticaJpaEntity.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "forma_farmaceutica", schema = "sch_farmacia")
public class FormaFarmaceuticaJpaEntity {

    @Id
    @Column(length = 30)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String denominacion;

    @Column(length = 300)
    private String fuente;

    @Column(nullable = false, length = 20)
    private String estado;

    protected FormaFarmaceuticaJpaEntity() {
    }

    public FormaFarmaceuticaJpaEntity(String codigo, String denominacion, String fuente, String estado) {
        this.codigo = codigo;
        this.denominacion = denominacion;
        this.fuente = fuente;
        this.estado = estado;
    }

    public String getCodigo() { return codigo; }
    public String getDenominacion() { return denominacion; }
    public String getFuente() { return fuente; }
    public String getEstado() { return estado; }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/ViaAdministracionJpaEntity.java` (misma forma exacta que `FormaFarmaceuticaJpaEntity`, cambiando `@Table(name = "via_administracion", ...)` y el nombre de la clase):

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "via_administracion", schema = "sch_farmacia")
public class ViaAdministracionJpaEntity {

    @Id
    @Column(length = 30)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String denominacion;

    @Column(length = 300)
    private String fuente;

    @Column(nullable = false, length = 20)
    private String estado;

    protected ViaAdministracionJpaEntity() {
    }

    public ViaAdministracionJpaEntity(String codigo, String denominacion, String fuente, String estado) {
        this.codigo = codigo;
        this.denominacion = denominacion;
        this.fuente = fuente;
        this.estado = estado;
    }

    public String getCodigo() { return codigo; }
    public String getDenominacion() { return denominacion; }
    public String getFuente() { return fuente; }
    public String getEstado() { return estado; }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/UnidadMedidaJpaEntity.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "unidad_medida", schema = "sch_farmacia")
public class UnidadMedidaJpaEntity {

    @Id
    @Column(length = 30)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String denominacion;

    @Column(length = 30)
    private String simbolo;

    @Column(name = "permite_decimal", nullable = false)
    private boolean permiteDecimal;

    @Column(length = 300)
    private String fuente;

    @Column(nullable = false, length = 20)
    private String estado;

    protected UnidadMedidaJpaEntity() {
    }

    public UnidadMedidaJpaEntity(
            String codigo, String denominacion, String simbolo, boolean permiteDecimal, String fuente,
            String estado) {
        this.codigo = codigo;
        this.denominacion = denominacion;
        this.simbolo = simbolo;
        this.permiteDecimal = permiteDecimal;
        this.fuente = fuente;
        this.estado = estado;
    }

    public String getCodigo() { return codigo; }
    public String getDenominacion() { return denominacion; }
    public String getSimbolo() { return simbolo; }
    public boolean isPermiteDecimal() { return permiteDecimal; }
    public String getFuente() { return fuente; }
    public String getEstado() { return estado; }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/ClasificacionControladaJpaEntity.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clasificacion_controlada", schema = "sch_farmacia")
public class ClasificacionControladaJpaEntity {

    @Id
    @Column(length = 40)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String denominacion;

    @Column(name = "norma_fuente", length = 300)
    private String normaFuente;

    @Column(name = "requiere_receta_especial", nullable = false)
    private boolean requiereRecetaEspecial;

    @Column(name = "retiene_receta", nullable = false)
    private boolean retieneReceta;

    @Column(name = "vigencia_receta_dias")
    private Integer vigenciaRecetaDias;

    @Column(nullable = false, length = 20)
    private String estado;

    protected ClasificacionControladaJpaEntity() {
    }

    public ClasificacionControladaJpaEntity(
            String codigo, String denominacion, String normaFuente, boolean requiereRecetaEspecial,
            boolean retieneReceta, Integer vigenciaRecetaDias, String estado) {
        this.codigo = codigo;
        this.denominacion = denominacion;
        this.normaFuente = normaFuente;
        this.requiereRecetaEspecial = requiereRecetaEspecial;
        this.retieneReceta = retieneReceta;
        this.vigenciaRecetaDias = vigenciaRecetaDias;
        this.estado = estado;
    }

    public String getCodigo() { return codigo; }
    public String getDenominacion() { return denominacion; }
    public String getNormaFuente() { return normaFuente; }
    public boolean isRequiereRecetaEspecial() { return requiereRecetaEspecial; }
    public boolean isRetieneReceta() { return retieneReceta; }
    public Integer getVigenciaRecetaDias() { return vigenciaRecetaDias; }
    public String getEstado() { return estado; }
}
```

- [ ] **Step 2: Crear `PrincipioActivoJpaEntity`, `MarcaJpaEntity`, `CategoriaProductoJpaEntity` (PK BIGINT+UUID)**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/PrincipioActivoJpaEntity.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "principio_activo", schema = "sch_farmacia")
public class PrincipioActivoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "codigo_fuente", length = 80)
    private String codigoFuente;

    @Column(nullable = false, length = 300)
    private String denominacion;

    @Column(name = "nombre_normalizado", length = 300)
    private String nombreNormalizado;

    @Column(length = 300)
    private String fuente;

    @Column(nullable = false, length = 20)
    private String estado;

    protected PrincipioActivoJpaEntity() {
    }

    public PrincipioActivoJpaEntity(
            UUID uuidPublico, String codigoFuente, String denominacion, String nombreNormalizado,
            String fuente, String estado) {
        this.uuidPublico = uuidPublico;
        this.codigoFuente = codigoFuente;
        this.denominacion = denominacion;
        this.nombreNormalizado = nombreNormalizado;
        this.fuente = fuente;
        this.estado = estado;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public String getCodigoFuente() { return codigoFuente; }
    public String getDenominacion() { return denominacion; }
    public String getNombreNormalizado() { return nombreNormalizado; }
    public String getFuente() { return fuente; }
    public String getEstado() { return estado; }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/MarcaJpaEntity.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "marca", schema = "sch_farmacia")
public class MarcaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false, length = 180)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, length = 20)
    private String estado;

    protected MarcaJpaEntity() {
    }

    public MarcaJpaEntity(
            UUID uuidPublico, Long tenantId, String codigo, String nombre, String descripcion, String estado) {
        this.uuidPublico = uuidPublico;
        this.tenantId = tenantId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getTenantId() { return tenantId; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getEstado() { return estado; }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/CategoriaProductoJpaEntity.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "categoria_producto", schema = "sch_farmacia")
public class CategoriaProductoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "categoria_padre_id")
    private Long categoriaPadreId;

    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false, length = 180)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private int nivel;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false, length = 20)
    private String estado;

    protected CategoriaProductoJpaEntity() {
    }

    public CategoriaProductoJpaEntity(
            UUID uuidPublico, Long tenantId, Long categoriaPadreId, String codigo, String nombre,
            String descripcion, int nivel, int orden, String estado) {
        this.uuidPublico = uuidPublico;
        this.tenantId = tenantId;
        this.categoriaPadreId = categoriaPadreId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.nivel = nivel;
        this.orden = orden;
        this.estado = estado;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getTenantId() { return tenantId; }
    public Long getCategoriaPadreId() { return categoriaPadreId; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public int getNivel() { return nivel; }
    public int getOrden() { return orden; }
    public String getEstado() { return estado; }
}
```

- [ ] **Step 3: Crear `ProductoReguladoJpaEntity` y `ProductoPrincipioActivoJpaEntity`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/ProductoReguladoJpaEntity.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "producto_regulado", schema = "sch_farmacia")
public class ProductoReguladoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "tipo_producto", nullable = false, length = 40)
    private String tipoProducto;

    @Column(name = "rubro_codigo", length = 50)
    private String rubroCodigo;

    @Column(name = "tipo_registro", length = 40)
    private String tipoRegistro;

    @Column(name = "numero_registro", length = 100)
    private String numeroRegistro;

    @Column(nullable = false, length = 500)
    private String denominacion;

    @Column(name = "concentracion_texto", length = 300)
    private String concentracionTexto;

    @Column(name = "presentacion_regulatoria", length = 500)
    private String presentacionRegulatoria;

    @Column(name = "forma_farmaceutica_codigo", length = 30)
    private String formaFarmaceuticaCodigo;

    @Column(name = "via_administracion_codigo", length = 30)
    private String viaAdministracionCodigo;

    @Column(name = "unidad_medida_codigo", length = 30)
    private String unidadMedidaCodigo;

    @Column(name = "condicion_venta_codigo", length = 30)
    private String condicionVentaCodigo;

    @Column(name = "clasificacion_atc", length = 30)
    private String clasificacionAtc;

    @Column(name = "clasificacion_controlada_codigo", length = 40)
    private String clasificacionControladaCodigo;

    @Column(name = "tipo_liberacion", length = 40)
    private String tipoLiberacion;

    @Column(name = "origen_fabricacion", length = 40)
    private String origenFabricacion;

    @Column(name = "pais_origen", length = 100)
    private String paisOrigen;

    @Column(name = "subpartida_nacional", length = 30)
    private String subpartidaNacional;

    @Column(name = "titular_registro", length = 300)
    private String titularRegistro;

    @Column(length = 300)
    private String fabricante;

    @Column(length = 300)
    private String importador;

    @Column(name = "establecimiento_expendio", length = 200)
    private String establecimientoExpendio;

    @Column(name = "vigente_desde")
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    @Column(name = "estado_regulatorio", nullable = false, length = 30)
    private String estadoRegulatorio;

    @Column(length = 300)
    private String fuente;

    @Column(name = "version_fuente", length = 100)
    private String versionFuente;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected ProductoReguladoJpaEntity() {
    }

    public ProductoReguladoJpaEntity(
            UUID uuidPublico, String tipoProducto, String rubroCodigo, String tipoRegistro,
            String numeroRegistro, String denominacion, String concentracionTexto,
            String presentacionRegulatoria, String formaFarmaceuticaCodigo, String viaAdministracionCodigo,
            String unidadMedidaCodigo, String condicionVentaCodigo, String clasificacionAtc,
            String clasificacionControladaCodigo, String tipoLiberacion, String origenFabricacion,
            String paisOrigen, String subpartidaNacional, String titularRegistro, String fabricante,
            String importador, String establecimientoExpendio, LocalDate vigenteDesde, LocalDate vigenteHasta,
            String estadoRegulatorio, String fuente, String versionFuente, Instant createdAt,
            Instant updatedAt) {
        this.uuidPublico = uuidPublico;
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
        this.estadoRegulatorio = estadoRegulatorio;
        this.fuente = fuente;
        this.versionFuente = versionFuente;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public String getTipoProducto() { return tipoProducto; }
    public String getRubroCodigo() { return rubroCodigo; }
    public String getTipoRegistro() { return tipoRegistro; }
    public String getNumeroRegistro() { return numeroRegistro; }
    public String getDenominacion() { return denominacion; }
    public String getConcentracionTexto() { return concentracionTexto; }
    public String getPresentacionRegulatoria() { return presentacionRegulatoria; }
    public String getFormaFarmaceuticaCodigo() { return formaFarmaceuticaCodigo; }
    public String getViaAdministracionCodigo() { return viaAdministracionCodigo; }
    public String getUnidadMedidaCodigo() { return unidadMedidaCodigo; }
    public String getCondicionVentaCodigo() { return condicionVentaCodigo; }
    public String getClasificacionAtc() { return clasificacionAtc; }
    public String getClasificacionControladaCodigo() { return clasificacionControladaCodigo; }
    public String getTipoLiberacion() { return tipoLiberacion; }
    public String getOrigenFabricacion() { return origenFabricacion; }
    public String getPaisOrigen() { return paisOrigen; }
    public String getSubpartidaNacional() { return subpartidaNacional; }
    public String getTitularRegistro() { return titularRegistro; }
    public String getFabricante() { return fabricante; }
    public String getImportador() { return importador; }
    public String getEstablecimientoExpendio() { return establecimientoExpendio; }
    public LocalDate getVigenteDesde() { return vigenteDesde; }
    public LocalDate getVigenteHasta() { return vigenteHasta; }
    public String getEstadoRegulatorio() { return estadoRegulatorio; }
    public String getFuente() { return fuente; }
    public String getVersionFuente() { return versionFuente; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/ProductoPrincipioActivoJpaEntity.java` (usa `@IdClass` con PK compuesta, siguiendo `jakarta.persistence` estándar — no hay precedente exacto en `security` para PK compuesta de 2 columnas sin `id` autogenerado, pero es el mapeo natural de la tabla puente real que solo tiene `(producto_regulado_id, principio_activo_id)` como PK):

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "producto_principio_activo", schema = "sch_farmacia")
@IdClass(ProductoPrincipioActivoJpaEntity.Key.class)
public class ProductoPrincipioActivoJpaEntity {

    @Id
    @Column(name = "producto_regulado_id")
    private Long productoReguladoId;

    @Id
    @Column(name = "principio_activo_id")
    private Long principioActivoId;

    @Column(name = "concentracion_texto", length = 200)
    private String concentracionTexto;

    private BigDecimal cantidad;

    @Column(name = "unidad_medida_codigo", length = 30)
    private String unidadMedidaCodigo;

    @Column(name = "es_principal", nullable = false)
    private boolean esPrincipal;

    private short orden;

    protected ProductoPrincipioActivoJpaEntity() {
    }

    public ProductoPrincipioActivoJpaEntity(
            Long productoReguladoId, Long principioActivoId, String concentracionTexto, BigDecimal cantidad,
            String unidadMedidaCodigo, boolean esPrincipal, short orden) {
        this.productoReguladoId = productoReguladoId;
        this.principioActivoId = principioActivoId;
        this.concentracionTexto = concentracionTexto;
        this.cantidad = cantidad;
        this.unidadMedidaCodigo = unidadMedidaCodigo;
        this.esPrincipal = esPrincipal;
        this.orden = orden;
    }

    public Long getProductoReguladoId() { return productoReguladoId; }
    public Long getPrincipioActivoId() { return principioActivoId; }
    public String getConcentracionTexto() { return concentracionTexto; }
    public BigDecimal getCantidad() { return cantidad; }
    public String getUnidadMedidaCodigo() { return unidadMedidaCodigo; }
    public boolean isEsPrincipal() { return esPrincipal; }
    public short getOrden() { return orden; }

    public static final class Key implements Serializable {
        private Long productoReguladoId;
        private Long principioActivoId;

        public Key() {
        }

        public Key(Long productoReguladoId, Long principioActivoId) {
            this.productoReguladoId = productoReguladoId;
            this.principioActivoId = principioActivoId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key key)) return false;
            return Objects.equals(productoReguladoId, key.productoReguladoId)
                    && Objects.equals(principioActivoId, key.principioActivoId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productoReguladoId, principioActivoId);
        }
    }
}
```

- [ ] **Step 4: Crear `SkuComercialJpaEntity` y `SkuCodigoBarraJpaEntity`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/SkuComercialJpaEntity.java`:

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
@Table(name = "sku_comercial", schema = "sch_farmacia")
public class SkuComercialJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "producto_regulado_id")
    private Long productoReguladoId;

    @Column(name = "categoria_id")
    private Long categoriaId;

    @Column(name = "marca_id")
    private Long marcaId;

    @Column(name = "tipo_sku", nullable = false, length = 30)
    private String tipoSku;

    @Column(name = "codigo_interno", nullable = false, length = 60)
    private String codigoInterno;

    @Column(name = "descripcion_comercial", nullable = false, length = 500)
    private String descripcionComercial;

    @Column(name = "nombre_corto", length = 200)
    private String nombreCorto;

    @Column(name = "presentacion_comercial", length = 300)
    private String presentacionComercial;

    @Column(name = "unidad_venta_codigo", length = 30)
    private String unidadVentaCodigo;

    private BigDecimal contenido;

    @Column(name = "unidad_contenido_codigo", length = 30)
    private String unidadContenidoCodigo;

    @Column(name = "peso_gramos")
    private BigDecimal pesoGramos;

    @Column(name = "alto_cm")
    private BigDecimal altoCm;

    @Column(name = "ancho_cm")
    private BigDecimal anchoCm;

    @Column(name = "largo_cm")
    private BigDecimal largoCm;

    @Column(name = "permite_venta_fraccion", nullable = false)
    private boolean permiteVentaFraccion;

    @Column(name = "factor_fraccion")
    private BigDecimal factorFraccion;

    @Column(name = "requiere_lote", nullable = false)
    private boolean requiereLote;

    @Column(name = "requiere_vencimiento", nullable = false)
    private boolean requiereVencimiento;

    @Column(name = "afecto_igv", nullable = false)
    private boolean afectoIgv;

    @Column(name = "stock_minimo_default", nullable = false)
    private BigDecimal stockMinimoDefault;

    @Column(name = "stock_maximo_default")
    private BigDecimal stockMaximoDefault;

    @Column(name = "imagen_uri")
    private String imagenUri;

    @Column(name = "estado_comercial", nullable = false, length = 20)
    private String estadoComercial;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected SkuComercialJpaEntity() {
    }

    public SkuComercialJpaEntity(
            UUID uuidPublico, Long tenantId, Long productoReguladoId, Long categoriaId, Long marcaId,
            String tipoSku, String codigoInterno, String descripcionComercial, String nombreCorto,
            String presentacionComercial, String unidadVentaCodigo, BigDecimal contenido,
            String unidadContenidoCodigo, BigDecimal pesoGramos, BigDecimal altoCm, BigDecimal anchoCm,
            BigDecimal largoCm, boolean permiteVentaFraccion, BigDecimal factorFraccion, boolean requiereLote,
            boolean requiereVencimiento, boolean afectoIgv, BigDecimal stockMinimoDefault,
            BigDecimal stockMaximoDefault, String imagenUri, String estadoComercial, String createdBy,
            Instant createdAt, String updatedBy, Instant updatedAt) {
        this.uuidPublico = uuidPublico;
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
        this.estadoComercial = estadoComercial;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getTenantId() { return tenantId; }
    public Long getProductoReguladoId() { return productoReguladoId; }
    public Long getCategoriaId() { return categoriaId; }
    public Long getMarcaId() { return marcaId; }
    public String getTipoSku() { return tipoSku; }
    public String getCodigoInterno() { return codigoInterno; }
    public String getDescripcionComercial() { return descripcionComercial; }
    public String getNombreCorto() { return nombreCorto; }
    public String getPresentacionComercial() { return presentacionComercial; }
    public String getUnidadVentaCodigo() { return unidadVentaCodigo; }
    public BigDecimal getContenido() { return contenido; }
    public String getUnidadContenidoCodigo() { return unidadContenidoCodigo; }
    public BigDecimal getPesoGramos() { return pesoGramos; }
    public BigDecimal getAltoCm() { return altoCm; }
    public BigDecimal getAnchoCm() { return anchoCm; }
    public BigDecimal getLargoCm() { return largoCm; }
    public boolean isPermiteVentaFraccion() { return permiteVentaFraccion; }
    public BigDecimal getFactorFraccion() { return factorFraccion; }
    public boolean isRequiereLote() { return requiereLote; }
    public boolean isRequiereVencimiento() { return requiereVencimiento; }
    public boolean isAfectoIgv() { return afectoIgv; }
    public BigDecimal getStockMinimoDefault() { return stockMinimoDefault; }
    public BigDecimal getStockMaximoDefault() { return stockMaximoDefault; }
    public String getImagenUri() { return imagenUri; }
    public String getEstadoComercial() { return estadoComercial; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public String getUpdatedBy() { return updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/SkuCodigoBarraJpaEntity.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "sku_codigo_barra", schema = "sch_farmacia")
public class SkuCodigoBarraJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "tipo_codigo", nullable = false, length = 30)
    private String tipoCodigo;

    @Column(name = "codigo_barra", nullable = false, length = 80)
    private String codigoBarra;

    @Column(name = "es_principal", nullable = false)
    private boolean esPrincipal;

    @Column(name = "vigente_desde")
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    @Column(nullable = false, length = 20)
    private String estado;

    protected SkuCodigoBarraJpaEntity() {
    }

    public SkuCodigoBarraJpaEntity(
            Long tenantId, Long skuId, String tipoCodigo, String codigoBarra, boolean esPrincipal,
            LocalDate vigenteDesde, LocalDate vigenteHasta, String estado) {
        this.tenantId = tenantId;
        this.skuId = skuId;
        this.tipoCodigo = tipoCodigo;
        this.codigoBarra = codigoBarra;
        this.esPrincipal = esPrincipal;
        this.vigenteDesde = vigenteDesde;
        this.vigenteHasta = vigenteHasta;
        this.estado = estado;
    }

    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public Long getSkuId() { return skuId; }
    public String getTipoCodigo() { return tipoCodigo; }
    public String getCodigoBarra() { return codigoBarra; }
    public boolean isEsPrincipal() { return esPrincipal; }
    public LocalDate getVigenteDesde() { return vigenteDesde; }
    public LocalDate getVigenteHasta() { return vigenteHasta; }
    public String getEstado() { return estado; }
}
```

- [ ] **Step 5: Crear los 12 repositorios Spring Data**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/CondicionVentaJpaRepository.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.CondicionVentaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CondicionVentaJpaRepository extends JpaRepository<CondicionVentaJpaEntity, String> {
}
```

Crear, con la misma forma (`JpaRepository<XJpaEntity, String>`, sin métodos adicionales — la PK ya es el `codigo` de negocio), los siguientes 4 repositorios:
- `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/FormaFarmaceuticaJpaRepository.java` → `JpaRepository<FormaFarmaceuticaJpaEntity, String>`
- `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/ViaAdministracionJpaRepository.java` → `JpaRepository<ViaAdministracionJpaEntity, String>`
- `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/UnidadMedidaJpaRepository.java` → `JpaRepository<UnidadMedidaJpaEntity, String>`
- `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/ClasificacionControladaJpaRepository.java` → `JpaRepository<ClasificacionControladaJpaEntity, String>`

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/PrincipioActivoJpaRepository.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.PrincipioActivoJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrincipioActivoJpaRepository extends JpaRepository<PrincipioActivoJpaEntity, Long> {
    Optional<PrincipioActivoJpaEntity> findByUuidPublico(UUID uuidPublico);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/MarcaJpaRepository.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.MarcaJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarcaJpaRepository extends JpaRepository<MarcaJpaEntity, Long> {
    Optional<MarcaJpaEntity> findByUuidPublico(UUID uuidPublico);
    boolean existsByTenantIdAndCodigo(Long tenantId, String codigo);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/CategoriaProductoJpaRepository.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.CategoriaProductoJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaProductoJpaRepository extends JpaRepository<CategoriaProductoJpaEntity, Long> {
    Optional<CategoriaProductoJpaEntity> findByUuidPublico(UUID uuidPublico);
    boolean existsByTenantIdAndCodigo(Long tenantId, String codigo);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/ProductoReguladoJpaRepository.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.ProductoReguladoJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoReguladoJpaRepository extends JpaRepository<ProductoReguladoJpaEntity, Long> {
    Optional<ProductoReguladoJpaEntity> findByUuidPublico(UUID uuidPublico);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/ProductoPrincipioActivoJpaRepository.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.ProductoPrincipioActivoJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoPrincipioActivoJpaRepository
        extends JpaRepository<ProductoPrincipioActivoJpaEntity, ProductoPrincipioActivoJpaEntity.Key> {
    List<ProductoPrincipioActivoJpaEntity> findByProductoReguladoId(Long productoReguladoId);
    void deleteByProductoReguladoId(Long productoReguladoId);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/SkuComercialJpaRepository.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.SkuComercialJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkuComercialJpaRepository extends JpaRepository<SkuComercialJpaEntity, Long> {
    Optional<SkuComercialJpaEntity> findByUuidPublico(UUID uuidPublico);
    boolean existsByTenantIdAndCodigoInterno(Long tenantId, String codigoInterno);
}
```

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/SkuCodigoBarraJpaRepository.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.SkuCodigoBarraJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkuCodigoBarraJpaRepository extends JpaRepository<SkuCodigoBarraJpaEntity, Long> {
    List<SkuCodigoBarraJpaEntity> findBySkuId(Long skuId);
    boolean existsByTenantIdAndCodigoBarra(Long tenantId, String codigoBarra);
    void deleteBySkuId(Long skuId);
}
```

- [ ] **Step 6: Compilar el módulo**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/entity/ service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/repository/
git commit -m "feat(catalogo): agregar entidades y repositorios JPA de las 12 tablas"
```

---

### Task 18: `CatalogoSoporteJpaWriteAdapter` (implementa `CatalogoSoportePort` completo)

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/mapper/CatalogoSoporteWriteMapper.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoSoporteJpaWriteAdapter.java`

**Interfaces:**
- Consumes: las 6 entidades de dominio de soporte (Task 3, 4 del documento anterior), sus entidades JPA y repositorios (Task 17), `CatalogoSoportePort` (Task 10 del documento anterior).
- Produces: `CatalogoSoporteJpaWriteAdapter implements CatalogoSoportePort`, registrado como `@Repository`. Usado por Task 24 (wiring), verificado por Task 25.

No hay test unitario dedicado (requiere BD real); se valida vía el test de integración de Task 25.

- [ ] **Step 1: Crear `CatalogoSoporteWriteMapper`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/mapper/CatalogoSoporteWriteMapper.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.mapper;

import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.ClasificacionControladaJpaEntity;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.CondicionVentaJpaEntity;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.FormaFarmaceuticaJpaEntity;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.PrincipioActivoJpaEntity;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.UnidadMedidaJpaEntity;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.ViaAdministracionJpaEntity;

public final class CatalogoSoporteWriteMapper {

    private CatalogoSoporteWriteMapper() {
    }

    public static CondicionVentaJpaEntity toEntity(CondicionVenta condicionVenta) {
        return new CondicionVentaJpaEntity(
                condicionVenta.codigo(), condicionVenta.denominacion(), condicionVenta.requiereReceta(),
                condicionVenta.requiereRetencion(), condicionVenta.fuente(), condicionVenta.versionFuente(),
                condicionVenta.vigenteDesde(), condicionVenta.vigenteHasta(), condicionVenta.estado().name());
    }

    public static FormaFarmaceuticaJpaEntity toEntity(FormaFarmaceutica formaFarmaceutica) {
        return new FormaFarmaceuticaJpaEntity(
                formaFarmaceutica.codigo(), formaFarmaceutica.denominacion(), formaFarmaceutica.fuente(),
                formaFarmaceutica.estado().name());
    }

    public static ViaAdministracionJpaEntity toEntity(ViaAdministracion viaAdministracion) {
        return new ViaAdministracionJpaEntity(
                viaAdministracion.codigo(), viaAdministracion.denominacion(), viaAdministracion.fuente(),
                viaAdministracion.estado().name());
    }

    public static UnidadMedidaJpaEntity toEntity(UnidadMedida unidadMedida) {
        return new UnidadMedidaJpaEntity(
                unidadMedida.codigo(), unidadMedida.denominacion(), unidadMedida.simbolo(),
                unidadMedida.permiteDecimal(), unidadMedida.fuente(), unidadMedida.estado().name());
    }

    public static ClasificacionControladaJpaEntity toEntity(ClasificacionControlada clasificacionControlada) {
        return new ClasificacionControladaJpaEntity(
                clasificacionControlada.codigo(), clasificacionControlada.denominacion(),
                clasificacionControlada.normaFuente(), clasificacionControlada.requiereRecetaEspecial(),
                clasificacionControlada.retieneReceta(), clasificacionControlada.vigenciaRecetaDias(),
                clasificacionControlada.estado().name());
    }

    public static PrincipioActivoJpaEntity toEntity(PrincipioActivo principioActivo) {
        return new PrincipioActivoJpaEntity(
                principioActivo.id().value(), principioActivo.codigoFuente(), principioActivo.denominacion(),
                principioActivo.nombreNormalizado(), principioActivo.fuente(), principioActivo.estado().name());
    }
}
```

- [ ] **Step 2: Crear `CatalogoSoporteJpaWriteAdapter`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoSoporteJpaWriteAdapter.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.adapter;

import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.mapper.CatalogoSoporteWriteMapper;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.ClasificacionControladaJpaRepository;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.CondicionVentaJpaRepository;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.FormaFarmaceuticaJpaRepository;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.PrincipioActivoJpaRepository;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.UnidadMedidaJpaRepository;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.ViaAdministracionJpaRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CatalogoSoporteJpaWriteAdapter implements CatalogoSoportePort {

    private final CondicionVentaJpaRepository condicionVentaRepository;
    private final FormaFarmaceuticaJpaRepository formaFarmaceuticaRepository;
    private final ViaAdministracionJpaRepository viaAdministracionRepository;
    private final UnidadMedidaJpaRepository unidadMedidaRepository;
    private final ClasificacionControladaJpaRepository clasificacionControladaRepository;
    private final PrincipioActivoJpaRepository principioActivoRepository;
    private final JdbcClient jdbcClient;

    public CatalogoSoporteJpaWriteAdapter(
            CondicionVentaJpaRepository condicionVentaRepository,
            FormaFarmaceuticaJpaRepository formaFarmaceuticaRepository,
            ViaAdministracionJpaRepository viaAdministracionRepository,
            UnidadMedidaJpaRepository unidadMedidaRepository,
            ClasificacionControladaJpaRepository clasificacionControladaRepository,
            PrincipioActivoJpaRepository principioActivoRepository,
            JdbcClient jdbcClient) {
        this.condicionVentaRepository = condicionVentaRepository;
        this.formaFarmaceuticaRepository = formaFarmaceuticaRepository;
        this.viaAdministracionRepository = viaAdministracionRepository;
        this.unidadMedidaRepository = unidadMedidaRepository;
        this.clasificacionControladaRepository = clasificacionControladaRepository;
        this.principioActivoRepository = principioActivoRepository;
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional
    public SaveOutcome save(CondicionVenta condicionVenta) {
        var existing = condicionVentaRepository.findById(condicionVenta.codigo());
        if (existing.isEmpty()) {
            try {
                condicionVentaRepository.saveAndFlush(CatalogoSoporteWriteMapper.toEntity(condicionVenta));
                return SaveOutcome.CREATED;
            } catch (DataIntegrityViolationException exception) {
                return SaveOutcome.DUPLICATE_CODIGO;
            }
        }
        jdbcClient.sql("""
                        UPDATE sch_farmacia.condicion_venta
                           SET denominacion = :denominacion, requiere_receta = :requiereReceta,
                               requiere_retencion = :requiereRetencion, fuente = :fuente,
                               version_fuente = :versionFuente, vigente_desde = :vigenteDesde,
                               vigente_hasta = :vigenteHasta
                         WHERE codigo = :codigo
                        """)
                .param("denominacion", condicionVenta.denominacion())
                .param("requiereReceta", condicionVenta.requiereReceta())
                .param("requiereRetencion", condicionVenta.requiereRetencion())
                .param("fuente", condicionVenta.fuente())
                .param("versionFuente", condicionVenta.versionFuente())
                .param("vigenteDesde", condicionVenta.vigenteDesde())
                .param("vigenteHasta", condicionVenta.vigenteHasta())
                .param("codigo", condicionVenta.codigo())
                .update();
        return SaveOutcome.UPDATED;
    }

    @Override
    @Transactional
    public SaveOutcome save(FormaFarmaceutica formaFarmaceutica) {
        var existing = formaFarmaceuticaRepository.findById(formaFarmaceutica.codigo());
        if (existing.isEmpty()) {
            try {
                formaFarmaceuticaRepository.saveAndFlush(CatalogoSoporteWriteMapper.toEntity(formaFarmaceutica));
                return SaveOutcome.CREATED;
            } catch (DataIntegrityViolationException exception) {
                return SaveOutcome.DUPLICATE_CODIGO;
            }
        }
        jdbcClient.sql("""
                        UPDATE sch_farmacia.forma_farmaceutica SET denominacion = :denominacion, fuente = :fuente
                         WHERE codigo = :codigo
                        """)
                .param("denominacion", formaFarmaceutica.denominacion())
                .param("fuente", formaFarmaceutica.fuente())
                .param("codigo", formaFarmaceutica.codigo())
                .update();
        return SaveOutcome.UPDATED;
    }

    @Override
    @Transactional
    public SaveOutcome save(ViaAdministracion viaAdministracion) {
        var existing = viaAdministracionRepository.findById(viaAdministracion.codigo());
        if (existing.isEmpty()) {
            try {
                viaAdministracionRepository.saveAndFlush(CatalogoSoporteWriteMapper.toEntity(viaAdministracion));
                return SaveOutcome.CREATED;
            } catch (DataIntegrityViolationException exception) {
                return SaveOutcome.DUPLICATE_CODIGO;
            }
        }
        jdbcClient.sql("""
                        UPDATE sch_farmacia.via_administracion SET denominacion = :denominacion, fuente = :fuente
                         WHERE codigo = :codigo
                        """)
                .param("denominacion", viaAdministracion.denominacion())
                .param("fuente", viaAdministracion.fuente())
                .param("codigo", viaAdministracion.codigo())
                .update();
        return SaveOutcome.UPDATED;
    }

    @Override
    @Transactional
    public SaveOutcome save(UnidadMedida unidadMedida) {
        var existing = unidadMedidaRepository.findById(unidadMedida.codigo());
        if (existing.isEmpty()) {
            try {
                unidadMedidaRepository.saveAndFlush(CatalogoSoporteWriteMapper.toEntity(unidadMedida));
                return SaveOutcome.CREATED;
            } catch (DataIntegrityViolationException exception) {
                return SaveOutcome.DUPLICATE_CODIGO;
            }
        }
        jdbcClient.sql("""
                        UPDATE sch_farmacia.unidad_medida
                           SET denominacion = :denominacion, simbolo = :simbolo,
                               permite_decimal = :permiteDecimal, fuente = :fuente
                         WHERE codigo = :codigo
                        """)
                .param("denominacion", unidadMedida.denominacion())
                .param("simbolo", unidadMedida.simbolo())
                .param("permiteDecimal", unidadMedida.permiteDecimal())
                .param("fuente", unidadMedida.fuente())
                .param("codigo", unidadMedida.codigo())
                .update();
        return SaveOutcome.UPDATED;
    }

    @Override
    @Transactional
    public SaveOutcome save(ClasificacionControlada clasificacionControlada) {
        var existing = clasificacionControladaRepository.findById(clasificacionControlada.codigo());
        if (existing.isEmpty()) {
            try {
                clasificacionControladaRepository.saveAndFlush(
                        CatalogoSoporteWriteMapper.toEntity(clasificacionControlada));
                return SaveOutcome.CREATED;
            } catch (DataIntegrityViolationException exception) {
                return SaveOutcome.DUPLICATE_CODIGO;
            }
        }
        jdbcClient.sql("""
                        UPDATE sch_farmacia.clasificacion_controlada
                           SET denominacion = :denominacion, norma_fuente = :normaFuente,
                               requiere_receta_especial = :requiereRecetaEspecial,
                               retiene_receta = :retieneReceta, vigencia_receta_dias = :vigenciaRecetaDias
                         WHERE codigo = :codigo
                        """)
                .param("denominacion", clasificacionControlada.denominacion())
                .param("normaFuente", clasificacionControlada.normaFuente())
                .param("requiereRecetaEspecial", clasificacionControlada.requiereRecetaEspecial())
                .param("retieneReceta", clasificacionControlada.retieneReceta())
                .param("vigenciaRecetaDias", clasificacionControlada.vigenciaRecetaDias())
                .param("codigo", clasificacionControlada.codigo())
                .update();
        return SaveOutcome.UPDATED;
    }

    @Override
    @Transactional
    public SavePrincipioActivoOutcome save(PrincipioActivo principioActivo) {
        var existing = principioActivoRepository.findByUuidPublico(principioActivo.id().value());
        if (existing.isEmpty()) {
            principioActivoRepository.saveAndFlush(CatalogoSoporteWriteMapper.toEntity(principioActivo));
            return SavePrincipioActivoOutcome.CREATED;
        }
        jdbcClient.sql("""
                        UPDATE sch_farmacia.principio_activo
                           SET codigo_fuente = :codigoFuente, denominacion = :denominacion,
                               nombre_normalizado = :nombreNormalizado, fuente = :fuente
                         WHERE uuid_publico = :principioActivoId
                        """)
                .param("codigoFuente", principioActivo.codigoFuente())
                .param("denominacion", principioActivo.denominacion())
                .param("nombreNormalizado", principioActivo.nombreNormalizado())
                .param("fuente", principioActivo.fuente())
                .param("principioActivoId", principioActivo.id().value())
                .update();
        return SavePrincipioActivoOutcome.UPDATED;
    }

    @Override
    public boolean condicionVentaExists(String codigo) {
        return condicionVentaRepository.existsById(codigo);
    }

    @Override
    public boolean formaFarmaceuticaExists(String codigo) {
        return formaFarmaceuticaRepository.existsById(codigo);
    }

    @Override
    public boolean viaAdministracionExists(String codigo) {
        return viaAdministracionRepository.existsById(codigo);
    }

    @Override
    public boolean unidadMedidaExists(String codigo) {
        return unidadMedidaRepository.existsById(codigo);
    }

    @Override
    public boolean clasificacionControladaExists(String codigo) {
        return clasificacionControladaRepository.existsById(codigo);
    }

    @Override
    public boolean principioActivoExists(UUID principioActivoId) {
        return principioActivoRepository.findByUuidPublico(principioActivoId).isPresent();
    }

    @Override
    @Transactional
    public boolean changeCondicionVentaStatus(String codigo, String status, Instant changedAt) {
        return jdbcClient.sql("UPDATE sch_farmacia.condicion_venta SET estado = :status WHERE codigo = :codigo")
                .param("status", status).param("codigo", codigo).update() == 1;
    }

    @Override
    @Transactional
    public boolean changeFormaFarmaceuticaStatus(String codigo, String status, Instant changedAt) {
        return jdbcClient.sql("UPDATE sch_farmacia.forma_farmaceutica SET estado = :status WHERE codigo = :codigo")
                .param("status", status).param("codigo", codigo).update() == 1;
    }

    @Override
    @Transactional
    public boolean changeViaAdministracionStatus(String codigo, String status, Instant changedAt) {
        return jdbcClient.sql("UPDATE sch_farmacia.via_administracion SET estado = :status WHERE codigo = :codigo")
                .param("status", status).param("codigo", codigo).update() == 1;
    }

    @Override
    @Transactional
    public boolean changeUnidadMedidaStatus(String codigo, String status, Instant changedAt) {
        return jdbcClient.sql("UPDATE sch_farmacia.unidad_medida SET estado = :status WHERE codigo = :codigo")
                .param("status", status).param("codigo", codigo).update() == 1;
    }

    @Override
    @Transactional
    public boolean changeClasificacionControladaStatus(String codigo, String status, Instant changedAt) {
        return jdbcClient.sql("UPDATE sch_farmacia.clasificacion_controlada SET estado = :status WHERE codigo = :codigo")
                .param("status", status).param("codigo", codigo).update() == 1;
    }

    @Override
    @Transactional
    public boolean changePrincipioActivoStatus(UUID principioActivoId, String status, Instant changedAt) {
        return jdbcClient.sql("UPDATE sch_farmacia.principio_activo SET estado = :status WHERE uuid_publico = :principioActivoId")
                .param("status", status).param("principioActivoId", principioActivoId).update() == 1;
    }
}
```

- [ ] **Step 3: Compilar el módulo**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/mapper/CatalogoSoporteWriteMapper.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoSoporteJpaWriteAdapter.java
git commit -m "feat(catalogo): agregar CatalogoSoporteJpaWriteAdapter"
```

---

### Task 19: `CatalogoComercialJpaWriteAdapter` (Marca, CategoriaProducto, SKUComercial + códigos de barra)

**Files:**
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/mapper/CatalogoComercialWriteMapper.java`
- Create: `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoComercialJpaWriteAdapter.java`

**Interfaces:**
- Consumes: `Marca`, `CategoriaProducto`, `SKUComercial`, `CodigoBarraSku` (Task 5, 6, 8 del documento anterior), sus entidades JPA y repositorios (Task 17), `CatalogoComercialPort` (Task 10/14 del documento anterior, con `findSkuById` agregado en la Task 14).
- Produces: `CatalogoComercialJpaWriteAdapter implements CatalogoComercialPort`, registrado como `@Repository`. Usado por Task 24 (wiring), verificado por Task 25.

- [ ] **Step 1: Crear `CatalogoComercialWriteMapper`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/mapper/CatalogoComercialWriteMapper.java`:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.mapper;

import com.softprimesolutions.catalogo.domain.model.CategoriaProducto;
import com.softprimesolutions.catalogo.domain.model.Marca;
import com.softprimesolutions.catalogo.domain.model.SKUComercial;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.CategoriaProductoJpaEntity;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.MarcaJpaEntity;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.SkuComercialJpaEntity;

public final class CatalogoComercialWriteMapper {

    private CatalogoComercialWriteMapper() {
    }

    public static MarcaJpaEntity toEntity(Marca marca, Long tenantId) {
        return new MarcaJpaEntity(
                marca.id().value(), tenantId, marca.codigo(), marca.nombre(), marca.descripcion(),
                marca.estado().name());
    }

    public static CategoriaProductoJpaEntity toEntity(
            CategoriaProducto categoria, Long tenantId, Long categoriaPadreId) {
        return new CategoriaProductoJpaEntity(
                categoria.id().value(), tenantId, categoriaPadreId, categoria.codigo(), categoria.nombre(),
                categoria.descripcion(), categoria.nivel(), categoria.orden(), categoria.estado().name());
    }

    public static SkuComercialJpaEntity toEntity(
            SKUComercial sku, Long tenantId, Long productoReguladoId, Long categoriaId, Long marcaId) {
        return new SkuComercialJpaEntity(
                sku.id().value(), tenantId, productoReguladoId, categoriaId, marcaId, sku.tipoSku().name(),
                sku.codigoInterno(), sku.descripcionComercial(), sku.nombreCorto(), sku.presentacionComercial(),
                sku.unidadVentaCodigo(), sku.contenido(), sku.unidadContenidoCodigo(), sku.pesoGramos(),
                sku.altoCm(), sku.anchoCm(), sku.largoCm(), sku.permiteVentaFraccion(), sku.factorFraccion(),
                sku.requiereLote(), sku.requiereVencimiento(), sku.afectoIgv(), sku.stockMinimoDefault(),
                sku.stockMaximoDefault(), sku.imagenUri(), sku.estado().name(), sku.createdBy(),
                sku.createdAt(), sku.updatedBy(), sku.updatedAt());
    }
}
```

- [ ] **Step 2: Crear `CatalogoComercialJpaWriteAdapter` — parte 1: constructor, `save(Marca)`, `save(CategoriaProducto)`**

Crear `service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoComercialJpaWriteAdapter.java`, empezando con:

```java
package com.softprimesolutions.catalogo.infrastructure.persistence.write.adapter;

import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.catalogo.domain.model.CategoriaProducto;
import com.softprimesolutions.catalogo.domain.model.CodigoBarraSku;
import com.softprimesolutions.catalogo.domain.model.Marca;
import com.softprimesolutions.catalogo.domain.model.SKUComercial;
import com.softprimesolutions.catalogo.domain.model.soporte.EstadoCatalogoSoporte;
import com.softprimesolutions.catalogo.domain.valueobject.CategoriaProductoId;
import com.softprimesolutions.catalogo.domain.valueobject.MarcaId;
import com.softprimesolutions.catalogo.domain.valueobject.ProductoReguladoId;
import com.softprimesolutions.catalogo.domain.valueobject.SkuId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.catalogo.domain.model.TipoSku;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.SkuCodigoBarraJpaEntity;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.mapper.CatalogoComercialWriteMapper;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.CategoriaProductoJpaRepository;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.MarcaJpaRepository;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.SkuCodigoBarraJpaRepository;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.SkuComercialJpaRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CatalogoComercialJpaWriteAdapter implements CatalogoComercialPort {

    private final MarcaJpaRepository marcaRepository;
    private final CategoriaProductoJpaRepository categoriaRepository;
    private final SkuComercialJpaRepository skuRepository;
    private final SkuCodigoBarraJpaRepository codigoBarraRepository;
    private final JdbcClient jdbcClient;

    public CatalogoComercialJpaWriteAdapter(
            MarcaJpaRepository marcaRepository,
            CategoriaProductoJpaRepository categoriaRepository,
            SkuComercialJpaRepository skuRepository,
            SkuCodigoBarraJpaRepository codigoBarraRepository,
            JdbcClient jdbcClient) {
        this.marcaRepository = marcaRepository;
        this.categoriaRepository = categoriaRepository;
        this.skuRepository = skuRepository;
        this.codigoBarraRepository = codigoBarraRepository;
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional
    public SaveMarcaOutcome save(Marca marca) {
        var tenantId = findTenantId(marca.tenantId() == null ? null : marca.tenantId().value());
        if (tenantId.isEmpty()) return SaveMarcaOutcome.TENANT_NOT_FOUND;

        var existing = marcaRepository.findByUuidPublico(marca.id().value());
        if (existing.isEmpty()) {
            if (marcaRepository.existsByTenantIdAndCodigo(tenantId.get(), marca.codigo())) {
                return SaveMarcaOutcome.DUPLICATE_CODIGO;
            }
            try {
                marcaRepository.saveAndFlush(CatalogoComercialWriteMapper.toEntity(marca, tenantId.get()));
                return SaveMarcaOutcome.CREATED;
            } catch (DataIntegrityViolationException exception) {
                return SaveMarcaOutcome.DUPLICATE_CODIGO;
            }
        }

        var entity = existing.get();
        if (!entity.getCodigo().equals(marca.codigo())
                && marcaRepository.existsByTenantIdAndCodigo(tenantId.get(), marca.codigo())) {
            return SaveMarcaOutcome.DUPLICATE_CODIGO;
        }
        jdbcClient.sql("""
                        UPDATE sch_farmacia.marca SET codigo = :codigo, nombre = :nombre, descripcion = :descripcion
                         WHERE uuid_publico = :marcaId
                        """)
                .param("codigo", marca.codigo())
                .param("nombre", marca.nombre())
                .param("descripcion", marca.descripcion())
                .param("marcaId", marca.id().value())
                .update();
        return SaveMarcaOutcome.UPDATED;
    }

    @Override
    @Transactional
    public SaveCategoriaOutcome save(CategoriaProducto categoria) {
        var tenantId = findTenantId(categoria.tenantId() == null ? null : categoria.tenantId().value());
        if (tenantId.isEmpty()) return SaveCategoriaOutcome.TENANT_NOT_FOUND;

        Long categoriaPadreInternalId = null;
        if (categoria.categoriaPadreId() != null) {
            var padreInternalId = findCategoriaInternalId(categoria.categoriaPadreId().value());
            if (padreInternalId.isEmpty()) return SaveCategoriaOutcome.CATEGORIA_PADRE_NOT_FOUND;
            categoriaPadreInternalId = padreInternalId.get();
        }

        var existing = categoriaRepository.findByUuidPublico(categoria.id().value());
        if (existing.isEmpty()) {
            if (categoriaRepository.existsByTenantIdAndCodigo(tenantId.get(), categoria.codigo())) {
                return SaveCategoriaOutcome.DUPLICATE_CODIGO;
            }
            try {
                categoriaRepository.saveAndFlush(CatalogoComercialWriteMapper.toEntity(
                        categoria, tenantId.get(), categoriaPadreInternalId));
                return SaveCategoriaOutcome.CREATED;
            } catch (DataIntegrityViolationException exception) {
                return SaveCategoriaOutcome.DUPLICATE_CODIGO;
            }
        }

        var entity = existing.get();
        if (!entity.getCodigo().equals(categoria.codigo())
                && categoriaRepository.existsByTenantIdAndCodigo(tenantId.get(), categoria.codigo())) {
            return SaveCategoriaOutcome.DUPLICATE_CODIGO;
        }
        jdbcClient.sql("""
                        UPDATE sch_farmacia.categoria_producto
                           SET categoria_padre_id = :categoriaPadreId, codigo = :codigo, nombre = :nombre,
                               descripcion = :descripcion, nivel = :nivel, orden = :orden
                         WHERE uuid_publico = :categoriaId
                        """)
                .param("categoriaPadreId", categoriaPadreInternalId)
                .param("codigo", categoria.codigo())
                .param("nombre", categoria.nombre())
                .param("descripcion", categoria.descripcion())
                .param("nivel", categoria.nivel())
                .param("orden", categoria.orden())
                .param("categoriaId", categoria.id().value())
                .update();
        return SaveCategoriaOutcome.UPDATED;
    }
```

- [ ] **Step 3: Continuar el mismo archivo — `save(SKUComercial)` y `findSkuById`**

Añadir, dentro de la misma clase `CatalogoComercialJpaWriteAdapter`:

```java
    @Override
    @Transactional
    public SaveSkuOutcome save(SKUComercial sku) {
        var tenantId = findTenantId(sku.tenantId() == null ? null : sku.tenantId().value());
        if (tenantId.isEmpty()) return SaveSkuOutcome.TENANT_NOT_FOUND;

        Long productoReguladoInternalId = null;
        if (sku.productoReguladoId() != null) {
            var internalId = findProductoReguladoInternalId(sku.productoReguladoId().value());
            if (internalId.isEmpty()) return SaveSkuOutcome.PRODUCTO_REGULADO_NOT_FOUND;
            productoReguladoInternalId = internalId.get();
        }

        Long categoriaInternalId = null;
        if (sku.categoriaId() != null) {
            var internalId = findCategoriaInternalId(sku.categoriaId().value());
            if (internalId.isEmpty()) return SaveSkuOutcome.CATEGORIA_NOT_FOUND;
            categoriaInternalId = internalId.get();
        }

        Long marcaInternalId = null;
        if (sku.marcaId() != null) {
            var internalId = findMarcaInternalId(sku.marcaId().value());
            if (internalId.isEmpty()) return SaveSkuOutcome.MARCA_NOT_FOUND;
            marcaInternalId = internalId.get();
        }

        var existing = skuRepository.findByUuidPublico(sku.id().value());
        Long skuInternalId;
        if (existing.isEmpty()) {
            if (skuRepository.existsByTenantIdAndCodigoInterno(tenantId.get(), sku.codigoInterno())) {
                return SaveSkuOutcome.DUPLICATE_CODIGO_INTERNO;
            }
            try {
                var saved = skuRepository.saveAndFlush(CatalogoComercialWriteMapper.toEntity(
                        sku, tenantId.get(), productoReguladoInternalId, categoriaInternalId, marcaInternalId));
                skuInternalId = saved.getId();
            } catch (DataIntegrityViolationException exception) {
                return SaveSkuOutcome.DUPLICATE_CODIGO_INTERNO;
            }
        } else {
            var entity = existing.get();
            if (!entity.getCodigoInterno().equals(sku.codigoInterno())
                    && skuRepository.existsByTenantIdAndCodigoInterno(tenantId.get(), sku.codigoInterno())) {
                return SaveSkuOutcome.DUPLICATE_CODIGO_INTERNO;
            }
            jdbcClient.sql("""
                            UPDATE sch_farmacia.sku_comercial
                               SET producto_regulado_id = :productoReguladoId, categoria_id = :categoriaId,
                                   marca_id = :marcaId, tipo_sku = :tipoSku, codigo_interno = :codigoInterno,
                                   descripcion_comercial = :descripcionComercial, nombre_corto = :nombreCorto,
                                   presentacion_comercial = :presentacionComercial,
                                   unidad_venta_codigo = :unidadVentaCodigo, contenido = :contenido,
                                   unidad_contenido_codigo = :unidadContenidoCodigo, peso_gramos = :pesoGramos,
                                   alto_cm = :altoCm, ancho_cm = :anchoCm, largo_cm = :largoCm,
                                   permite_venta_fraccion = :permiteVentaFraccion,
                                   factor_fraccion = :factorFraccion, requiere_lote = :requiereLote,
                                   requiere_vencimiento = :requiereVencimiento, afecto_igv = :afectoIgv,
                                   stock_minimo_default = :stockMinimoDefault,
                                   stock_maximo_default = :stockMaximoDefault, imagen_uri = :imagenUri,
                                   updated_by = :updatedBy, updated_at = :updatedAt
                             WHERE uuid_publico = :skuId
                            """)
                    .param("productoReguladoId", productoReguladoInternalId)
                    .param("categoriaId", categoriaInternalId)
                    .param("marcaId", marcaInternalId)
                    .param("tipoSku", sku.tipoSku().name())
                    .param("codigoInterno", sku.codigoInterno())
                    .param("descripcionComercial", sku.descripcionComercial())
                    .param("nombreCorto", sku.nombreCorto())
                    .param("presentacionComercial", sku.presentacionComercial())
                    .param("unidadVentaCodigo", sku.unidadVentaCodigo())
                    .param("contenido", sku.contenido())
                    .param("unidadContenidoCodigo", sku.unidadContenidoCodigo())
                    .param("pesoGramos", sku.pesoGramos())
                    .param("altoCm", sku.altoCm())
                    .param("anchoCm", sku.anchoCm())
                    .param("largoCm", sku.largoCm())
                    .param("permiteVentaFraccion", sku.permiteVentaFraccion())
                    .param("factorFraccion", sku.factorFraccion())
                    .param("requiereLote", sku.requiereLote())
                    .param("requiereVencimiento", sku.requiereVencimiento())
                    .param("afectoIgv", sku.afectoIgv())
                    .param("stockMinimoDefault", sku.stockMinimoDefault())
                    .param("stockMaximoDefault", sku.stockMaximoDefault())
                    .param("imagenUri", sku.imagenUri())
                    .param("updatedBy", sku.updatedBy())
                    .param("updatedAt", sku.updatedAt())
                    .param("skuId", sku.id().value())
                    .update();
            skuInternalId = entity.getId();
        }

        codigoBarraRepository.deleteBySkuId(skuInternalId);
        for (var codigo : sku.codigosBarra()) {
            if (codigoBarraRepository.existsByTenantIdAndCodigoBarra(tenantId.get(), codigo.codigoBarra())) {
                return SaveSkuOutcome.DUPLICATE_CODIGO_BARRA;
            }
            codigoBarraRepository.save(new SkuCodigoBarraJpaEntity(
                    tenantId.get(), skuInternalId, codigo.tipoCodigo(), codigo.codigoBarra(),
                    codigo.esPrincipal(), codigo.vigenteDesde(), codigo.vigenteHasta(), codigo.estado().name()));
        }
        return existing.isEmpty() ? SaveSkuOutcome.CREATED : SaveSkuOutcome.UPDATED;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SKUComercial> findSkuById(UUID tenantId, UUID skuId) {
        var tenantInternalId = findTenantId(tenantId);
        if (tenantInternalId.isEmpty()) return Optional.empty();
        var entity = skuRepository.findByUuidPublico(skuId);
        if (entity.isEmpty() || !entity.get().getTenantId().equals(tenantInternalId.get())) {
            return Optional.empty();
        }

        var codigos = codigoBarraRepository.findBySkuId(entity.get().getId()).stream()
                .map(codigoEntity -> new CodigoBarraSku(
                        codigoEntity.getCodigoBarra(), codigoEntity.getTipoCodigo(), codigoEntity.isEsPrincipal(),
                        codigoEntity.getVigenteDesde(), codigoEntity.getVigenteHasta(),
                        EstadoCatalogoSoporte.valueOf(codigoEntity.getEstado())))
                .toList();

        var productoReguladoId = entity.get().getProductoReguladoId() == null ? null
                : findProductoReguladoUuid(entity.get().getProductoReguladoId());
        var categoriaId = entity.get().getCategoriaId() == null ? null
                : findCategoriaUuid(entity.get().getCategoriaId());
        var marcaId = entity.get().getMarcaId() == null ? null : findMarcaUuid(entity.get().getMarcaId());

        var sku = SKUComercial.restore(
                new SkuId(entity.get().getUuidPublico()), new TenantId(tenantId),
                productoReguladoId == null ? null : new ProductoReguladoId(productoReguladoId),
                categoriaId == null ? null : new CategoriaProductoId(categoriaId),
                marcaId == null ? null : new MarcaId(marcaId),
                TipoSku.valueOf(entity.get().getTipoSku()), entity.get().getCodigoInterno(),
                entity.get().getDescripcionComercial(), entity.get().getNombreCorto(),
                entity.get().getPresentacionComercial(), entity.get().getUnidadVentaCodigo(),
                entity.get().getContenido(), entity.get().getUnidadContenidoCodigo(),
                entity.get().getPesoGramos(), entity.get().getAltoCm(), entity.get().getAnchoCm(),
                entity.get().getLargoCm(), entity.get().isPermiteVentaFraccion(), entity.get().getFactorFraccion(),
                entity.get().isRequiereLote(), entity.get().isRequiereVencimiento(), entity.get().isAfectoIgv(),
                entity.get().getStockMinimoDefault(), entity.get().getStockMaximoDefault(),
                entity.get().getImagenUri(), codigos,
                com.softprimesolutions.catalogo.domain.model.EstadoComercialSku.valueOf(entity.get().getEstadoComercial()),
                entity.get().getCreatedBy(), entity.get().getCreatedAt(), entity.get().getUpdatedBy(),
                entity.get().getUpdatedAt());
        return Optional.of(sku);
    }
```

- [ ] **Step 4: Continuar el mismo archivo — `categoriaExists`, `marcaExists`, los `changeXStatus`, y los métodos privados de resolución de ids**

Añadir, dentro de la misma clase:

```java
    @Override
    public boolean categoriaExists(UUID tenantId, UUID categoriaId) {
        var tenantInternalId = findTenantId(tenantId);
        if (tenantInternalId.isEmpty()) return false;
        return jdbcClient.sql("""
                        SELECT COUNT(*) FROM sch_farmacia.categoria_producto
                         WHERE tenant_id = :tenantId AND uuid_publico = :categoriaId
                        """)
                .param("tenantId", tenantInternalId.get()).param("categoriaId", categoriaId)
                .query(Long.class).single() > 0;
    }

    @Override
    public boolean marcaExists(UUID tenantId, UUID marcaId) {
        var tenantInternalId = findTenantId(tenantId);
        if (tenantInternalId.isEmpty()) return false;
        return jdbcClient.sql("""
                        SELECT COUNT(*) FROM sch_farmacia.marca WHERE tenant_id = :tenantId AND uuid_publico = :marcaId
                        """)
                .param("tenantId", tenantInternalId.get()).param("marcaId", marcaId)
                .query(Long.class).single() > 0;
    }

    @Override
    @Transactional
    public boolean changeMarcaStatus(UUID tenantId, UUID marcaId, String status, Instant changedAt) {
        var tenantInternalId = findTenantId(tenantId);
        if (tenantInternalId.isEmpty()) return false;
        return jdbcClient.sql("""
                        UPDATE sch_farmacia.marca SET estado = :status
                         WHERE tenant_id = :tenantId AND uuid_publico = :marcaId
                        """)
                .param("status", status).param("tenantId", tenantInternalId.get()).param("marcaId", marcaId)
                .update() == 1;
    }

    @Override
    @Transactional
    public boolean changeCategoriaStatus(UUID tenantId, UUID categoriaId, String status, Instant changedAt) {
        var tenantInternalId = findTenantId(tenantId);
        if (tenantInternalId.isEmpty()) return false;
        return jdbcClient.sql("""
                        UPDATE sch_farmacia.categoria_producto SET estado = :status
                         WHERE tenant_id = :tenantId AND uuid_publico = :categoriaId
                        """)
                .param("status", status).param("tenantId", tenantInternalId.get()).param("categoriaId", categoriaId)
                .update() == 1;
    }

    @Override
    @Transactional
    public boolean changeSkuStatus(UUID tenantId, UUID skuId, String status, Instant changedAt) {
        var tenantInternalId = findTenantId(tenantId);
        if (tenantInternalId.isEmpty()) return false;
        return jdbcClient.sql("""
                        UPDATE sch_farmacia.sku_comercial SET estado_comercial = :status
                         WHERE tenant_id = :tenantId AND uuid_publico = :skuId
                        """)
                .param("status", status).param("tenantId", tenantInternalId.get()).param("skuId", skuId)
                .update() == 1;
    }

    private Optional<Long> findTenantId(UUID tenantUuid) {
        if (tenantUuid == null) return Optional.empty();
        return jdbcClient.sql("SELECT id FROM sch_farmacia.tenant WHERE uuid_publico = :tenantUuid")
                .param("tenantUuid", tenantUuid).query(Long.class).optional();
    }

    private Optional<Long> findCategoriaInternalId(UUID categoriaUuid) {
        if (categoriaUuid == null) return Optional.empty();
        return jdbcClient.sql("SELECT id FROM sch_farmacia.categoria_producto WHERE uuid_publico = :categoriaUuid")
                .param("categoriaUuid", categoriaUuid).query(Long.class).optional();
    }

    private Optional<Long> findMarcaInternalId(UUID marcaUuid) {
        if (marcaUuid == null) return Optional.empty();
        return jdbcClient.sql("SELECT id FROM sch_farmacia.marca WHERE uuid_publico = :marcaUuid")
                .param("marcaUuid", marcaUuid).query(Long.class).optional();
    }

    private Optional<Long> findProductoReguladoInternalId(UUID productoReguladoUuid) {
        if (productoReguladoUuid == null) return Optional.empty();
        return jdbcClient.sql("SELECT id FROM sch_farmacia.producto_regulado WHERE uuid_publico = :productoReguladoUuid")
                .param("productoReguladoUuid", productoReguladoUuid).query(Long.class).optional();
    }

    private UUID findProductoReguladoUuid(Long internalId) {
        return jdbcClient.sql("SELECT uuid_publico FROM sch_farmacia.producto_regulado WHERE id = :internalId")
                .param("internalId", internalId).query(UUID.class).single();
    }

    private UUID findCategoriaUuid(Long internalId) {
        return jdbcClient.sql("SELECT uuid_publico FROM sch_farmacia.categoria_producto WHERE id = :internalId")
                .param("internalId", internalId).query(UUID.class).single();
    }

    private UUID findMarcaUuid(Long internalId) {
        return jdbcClient.sql("SELECT uuid_publico FROM sch_farmacia.marca WHERE id = :internalId")
                .param("internalId", internalId).query(UUID.class).single();
    }
}
```

- [ ] **Step 5: Compilar el módulo**

Run: `cd service-botica && .\gradlew.bat :modules:catalogo:compileJava`
Expected: BUILD SUCCESSFUL. Nota: revisar que los imports no utilizados listados al inicio del Step 2 (`ArrayList`, `OffsetDateTime`, `ZoneOffset`) se eliminen si el compilador los marca como no usados — se incluyeron por si acaso al ensamblar el archivo en partes, pero el código final de los Steps 2-4 no los necesita (no hay manipulación manual de listas mutables ni conversión de zona horaria en este adapter, a diferencia de `IamJpaWriteAdapter`).

- [ ] **Step 6: Commit**

```bash
git add service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/mapper/CatalogoComercialWriteMapper.java service-botica/modules/catalogo/src/main/java/com/softprimesolutions/catalogo/infrastructure/persistence/write/adapter/CatalogoComercialJpaWriteAdapter.java
git commit -m "feat(catalogo): agregar CatalogoComercialJpaWriteAdapter"
```

---
