# Módulo ventas, parte 1: fundación y turno de caja — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dejar el módulo `ventas` operativo con apertura, consulta y cierre de turno de caja (REST + persistencia + permisos), listo para que la parte 2 agregue la venta.

**Architecture:** Mismo patrón que `compras`: `domain` (modelo `TurnoCaja`), `application` (casos de uso CQRS con `Result`, puertos), `infrastructure` (adapters JDBC sobre `sch_venta`, transacción por `TransaccionPort`) y `api` (controller + DTOs). Las lecturas devuelven DTOs mapeados desde el dominio; la escritura serializa el cierre con `FOR UPDATE`.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Modulith 2.1, JdbcClient, JUnit 5 + AssertJ + Mockito, Testcontainers (PostgreSQL), Gradle 9.5.1.

Spec: `docs/superpowers/specs/2026-10-03-ventas-turno-caja-venta-simple-design.md`. Parte 2 (venta): `docs/superpowers/plans/2026-10-03-ventas-parte-2-venta.md`.

## Global Constraints

- Todo archivo fuente **nuevo** debe tener 100% de cobertura de líneas y ramas (gate JaCoCo por clase; el módulo `ventas` no tiene baseline).
- Sin comentarios explicativos en el código; preferir lambdas; prohibido código duplicado.
- Clean Architecture: `domain` sin Spring; los casos de uso devuelven `Result<T, ApplicationError>`, nunca `null` ni excepciones para casos previstos.
- Un solo turno abierto por terminal (`uk_turno_terminal_abierto`); cierre `ABIERTO` -> `CERRADO` directo (sin `EN_ARQUEO`); `total_sistema = fondo_inicial + total_ventas_sistema`; `diferencia = total_declarado - total_sistema`.
- Montos con escala 2, rango 0..1000000000.
- Actor y tenant salen del JWT (`sub` = `membership.uuid_publico`, claim `tid`); el usuario de BD se resuelve por `sch_seguridad.membership`.
- Permisos: `ventas.turnos.abrir`, `ventas.turnos.cerrar`, `ventas.turnos.consultar`, `ventas.ventas.registrar`, `ventas.ventas.consultar`, concedidos al rol `ADMIN` del tenant `FARMALAB`.
- Rutas REST bajo `/api/v1/ventas/turnos`.
- Migraciones nuevas desde `V033` en `service-botica/bootstrap-app/src/main/resources/db/migration`.
- Comandos desde `service-botica/` en PowerShell: `.\gradlew.bat :modules:ventas:test --tests "<clase>"`.
- Commits terminan con `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.

Rutas abreviadas: `MAIN` = `service-botica/modules/ventas/src/main/java/com/softprimesolutions/ventas`, `TEST` = `service-botica/modules/ventas/src/test/java/com/softprimesolutions/ventas`, `MIG` = `service-botica/bootstrap-app/src/main/resources/db/migration`, `BTEST` = `service-botica/bootstrap-app/src/test/java/com/softprimesolutions`.

---

### Task 1: Cableado del módulo y migraciones

**Files:**
- Modify: `service-botica/modules/ventas/build.gradle`
- Modify: `MAIN/package-info.java`
- Create: `MIG/V033__ventas_turno_venta_base.sql`
- Create: `MIG/V034__seed_ventas_permissions.sql`
- Test: `BTEST/ventas/db/VentasMigrationsIntegrationTest.java`

**Interfaces:**
- Produces: tabla `sch_venta.secuencia_operacion(tenant_id, terminal_id, ultimo_numero)`; columnas `created_by`/`updated_by` `VARCHAR(36)` en `turno_caja`, `venta`, `venta_linea`, `medio_pago`; columna `venta.huella_solicitud VARCHAR(64)`; columna `venta_linea.uuid_publico UUID`; los 5 permisos `ventas.*`.

- [ ] **Step 1: Escribir el test que falla**

`BTEST/ventas/db/VentasMigrationsIntegrationTest.java`:

```java
package com.softprimesolutions.ventas.db;

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
class VentasMigrationsIntegrationTest {

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void seedsTheVentasPermissionsAndGrantsThemToTheFarmalabAdministrator() {
        var permisos = jdbcClient.sql("""
                        SELECT codigo FROM sch_seguridad.permiso
                         WHERE codigo LIKE 'ventas.%' AND es_activo = '1' ORDER BY codigo
                        """).query(String.class).list();
        assertThat(permisos).containsExactly(
                "ventas.turnos.abrir", "ventas.turnos.cerrar", "ventas.turnos.consultar",
                "ventas.ventas.consultar", "ventas.ventas.registrar");

        var concedidos = jdbcClient.sql("""
                        SELECT COUNT(*) FROM sch_seguridad.rol_permiso rp
                          JOIN sch_seguridad.rol r ON r.id = rp.rol_id
                          JOIN sch_admin.tenant t ON t.id = r.tenant_id AND t.codigo = 'FARMALAB'
                          JOIN sch_seguridad.permiso p ON p.id = rp.permiso_id
                         WHERE r.codigo = 'ADMIN' AND p.codigo LIKE 'ventas.%'
                        """).query(Long.class).single();
        assertThat(concedidos).isEqualTo(5L);
    }

    @Test
    void createsTheOperationSequenceAndWidensTheActorColumns() {
        assertThat(jdbcClient.sql("""
                        SELECT COUNT(*) FROM information_schema.columns
                         WHERE table_schema = 'sch_venta' AND table_name = 'secuencia_operacion'
                           AND column_name IN ('tenant_id', 'terminal_id', 'ultimo_numero')
                        """).query(Long.class).single()).isEqualTo(3L);
        for (var tabla : new String[] {"turno_caja", "venta", "venta_linea", "medio_pago"}) {
            for (var columna : new String[] {"created_by", "updated_by"}) {
                assertThat(jdbcClient.sql("""
                                SELECT character_maximum_length FROM information_schema.columns
                                 WHERE table_schema = 'sch_venta' AND table_name = :tabla
                                   AND column_name = :columna
                                """).param("tabla", tabla).param("columna", columna)
                        .query(Integer.class).single()).isEqualTo(36);
            }
        }
        assertThat(jdbcClient.sql("""
                        SELECT COUNT(*) FROM information_schema.columns
                         WHERE table_schema = 'sch_venta' AND table_name = 'venta'
                           AND column_name = 'huella_solicitud'
                        """).query(Long.class).single()).isEqualTo(1L);
        assertThat(jdbcClient.sql("""
                        SELECT COUNT(*) FROM information_schema.columns
                         WHERE table_schema = 'sch_venta' AND table_name = 'venta_linea'
                           AND column_name = 'uuid_publico'
                        """).query(Long.class).single()).isEqualTo(1L);
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que falla**

Run: `.\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.ventas.db.VentasMigrationsIntegrationTest"`
Expected: FAIL (`containsExactly` con lista vacía y tabla `secuencia_operacion` inexistente).

- [ ] **Step 3: Implementar**

`service-botica/modules/ventas/build.gradle` (reemplazar completo):

```groovy
plugins { id 'boticas.spring-module'; id 'boticas.quality' }
dependencies {
    api project(':shared-kernel')
    implementation project(':shared-application')
    implementation project(':shared-web')
    implementation project(':shared-persistence')

    implementation 'org.springframework:spring-context'
    implementation 'org.springframework:spring-web'
    implementation 'org.springframework:spring-tx'
    implementation 'org.springframework:spring-jdbc'
    implementation 'org.springframework.data:spring-data-jpa'
    implementation 'org.springframework.security:spring-security-core'
    implementation 'org.springframework.security:spring-security-web'
    implementation 'org.springframework.security:spring-security-oauth2-jose'
    implementation 'jakarta.persistence:jakarta.persistence-api'
    compileOnly 'org.hibernate.orm:hibernate-core'
    implementation 'jakarta.validation:jakarta.validation-api'

    testImplementation 'org.assertj:assertj-core'
    testImplementation 'org.mockito:mockito-core'
    testImplementation 'org.mockito:mockito-junit-jupiter'
}
```

`MAIN/package-info.java` (reemplazar completo):

```java
@org.springframework.modulith.ApplicationModule(
        id = "ventas",
        displayName = "Ventas",
        allowedDependencies = {"inventario::api", "organizacion::api"})
package com.softprimesolutions.ventas;
```

`MIG/V033__ventas_turno_venta_base.sql`:

```sql
ALTER TABLE sch_venta.turno_caja
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36);

ALTER TABLE sch_venta.venta
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36),
    ADD COLUMN huella_solicitud VARCHAR(64);

ALTER TABLE sch_venta.venta_linea
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36),
    ADD COLUMN uuid_publico UUID NOT NULL DEFAULT uuidv7(),
    ADD CONSTRAINT uk_venta_linea_uuid UNIQUE (uuid_publico);

ALTER TABLE sch_venta.medio_pago
    ALTER COLUMN created_by TYPE VARCHAR(36),
    ALTER COLUMN updated_by TYPE VARCHAR(36);

CREATE TABLE sch_venta.secuencia_operacion (
    tenant_id       BIGINT NOT NULL,
    terminal_id     BIGINT NOT NULL,
    ultimo_numero   BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_secuencia_operacion PRIMARY KEY (tenant_id, terminal_id),
    CONSTRAINT fk_secuencia_operacion_tenant FOREIGN KEY (tenant_id) REFERENCES sch_admin.tenant(id),
    CONSTRAINT fk_secuencia_operacion_terminal FOREIGN KEY (terminal_id) REFERENCES sch_organizacion.terminal_pos(id),
    CONSTRAINT ck_secuencia_operacion_ultimo CHECK (ultimo_numero >= 0)
);
```

`MIG/V034__seed_ventas_permissions.sql`:

```sql
INSERT INTO sch_seguridad.permiso
    (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
SELECT m.id, seed.codigo, seed.recurso, seed.accion, seed.nombre, seed.descripcion, seed.es_critico, 'ACTIVO'
FROM sch_seguridad.modulo_sistema m
CROSS JOIN (VALUES
    ('ventas.turnos.abrir', 'TURNO_CAJA', 'ABRIR', 'Abrir turno de caja',
     'Permite abrir un turno de caja en una terminal POS.', FALSE),
    ('ventas.turnos.cerrar', 'TURNO_CAJA', 'CERRAR', 'Cerrar turno de caja',
     'Permite cerrar un turno de caja declarando el efectivo contado.', TRUE),
    ('ventas.turnos.consultar', 'TURNO_CAJA', 'CONSULTAR', 'Consultar turnos de caja',
     'Permite consultar el turno actual y el detalle de un turno.', FALSE),
    ('ventas.ventas.registrar', 'VENTA', 'REGISTRAR', 'Registrar ventas',
     'Permite registrar ventas presenciales y descontar stock.', TRUE),
    ('ventas.ventas.consultar', 'VENTA', 'CONSULTAR', 'Consultar ventas',
     'Permite consultar y listar ventas.', FALSE)
) AS seed(codigo, recurso, accion, nombre, descripcion, es_critico)
WHERE m.codigo = 'VENTAS'
ON CONFLICT (codigo) WHERE es_activo = '1' DO UPDATE SET
    modulo_id = EXCLUDED.modulo_id,
    recurso = EXCLUDED.recurso,
    accion = EXCLUDED.accion,
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    es_critico = EXCLUDED.es_critico,
    estado = 'ACTIVO';

INSERT INTO sch_seguridad.rol_permiso
    (tenant_id, rol_id, permiso_id)
SELECT r.tenant_id, r.id, p.id
FROM sch_seguridad.rol r
JOIN sch_admin.tenant t ON t.id = r.tenant_id AND t.codigo = 'FARMALAB'
JOIN sch_seguridad.permiso p ON p.codigo LIKE 'ventas.%' AND p.es_activo = '1'
WHERE r.codigo = 'ADMIN'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
```

- [ ] **Step 4: Ejecutar y verificar que pasa**

Run: `.\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.ventas.db.VentasMigrationsIntegrationTest" --tests "com.softprimesolutions.inventario.api.InventarioApiIntegrationTest"` y luego `.\gradlew.bat architectureTest`
Expected: PASS (las migraciones aplican sobre PostgreSQL real; Modulith y ArchUnit siguen en verde).

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/ventas/build.gradle service-botica/modules/ventas/src/main/java/com/softprimesolutions/ventas/package-info.java service-botica/bootstrap-app/src/main/resources/db/migration/V033__ventas_turno_venta_base.sql service-botica/bootstrap-app/src/main/resources/db/migration/V034__seed_ventas_permissions.sql service-botica/bootstrap-app/src/test/java/com/softprimesolutions/ventas
git commit -m "feat(ventas): cablear modulo, migraciones base y permisos

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Dominio del turno de caja

**Files:**
- Create: `MAIN/domain/valueobject/Actor.java`, `MAIN/domain/exception/VentasErrorCodes.java`, `MAIN/domain/model/Importes.java`, `MAIN/domain/model/Failures.java`, `MAIN/domain/model/EstadoTurno.java`, `MAIN/domain/model/TurnoCaja.java`
- Create: `TEST/VentasFixtures.java`
- Test: `TEST/domain/valueobject/ActorTest.java`, `TEST/domain/model/ImportesTest.java`, `TEST/domain/model/TurnoCajaTest.java`

**Interfaces:**
- Produces: `Failures.failure(String code, String message): Result<T, ErrorDetail>` (package-private helper reutilizado por todos los modelos); `Importes.precioValido(BigDecimal)` y `Importes.cantidadValida(BigDecimal)` (hasta 4 decimales); `Actor(UUID id)` con `codigo()`; `VentasErrorCodes.{MONTO_INVALIDO="VEN_MONTO_INVALIDO", OBSERVACION_INVALIDA="VEN_OBSERVACION_INVALIDA", TURNO_ESTADO_INVALIDO="VEN_TURNO_ESTADO_INVALIDO"}`; `Importes.montoValido(BigDecimal): boolean`, `Importes.redondear(BigDecimal): BigDecimal` (escala 2, HALF_UP); `EstadoTurno {ABIERTO, EN_ARQUEO, CERRADO, ANULADO}`; `TurnoCaja.abrir(UUID id, UUID tenantId, UUID terminalId, UUID establecimientoId, Actor cajero, BigDecimal fondoInicial, Instant ahora): Result<TurnoCaja, ErrorDetail>`; `TurnoCaja#cerrar(BigDecimal totalVentas, BigDecimal totalDeclarado, String observacion, Instant ahora): Result<TurnoCaja, ErrorDetail>`; `TurnoCaja.restore(UUID id, UUID tenantId, UUID terminalId, UUID establecimientoId, Actor cajero, Instant aperturaAt, BigDecimal fondoInicial, EstadoTurno estado, Instant cierreAt, BigDecimal totalVentasSistema, BigDecimal totalSistema, BigDecimal totalDeclarado, BigDecimal diferencia, String observacionCierre)`; accesores con el nombre de cada campo (`id()`, `tenantId()`, `terminalId()`, `establecimientoId()`, `cajero()`, `aperturaAt()`, `fondoInicial()`, `estado()`, `cierreAt()`, `totalVentasSistema()`, `totalSistema()`, `totalDeclarado()`, `diferencia()`, `observacionCierre()`).

- [ ] **Step 1: Escribir los tests que fallan**

`TEST/VentasFixtures.java`:

```java
package com.softprimesolutions.ventas;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.port.out.TransaccionPort;
import com.softprimesolutions.ventas.domain.model.EstadoTurno;
import com.softprimesolutions.ventas.domain.model.TurnoCaja;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

public final class VentasFixtures {

    public static final UUID TENANT = UUID.fromString("0f6d4c2e-3b1a-4c8e-9a51-7d2b6e4f1a90");
    public static final UUID TERMINAL = UUID.fromString("11111111-1111-4111-8111-111111111111");
    public static final UUID ESTABLECIMIENTO = UUID.fromString("44444444-4444-4444-8444-444444444444");
    public static final UUID TURNO = UUID.fromString("66666666-6666-4666-8666-666666666666");
    public static final UUID ACTOR_ID = UUID.fromString("77777777-7777-4777-8777-777777777777");
    public static final Actor ACTOR = new Actor(ACTOR_ID);
    public static final Instant AHORA = Instant.parse("2026-06-15T12:00:00Z");
    public static final ApplicationError CONFLICTO =
            new StandardApplicationError("TEST_CONFLICT", "Conflicto de prueba.", ErrorCategory.CONFLICT);

    public static final TransaccionPort TRANSACCION_DIRECTA = new TransaccionPort() {
        @Override
        public <T> Result<T, ApplicationError> ejecutar(Supplier<Result<T, ApplicationError>> trabajo) {
            return trabajo.get();
        }
    };

    private VentasFixtures() {
    }

    public static BigDecimal dec(String valor) {
        return new BigDecimal(valor);
    }

    public static TurnoCaja turno(EstadoTurno estado, String fondo) {
        return TurnoCaja.restore(
                TURNO, TENANT, TERMINAL, ESTABLECIMIENTO, ACTOR, AHORA, dec(fondo), estado, null, dec("0.00"),
                dec(fondo), null, null, null);
    }

    public static TurnoResult turnoResult() {
        return new TurnoResult(
                TURNO, TERMINAL, ESTABLECIMIENTO, ACTOR_ID, AHORA, dec("50.00"), "ABIERTO", null, dec("0.00"),
                dec("50.00"), null, null, null);
    }

    public static <T> Result<T, ApplicationError> ok(T valor) {
        return Result.success(valor);
    }

    public static <T> Result<T, ApplicationError> conflict() {
        return Result.failure(CONFLICTO);
    }
}
```

`TEST/domain/valueobject/ActorTest.java`:

```java
package com.softprimesolutions.ventas.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActorTest {

    @Test
    void keepsTheFullUserIdAsTheActorCode() {
        var id = UUID.fromString("12345678-9abc-4def-8123-456789abcdef");

        assertThat(new Actor(id).codigo()).isEqualTo("12345678-9abc-4def-8123-456789abcdef").hasSize(36);
        assertThat(new Actor(id).id()).isEqualTo(id);
    }

    @Test
    void requiresAnId() {
        assertThatNullPointerException().isThrownBy(() -> new Actor(null));
    }
}
```

`TEST/domain/model/ImportesTest.java`:

```java
package com.softprimesolutions.ventas.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ImportesTest {

    @Test
    void acceptsAmountsFromZeroUpToTheMaximumWithAtMostTwoDecimals() {
        assertThat(Importes.montoValido(new BigDecimal("0"))).isTrue();
        assertThat(Importes.montoValido(new BigDecimal("12.50"))).isTrue();
        assertThat(Importes.montoValido(new BigDecimal("12.500"))).isTrue();
        assertThat(Importes.montoValido(new BigDecimal("1000000000"))).isTrue();
        assertThat(Importes.montoValido(new BigDecimal("1E+3"))).isTrue();
    }

    @Test
    void rejectsMissingNegativeTooPreciseOrTooLargeAmounts() {
        assertThat(Importes.montoValido(null)).isFalse();
        assertThat(Importes.montoValido(new BigDecimal("-0.01"))).isFalse();
        assertThat(Importes.montoValido(new BigDecimal("1.001"))).isFalse();
        assertThat(Importes.montoValido(new BigDecimal("1000000000.01"))).isFalse();
    }

    @Test
    void validatesUnitPricesWithUpToFourDecimalsAndNeverNegative() {
        assertThat(Importes.precioValido(new BigDecimal("0"))).isTrue();
        assertThat(Importes.precioValido(new BigDecimal("2.5001"))).isTrue();
        assertThat(Importes.precioValido(null)).isFalse();
        assertThat(Importes.precioValido(new BigDecimal("-0.0001"))).isFalse();
        assertThat(Importes.precioValido(new BigDecimal("1.00001"))).isFalse();
        assertThat(Importes.precioValido(new BigDecimal("1000000000.0001"))).isFalse();
    }

    @Test
    void validatesQuantitiesAsPositiveWithUpToFourDecimals() {
        assertThat(Importes.cantidadValida(new BigDecimal("0.0001"))).isTrue();
        assertThat(Importes.cantidadValida(new BigDecimal("12"))).isTrue();
        assertThat(Importes.cantidadValida(null)).isFalse();
        assertThat(Importes.cantidadValida(BigDecimal.ZERO)).isFalse();
        assertThat(Importes.cantidadValida(new BigDecimal("-1"))).isFalse();
        assertThat(Importes.cantidadValida(new BigDecimal("1.00001"))).isFalse();
        assertThat(Importes.cantidadValida(new BigDecimal("1000000000.0001"))).isFalse();
    }

    @Test
    void roundsToTwoDecimalsHalfUp() {
        assertThat(Importes.redondear(new BigDecimal("1.005"))).isEqualTo(new BigDecimal("1.01"));
        assertThat(Importes.redondear(new BigDecimal("7"))).isEqualTo(new BigDecimal("7.00"));
    }
}
```

`TEST/domain/model/TurnoCajaTest.java`:

```java
package com.softprimesolutions.ventas.domain.model;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR;
import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.turno;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TurnoCajaTest {

    private static final Instant CIERRE = Instant.parse("2026-06-15T20:00:00Z");

    private static TurnoCaja value(Result<TurnoCaja, ErrorDetail> result) {
        return result.fold(value -> value, error -> { throw new AssertionError(error); });
    }

    private static String code(Result<TurnoCaja, ErrorDetail> result) {
        return result.fold(value -> { throw new AssertionError(value); }, ErrorDetail::code);
    }

    private static Result<TurnoCaja, ErrorDetail> abrir(BigDecimal fondo) {
        return TurnoCaja.abrir(TURNO, TENANT, TERMINAL, ESTABLECIMIENTO, ACTOR, fondo, AHORA);
    }

    @Test
    void opensATurnoWithTheInitialFundAsTheSystemTotal() {
        var turno = value(abrir(dec("50")));

        assertThat(turno.id()).isEqualTo(TURNO);
        assertThat(turno.tenantId()).isEqualTo(TENANT);
        assertThat(turno.terminalId()).isEqualTo(TERMINAL);
        assertThat(turno.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(turno.cajero()).isEqualTo(ACTOR);
        assertThat(turno.aperturaAt()).isEqualTo(AHORA);
        assertThat(turno.estado()).isEqualTo(EstadoTurno.ABIERTO);
        assertThat(turno.fondoInicial()).isEqualTo(dec("50.00"));
        assertThat(turno.totalVentasSistema()).isEqualTo(dec("0.00"));
        assertThat(turno.totalSistema()).isEqualTo(dec("50.00"));
        assertThat(turno.cierreAt()).isNull();
        assertThat(turno.totalDeclarado()).isNull();
        assertThat(turno.diferencia()).isNull();
        assertThat(turno.observacionCierre()).isNull();
    }

    @Test
    void rejectsAnInvalidInitialFund() {
        assertThat(code(abrir(null))).isEqualTo(VentasErrorCodes.MONTO_INVALIDO);
        assertThat(code(abrir(dec("-1")))).isEqualTo(VentasErrorCodes.MONTO_INVALIDO);
        assertThat(code(abrir(dec("1.234")))).isEqualTo(VentasErrorCodes.MONTO_INVALIDO);
        assertThat(code(abrir(dec("1000000001")))).isEqualTo(VentasErrorCodes.MONTO_INVALIDO);
    }

    @Test
    void closesComputingTheSystemTotalAndTheDifference() {
        var cerrado = value(turno(EstadoTurno.ABIERTO, "100.00")
                .cerrar(dec("40"), dec("135"), "  Faltan 5 soles  ", CIERRE));

        assertThat(cerrado.estado()).isEqualTo(EstadoTurno.CERRADO);
        assertThat(cerrado.cierreAt()).isEqualTo(CIERRE);
        assertThat(cerrado.totalVentasSistema()).isEqualTo(dec("40.00"));
        assertThat(cerrado.totalSistema()).isEqualTo(dec("140.00"));
        assertThat(cerrado.totalDeclarado()).isEqualTo(dec("135.00"));
        assertThat(cerrado.diferencia()).isEqualTo(dec("-5.00"));
        assertThat(cerrado.observacionCierre()).isEqualTo("Faltan 5 soles");
        assertThat(cerrado.id()).isEqualTo(TURNO);
        assertThat(cerrado.fondoInicial()).isEqualTo(dec("100.00"));
    }

    @Test
    void aBlankOrMissingObservationIsStoredAsNull() {
        var sinNota = value(turno(EstadoTurno.ABIERTO, "10").cerrar(dec("0"), dec("10"), null, CIERRE));
        var enBlanco = value(turno(EstadoTurno.ABIERTO, "10").cerrar(dec("0"), dec("10"), "   ", CIERRE));

        assertThat(sinNota.observacionCierre()).isNull();
        assertThat(enBlanco.observacionCierre()).isNull();
        assertThat(sinNota.diferencia()).isEqualTo(dec("0.00"));
    }

    @Test
    void onlyAnOpenTurnoCanBeClosed() {
        assertThat(code(turno(EstadoTurno.CERRADO, "10").cerrar(dec("0"), dec("10"), null, CIERRE)))
                .isEqualTo(VentasErrorCodes.TURNO_ESTADO_INVALIDO);
        assertThat(code(turno(EstadoTurno.EN_ARQUEO, "10").cerrar(dec("0"), dec("10"), null, CIERRE)))
                .isEqualTo(VentasErrorCodes.TURNO_ESTADO_INVALIDO);
    }

    @Test
    void rejectsAnInvalidDeclaredTotalOrATooLongObservation() {
        var abierto = turno(EstadoTurno.ABIERTO, "10");

        assertThat(code(abierto.cerrar(dec("0"), null, null, CIERRE))).isEqualTo(VentasErrorCodes.MONTO_INVALIDO);
        assertThat(code(abierto.cerrar(dec("0"), dec("-1"), null, CIERRE)))
                .isEqualTo(VentasErrorCodes.MONTO_INVALIDO);
        assertThat(code(abierto.cerrar(dec("0"), dec("10"), "x".repeat(1001), CIERRE)))
                .isEqualTo(VentasErrorCodes.OBSERVACION_INVALIDA);
        assertThat(value(abierto.cerrar(dec("0"), dec("10"), "x".repeat(1000), CIERRE)).observacionCierre())
                .hasSize(1000);
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `.\gradlew.bat :modules:ventas:test`
Expected: FAIL de compilación (clases de dominio, `TurnoResult` y `TransaccionPort` no existen todavía).

Nota: `VentasFixtures` referencia `TurnoResult` y `TransaccionPort` (Task 3). Para poder ejecutar los tests de dominio en esta tarea, crear primero esos dos tipos tal como se definen en el Step 3 de la Task 3 (solo los archivos `TurnoResult.java` y `TransaccionPort.java`); se commitean junto con esta tarea.

- [ ] **Step 3: Implementar**

`MAIN/domain/valueobject/Actor.java`:

```java
package com.softprimesolutions.ventas.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record Actor(UUID id) {

    public Actor {
        Objects.requireNonNull(id, "id es obligatorio");
    }

    public String codigo() {
        return id.toString();
    }
}
```

`MAIN/domain/exception/VentasErrorCodes.java`:

```java
package com.softprimesolutions.ventas.domain.exception;

public final class VentasErrorCodes {

    public static final String MONTO_INVALIDO = "VEN_MONTO_INVALIDO";
    public static final String OBSERVACION_INVALIDA = "VEN_OBSERVACION_INVALIDA";
    public static final String TURNO_ESTADO_INVALIDO = "VEN_TURNO_ESTADO_INVALIDO";

    private VentasErrorCodes() {
    }
}
```

`MAIN/domain/model/Importes.java`:

```java
package com.softprimesolutions.ventas.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Importes {

    private static final BigDecimal MAXIMO = new BigDecimal("1000000000");
    private static final int ESCALA = 2;
    private static final int ESCALA_UNITARIA = 4;

    private Importes() {
    }

    public static boolean montoValido(BigDecimal valor) {
        return enRango(valor, ESCALA) && valor.signum() >= 0;
    }

    public static boolean precioValido(BigDecimal valor) {
        return enRango(valor, ESCALA_UNITARIA) && valor.signum() >= 0;
    }

    public static boolean cantidadValida(BigDecimal valor) {
        return enRango(valor, ESCALA_UNITARIA) && valor.signum() > 0;
    }

    public static BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(ESCALA, RoundingMode.HALF_UP);
    }

    private static boolean enRango(BigDecimal valor, int escalaMaxima) {
        return valor != null
                && valor.compareTo(MAXIMO) <= 0
                && valor.stripTrailingZeros().scale() <= escalaMaxima;
    }
}
```

`MAIN/domain/model/Failures.java`:

```java
package com.softprimesolutions.ventas.domain.model;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Map;

final class Failures {

    private Failures() {
    }

    static <T> Result<T, ErrorDetail> failure(String code, String message) {
        return Result.failure(new ErrorDetail(code, message, Map.of()));
    }
}
```

`MAIN/domain/model/EstadoTurno.java`:

```java
package com.softprimesolutions.ventas.domain.model;

public enum EstadoTurno {
    ABIERTO,
    EN_ARQUEO,
    CERRADO,
    ANULADO
}
```

`MAIN/domain/model/TurnoCaja.java`:

```java
package com.softprimesolutions.ventas.domain.model;

import static com.softprimesolutions.ventas.domain.model.Failures.failure;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public final class TurnoCaja {

    private static final int OBSERVACION_MAXIMA = 1000;

    private final UUID id;
    private final UUID tenantId;
    private final UUID terminalId;
    private final UUID establecimientoId;
    private final Actor cajero;
    private final Instant aperturaAt;
    private final BigDecimal fondoInicial;
    private final EstadoTurno estado;
    private final Instant cierreAt;
    private final BigDecimal totalVentasSistema;
    private final BigDecimal totalSistema;
    private final BigDecimal totalDeclarado;
    private final BigDecimal diferencia;
    private final String observacionCierre;

    private TurnoCaja(
            UUID id, UUID tenantId, UUID terminalId, UUID establecimientoId, Actor cajero, Instant aperturaAt,
            BigDecimal fondoInicial, EstadoTurno estado, Instant cierreAt, BigDecimal totalVentasSistema,
            BigDecimal totalSistema, BigDecimal totalDeclarado, BigDecimal diferencia, String observacionCierre) {
        this.id = Objects.requireNonNull(id, "id es obligatorio");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId es obligatorio");
        this.terminalId = Objects.requireNonNull(terminalId, "terminalId es obligatorio");
        this.establecimientoId = Objects.requireNonNull(establecimientoId, "establecimientoId es obligatorio");
        this.cajero = Objects.requireNonNull(cajero, "cajero es obligatorio");
        this.aperturaAt = Objects.requireNonNull(aperturaAt, "aperturaAt es obligatorio");
        this.fondoInicial = Objects.requireNonNull(fondoInicial, "fondoInicial es obligatorio");
        this.estado = Objects.requireNonNull(estado, "estado es obligatorio");
        this.cierreAt = cierreAt;
        this.totalVentasSistema = Objects.requireNonNull(totalVentasSistema, "totalVentasSistema es obligatorio");
        this.totalSistema = Objects.requireNonNull(totalSistema, "totalSistema es obligatorio");
        this.totalDeclarado = totalDeclarado;
        this.diferencia = diferencia;
        this.observacionCierre = observacionCierre;
    }

    public static Result<TurnoCaja, ErrorDetail> abrir(
            UUID id, UUID tenantId, UUID terminalId, UUID establecimientoId, Actor cajero, BigDecimal fondoInicial,
            Instant ahora) {
        if (!Importes.montoValido(fondoInicial)) {
            return failure(VentasErrorCodes.MONTO_INVALIDO,
                    "El fondo inicial debe estar entre 0 y 1000000000 con hasta 2 decimales.");
        }
        var fondo = Importes.redondear(fondoInicial);
        return Result.success(new TurnoCaja(
                id, tenantId, terminalId, establecimientoId, cajero, ahora, fondo, EstadoTurno.ABIERTO, null,
                Importes.redondear(BigDecimal.ZERO), fondo, null, null, null));
    }

    public static TurnoCaja restore(
            UUID id, UUID tenantId, UUID terminalId, UUID establecimientoId, Actor cajero, Instant aperturaAt,
            BigDecimal fondoInicial, EstadoTurno estado, Instant cierreAt, BigDecimal totalVentasSistema,
            BigDecimal totalSistema, BigDecimal totalDeclarado, BigDecimal diferencia, String observacionCierre) {
        return new TurnoCaja(
                id, tenantId, terminalId, establecimientoId, cajero, aperturaAt, fondoInicial, estado, cierreAt,
                totalVentasSistema, totalSistema, totalDeclarado, diferencia, observacionCierre);
    }

    public Result<TurnoCaja, ErrorDetail> cerrar(
            BigDecimal totalVentas, BigDecimal declarado, String observacion, Instant ahora) {
        if (estado != EstadoTurno.ABIERTO) {
            return failure(VentasErrorCodes.TURNO_ESTADO_INVALIDO, "Solo se puede cerrar un turno abierto.");
        }
        if (!Importes.montoValido(declarado)) {
            return failure(VentasErrorCodes.MONTO_INVALIDO,
                    "El total declarado debe estar entre 0 y 1000000000 con hasta 2 decimales.");
        }
        var nota = Optional.ofNullable(observacion).map(String::trim).filter(Predicate.not(String::isEmpty));
        if (nota.filter(valor -> valor.length() > OBSERVACION_MAXIMA).isPresent()) {
            return failure(VentasErrorCodes.OBSERVACION_INVALIDA,
                    "La observacion de cierre admite hasta 1000 caracteres.");
        }
        var ventas = Importes.redondear(totalVentas);
        var sistema = fondoInicial.add(ventas);
        var declaradoRedondeado = Importes.redondear(declarado);
        return Result.success(new TurnoCaja(
                id, tenantId, terminalId, establecimientoId, cajero, aperturaAt, fondoInicial, EstadoTurno.CERRADO,
                ahora, ventas, sistema, declaradoRedondeado, declaradoRedondeado.subtract(sistema),
                nota.orElse(null)));
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID terminalId() { return terminalId; }
    public UUID establecimientoId() { return establecimientoId; }
    public Actor cajero() { return cajero; }
    public Instant aperturaAt() { return aperturaAt; }
    public BigDecimal fondoInicial() { return fondoInicial; }
    public EstadoTurno estado() { return estado; }
    public Instant cierreAt() { return cierreAt; }
    public BigDecimal totalVentasSistema() { return totalVentasSistema; }
    public BigDecimal totalSistema() { return totalSistema; }
    public BigDecimal totalDeclarado() { return totalDeclarado; }
    public BigDecimal diferencia() { return diferencia; }
    public String observacionCierre() { return observacionCierre; }
}
```

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `.\gradlew.bat :modules:ventas:test --tests "*ActorTest" --tests "*ImportesTest" --tests "*TurnoCajaTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/ventas
git commit -m "feat(ventas): modelo de dominio del turno de caja

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Aplicación del turno de caja

**Files:**
- Create: `MAIN/application/dto/command/AbrirTurnoCommand.java`, `CerrarTurnoCommand.java`
- Create: `MAIN/application/dto/query/ObtenerTurnoQuery.java`, `TurnoActualQuery.java`
- Create: `MAIN/application/dto/result/TurnoResult.java`
- Create: `MAIN/application/error/VentasErrors.java`
- Create: `MAIN/application/mapper/VentasApplicationMapper.java`
- Create: `MAIN/application/port/in/AbrirTurnoUseCase.java`, `CerrarTurnoUseCase.java`, `ConsultarTurnosUseCase.java`
- Create: `MAIN/application/port/out/GuardadoOutcome.java`, `TransaccionPort.java`, `ReferenciasVentasPort.java`, `TurnoWritePort.java`, `VentasReadPort.java`
- Create: `MAIN/application/usecase/command/AbrirTurnoHandler.java`, `CerrarTurnoHandler.java`
- Create: `MAIN/application/usecase/query/ConsultarTurnosHandler.java`
- Test: `TEST/application/error/VentasErrorsTest.java`, `TEST/application/mapper/VentasApplicationMapperTest.java`, `TEST/application/usecase/command/AbrirTurnoHandlerTest.java`, `TEST/application/usecase/command/CerrarTurnoHandlerTest.java`, `TEST/application/usecase/query/ConsultarTurnosHandlerTest.java`

**Interfaces:**
- Consumes: `TurnoCaja`, `Actor`, `VentasErrorCodes` (Task 2); `ClockPort#now(): Instant`, `IdentifierGenerator#next(): UUID` (shared-application).
- Produces:
  - `AbrirTurnoCommand(UUID tenantId, UUID actorId, UUID terminalId, BigDecimal fondoInicial)`; `CerrarTurnoCommand(UUID tenantId, UUID actorId, UUID turnoId, BigDecimal totalDeclarado, String observacion)`; `ObtenerTurnoQuery(UUID tenantId, UUID turnoId)`; `TurnoActualQuery(UUID tenantId, UUID terminalId)`.
  - `TurnoResult(UUID id, UUID terminalId, UUID establecimientoId, UUID cajeroId, Instant aperturaAt, BigDecimal fondoInicial, String estado, Instant cierreAt, BigDecimal totalVentasSistema, BigDecimal totalSistema, BigDecimal totalDeclarado, BigDecimal diferencia, String observacionCierre)`.
  - `AbrirTurnoUseCase#execute(AbrirTurnoCommand)`, `CerrarTurnoUseCase#execute(CerrarTurnoCommand)`: `Result<TurnoResult, ApplicationError>`; `ConsultarTurnosUseCase#obtener(ObtenerTurnoQuery)` y `#actual(TurnoActualQuery)`: `Result<TurnoResult, ApplicationError>`.
  - `GuardadoOutcome {GUARDADO, DUPLICADO}`; `TransaccionPort#ejecutar(Supplier<Result<T, ApplicationError>>): Result<T, ApplicationError>`.
  - `ReferenciasVentasPort#terminal(UUID tenantId, UUID terminalId): Optional<TerminalRef>` con `TerminalRef(UUID id, UUID establecimientoId, String codigo, boolean operable)`.
  - `TurnoWritePort`: `GuardadoOutcome insertar(TurnoCaja)`, `Optional<TurnoCaja> findPorIdParaActualizar(UUID tenantId, UUID turnoId)`, `BigDecimal totalVentasEfectivo(UUID tenantId, UUID turnoId)`, `boolean actualizarCierre(TurnoCaja turno, Actor actor)`.
  - `VentasReadPort`: `Optional<TurnoResult> findTurno(UUID tenantId, UUID turnoId)`, `Optional<TurnoResult> findTurnoAbierto(UUID tenantId, UUID terminalId)`.
  - `VentasErrors.CONCURRENCIA = "VEN_MODIFICACION_CONCURRENTE"`; `VentasErrors.{fromDomain(ErrorDetail), turnoNoEncontrado(), terminalNoEncontrada(), terminalNoOperable(), turnoYaAbierto(), modificacionConcurrente()}`.
  - `VentasApplicationMapper.toResult(TurnoCaja): TurnoResult`.

- [ ] **Step 1: Escribir los tests que fallan**

`TEST/application/error/VentasErrorsTest.java`:

```java
package com.softprimesolutions.ventas.application.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VentasErrorsTest {

    @Test
    void mapsDomainStateErrorsToConflictAndTheRestToValidation() {
        var estado = VentasErrors.fromDomain(
                new ErrorDetail(VentasErrorCodes.TURNO_ESTADO_INVALIDO, "estado", Map.of("a", 1)));
        var monto = VentasErrors.fromDomain(new ErrorDetail(VentasErrorCodes.MONTO_INVALIDO, "monto", Map.of()));

        assertThat(estado.category()).isEqualTo(ErrorCategory.CONFLICT);
        assertThat(estado.code()).isEqualTo("VEN_TURNO_ESTADO_INVALIDO");
        assertThat(estado.metadata()).containsEntry("a", 1);
        assertThat(monto.category()).isEqualTo(ErrorCategory.VALIDATION);
    }

    @Test
    void exposesTheTurnoErrors() {
        assertThat(VentasErrors.turnoNoEncontrado().code()).isEqualTo("VEN_TURNO_NO_ENCONTRADO");
        assertThat(VentasErrors.turnoNoEncontrado().category()).isEqualTo(ErrorCategory.NOT_FOUND);
        assertThat(VentasErrors.terminalNoEncontrada().code()).isEqualTo("VEN_TERMINAL_NO_ENCONTRADA");
        assertThat(VentasErrors.terminalNoEncontrada().category()).isEqualTo(ErrorCategory.NOT_FOUND);
        assertThat(VentasErrors.terminalNoOperable().code()).isEqualTo("VEN_TERMINAL_NO_OPERABLE");
        assertThat(VentasErrors.terminalNoOperable().category()).isEqualTo(ErrorCategory.CONFLICT);
        assertThat(VentasErrors.turnoYaAbierto().code()).isEqualTo("VEN_TURNO_YA_ABIERTO");
        assertThat(VentasErrors.turnoYaAbierto().category()).isEqualTo(ErrorCategory.CONFLICT);
        assertThat(VentasErrors.modificacionConcurrente().code()).isEqualTo(VentasErrors.CONCURRENCIA);
        assertThat(VentasErrors.modificacionConcurrente().category()).isEqualTo(ErrorCategory.CONFLICT);
    }
}
```

`TEST/application/mapper/VentasApplicationMapperTest.java`:

```java
package com.softprimesolutions.ventas.application.mapper;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.turno;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.ventas.domain.model.EstadoTurno;
import org.junit.jupiter.api.Test;

class VentasApplicationMapperTest {

    @Test
    void mapsATurnoToItsResult() {
        var result = VentasApplicationMapper.toResult(turno(EstadoTurno.ABIERTO, "50.00"));

        assertThat(result.id()).isEqualTo(TURNO);
        assertThat(result.terminalId()).isEqualTo(TERMINAL);
        assertThat(result.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(result.cajeroId()).isEqualTo(ACTOR_ID);
        assertThat(result.aperturaAt()).isEqualTo(AHORA);
        assertThat(result.fondoInicial()).isEqualTo(dec("50.00"));
        assertThat(result.estado()).isEqualTo("ABIERTO");
        assertThat(result.totalVentasSistema()).isEqualTo(dec("0.00"));
        assertThat(result.totalSistema()).isEqualTo(dec("50.00"));
        assertThat(result.cierreAt()).isNull();
        assertThat(result.totalDeclarado()).isNull();
        assertThat(result.diferencia()).isNull();
        assertThat(result.observacionCierre()).isNull();
    }
}
```

`TEST/application/usecase/query/ConsultarTurnosHandlerTest.java`:

```java
package com.softprimesolutions.ventas.application.usecase.query;

import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.turnoResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.query.ObtenerTurnoQuery;
import com.softprimesolutions.ventas.application.dto.query.TurnoActualQuery;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.port.out.VentasReadPort;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConsultarTurnosHandlerTest {

    private final VentasReadPort readPort = mock(VentasReadPort.class);
    private final ConsultarTurnosHandler handler = new ConsultarTurnosHandler(readPort);

    private static ApplicationError error(Result<TurnoResult, ApplicationError> result) {
        return result.fold(value -> { throw new AssertionError(value); }, error -> error);
    }

    @Test
    void returnsTheTurnoById() {
        when(readPort.findTurno(TENANT, TURNO)).thenReturn(Optional.of(turnoResult()));

        var result = handler.obtener(new ObtenerTurnoQuery(TENANT, TURNO));

        assertThat(result.fold(value -> value, error -> null)).isEqualTo(turnoResult());
    }

    @Test
    void aMissingTurnoIsNotFound() {
        var error = error(handler.obtener(new ObtenerTurnoQuery(TENANT, TURNO)));

        assertThat(error.code()).isEqualTo("VEN_TURNO_NO_ENCONTRADO");
        assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
    }

    @Test
    void returnsTheOpenTurnoOfATerminal() {
        when(readPort.findTurnoAbierto(TENANT, TERMINAL)).thenReturn(Optional.of(turnoResult()));

        var result = handler.actual(new TurnoActualQuery(TENANT, TERMINAL));

        assertThat(result.fold(value -> value, error -> null)).isEqualTo(turnoResult());
    }

    @Test
    void aTerminalWithoutAnOpenTurnoIsNotFound() {
        assertThat(error(handler.actual(new TurnoActualQuery(TENANT, TERMINAL))).code())
                .isEqualTo("VEN_TURNO_NO_ENCONTRADO");
    }

    @Test
    void requiresItsCollaboratorAndTheQueries() {
        assertThatNullPointerException().isThrownBy(() -> new ConsultarTurnosHandler(null));
        assertThatNullPointerException().isThrownBy(() -> handler.obtener(null));
        assertThatNullPointerException().isThrownBy(() -> handler.actual(null));
    }
}
```

`TEST/application/usecase/command/AbrirTurnoHandlerTest.java`:

```java
package com.softprimesolutions.ventas.application.usecase.command;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.conflict;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.ok;
import static com.softprimesolutions.ventas.VentasFixtures.turnoResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.AbrirTurnoCommand;
import com.softprimesolutions.ventas.application.dto.query.ObtenerTurnoQuery;
import com.softprimesolutions.ventas.application.dto.query.TurnoActualQuery;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.port.in.ConsultarTurnosUseCase;
import com.softprimesolutions.ventas.application.port.out.GuardadoOutcome;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort.TerminalRef;
import com.softprimesolutions.ventas.application.port.out.TurnoWritePort;
import com.softprimesolutions.ventas.domain.model.EstadoTurno;
import com.softprimesolutions.ventas.domain.model.TurnoCaja;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AbrirTurnoHandlerTest {

    private final TurnoWritePort turnos = mock(TurnoWritePort.class);
    private final ReferenciasVentasPort referencias = mock(ReferenciasVentasPort.class);
    private final ConsultarTurnosUseCase consultas = mock(ConsultarTurnosUseCase.class);
    private final AbrirTurnoHandler handler =
            new AbrirTurnoHandler(turnos, referencias, consultas, () -> TURNO, () -> AHORA);

    @BeforeEach
    void theTerminalIsOperableWithoutAnOpenTurnoAndTheInsertSucceedsByDefault() {
        when(referencias.terminal(TENANT, TERMINAL))
                .thenReturn(Optional.of(new TerminalRef(TERMINAL, ESTABLECIMIENTO, "POS01", true)));
        when(consultas.actual(new TurnoActualQuery(TENANT, TERMINAL))).thenReturn(conflict());
        when(turnos.insertar(any())).thenReturn(GuardadoOutcome.GUARDADO);
        when(consultas.obtener(new ObtenerTurnoQuery(TENANT, TURNO))).thenReturn(ok(turnoResult()));
    }

    private static AbrirTurnoCommand command(String fondo) {
        return new AbrirTurnoCommand(TENANT, ACTOR_ID, TERMINAL, fondo == null ? null : dec(fondo));
    }

    private static ApplicationError error(Result<TurnoResult, ApplicationError> result) {
        return result.fold(value -> { throw new AssertionError(value); }, error -> error);
    }

    @Test
    void opensTheTurnoAndReturnsTheStoredOne() {
        var result = handler.execute(command("50"));

        assertThat(result.fold(value -> value, error -> null)).isEqualTo(turnoResult());
        var captor = ArgumentCaptor.forClass(TurnoCaja.class);
        verify(turnos).insertar(captor.capture());
        var turno = captor.getValue();
        assertThat(turno.id()).isEqualTo(TURNO);
        assertThat(turno.tenantId()).isEqualTo(TENANT);
        assertThat(turno.terminalId()).isEqualTo(TERMINAL);
        assertThat(turno.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(turno.cajero().id()).isEqualTo(ACTOR_ID);
        assertThat(turno.fondoInicial()).isEqualTo(dec("50.00"));
        assertThat(turno.estado()).isEqualTo(EstadoTurno.ABIERTO);
        assertThat(turno.aperturaAt()).isEqualTo(AHORA);
    }

    @Test
    void anUnknownTerminalIsNotFound() {
        when(referencias.terminal(TENANT, TERMINAL)).thenReturn(Optional.empty());

        assertThat(error(handler.execute(command("50"))).code()).isEqualTo("VEN_TERMINAL_NO_ENCONTRADA");
        verify(turnos, never()).insertar(any());
    }

    @Test
    void aTerminalThatIsNotOperableIsRejected() {
        when(referencias.terminal(TENANT, TERMINAL))
                .thenReturn(Optional.of(new TerminalRef(TERMINAL, ESTABLECIMIENTO, "POS01", false)));

        assertThat(error(handler.execute(command("50"))).code()).isEqualTo("VEN_TERMINAL_NO_OPERABLE");
        verify(turnos, never()).insertar(any());
    }

    @Test
    void aTerminalThatAlreadyHasAnOpenTurnoIsRejectedBeforeInserting() {
        when(consultas.actual(new TurnoActualQuery(TENANT, TERMINAL))).thenReturn(ok(turnoResult()));

        assertThat(error(handler.execute(command("50"))).code()).isEqualTo("VEN_TURNO_YA_ABIERTO");
        verify(turnos, never()).insertar(any());
    }

    @Test
    void anInvalidInitialFundIsRejectedWithoutInserting() {
        assertThat(error(handler.execute(command("-1"))).code()).isEqualTo("VEN_MONTO_INVALIDO");
        assertThat(error(handler.execute(command(null))).code()).isEqualTo("VEN_MONTO_INVALIDO");
        verify(turnos, never()).insertar(any());
    }

    @Test
    void aConcurrentOpenThatLosesTheUniqueIndexRaceIsAConflict() {
        when(turnos.insertar(any())).thenReturn(GuardadoOutcome.DUPLICADO);

        assertThat(error(handler.execute(command("50"))).code()).isEqualTo("VEN_TURNO_YA_ABIERTO");
    }

    @Test
    void requiresItsCollaboratorsAndTheCommand() {
        assertThatNullPointerException().isThrownBy(
                () -> new AbrirTurnoHandler(null, referencias, consultas, () -> TURNO, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new AbrirTurnoHandler(turnos, null, consultas, () -> TURNO, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new AbrirTurnoHandler(turnos, referencias, null, () -> TURNO, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new AbrirTurnoHandler(turnos, referencias, consultas, null, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new AbrirTurnoHandler(turnos, referencias, consultas, () -> TURNO, null));
        assertThatNullPointerException().isThrownBy(() -> handler.execute(null));
    }
}
```

`TEST/application/usecase/command/CerrarTurnoHandlerTest.java`:

```java
package com.softprimesolutions.ventas.application.usecase.command;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TRANSACCION_DIRECTA;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.ok;
import static com.softprimesolutions.ventas.VentasFixtures.turno;
import static com.softprimesolutions.ventas.VentasFixtures.turnoResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.CerrarTurnoCommand;
import com.softprimesolutions.ventas.application.dto.query.ObtenerTurnoQuery;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.port.in.ConsultarTurnosUseCase;
import com.softprimesolutions.ventas.application.port.out.TurnoWritePort;
import com.softprimesolutions.ventas.domain.model.EstadoTurno;
import com.softprimesolutions.ventas.domain.model.TurnoCaja;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CerrarTurnoHandlerTest {

    private final TurnoWritePort turnos = mock(TurnoWritePort.class);
    private final ConsultarTurnosUseCase consultas = mock(ConsultarTurnosUseCase.class);
    private final CerrarTurnoHandler handler =
            new CerrarTurnoHandler(turnos, consultas, TRANSACCION_DIRECTA, () -> AHORA);

    @BeforeEach
    void theTurnoIsOpenWithFortyInCashSalesByDefault() {
        when(turnos.findPorIdParaActualizar(TENANT, TURNO))
                .thenReturn(Optional.of(turno(EstadoTurno.ABIERTO, "100.00")));
        when(turnos.totalVentasEfectivo(TENANT, TURNO)).thenReturn(dec("40.00"));
        when(turnos.actualizarCierre(any(), any())).thenReturn(true);
        when(consultas.obtener(new ObtenerTurnoQuery(TENANT, TURNO))).thenReturn(ok(turnoResult()));
    }

    private static CerrarTurnoCommand command(String declarado) {
        return new CerrarTurnoCommand(TENANT, ACTOR_ID, TURNO, declarado == null ? null : dec(declarado), "Cierre");
    }

    private static ApplicationError error(Result<TurnoResult, ApplicationError> result) {
        return result.fold(value -> { throw new AssertionError(value); }, error -> error);
    }

    @Test
    void closesTheTurnoWithTheSystemTotalsAndReturnsTheStoredOne() {
        var result = handler.execute(command("135"));

        assertThat(result.fold(value -> value, error -> null)).isEqualTo(turnoResult());
        var turnoCaptor = ArgumentCaptor.forClass(TurnoCaja.class);
        var actorCaptor = ArgumentCaptor.forClass(Actor.class);
        verify(turnos).actualizarCierre(turnoCaptor.capture(), actorCaptor.capture());
        var cerrado = turnoCaptor.getValue();
        assertThat(cerrado.estado()).isEqualTo(EstadoTurno.CERRADO);
        assertThat(cerrado.cierreAt()).isEqualTo(AHORA);
        assertThat(cerrado.totalVentasSistema()).isEqualTo(dec("40.00"));
        assertThat(cerrado.totalSistema()).isEqualTo(dec("140.00"));
        assertThat(cerrado.totalDeclarado()).isEqualTo(dec("135.00"));
        assertThat(cerrado.diferencia()).isEqualTo(dec("-5.00"));
        assertThat(cerrado.observacionCierre()).isEqualTo("Cierre");
        assertThat(actorCaptor.getValue().id()).isEqualTo(ACTOR_ID);
    }

    @Test
    void anUnknownTurnoIsNotFound() {
        when(turnos.findPorIdParaActualizar(TENANT, TURNO)).thenReturn(Optional.empty());

        assertThat(error(handler.execute(command("135"))).code()).isEqualTo("VEN_TURNO_NO_ENCONTRADO");
        verify(turnos, never()).actualizarCierre(any(), any());
    }

    @Test
    void aClosedTurnoCannotBeClosedAgain() {
        when(turnos.findPorIdParaActualizar(TENANT, TURNO))
                .thenReturn(Optional.of(turno(EstadoTurno.CERRADO, "100.00")));

        assertThat(error(handler.execute(command("135"))).code()).isEqualTo("VEN_TURNO_ESTADO_INVALIDO");
        verify(turnos, never()).actualizarCierre(any(), any());
    }

    @Test
    void anInvalidDeclaredTotalIsRejected() {
        assertThat(error(handler.execute(command("-1"))).code()).isEqualTo("VEN_MONTO_INVALIDO");
        assertThat(error(handler.execute(command(null))).code()).isEqualTo("VEN_MONTO_INVALIDO");
        verify(turnos, never()).actualizarCierre(any(), any());
    }

    @Test
    void aLostUpdateIsReportedAsAConcurrentModification() {
        when(turnos.actualizarCierre(any(), any())).thenReturn(false);

        assertThat(error(handler.execute(command("135"))).code()).isEqualTo("VEN_MODIFICACION_CONCURRENTE");
    }

    @Test
    void requiresItsCollaboratorsAndTheCommand() {
        assertThatNullPointerException().isThrownBy(
                () -> new CerrarTurnoHandler(null, consultas, TRANSACCION_DIRECTA, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new CerrarTurnoHandler(turnos, null, TRANSACCION_DIRECTA, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new CerrarTurnoHandler(turnos, consultas, null, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new CerrarTurnoHandler(turnos, consultas, TRANSACCION_DIRECTA, null));
        assertThatNullPointerException().isThrownBy(() -> handler.execute(null));
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `.\gradlew.bat :modules:ventas:test`
Expected: FAIL de compilación (tipos de aplicación inexistentes).

- [ ] **Step 3: Implementar**

`MAIN/application/dto/command/AbrirTurnoCommand.java`:

```java
package com.softprimesolutions.ventas.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record AbrirTurnoCommand(UUID tenantId, UUID actorId, UUID terminalId, BigDecimal fondoInicial) {
}
```

`MAIN/application/dto/command/CerrarTurnoCommand.java`:

```java
package com.softprimesolutions.ventas.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CerrarTurnoCommand(
        UUID tenantId, UUID actorId, UUID turnoId, BigDecimal totalDeclarado, String observacion) {
}
```

`MAIN/application/dto/query/ObtenerTurnoQuery.java`:

```java
package com.softprimesolutions.ventas.application.dto.query;

import java.util.UUID;

public record ObtenerTurnoQuery(UUID tenantId, UUID turnoId) {
}
```

`MAIN/application/dto/query/TurnoActualQuery.java`:

```java
package com.softprimesolutions.ventas.application.dto.query;

import java.util.UUID;

public record TurnoActualQuery(UUID tenantId, UUID terminalId) {
}
```

`MAIN/application/dto/result/TurnoResult.java`:

```java
package com.softprimesolutions.ventas.application.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TurnoResult(
        UUID id,
        UUID terminalId,
        UUID establecimientoId,
        UUID cajeroId,
        Instant aperturaAt,
        BigDecimal fondoInicial,
        String estado,
        Instant cierreAt,
        BigDecimal totalVentasSistema,
        BigDecimal totalSistema,
        BigDecimal totalDeclarado,
        BigDecimal diferencia,
        String observacionCierre) {
}
```

`MAIN/application/error/VentasErrors.java`:

```java
package com.softprimesolutions.ventas.application.error;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import java.util.Set;

public final class VentasErrors {

    public static final String CONCURRENCIA = "VEN_MODIFICACION_CONCURRENTE";

    private static final Set<String> CONFLICTOS = Set.of(VentasErrorCodes.TURNO_ESTADO_INVALIDO);

    private VentasErrors() {
    }

    public static ApplicationError fromDomain(ErrorDetail error) {
        var category = CONFLICTOS.contains(error.code()) ? ErrorCategory.CONFLICT : ErrorCategory.VALIDATION;
        return new StandardApplicationError(error.code(), error.message(), category, error.metadata());
    }

    public static ApplicationError turnoNoEncontrado() {
        return notFound("VEN_TURNO_NO_ENCONTRADO", "El turno de caja indicado no existe.");
    }

    public static ApplicationError terminalNoEncontrada() {
        return notFound("VEN_TERMINAL_NO_ENCONTRADA", "La terminal indicada no existe.");
    }

    public static ApplicationError terminalNoOperable() {
        return conflict("VEN_TERMINAL_NO_OPERABLE", "La terminal debe estar activa para operar.");
    }

    public static ApplicationError turnoYaAbierto() {
        return conflict("VEN_TURNO_YA_ABIERTO", "La terminal ya tiene un turno abierto.");
    }

    public static ApplicationError modificacionConcurrente() {
        return conflict(CONCURRENCIA, "La operacion fue modificada por otra solicitud; reintenta.");
    }

    private static ApplicationError notFound(String code, String message) {
        return new StandardApplicationError(code, message, ErrorCategory.NOT_FOUND);
    }

    private static ApplicationError conflict(String code, String message) {
        return new StandardApplicationError(code, message, ErrorCategory.CONFLICT);
    }
}
```

`MAIN/application/mapper/VentasApplicationMapper.java`:

```java
package com.softprimesolutions.ventas.application.mapper;

import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.domain.model.TurnoCaja;

public final class VentasApplicationMapper {

    private VentasApplicationMapper() {
    }

    public static TurnoResult toResult(TurnoCaja turno) {
        return new TurnoResult(
                turno.id(), turno.terminalId(), turno.establecimientoId(), turno.cajero().id(), turno.aperturaAt(),
                turno.fondoInicial(), turno.estado().name(), turno.cierreAt(), turno.totalVentasSistema(),
                turno.totalSistema(), turno.totalDeclarado(), turno.diferencia(), turno.observacionCierre());
    }
}
```

`MAIN/application/port/in/AbrirTurnoUseCase.java`:

```java
package com.softprimesolutions.ventas.application.port.in;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.AbrirTurnoCommand;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;

@FunctionalInterface
public interface AbrirTurnoUseCase {
    Result<TurnoResult, ApplicationError> execute(AbrirTurnoCommand command);
}
```

`MAIN/application/port/in/CerrarTurnoUseCase.java`:

```java
package com.softprimesolutions.ventas.application.port.in;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.CerrarTurnoCommand;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;

@FunctionalInterface
public interface CerrarTurnoUseCase {
    Result<TurnoResult, ApplicationError> execute(CerrarTurnoCommand command);
}
```

`MAIN/application/port/in/ConsultarTurnosUseCase.java`:

```java
package com.softprimesolutions.ventas.application.port.in;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.query.ObtenerTurnoQuery;
import com.softprimesolutions.ventas.application.dto.query.TurnoActualQuery;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;

public interface ConsultarTurnosUseCase {

    Result<TurnoResult, ApplicationError> obtener(ObtenerTurnoQuery query);

    Result<TurnoResult, ApplicationError> actual(TurnoActualQuery query);
}
```

`MAIN/application/port/out/GuardadoOutcome.java`:

```java
package com.softprimesolutions.ventas.application.port.out;

public enum GuardadoOutcome {
    GUARDADO,
    DUPLICADO
}
```

`MAIN/application/port/out/TransaccionPort.java`:

```java
package com.softprimesolutions.ventas.application.port.out;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.function.Supplier;

@FunctionalInterface
public interface TransaccionPort {

    <T> Result<T, ApplicationError> ejecutar(Supplier<Result<T, ApplicationError>> trabajo);
}
```

`MAIN/application/port/out/ReferenciasVentasPort.java`:

```java
package com.softprimesolutions.ventas.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface ReferenciasVentasPort {

    Optional<TerminalRef> terminal(UUID tenantId, UUID terminalId);

    record TerminalRef(UUID id, UUID establecimientoId, String codigo, boolean operable) {
    }
}
```

`MAIN/application/port/out/TurnoWritePort.java`:

```java
package com.softprimesolutions.ventas.application.port.out;

import com.softprimesolutions.ventas.domain.model.TurnoCaja;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface TurnoWritePort {

    GuardadoOutcome insertar(TurnoCaja turno);

    Optional<TurnoCaja> findPorIdParaActualizar(UUID tenantId, UUID turnoId);

    BigDecimal totalVentasEfectivo(UUID tenantId, UUID turnoId);

    boolean actualizarCierre(TurnoCaja turno, Actor actor);
}
```

`MAIN/application/port/out/VentasReadPort.java`:

```java
package com.softprimesolutions.ventas.application.port.out;

import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import java.util.Optional;
import java.util.UUID;

public interface VentasReadPort {

    Optional<TurnoResult> findTurno(UUID tenantId, UUID turnoId);

    Optional<TurnoResult> findTurnoAbierto(UUID tenantId, UUID terminalId);
}
```

`MAIN/application/usecase/query/ConsultarTurnosHandler.java`:

```java
package com.softprimesolutions.ventas.application.usecase.query;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.query.ObtenerTurnoQuery;
import com.softprimesolutions.ventas.application.dto.query.TurnoActualQuery;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.application.port.in.ConsultarTurnosUseCase;
import com.softprimesolutions.ventas.application.port.out.VentasReadPort;
import java.util.Objects;
import java.util.Optional;

public final class ConsultarTurnosHandler implements ConsultarTurnosUseCase {

    private final VentasReadPort readPort;

    public ConsultarTurnosHandler(VentasReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<TurnoResult, ApplicationError> obtener(ObtenerTurnoQuery query) {
        Objects.requireNonNull(query, "query es obligatoria");
        return resultado(readPort.findTurno(query.tenantId(), query.turnoId()));
    }

    @Override
    public Result<TurnoResult, ApplicationError> actual(TurnoActualQuery query) {
        Objects.requireNonNull(query, "query es obligatoria");
        return resultado(readPort.findTurnoAbierto(query.tenantId(), query.terminalId()));
    }

    private static Result<TurnoResult, ApplicationError> resultado(Optional<TurnoResult> turno) {
        return turno.<Result<TurnoResult, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(VentasErrors.turnoNoEncontrado()));
    }
}
```

`MAIN/application/usecase/command/AbrirTurnoHandler.java`:

```java
package com.softprimesolutions.ventas.application.usecase.command;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.AbrirTurnoCommand;
import com.softprimesolutions.ventas.application.dto.query.ObtenerTurnoQuery;
import com.softprimesolutions.ventas.application.dto.query.TurnoActualQuery;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.application.port.in.AbrirTurnoUseCase;
import com.softprimesolutions.ventas.application.port.in.ConsultarTurnosUseCase;
import com.softprimesolutions.ventas.application.port.out.GuardadoOutcome;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort.TerminalRef;
import com.softprimesolutions.ventas.application.port.out.TurnoWritePort;
import com.softprimesolutions.ventas.domain.model.TurnoCaja;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import java.util.Objects;

public final class AbrirTurnoHandler implements AbrirTurnoUseCase {

    private final TurnoWritePort turnos;
    private final ReferenciasVentasPort referencias;
    private final ConsultarTurnosUseCase consultas;
    private final IdentifierGenerator identifiers;
    private final ClockPort clock;

    public AbrirTurnoHandler(
            TurnoWritePort turnos, ReferenciasVentasPort referencias, ConsultarTurnosUseCase consultas,
            IdentifierGenerator identifiers, ClockPort clock) {
        this.turnos = Objects.requireNonNull(turnos, "turnos es obligatorio");
        this.referencias = Objects.requireNonNull(referencias, "referencias es obligatorio");
        this.consultas = Objects.requireNonNull(consultas, "consultas es obligatorio");
        this.identifiers = Objects.requireNonNull(identifiers, "identifiers es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<TurnoResult, ApplicationError> execute(AbrirTurnoCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var terminal = referencias.terminal(command.tenantId(), command.terminalId());
        if (terminal.isEmpty()) return Result.failure(VentasErrors.terminalNoEncontrada());
        if (!terminal.get().operable()) return Result.failure(VentasErrors.terminalNoOperable());
        if (consultas.actual(new TurnoActualQuery(command.tenantId(), command.terminalId())).isSuccess()) {
            return Result.failure(VentasErrors.turnoYaAbierto());
        }
        return TurnoCaja.abrir(
                        identifiers.next(), command.tenantId(), terminal.get().id(),
                        terminal.get().establecimientoId(), new Actor(command.actorId()), command.fondoInicial(),
                        clock.now())
                .fold(
                        this::persistir,
                        error -> Result.<TurnoResult, ApplicationError>failure(VentasErrors.fromDomain(error)));
    }

    private Result<TurnoResult, ApplicationError> persistir(TurnoCaja turno) {
        if (turnos.insertar(turno) == GuardadoOutcome.DUPLICADO) {
            return Result.failure(VentasErrors.turnoYaAbierto());
        }
        return consultas.obtener(new ObtenerTurnoQuery(turno.tenantId(), turno.id()));
    }
}
```

`MAIN/application/usecase/command/CerrarTurnoHandler.java`:

```java
package com.softprimesolutions.ventas.application.usecase.command;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.CerrarTurnoCommand;
import com.softprimesolutions.ventas.application.dto.query.ObtenerTurnoQuery;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.application.port.in.CerrarTurnoUseCase;
import com.softprimesolutions.ventas.application.port.in.ConsultarTurnosUseCase;
import com.softprimesolutions.ventas.application.port.out.TransaccionPort;
import com.softprimesolutions.ventas.application.port.out.TurnoWritePort;
import com.softprimesolutions.ventas.domain.model.TurnoCaja;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import java.util.Objects;
import java.util.UUID;

public final class CerrarTurnoHandler implements CerrarTurnoUseCase {

    private final TurnoWritePort turnos;
    private final ConsultarTurnosUseCase consultas;
    private final TransaccionPort transaccion;
    private final ClockPort clock;

    public CerrarTurnoHandler(
            TurnoWritePort turnos, ConsultarTurnosUseCase consultas, TransaccionPort transaccion, ClockPort clock) {
        this.turnos = Objects.requireNonNull(turnos, "turnos es obligatorio");
        this.consultas = Objects.requireNonNull(consultas, "consultas es obligatorio");
        this.transaccion = Objects.requireNonNull(transaccion, "transaccion es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<TurnoResult, ApplicationError> execute(CerrarTurnoCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        return transaccion.ejecutar(() -> cerrar(command))
                .flatMap(turnoId -> consultas.obtener(new ObtenerTurnoQuery(command.tenantId(), turnoId)));
    }

    private Result<UUID, ApplicationError> cerrar(CerrarTurnoCommand command) {
        var turno = turnos.findPorIdParaActualizar(command.tenantId(), command.turnoId());
        if (turno.isEmpty()) return Result.failure(VentasErrors.turnoNoEncontrado());
        var totalVentas = turnos.totalVentasEfectivo(command.tenantId(), command.turnoId());
        return turno.get().cerrar(totalVentas, command.totalDeclarado(), command.observacion(), clock.now())
                .fold(
                        cerrado -> guardar(cerrado, new Actor(command.actorId())),
                        error -> Result.<UUID, ApplicationError>failure(VentasErrors.fromDomain(error)));
    }

    private Result<UUID, ApplicationError> guardar(TurnoCaja cerrado, Actor actor) {
        if (!turnos.actualizarCierre(cerrado, actor)) return Result.failure(VentasErrors.modificacionConcurrente());
        return Result.success(cerrado.id());
    }
}
```

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `.\gradlew.bat :modules:ventas:test`
Expected: PASS (dominio + aplicación).

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/ventas
git commit -m "feat(ventas): casos de uso de apertura, cierre y consulta de turno

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Infraestructura del turno de caja

**Files:**
- Create: `MAIN/infrastructure/persistence/JdbcColumns.java`, `MAIN/infrastructure/persistence/TurnoRows.java`
- Create: `MAIN/infrastructure/persistence/write/adapter/TurnoJdbcWriteAdapter.java`, `ReferenciasVentasJdbcAdapter.java`, `SpringTransaccionAdapter.java`
- Create: `MAIN/infrastructure/persistence/read/adapter/VentasJdbcReadAdapter.java`
- Create: `MAIN/infrastructure/configuration/VentasModuleConfiguration.java`
- Create (copia): `TEST/infrastructure/persistence/JdbcClientStub.java`
- Test: `TEST/infrastructure/persistence/JdbcColumnsTest.java`, `TEST/infrastructure/persistence/Rows.java`, `TEST/infrastructure/persistence/write/adapter/TurnoJdbcWriteAdapterTest.java`, `ReferenciasVentasJdbcAdapterTest.java`, `SpringTransaccionAdapterTest.java`, `TEST/infrastructure/persistence/read/adapter/VentasJdbcReadAdapterTest.java`, `TEST/infrastructure/configuration/VentasModuleConfigurationTest.java`

**Interfaces:**
- Consumes: puertos de la Task 3; `TurnoCaja.restore`, `VentasApplicationMapper.toResult`.
- Produces: `JdbcColumns.{uuid, date, instant, offset}` (mismo contrato que `compras`); `TurnoRows.SELECT`, `TurnoRows.POR_ID`, `TurnoRows.ABIERTO_POR_TERMINAL`, `TurnoRows.map(ResultSet, UUID tenantId): TurnoCaja`; adapters `@Repository`/`@Component` para los puertos; `VentasModuleConfiguration` con beans `ClockPort ventasClockPort`, `IdentifierGenerator ventasIdentifierGenerator`, `ConsultarTurnosUseCase`, `AbrirTurnoUseCase`, `CerrarTurnoUseCase`.

- [ ] **Step 1: Copiar el stub JDBC y escribir los tests que fallan**

Copiar el stub (mismo contenido que el de `inventario`, cambiando solo el paquete):

```bash
sed 's/package com.softprimesolutions.inventario.infrastructure.persistence;/package com.softprimesolutions.ventas.infrastructure.persistence;/' service-botica/modules/inventario/src/test/java/com/softprimesolutions/inventario/infrastructure/persistence/JdbcClientStub.java > service-botica/modules/ventas/src/test/java/com/softprimesolutions/ventas/infrastructure/persistence/JdbcClientStub.java
```
(crear antes el directorio con `mkdir -p`).

`TEST/infrastructure/persistence/Rows.java`:

```java
package com.softprimesolutions.ventas.infrastructure.persistence;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.dec;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

public final class Rows {

    public static final OffsetDateTime MOMENTO = AHORA.atOffset(ZoneOffset.UTC);

    private Rows() {
    }

    public static Map<String, Object> turno() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", TURNO);
        row.put("terminal_uuid", TERMINAL);
        row.put("establecimiento_uuid", ESTABLECIMIENTO);
        row.put("cajero_uuid", ACTOR_ID);
        row.put("apertura_at", MOMENTO);
        row.put("fondo_inicial", dec("50.00"));
        row.put("estado", "ABIERTO");
        row.put("cierre_at", null);
        row.put("total_ventas_sistema", dec("0.00"));
        row.put("total_sistema", dec("50.00"));
        row.put("total_declarado", null);
        row.put("diferencia", null);
        row.put("observacion_cierre", null);
        return row;
    }
}
```

`TEST/infrastructure/persistence/JdbcColumnsTest.java`:

```java
package com.softprimesolutions.ventas.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JdbcColumnsTest {

    private final ResultSet rs = mock(ResultSet.class);

    @Test
    void readsUuidsDatesAndInstants() throws SQLException {
        var id = UUID.randomUUID();
        var fecha = LocalDate.of(2026, 6, 15);
        var momento = OffsetDateTime.of(2026, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        when(rs.getObject("id", UUID.class)).thenReturn(id);
        when(rs.getObject("fecha", LocalDate.class)).thenReturn(fecha);
        when(rs.getObject("momento", OffsetDateTime.class)).thenReturn(momento);

        assertThat(JdbcColumns.uuid(rs, "id")).isEqualTo(id);
        assertThat(JdbcColumns.date(rs, "fecha")).isEqualTo(fecha);
        assertThat(JdbcColumns.instant(rs, "momento")).isEqualTo(Instant.parse("2026-06-15T12:00:00Z"));
    }

    @Test
    void aMissingTimestampIsNull() throws SQLException {
        assertThat(JdbcColumns.instant(rs, "momento")).isNull();
    }

    @Test
    void convertsInstantsToUtcOffsetsAndKeepsNulls() {
        var instante = Instant.parse("2026-06-15T12:00:00Z");

        assertThat(JdbcColumns.offset(instante)).isEqualTo(OffsetDateTime.of(2026, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC));
        assertThat(JdbcColumns.offset(null)).isNull();
    }
}
```

`TEST/infrastructure/persistence/write/adapter/SpringTransaccionAdapterTest.java`:

```java
package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import static com.softprimesolutions.ventas.VentasFixtures.CONFLICTO;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.shared.kernel.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

class SpringTransaccionAdapterTest {

    private final SimpleTransactionStatus status = new SimpleTransactionStatus();
    private final TransactionOperations operations = new TransactionOperations() {
        @Override
        public <T> T execute(TransactionCallback<T> action) {
            return action.doInTransaction(status);
        }
    };
    private final SpringTransaccionAdapter adapter = new SpringTransaccionAdapter(operations);

    @Test
    void aSuccessfulResultLeavesTheTransactionToCommit() {
        var result = adapter.ejecutar(() -> Result.success("ok"));

        String texto = result.fold(value -> value, error -> "error");
        assertThat(texto).isEqualTo("ok");
        assertThat(status.isRollbackOnly()).isFalse();
    }

    @Test
    void aFailureResultMarksTheTransactionForRollbackAndIsReturnedUntouched() {
        var result = adapter.<String>ejecutar(() -> Result.failure(CONFLICTO));

        assertThat(result.isFailure()).isTrue();
        assertThat(status.isRollbackOnly()).isTrue();
    }
}
```

`TEST/infrastructure/persistence/write/adapter/ReferenciasVentasJdbcAdapterTest.java`:

```java
package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.ventas.infrastructure.persistence.JdbcClientStub;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class ReferenciasVentasJdbcAdapterTest {

    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final ReferenciasVentasJdbcAdapter adapter = new ReferenciasVentasJdbcAdapter(jdbc.client());

    @Test
    void findsATerminalWithItsEstablishmentAndOperability() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", TERMINAL);
        row.put("establecimiento_uuid", ESTABLECIMIENTO);
        row.put("codigo", "POS01");
        row.put("operable", true);
        jdbc.rows("FROM sch_organizacion.terminal_pos tp", row);

        var terminal = adapter.terminal(TENANT, TERMINAL).orElseThrow();

        assertThat(terminal.id()).isEqualTo(TERMINAL);
        assertThat(terminal.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(terminal.codigo()).isEqualTo("POS01");
        assertThat(terminal.operable()).isTrue();
        assertThat(jdbc.statementContaining("FROM sch_organizacion.terminal_pos tp").params())
                .containsEntry("tenantId", TENANT).containsEntry("terminalId", TERMINAL);
    }

    @Test
    void anUnknownTerminalIsEmpty() {
        assertThat(adapter.terminal(TENANT, TERMINAL)).isEmpty();
    }
}
```

`TEST/infrastructure/persistence/write/adapter/TurnoJdbcWriteAdapterTest.java`:

```java
package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR;
import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.turno;
import static com.softprimesolutions.ventas.infrastructure.persistence.Rows.MOMENTO;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.ventas.application.port.out.GuardadoOutcome;
import com.softprimesolutions.ventas.domain.model.EstadoTurno;
import com.softprimesolutions.ventas.infrastructure.persistence.JdbcClientStub;
import com.softprimesolutions.ventas.infrastructure.persistence.Rows;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionOperations;

class TurnoJdbcWriteAdapterTest {

    private static final String INSERT = "INSERT INTO sch_venta.turno_caja";
    private static final String UPDATE = "UPDATE sch_venta.turno_caja";

    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final TurnoJdbcWriteAdapter adapter =
            new TurnoJdbcWriteAdapter(jdbc.client(), TransactionOperations.withoutTransaction());

    @Test
    void insertsTheTurnoResolvingTheTerminalAndTheCashierFromTheirPublicIds() {
        var outcome = adapter.insertar(turno(EstadoTurno.ABIERTO, "50.00"));

        assertThat(outcome).isEqualTo(GuardadoOutcome.GUARDADO);
        assertThat(jdbc.statementContaining(INSERT).params())
                .containsEntry("turnoId", TURNO).containsEntry("tenantId", TENANT)
                .containsEntry("terminalId", TERMINAL).containsEntry("cajeroId", ACTOR_ID)
                .containsEntry("aperturaAt", MOMENTO).containsEntry("fondoInicial", dec("50.00"))
                .containsEntry("actor", ACTOR.codigo());
    }

    @Test
    void aUniqueViolationOrAnInsertThatTouchesNoRowIsReportedAsDuplicate() {
        jdbc.failsWith(INSERT, new DuplicateKeyException("uk_turno_terminal_abierto"));
        assertThat(adapter.insertar(turno(EstadoTurno.ABIERTO, "50.00"))).isEqualTo(GuardadoOutcome.DUPLICADO);

        var sinFilas = new JdbcClientStub().updates(INSERT, 0);
        var otro = new TurnoJdbcWriteAdapter(sinFilas.client(), TransactionOperations.withoutTransaction());
        assertThat(otro.insertar(turno(EstadoTurno.ABIERTO, "50.00"))).isEqualTo(GuardadoOutcome.DUPLICADO);
    }

    @Test
    void locksTheTurnoRowWhenLoadingItForUpdate() {
        jdbc.rows("FOR UPDATE OF tc", Rows.turno());

        var encontrado = adapter.findPorIdParaActualizar(TENANT, TURNO).orElseThrow();

        assertThat(encontrado.id()).isEqualTo(TURNO);
        assertThat(encontrado.tenantId()).isEqualTo(TENANT);
        assertThat(encontrado.estado()).isEqualTo(EstadoTurno.ABIERTO);
        assertThat(encontrado.fondoInicial()).isEqualTo(dec("50.00"));
        assertThat(jdbc.statementContaining("FOR UPDATE OF tc").params())
                .containsEntry("tenantId", TENANT).containsEntry("turnoId", TURNO);
    }

    @Test
    void aMissingTurnoIsEmpty() {
        assertThat(adapter.findPorIdParaActualizar(TENANT, TURNO)).isEmpty();
    }

    @Test
    void sumsTheConfirmedCashPaymentsOfTheTurno() {
        jdbc.scalar("COALESCE(SUM(p.monto), 0)", dec("40.00"));

        assertThat(adapter.totalVentasEfectivo(TENANT, TURNO)).isEqualTo(dec("40.00"));
        assertThat(jdbc.statementContaining("COALESCE(SUM(p.monto), 0)").params())
                .containsEntry("tenantId", TENANT).containsEntry("turnoId", TURNO);
    }

    @Test
    void updatesTheCloseOnlyWhileTheTurnoIsOpen() {
        var cerrado = turno(EstadoTurno.ABIERTO, "100.00")
                .cerrar(dec("40"), dec("135"), "Cierre", MOMENTO.toInstant())
                .fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(adapter.actualizarCierre(cerrado, ACTOR)).isTrue();

        var statement = jdbc.statementContaining(UPDATE);
        assertThat(statement.sql()).contains("estado = 'ABIERTO'");
        assertThat(statement.params())
                .containsEntry("turnoId", TURNO).containsEntry("tenantId", TENANT)
                .containsEntry("cierreAt", MOMENTO).containsEntry("totalVentas", dec("40.00"))
                .containsEntry("totalSistema", dec("140.00")).containsEntry("totalDeclarado", dec("135.00"))
                .containsEntry("diferencia", dec("-5.00")).containsEntry("observacion", "Cierre")
                .containsEntry("actor", ACTOR.codigo());
    }

    @Test
    void reportsALostUpdateWhenNoRowIsTouched() {
        jdbc.updates(UPDATE, 0);
        var cerrado = turno(EstadoTurno.ABIERTO, "100.00")
                .cerrar(dec("0"), dec("100"), null, MOMENTO.toInstant())
                .fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(adapter.actualizarCierre(cerrado, ACTOR)).isFalse();
    }
}
```

`TEST/infrastructure/persistence/read/adapter/VentasJdbcReadAdapterTest.java`:

```java
package com.softprimesolutions.ventas.infrastructure.persistence.read.adapter;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.ventas.infrastructure.persistence.JdbcClientStub;
import com.softprimesolutions.ventas.infrastructure.persistence.Rows;
import org.junit.jupiter.api.Test;

class VentasJdbcReadAdapterTest {

    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final VentasJdbcReadAdapter adapter = new VentasJdbcReadAdapter(jdbc.client());

    @Test
    void findsATurnoByIdAsAResult() {
        jdbc.rows("tc.uuid_publico = :turnoId", Rows.turno());

        var turno = adapter.findTurno(TENANT, TURNO).orElseThrow();

        assertThat(turno.id()).isEqualTo(TURNO);
        assertThat(turno.terminalId()).isEqualTo(TERMINAL);
        assertThat(turno.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(turno.cajeroId()).isEqualTo(ACTOR_ID);
        assertThat(turno.estado()).isEqualTo("ABIERTO");
        assertThat(turno.fondoInicial()).isEqualTo(dec("50.00"));
        assertThat(jdbc.statementContaining("tc.uuid_publico = :turnoId").params())
                .containsEntry("tenantId", TENANT).containsEntry("turnoId", TURNO);
    }

    @Test
    void findsTheOpenTurnoOfATerminalIncludingOnesBeingCounted() {
        jdbc.rows("tc.estado IN ('ABIERTO', 'EN_ARQUEO')", Rows.turno());

        var turno = adapter.findTurnoAbierto(TENANT, TERMINAL).orElseThrow();

        assertThat(turno.id()).isEqualTo(TURNO);
        assertThat(jdbc.statementContaining("tc.estado IN ('ABIERTO', 'EN_ARQUEO')").params())
                .containsEntry("tenantId", TENANT).containsEntry("terminalId", TERMINAL);
    }

    @Test
    void missingTurnosAreEmpty() {
        assertThat(adapter.findTurno(TENANT, TURNO)).isEmpty();
        assertThat(adapter.findTurnoAbierto(TENANT, TERMINAL)).isEmpty();
    }
}
```

`TEST/infrastructure/configuration/VentasModuleConfigurationTest.java`:

```java
package com.softprimesolutions.ventas.infrastructure.configuration;

import static com.softprimesolutions.ventas.VentasFixtures.TRANSACCION_DIRECTA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort;
import com.softprimesolutions.ventas.application.port.out.TurnoWritePort;
import com.softprimesolutions.ventas.application.port.out.VentasReadPort;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class VentasModuleConfigurationTest {

    private final VentasModuleConfiguration configuration = new VentasModuleConfiguration();
    private final TurnoWritePort turnos = mock(TurnoWritePort.class);
    private final ReferenciasVentasPort referencias = mock(ReferenciasVentasPort.class);
    private final VentasReadPort readPort = mock(VentasReadPort.class);
    private final ClockPort clock = configuration.ventasClockPort();
    private final IdentifierGenerator identifiers = configuration.ventasIdentifierGenerator();

    @Test
    void providesAClockCloseToNowAndUniqueIdentifiers() {
        assertThat(Duration.between(clock.now(), Instant.now()).abs()).isLessThan(Duration.ofSeconds(5));
        assertThat(identifiers.next()).isNotEqualTo(identifiers.next());
    }

    @Test
    void wiresTheTurnoUseCases() {
        var consultas = configuration.consultarTurnosUseCase(readPort);

        assertThat(consultas).isNotNull();
        assertThat(configuration.abrirTurnoUseCase(turnos, referencias, consultas, identifiers, clock)).isNotNull();
        assertThat(configuration.cerrarTurnoUseCase(turnos, consultas, TRANSACCION_DIRECTA, clock)).isNotNull();
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `.\gradlew.bat :modules:ventas:test`
Expected: FAIL de compilación (adapters, `JdbcColumns`, `TurnoRows` y configuración no existen).

- [ ] **Step 3: Implementar**

`MAIN/infrastructure/persistence/JdbcColumns.java`:

```java
package com.softprimesolutions.ventas.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

public final class JdbcColumns {

    private JdbcColumns() {
    }

    public static UUID uuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    public static LocalDate date(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDate.class);
    }

    public static Instant instant(ResultSet rs, String column) throws SQLException {
        return Optional.ofNullable(rs.getObject(column, OffsetDateTime.class))
                .map(OffsetDateTime::toInstant)
                .orElse(null);
    }

    public static OffsetDateTime offset(Instant value) {
        return Optional.ofNullable(value).map(instant -> instant.atOffset(ZoneOffset.UTC)).orElse(null);
    }
}
```

`MAIN/infrastructure/persistence/TurnoRows.java`:

```java
package com.softprimesolutions.ventas.infrastructure.persistence;

import com.softprimesolutions.ventas.domain.model.EstadoTurno;
import com.softprimesolutions.ventas.domain.model.TurnoCaja;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class TurnoRows {

    public static final String SELECT = """
            SELECT tc.uuid_publico, tp.uuid_publico AS terminal_uuid, es.uuid_publico AS establecimiento_uuid,
                   m.uuid_publico AS cajero_uuid, tc.apertura_at, tc.fondo_inicial, tc.estado, tc.cierre_at,
                   tc.total_ventas_sistema, tc.total_sistema, tc.total_declarado, tc.diferencia,
                   tc.observacion_cierre
              FROM sch_venta.turno_caja tc
              JOIN sch_admin.tenant t ON t.id = tc.tenant_id
              JOIN sch_organizacion.terminal_pos tp ON tp.id = tc.terminal_id AND tp.tenant_id = tc.tenant_id
              JOIN sch_organizacion.establecimiento_farmaceutico es
                ON es.id = tc.establecimiento_id AND es.tenant_id = tc.tenant_id
              JOIN sch_seguridad.membership m ON m.id = tc.cajero_usuario_id
             WHERE t.uuid_publico = :tenantId AND tc.es_activo = '1'
            """;
    public static final String POR_ID = SELECT + " AND tc.uuid_publico = :turnoId";
    public static final String ABIERTO_POR_TERMINAL = SELECT
            + " AND tp.uuid_publico = :terminalId AND tc.estado IN ('ABIERTO', 'EN_ARQUEO')";

    private TurnoRows() {
    }

    public static TurnoCaja map(ResultSet rs, UUID tenantId) throws SQLException {
        return TurnoCaja.restore(
                JdbcColumns.uuid(rs, "uuid_publico"), tenantId, JdbcColumns.uuid(rs, "terminal_uuid"),
                JdbcColumns.uuid(rs, "establecimiento_uuid"), new Actor(JdbcColumns.uuid(rs, "cajero_uuid")),
                JdbcColumns.instant(rs, "apertura_at"), rs.getBigDecimal("fondo_inicial"),
                EstadoTurno.valueOf(rs.getString("estado")), JdbcColumns.instant(rs, "cierre_at"),
                rs.getBigDecimal("total_ventas_sistema"), rs.getBigDecimal("total_sistema"),
                rs.getBigDecimal("total_declarado"), rs.getBigDecimal("diferencia"),
                rs.getString("observacion_cierre"));
    }
}
```

`MAIN/infrastructure/persistence/write/adapter/SpringTransaccionAdapter.java`:

```java
package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.port.out.TransaccionPort;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

@Component
public class SpringTransaccionAdapter implements TransaccionPort {

    private final TransactionOperations transaction;

    public SpringTransaccionAdapter(TransactionOperations transaction) {
        this.transaction = transaction;
    }

    @Override
    public <T> Result<T, ApplicationError> ejecutar(Supplier<Result<T, ApplicationError>> trabajo) {
        return transaction.execute(status -> {
            var resultado = trabajo.get();
            if (resultado.isFailure()) status.setRollbackOnly();
            return resultado;
        });
    }
}
```

`MAIN/infrastructure/persistence/write/adapter/ReferenciasVentasJdbcAdapter.java`:

```java
package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort;
import com.softprimesolutions.ventas.infrastructure.persistence.JdbcColumns;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ReferenciasVentasJdbcAdapter implements ReferenciasVentasPort {

    private static final String TERMINAL = """
            SELECT tp.uuid_publico, es.uuid_publico AS establecimiento_uuid, tp.codigo,
                   (tp.es_activo = '1' AND tp.estado = 'ACTIVO') AS operable
              FROM sch_organizacion.terminal_pos tp
              JOIN sch_admin.tenant t ON t.id = tp.tenant_id
              JOIN sch_organizacion.establecimiento_farmaceutico es
                ON es.id = tp.establecimiento_id AND es.tenant_id = tp.tenant_id
             WHERE t.uuid_publico = :tenantId AND tp.uuid_publico = :terminalId
            """;

    private final JdbcClient jdbcClient;

    public ReferenciasVentasJdbcAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TerminalRef> terminal(UUID tenantId, UUID terminalId) {
        return jdbcClient.sql(TERMINAL)
                .param("tenantId", tenantId)
                .param("terminalId", terminalId)
                .query((rs, rowNumber) -> new TerminalRef(
                        JdbcColumns.uuid(rs, "uuid_publico"), JdbcColumns.uuid(rs, "establecimiento_uuid"),
                        rs.getString("codigo"), rs.getBoolean("operable")))
                .optional();
    }
}
```

`MAIN/infrastructure/persistence/write/adapter/TurnoJdbcWriteAdapter.java`:

```java
package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import com.softprimesolutions.ventas.application.port.out.GuardadoOutcome;
import com.softprimesolutions.ventas.application.port.out.TurnoWritePort;
import com.softprimesolutions.ventas.domain.model.TurnoCaja;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import com.softprimesolutions.ventas.infrastructure.persistence.JdbcColumns;
import com.softprimesolutions.ventas.infrastructure.persistence.TurnoRows;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionOperations;

@Repository
public class TurnoJdbcWriteAdapter implements TurnoWritePort {

    private static final String POR_ID_PARA_ACTUALIZAR = TurnoRows.POR_ID + " FOR UPDATE OF tc";
    private static final String INSERTAR = """
            INSERT INTO sch_venta.turno_caja
                (uuid_publico, tenant_id, empresa_id, establecimiento_id, terminal_id, cajero_usuario_id,
                 apertura_at, fondo_inicial, estado, total_ventas_sistema, total_ingresos_sistema,
                 total_retiros_sistema, total_sistema, created_at, created_by)
            SELECT :turnoId, tp.tenant_id, tp.empresa_id, tp.establecimiento_id, tp.id,
                   (SELECT m.id FROM sch_seguridad.membership m
                     WHERE m.uuid_publico = :cajeroId AND m.tenant_id = tp.tenant_id),
                   :aperturaAt, :fondoInicial, 'ABIERTO', 0, 0, 0, :fondoInicial, :aperturaAt, :actor
              FROM sch_organizacion.terminal_pos tp
              JOIN sch_admin.tenant t ON t.id = tp.tenant_id
             WHERE t.uuid_publico = :tenantId AND tp.uuid_publico = :terminalId
            """;
    private static final String TOTAL_VENTAS = """
            SELECT COALESCE(SUM(p.monto), 0)
              FROM sch_venta.pago_venta p
              JOIN sch_venta.venta v ON v.tenant_id = p.tenant_id AND v.id = p.venta_id
              JOIN sch_venta.turno_caja tc ON tc.tenant_id = v.tenant_id AND tc.id = v.turno_caja_id
              JOIN sch_admin.tenant t ON t.id = tc.tenant_id
             WHERE t.uuid_publico = :tenantId AND tc.uuid_publico = :turnoId
               AND v.estado = 'CONFIRMADA' AND v.es_activo = '1'
               AND p.estado = 'CONFIRMADO' AND p.es_activo = '1'
            """;
    private static final String ACTUALIZAR_CIERRE = """
            UPDATE sch_venta.turno_caja
               SET estado = 'CERRADO', cierre_at = :cierreAt, total_ventas_sistema = :totalVentas,
                   total_sistema = :totalSistema, total_declarado = :totalDeclarado, diferencia = :diferencia,
                   observacion_cierre = :observacion, updated_at = :cierreAt, updated_by = :actor
             WHERE uuid_publico = :turnoId AND estado = 'ABIERTO'
               AND tenant_id = (SELECT id FROM sch_admin.tenant WHERE uuid_publico = :tenantId)
            """;

    private final JdbcClient jdbcClient;
    private final TransactionOperations transaction;

    public TurnoJdbcWriteAdapter(JdbcClient jdbcClient, TransactionOperations transaction) {
        this.jdbcClient = jdbcClient;
        this.transaction = transaction;
    }

    @Override
    public GuardadoOutcome insertar(TurnoCaja turno) {
        try {
            transaction.executeWithoutResult(status -> exigirUnaFila(insertarFila(turno)));
            return GuardadoOutcome.GUARDADO;
        } catch (DataIntegrityViolationException | FilaNoInsertada exception) {
            return GuardadoOutcome.DUPLICADO;
        }
    }

    @Override
    public Optional<TurnoCaja> findPorIdParaActualizar(UUID tenantId, UUID turnoId) {
        return jdbcClient.sql(POR_ID_PARA_ACTUALIZAR)
                .param("tenantId", tenantId)
                .param("turnoId", turnoId)
                .query((rs, rowNumber) -> TurnoRows.map(rs, tenantId))
                .optional();
    }

    @Override
    public BigDecimal totalVentasEfectivo(UUID tenantId, UUID turnoId) {
        return jdbcClient.sql(TOTAL_VENTAS)
                .param("tenantId", tenantId)
                .param("turnoId", turnoId)
                .query(BigDecimal.class)
                .single();
    }

    @Override
    public boolean actualizarCierre(TurnoCaja turno, Actor actor) {
        return jdbcClient.sql(ACTUALIZAR_CIERRE)
                .param("turnoId", turno.id())
                .param("tenantId", turno.tenantId())
                .param("cierreAt", JdbcColumns.offset(turno.cierreAt()))
                .param("totalVentas", turno.totalVentasSistema())
                .param("totalSistema", turno.totalSistema())
                .param("totalDeclarado", turno.totalDeclarado())
                .param("diferencia", turno.diferencia())
                .param("observacion", turno.observacionCierre())
                .param("actor", actor.codigo())
                .update() == 1;
    }

    private int insertarFila(TurnoCaja turno) {
        return jdbcClient.sql(INSERTAR)
                .param("turnoId", turno.id())
                .param("tenantId", turno.tenantId())
                .param("terminalId", turno.terminalId())
                .param("cajeroId", turno.cajero().id())
                .param("aperturaAt", JdbcColumns.offset(turno.aperturaAt()))
                .param("fondoInicial", turno.fondoInicial())
                .param("actor", turno.cajero().codigo())
                .update();
    }

    private static void exigirUnaFila(int filas) {
        if (filas != 1) throw new FilaNoInsertada();
    }

    private static final class FilaNoInsertada extends RuntimeException {

        private static final long serialVersionUID = 1L;

        FilaNoInsertada() {
            super(null, null, false, false);
        }
    }
}
```

`MAIN/infrastructure/persistence/read/adapter/VentasJdbcReadAdapter.java`:

```java
package com.softprimesolutions.ventas.infrastructure.persistence.read.adapter;

import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.mapper.VentasApplicationMapper;
import com.softprimesolutions.ventas.application.port.out.VentasReadPort;
import com.softprimesolutions.ventas.infrastructure.persistence.TurnoRows;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class VentasJdbcReadAdapter implements VentasReadPort {

    private final JdbcClient jdbcClient;

    public VentasJdbcReadAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TurnoResult> findTurno(UUID tenantId, UUID turnoId) {
        return jdbcClient.sql(TurnoRows.POR_ID)
                .param("tenantId", tenantId)
                .param("turnoId", turnoId)
                .query((rs, rowNumber) -> VentasApplicationMapper.toResult(TurnoRows.map(rs, tenantId)))
                .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TurnoResult> findTurnoAbierto(UUID tenantId, UUID terminalId) {
        return jdbcClient.sql(TurnoRows.ABIERTO_POR_TERMINAL)
                .param("tenantId", tenantId)
                .param("terminalId", terminalId)
                .query((rs, rowNumber) -> VentasApplicationMapper.toResult(TurnoRows.map(rs, tenantId)))
                .optional();
    }
}
```

`MAIN/infrastructure/configuration/VentasModuleConfiguration.java`:

```java
package com.softprimesolutions.ventas.infrastructure.configuration;

import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.ventas.application.port.in.AbrirTurnoUseCase;
import com.softprimesolutions.ventas.application.port.in.CerrarTurnoUseCase;
import com.softprimesolutions.ventas.application.port.in.ConsultarTurnosUseCase;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort;
import com.softprimesolutions.ventas.application.port.out.TransaccionPort;
import com.softprimesolutions.ventas.application.port.out.TurnoWritePort;
import com.softprimesolutions.ventas.application.port.out.VentasReadPort;
import com.softprimesolutions.ventas.application.usecase.command.AbrirTurnoHandler;
import com.softprimesolutions.ventas.application.usecase.command.CerrarTurnoHandler;
import com.softprimesolutions.ventas.application.usecase.query.ConsultarTurnosHandler;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class VentasModuleConfiguration {

    @Bean
    ClockPort ventasClockPort() {
        return () -> Instant.now(Clock.systemUTC());
    }

    @Bean
    IdentifierGenerator ventasIdentifierGenerator() {
        return UUID::randomUUID;
    }

    @Bean
    ConsultarTurnosUseCase consultarTurnosUseCase(VentasReadPort readPort) {
        return new ConsultarTurnosHandler(readPort);
    }

    @Bean
    AbrirTurnoUseCase abrirTurnoUseCase(
            TurnoWritePort turnos, ReferenciasVentasPort referencias, ConsultarTurnosUseCase consultarTurnosUseCase,
            IdentifierGenerator ventasIdentifierGenerator, ClockPort ventasClockPort) {
        return new AbrirTurnoHandler(
                turnos, referencias, consultarTurnosUseCase, ventasIdentifierGenerator, ventasClockPort);
    }

    @Bean
    CerrarTurnoUseCase cerrarTurnoUseCase(
            TurnoWritePort turnos, ConsultarTurnosUseCase consultarTurnosUseCase, TransaccionPort transaccion,
            ClockPort ventasClockPort) {
        return new CerrarTurnoHandler(turnos, consultarTurnosUseCase, transaccion, ventasClockPort);
    }
}
```

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `.\gradlew.bat :modules:ventas:test`
Expected: PASS. Si `TurnoJdbcWriteAdapterTest.aUniqueViolation...` falla porque el stub no lanza al invocar `update()` dentro de `executeWithoutResult`, confirmar que `JdbcClientStub.failsWith` lanza la excepción en `update()` (lo hace) y que `TransactionOperations.withoutTransaction()` la propaga.

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/ventas
git commit -m "feat(ventas): adapters JDBC y configuracion del turno de caja

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 5: API REST del turno de caja

**Files:**
- Create: `MAIN/api/dto/request/AbrirTurnoRequest.java`, `CerrarTurnoRequest.java`
- Create: `MAIN/api/dto/response/TurnoResponse.java`
- Create: `MAIN/api/mapper/VentasApiMapper.java`
- Create: `MAIN/api/controller/VentasControllerSupport.java`, `TurnoController.java`
- Test: `TEST/api/mapper/VentasApiMapperTest.java`, `TEST/api/controller/TurnoControllerTest.java`

**Interfaces:**
- Consumes: casos de uso de la Task 3.
- Produces: `POST /api/v1/ventas/turnos` (201), `GET /api/v1/ventas/turnos/actual?terminalId=` (200), `GET /api/v1/ventas/turnos/{turnoId}` (200), `POST /api/v1/ventas/turnos/{turnoId}/cierre` (200). Errores como `ProblemDetail` por `ApplicationErrorHttpMapper`. `VentasApiMapper.toCommand(UUID tenantId, UUID actorId, AbrirTurnoRequest)`, `toCommand(UUID tenantId, UUID actorId, UUID turnoId, CerrarTurnoRequest)`, `toResponse(TurnoResult): TurnoResponse`.

- [ ] **Step 1: Escribir los tests que fallan**

`TEST/api/mapper/VentasApiMapperTest.java`:

```java
package com.softprimesolutions.ventas.api.mapper;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.turnoResult;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.ventas.api.dto.request.AbrirTurnoRequest;
import com.softprimesolutions.ventas.api.dto.request.CerrarTurnoRequest;
import org.junit.jupiter.api.Test;

class VentasApiMapperTest {

    @Test
    void mapsTheOpenRequestToACommandWithTheTenantAndActorFromTheToken() {
        var command = VentasApiMapper.toCommand(TENANT, ACTOR_ID, new AbrirTurnoRequest(TERMINAL, dec("50")));

        assertThat(command.tenantId()).isEqualTo(TENANT);
        assertThat(command.actorId()).isEqualTo(ACTOR_ID);
        assertThat(command.terminalId()).isEqualTo(TERMINAL);
        assertThat(command.fondoInicial()).isEqualTo(dec("50"));
    }

    @Test
    void mapsTheCloseRequestToACommand() {
        var command = VentasApiMapper.toCommand(
                TENANT, ACTOR_ID, TURNO, new CerrarTurnoRequest(dec("135"), "Cierre"));

        assertThat(command.tenantId()).isEqualTo(TENANT);
        assertThat(command.actorId()).isEqualTo(ACTOR_ID);
        assertThat(command.turnoId()).isEqualTo(TURNO);
        assertThat(command.totalDeclarado()).isEqualTo(dec("135"));
        assertThat(command.observacion()).isEqualTo("Cierre");
    }

    @Test
    void mapsATurnoResultToItsResponse() {
        var response = VentasApiMapper.toResponse(turnoResult());

        assertThat(response.id()).isEqualTo(TURNO);
        assertThat(response.terminalId()).isEqualTo(TERMINAL);
        assertThat(response.cajeroId()).isEqualTo(ACTOR_ID);
        assertThat(response.estado()).isEqualTo("ABIERTO");
        assertThat(response.fondoInicial()).isEqualTo(dec("50.00"));
        assertThat(response.totalSistema()).isEqualTo(dec("50.00"));
        assertThat(response.totalDeclarado()).isNull();
    }
}
```

`TEST/api/controller/TurnoControllerTest.java`:

```java
package com.softprimesolutions.ventas.api.controller;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.conflict;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.ok;
import static com.softprimesolutions.ventas.VentasFixtures.turnoResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.ventas.api.dto.request.AbrirTurnoRequest;
import com.softprimesolutions.ventas.api.dto.request.CerrarTurnoRequest;
import com.softprimesolutions.ventas.api.dto.response.TurnoResponse;
import com.softprimesolutions.ventas.application.dto.command.AbrirTurnoCommand;
import com.softprimesolutions.ventas.application.dto.command.CerrarTurnoCommand;
import com.softprimesolutions.ventas.application.dto.query.ObtenerTurnoQuery;
import com.softprimesolutions.ventas.application.dto.query.TurnoActualQuery;
import com.softprimesolutions.ventas.application.port.in.ConsultarTurnosUseCase;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

class TurnoControllerTest {

    private static final Jwt JWT = Jwt.withTokenValue("token").header("alg", "none")
            .subject(ACTOR_ID.toString()).claim("tid", TENANT.toString()).build();

    private final ConsultarTurnosUseCase consultas = mock(ConsultarTurnosUseCase.class);

    private static void assertConflict(ResponseEntity<?> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
    }

    @Test
    void opensATurnoWithTheTenantAndActorOfTheTokenAndAnswersCreated() {
        var received = new AtomicReference<AbrirTurnoCommand>();
        var controller = new TurnoController(command -> {
            received.set(command);
            return ok(turnoResult());
        }, command -> conflict(), consultas);

        var response = controller.open(JWT, new AbrirTurnoRequest(TERMINAL, dec("50")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(((TurnoResponse) response.getBody()).id()).isEqualTo(TURNO);
        assertThat(received.get().tenantId()).isEqualTo(TENANT);
        assertThat(received.get().actorId()).isEqualTo(ACTOR_ID);
        assertThat(received.get().terminalId()).isEqualTo(TERMINAL);
        assertThat(received.get().fondoInicial()).isEqualTo(dec("50"));
    }

    @Test
    void anOpenFailureBecomesAProblem() {
        var controller = new TurnoController(command -> conflict(), command -> conflict(), consultas);

        assertConflict(controller.open(JWT, new AbrirTurnoRequest(TERMINAL, dec("50"))));
    }

    @Test
    void closesATurnoAndAnswersOk() {
        var received = new AtomicReference<CerrarTurnoCommand>();
        var controller = new TurnoController(command -> conflict(), command -> {
            received.set(command);
            return ok(turnoResult());
        }, consultas);

        var response = controller.close(JWT, TURNO, new CerrarTurnoRequest(dec("135"), "Cierre"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((TurnoResponse) response.getBody()).id()).isEqualTo(TURNO);
        assertThat(received.get().turnoId()).isEqualTo(TURNO);
        assertThat(received.get().tenantId()).isEqualTo(TENANT);
        assertThat(received.get().actorId()).isEqualTo(ACTOR_ID);
        assertThat(received.get().totalDeclarado()).isEqualTo(dec("135"));
    }

    @Test
    void aCloseFailureBecomesAProblem() {
        var controller = new TurnoController(command -> conflict(), command -> conflict(), consultas);

        assertConflict(controller.close(JWT, TURNO, new CerrarTurnoRequest(dec("135"), null)));
    }

    @Test
    void getsATurnoById() {
        when(consultas.obtener(new ObtenerTurnoQuery(TENANT, TURNO))).thenReturn(ok(turnoResult()));
        var controller = new TurnoController(command -> conflict(), command -> conflict(), consultas);

        var response = controller.getById(JWT, TURNO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((TurnoResponse) response.getBody()).id()).isEqualTo(TURNO);
    }

    @Test
    void aMissingTurnoByIdBecomesAProblem() {
        when(consultas.obtener(new ObtenerTurnoQuery(TENANT, TURNO))).thenReturn(conflict());
        var controller = new TurnoController(command -> conflict(), command -> conflict(), consultas);

        assertConflict(controller.getById(JWT, TURNO));
    }

    @Test
    void getsTheCurrentTurnoOfATerminal() {
        when(consultas.actual(new TurnoActualQuery(TENANT, TERMINAL))).thenReturn(ok(turnoResult()));
        var controller = new TurnoController(command -> conflict(), command -> conflict(), consultas);

        var response = controller.current(JWT, TERMINAL);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((TurnoResponse) response.getBody()).terminalId()).isEqualTo(TERMINAL);
    }

    @Test
    void aTerminalWithoutAnOpenTurnoBecomesAProblem() {
        when(consultas.actual(new TurnoActualQuery(TENANT, TERMINAL))).thenReturn(conflict());
        var controller = new TurnoController(command -> conflict(), command -> conflict(), consultas);

        assertConflict(controller.current(JWT, TERMINAL));
    }
}
```

- [ ] **Step 2: Ejecutar y verificar que fallan**

Run: `.\gradlew.bat :modules:ventas:test --tests "*VentasApiMapperTest" --tests "*TurnoControllerTest"`
Expected: FAIL de compilación (controller, DTOs y mapper no existen).

- [ ] **Step 3: Implementar**

`MAIN/api/dto/request/AbrirTurnoRequest.java`:

```java
package com.softprimesolutions.ventas.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record AbrirTurnoRequest(
        @NotNull UUID terminalId,
        @NotNull @DecimalMin("0.00") BigDecimal fondoInicial) {
}
```

`MAIN/api/dto/request/CerrarTurnoRequest.java`:

```java
package com.softprimesolutions.ventas.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CerrarTurnoRequest(
        @NotNull @DecimalMin("0.00") BigDecimal totalDeclarado,
        @Size(max = 1000) String observacion) {
}
```

`MAIN/api/dto/response/TurnoResponse.java`:

```java
package com.softprimesolutions.ventas.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TurnoResponse(
        UUID id,
        UUID terminalId,
        UUID establecimientoId,
        UUID cajeroId,
        Instant aperturaAt,
        BigDecimal fondoInicial,
        String estado,
        Instant cierreAt,
        BigDecimal totalVentasSistema,
        BigDecimal totalSistema,
        BigDecimal totalDeclarado,
        BigDecimal diferencia,
        String observacionCierre) {
}
```

`MAIN/api/mapper/VentasApiMapper.java`:

```java
package com.softprimesolutions.ventas.api.mapper;

import com.softprimesolutions.ventas.api.dto.request.AbrirTurnoRequest;
import com.softprimesolutions.ventas.api.dto.request.CerrarTurnoRequest;
import com.softprimesolutions.ventas.api.dto.response.TurnoResponse;
import com.softprimesolutions.ventas.application.dto.command.AbrirTurnoCommand;
import com.softprimesolutions.ventas.application.dto.command.CerrarTurnoCommand;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import java.util.UUID;

public final class VentasApiMapper {

    private VentasApiMapper() {
    }

    public static AbrirTurnoCommand toCommand(UUID tenantId, UUID actorId, AbrirTurnoRequest request) {
        return new AbrirTurnoCommand(tenantId, actorId, request.terminalId(), request.fondoInicial());
    }

    public static CerrarTurnoCommand toCommand(
            UUID tenantId, UUID actorId, UUID turnoId, CerrarTurnoRequest request) {
        return new CerrarTurnoCommand(
                tenantId, actorId, turnoId, request.totalDeclarado(), request.observacion());
    }

    public static TurnoResponse toResponse(TurnoResult result) {
        return new TurnoResponse(
                result.id(), result.terminalId(), result.establecimientoId(), result.cajeroId(),
                result.aperturaAt(), result.fondoInicial(), result.estado(), result.cierreAt(),
                result.totalVentasSistema(), result.totalSistema(), result.totalDeclarado(), result.diferencia(),
                result.observacionCierre());
    }
}
```

`MAIN/api/controller/VentasControllerSupport.java`:

```java
package com.softprimesolutions.ventas.api.controller;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.web.error.ApplicationErrorHttpMapper;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

final class VentasControllerSupport {

    private static final String TENANT_CLAIM = "tid";

    private VentasControllerSupport() {
    }

    static ResponseEntity<ProblemDetail> problem(ApplicationError error) {
        var problem = ApplicationErrorHttpMapper.toProblemDetail(error);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    static UUID tenantOf(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString(TENANT_CLAIM));
    }

    static UUID actorOf(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
```

`MAIN/api/controller/TurnoController.java`:

```java
package com.softprimesolutions.ventas.api.controller;

import com.softprimesolutions.ventas.api.dto.request.AbrirTurnoRequest;
import com.softprimesolutions.ventas.api.dto.request.CerrarTurnoRequest;
import com.softprimesolutions.ventas.api.mapper.VentasApiMapper;
import com.softprimesolutions.ventas.application.dto.query.ObtenerTurnoQuery;
import com.softprimesolutions.ventas.application.dto.query.TurnoActualQuery;
import com.softprimesolutions.ventas.application.port.in.AbrirTurnoUseCase;
import com.softprimesolutions.ventas.application.port.in.CerrarTurnoUseCase;
import com.softprimesolutions.ventas.application.port.in.ConsultarTurnosUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(TurnoController.BASE_PATH)
public class TurnoController {

    static final String BASE_PATH = "/api/v1/ventas/turnos";

    private final AbrirTurnoUseCase openTurno;
    private final CerrarTurnoUseCase closeTurno;
    private final ConsultarTurnosUseCase queryTurnos;

    public TurnoController(
            AbrirTurnoUseCase openTurno, CerrarTurnoUseCase closeTurno, ConsultarTurnosUseCase queryTurnos) {
        this.openTurno = openTurno;
        this.closeTurno = closeTurno;
        this.queryTurnos = queryTurnos;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ventas.turnos.abrir')")
    public ResponseEntity<?> open(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AbrirTurnoRequest request) {
        var command = VentasApiMapper.toCommand(
                VentasControllerSupport.tenantOf(jwt), VentasControllerSupport.actorOf(jwt), request);
        return openTurno.execute(command).fold(
                result -> ResponseEntity.status(HttpStatus.CREATED).body(VentasApiMapper.toResponse(result)),
                VentasControllerSupport::problem);
    }

    @GetMapping("/actual")
    @PreAuthorize("hasAuthority('ventas.turnos.consultar')")
    public ResponseEntity<?> current(@AuthenticationPrincipal Jwt jwt, @RequestParam UUID terminalId) {
        return queryTurnos.actual(new TurnoActualQuery(VentasControllerSupport.tenantOf(jwt), terminalId))
                .fold(result -> ResponseEntity.ok(VentasApiMapper.toResponse(result)),
                        VentasControllerSupport::problem);
    }

    @GetMapping("/{turnoId}")
    @PreAuthorize("hasAuthority('ventas.turnos.consultar')")
    public ResponseEntity<?> getById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID turnoId) {
        return queryTurnos.obtener(new ObtenerTurnoQuery(VentasControllerSupport.tenantOf(jwt), turnoId))
                .fold(result -> ResponseEntity.ok(VentasApiMapper.toResponse(result)),
                        VentasControllerSupport::problem);
    }

    @PostMapping("/{turnoId}/cierre")
    @PreAuthorize("hasAuthority('ventas.turnos.cerrar')")
    public ResponseEntity<?> close(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID turnoId,
            @Valid @RequestBody CerrarTurnoRequest request) {
        var command = VentasApiMapper.toCommand(
                VentasControllerSupport.tenantOf(jwt), VentasControllerSupport.actorOf(jwt), turnoId, request);
        return closeTurno.execute(command).fold(
                result -> ResponseEntity.ok(VentasApiMapper.toResponse(result)), VentasControllerSupport::problem);
    }
}
```

- [ ] **Step 4: Ejecutar y verificar que pasan**

Run: `.\gradlew.bat :modules:ventas:check`
Expected: PASS (tests unitarios del módulo, cobertura JaCoCo 100% por clase y ArchUnit del módulo). Si JaCoCo reporta ramas sin cubrir, agregar el test faltante; no relajar el umbral.

- [ ] **Step 5: Commit**

```bash
git add service-botica/modules/ventas
git commit -m "feat(ventas): API REST del turno de caja

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Integración HTTP del turno contra PostgreSQL

**Files:**
- Create: `BTEST/ventas/api/TurnoApiIntegrationTest.java`

**Interfaces:**
- Consumes: endpoints de la Task 5, permisos de la Task 1, `RealLogin`, `PostgresTestContainerConfiguration`, API REST de `organizacion` para crear empresa/establecimiento/terminal.

- [ ] **Step 1: Escribir el test**

`BTEST/ventas/api/TurnoApiIntegrationTest.java`:

```java
package com.softprimesolutions.ventas.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.softprimesolutions.security.application.port.out.PasswordHashPort;
import com.softprimesolutions.testsupport.PostgresTestContainerConfiguration;
import com.softprimesolutions.testsupport.RealLogin;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
class TurnoApiIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("b7e1d2c3-4a5f-4e6d-9c8b-1a2b3c4d5e6f");
    private static final String ORG = "/api/v1/organizacion";
    private static final String TURNOS = "/api/v1/ventas/turnos";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PasswordHashPort passwordHash;

    private String bearer;
    private String noPermissions;
    private UUID userId;
    private UUID terminalId;

    @BeforeEach
    void createTenantAdministratorAndATerminal() throws Exception {
        jdbcClient.sql("""
                        INSERT INTO sch_admin.tenant (uuid_publico, codigo, nombre, slug, created_by)
                        VALUES (:tenantId, 'VENTEST', 'Tenant ventas', 'tenant-ventas', 'test')
                        """).param("tenantId", TENANT_ID).update();
        var realLogin = new RealLogin(jdbcClient, mockMvc, passwordHash);
        var session = realLogin.login(TENANT_ID, "ven.admin", "^(organizacion|ventas)\\.");
        userId = session.userId();
        bearer = session.bearer();
        noPermissions = realLogin.login(TENANT_ID, "sin.permisos", null).bearer();

        var empresaId = created(post(ORG + "/empresas").content("""
                {"tenantId":"%s","ruc":"20123456786","razonSocial":"Boticas Ventas SAC",
                 "monedaFuncional":"PEN","zonaHoraria":"America/Lima","permiteVentaOnline":false}
                """.formatted(TENANT_ID)));
        var establecimientoId = created(post(ORG + "/establecimientos").content("""
                {"tenantId":"%s","empresaId":"%s","codigo":"EST001","nombre":"Botica Central",
                 "tipoEstablecimiento":"BOTICA","codigoAnexoSunat":"0001","esPrincipal":true,
                 "permiteVentaOnline":false,"permiteDelivery":false,"perfilOperacion":"ONLINE",
                 "zonaHoraria":"America/Lima"}
                """.formatted(TENANT_ID, empresaId)));
        terminalId = created(post(ORG + "/terminales-pos").content("""
                {"establecimientoId":"%s","codigo":"POS001","nombre":"Caja 1",
                 "serieBoletaDefecto":"B001","serieFacturaDefecto":"F001","storeEdgeHabilitado":false}
                """.formatted(establecimientoId)));
    }

    @Test
    void opensConsultsAndClosesATurnoComputingTheCashDifference() throws Exception {
        var apertura = abrir("50.00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ABIERTO"))
                .andExpect(jsonPath("$.terminalId").value(terminalId.toString()))
                .andExpect(jsonPath("$.cajeroId").value(userId.toString()))
                .andExpect(jsonPath("$.fondoInicial").value(50.0))
                .andExpect(jsonPath("$.totalVentasSistema").value(0.0))
                .andExpect(jsonPath("$.totalSistema").value(50.0))
                .andReturn().getResponse().getContentAsString();
        var turnoId = UUID.fromString(JsonPath.read(apertura, "$.id"));

        mockMvc.perform(get(TURNOS + "/actual").header("Authorization", bearer)
                        .param("terminalId", terminalId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(turnoId.toString()));

        mockMvc.perform(post(TURNOS + "/{id}/cierre", turnoId).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalDeclarado\":45.50,\"observacion\":\"Faltante\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADO"))
                .andExpect(jsonPath("$.totalVentasSistema").value(0.0))
                .andExpect(jsonPath("$.totalSistema").value(50.0))
                .andExpect(jsonPath("$.totalDeclarado").value(45.5))
                .andExpect(jsonPath("$.diferencia").value(-4.5))
                .andExpect(jsonPath("$.observacionCierre").value("Faltante"));

        mockMvc.perform(get(TURNOS + "/{id}", turnoId).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADO"));
        mockMvc.perform(get(TURNOS + "/actual").header("Authorization", bearer)
                        .param("terminalId", terminalId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEN_TURNO_NO_ENCONTRADO"));

        var fila = jdbcClient.sql("""
                        SELECT tc.created_by, tc.updated_by, m.uuid_publico AS cajero
                          FROM sch_venta.turno_caja tc
                          JOIN sch_seguridad.membership m ON m.id = tc.cajero_usuario_id
                         WHERE tc.uuid_publico = :turnoId
                        """).param("turnoId", turnoId).query().singleRow();
        assertThat(fila).containsEntry("created_by", userId.toString())
                .containsEntry("updated_by", userId.toString());
        assertThat(fila.get("cajero")).hasToString(userId.toString());
    }

    @Test
    void aSecondOpenTurnoOnTheSameTerminalIsAConflictUntilTheFirstIsClosed() throws Exception {
        var primero = abrir("10")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        abrir("10").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VEN_TURNO_YA_ABIERTO"));

        mockMvc.perform(post(TURNOS + "/{id}/cierre", UUID.fromString(JsonPath.read(primero, "$.id")))
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalDeclarado\":10}"))
                .andExpect(status().isOk());
        abrir("20").andExpect(status().isCreated());
    }

    @Test
    void aClosedTurnoCannotBeClosedAgain() throws Exception {
        var apertura = abrir("10")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var turnoId = UUID.fromString(JsonPath.read(apertura, "$.id"));
        var cierre = post(TURNOS + "/{id}/cierre", turnoId).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"totalDeclarado\":10}");

        mockMvc.perform(cierre).andExpect(status().isOk());
        mockMvc.perform(cierre).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEN_TURNO_ESTADO_INVALIDO"));
    }

    @Test
    void validatesTheRequestsAndTheirReferences() throws Exception {
        abrir("-1").andExpect(status().isBadRequest());
        mockMvc.perform(post(TURNOS).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fondoInicial\":10}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(TURNOS).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"terminalId\":\"%s\",\"fondoInicial\":10}".formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEN_TERMINAL_NO_ENCONTRADA"));
        mockMvc.perform(get(TURNOS + "/{id}", UUID.randomUUID()).header("Authorization", bearer))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(TURNOS + "/{id}/cierre", UUID.randomUUID()).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"totalDeclarado\":10}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEN_TURNO_NO_ENCONTRADO"));
    }

    @Test
    void aTerminalThatIsNotActiveCannotOpenATurno() throws Exception {
        jdbcClient.sql("UPDATE sch_organizacion.terminal_pos SET estado = 'BLOQUEADO' WHERE uuid_publico = :id")
                .param("id", terminalId).update();

        abrir("10").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEN_TERMINAL_NO_OPERABLE"));
    }

    @Test
    void deniesAccessWithoutThePermissionOrTheToken() throws Exception {
        mockMvc.perform(post(TURNOS).with(csrf()).header("Authorization", noPermissions)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"terminalId\":\"%s\",\"fondoInicial\":10}".formatted(terminalId)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(TURNOS + "/actual").header("Authorization", noPermissions)
                        .param("terminalId", terminalId.toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(TURNOS + "/actual").param("terminalId", terminalId.toString()))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions abrir(String fondo) throws Exception {
        return mockMvc.perform(post(TURNOS).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"terminalId\":\"%s\",\"fondoInicial\":%s}".formatted(terminalId, fondo)));
    }

    private UUID created(MockHttpServletRequestBuilder request) throws Exception {
        var body = mockMvc.perform(request.header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }
}
```

- [ ] **Step 2: Ejecutar los tests de integración**

Run: `.\gradlew.bat :bootstrap-app:test --tests "com.softprimesolutions.ventas.api.TurnoApiIntegrationTest"` (requiere Docker).
Expected: PASS. El segundo `POST /turnos` se rechaza por la comprobación previa del handler (no por el índice único), así que la transacción del test no queda abortada. La carrera real entre dos aperturas simultáneas la cubre el índice `uk_turno_terminal_abierto` y su test unitario (`DUPLICADO`).

- [ ] **Step 3: Verificación completa del repositorio**

Run: `.\gradlew.bat check --warning-mode all`
Expected: BUILD SUCCESSFUL (compilación, tests, ArchUnit, Spring Modulith `verify()` y JaCoCo al 100% en `ventas`).

- [ ] **Step 4: Commit**

```bash
git add service-botica/bootstrap-app/src/test/java/com/softprimesolutions/ventas
git commit -m "test(ventas): validar el turno de caja contra PostgreSQL

Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage (parte 1):** abrir/cerrar/consultar turno con las reglas del spec (un turno por terminal, terminal `ACTIVO`, cierre directo, `total_sistema = fondo + ventas`, `diferencia`) — Tasks 2, 3, 5, 6; migraciones V033/V034 con huella, secuencia, ensanche de columnas y permisos — Task 1; `allowedDependencies` actualizado — Task 1; JDBC sobre `sch_venta` y actor por `membership` — Task 4; pruebas unitarias 100% e integración HTTP — Tasks 2–6. La venta, el correlativo por terminal, el pago y la concurrencia entre ventas quedan en la parte 2.

**Desviación respecto al spec (decisión de diseño):** el spec decía "medio de pago `EFECTIVO` por tenant" sembrado en V033. Como los tenants se crean dinámicamente (tests y altas reales), la parte 2 lo crea bajo demanda con `INSERT ... ON CONFLICT DO NOTHING` al registrar el pago, en vez de sembrarlo para los tenants existentes.

**Dependencia de la parte 2 sobre esta parte:** `VentasReadPort#findTurnoAbierto`, `TurnoRows`, `VentasFixtures`, `JdbcClientStub`, `VentasErrors`, `TransaccionPort`, `GuardadoOutcome`, `ReferenciasVentasPort`. La parte 2 añadirá `TurnoWritePort#bloquearTurnoAbierto` (`FOR SHARE`) para que un cierre concurrente no deje ventas fuera del total.

**Consistencia de tipos:** `TurnoCaja.restore` (14 parámetros) coincide entre Task 2, `TurnoRows.map` y `VentasFixtures.turno`; `TurnoResult` (13 campos) coincide entre Tasks 3, 4 y 5; `TurnoWritePort` (4 métodos) coincide entre Tasks 3 y 4; las firmas `toCommand` del mapper coinciden con las del controller.
