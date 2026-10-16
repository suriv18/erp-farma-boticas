package com.softprimesolutions.organizacion.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.organizacion.domain.valueobject.AlmacenId;
import com.softprimesolutions.organizacion.domain.valueobject.EstablecimientoId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlmacenTest {

    private static final AlmacenId ID = new AlmacenId(UUID.randomUUID());
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final EstablecimientoId ESTABLECIMIENTO_ID = new EstablecimientoId(UUID.randomUUID());
    private static final Instant NOW = Instant.parse("2026-09-27T00:00:00Z");

    @Test
    void createsWithValidData() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", TipoAlmacen.REFRIGERADO,
                true, true, true, true, true, new BigDecimal("2.0"), new BigDecimal("8.0"), NOW);

        assertThat(result.isSuccess()).isTrue();
        var almacen = result.fold(a -> a, error -> { throw new AssertionError(error.message()); });
        assertThat(almacen.id()).isEqualTo(ID);
        assertThat(almacen.tenantId()).isEqualTo(TENANT_ID);
        assertThat(almacen.establecimientoId()).isEqualTo(ESTABLECIMIENTO_ID);
        assertThat(almacen.codigo()).isEqualTo("WH-01");
        assertThat(almacen.nombre()).isEqualTo("Almacén central");
        assertThat(almacen.tipo()).isEqualTo(TipoAlmacen.REFRIGERADO);
        assertThat(almacen.permiteLotes()).isTrue();
        assertThat(almacen.permiteVencimiento()).isTrue();
        assertThat(almacen.permiteVenta()).isTrue();
        assertThat(almacen.permiteDespacho()).isTrue();
        assertThat(almacen.controlTemperatura()).isTrue();
        assertThat(almacen.temperaturaMinC()).isEqualTo(new BigDecimal("2.0"));
        assertThat(almacen.temperaturaMaxC()).isEqualTo(new BigDecimal("8.0"));
        assertThat(almacen.activo()).isTrue();
        assertThat(almacen.createdAt()).isEqualTo(NOW);
        assertThat(almacen.updatedAt()).isNull();
    }

    @Test
    void createsWithoutTemperatureControl() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-02", "Almacén general", TipoAlmacen.GENERAL,
                true, false, true, true, false, null, null, NOW);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void rejectsNullId() {
        var result = Almacen.create(
                null, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", TipoAlmacen.VENTA,
                true, true, true, true, false, null, null, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullTenantId() {
        var result = Almacen.create(
                ID, null, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", TipoAlmacen.VENTA,
                true, true, true, true, false, null, null, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullEstablecimientoId() {
        var result = Almacen.create(
                ID, TENANT_ID, null, "WH-01", "Almacén central", TipoAlmacen.VENTA,
                true, true, true, true, false, null, null, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsBlankCodigo() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, " ", "Almacén central", TipoAlmacen.VENTA,
                true, true, true, true, false, null, null, NOW);
        assertThat(result.isFailure()).isTrue();
        result.fold(a -> null, error -> {
            assertThat(error.code()).isEqualTo("ORG_ALMACEN_INVALIDO");
            return null;
        });
    }

    @Test
    void rejectsCodigoTooLong() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "W".repeat(41), "Almacén central", TipoAlmacen.VENTA,
                true, true, true, true, false, null, null, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullNombre() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", null, TipoAlmacen.VENTA,
                true, true, true, true, false, null, null, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsBlankNombre() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", " ", TipoAlmacen.VENTA,
                true, true, true, true, false, null, null, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNombreTooShort() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "A", TipoAlmacen.VENTA,
                true, true, true, true, false, null, null, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNombreTooLong() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "A".repeat(251), TipoAlmacen.VENTA,
                true, true, true, true, false, null, null, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullTipo() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", null,
                true, true, true, true, false, null, null, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullCreatedAt() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", TipoAlmacen.VENTA,
                true, true, true, true, false, null, null, null);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void restoresWithoutValidation() {
        var almacen = Almacen.restore(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", TipoAlmacen.VENTA,
                true, true, true, true, false, null, null, false, NOW, NOW.plusSeconds(60));

        assertThat(almacen.activo()).isFalse();
        assertThat(almacen.updatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void updatesDetails() {
        var almacen = Almacen.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", TipoAlmacen.VENTA,
                        true, true, true, true, false, null, null, NOW)
                .fold(a -> a, error -> { throw new AssertionError(error.message()); });

        var updated = almacen.updateDetails(
                        "Almacén central renovado", TipoAlmacen.CUARENTENA, false, false, false, false,
                        true, new BigDecimal("2.0"), new BigDecimal("8.0"), NOW.plusSeconds(120))
                .fold(a -> a, error -> { throw new AssertionError(error.message()); });

        assertThat(updated.nombre()).isEqualTo("Almacén central renovado");
        assertThat(updated.tipo()).isEqualTo(TipoAlmacen.CUARENTENA);
        assertThat(updated.controlTemperatura()).isTrue();
        assertThat(updated.updatedAt()).isEqualTo(NOW.plusSeconds(120));
    }

    @Test
    void rejectsUpdateWithNullNombre() {
        var almacen = Almacen.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", TipoAlmacen.VENTA,
                        true, true, true, true, false, null, null, NOW)
                .fold(a -> a, error -> { throw new AssertionError(error.message()); });

        var result = almacen.updateDetails(
                null, TipoAlmacen.VENTA, true, true, true, true, false, null, null, NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithNombreTooShort() {
        var almacen = Almacen.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", TipoAlmacen.VENTA,
                        true, true, true, true, false, null, null, NOW)
                .fold(a -> a, error -> { throw new AssertionError(error.message()); });

        var result = almacen.updateDetails(
                "A", TipoAlmacen.VENTA, true, true, true, true, false, null, null, NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithNombreTooLong() {
        var almacen = Almacen.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", TipoAlmacen.VENTA,
                        true, true, true, true, false, null, null, NOW)
                .fold(a -> a, error -> { throw new AssertionError(error.message()); });

        var result = almacen.updateDetails(
                "A".repeat(251), TipoAlmacen.VENTA, true, true, true, true, false, null, null,
                NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithNullTipo() {
        var almacen = Almacen.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", TipoAlmacen.VENTA,
                        true, true, true, true, false, null, null, NOW)
                .fold(a -> a, error -> { throw new AssertionError(error.message()); });

        var result = almacen.updateDetails(
                "Almacén", null, true, true, true, true, false, null, null, NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithNullUpdatedAt() {
        var almacen = Almacen.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", TipoAlmacen.VENTA,
                        true, true, true, true, false, null, null, NOW)
                .fold(a -> a, error -> { throw new AssertionError(error.message()); });

        var result = almacen.updateDetails(
                "Almacén", TipoAlmacen.VENTA, true, true, true, true, false, null, null, null);

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void deactivatesAndReactivates() {
        var almacen = Almacen.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", TipoAlmacen.VENTA,
                        true, true, true, true, false, null, null, NOW)
                .fold(a -> a, error -> { throw new AssertionError(error.message()); });

        var deactivated = almacen.desactivar(NOW.plusSeconds(60))
                .fold(a -> a, error -> { throw new AssertionError(error.message()); });
        assertThat(deactivated.activo()).isFalse();

        var reactivated = deactivated.activar(NOW.plusSeconds(120))
                .fold(a -> a, error -> { throw new AssertionError(error.message()); });
        assertThat(reactivated.activo()).isTrue();
    }

    @Test
    void rejectsActivarWithNullUpdatedAt() {
        var almacen = Almacen.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", TipoAlmacen.VENTA,
                        true, true, true, true, false, null, null, NOW)
                .fold(a -> a, error -> { throw new AssertionError(error.message()); });

        assertThat(almacen.activar(null).isFailure()).isTrue();
    }

    @Test
    void rejectsDesactivarWithNullUpdatedAt() {
        var almacen = Almacen.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", TipoAlmacen.VENTA,
                        true, true, true, true, false, null, null, NOW)
                .fold(a -> a, error -> { throw new AssertionError(error.message()); });

        assertThat(almacen.desactivar(null).isFailure()).isTrue();
    }

    private static Almacen baseAlmacen() {
        return Almacen.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-09", "Almacén base", TipoAlmacen.GENERAL,
                        true, true, true, true, false, null, null, NOW)
                .fold(almacen -> almacen, error -> { throw new AssertionError(error.message()); });
    }

    private static void assertRejected(Result<Almacen, ErrorDetail> result, String field, String message) {
        assertThat(result.isFailure()).isTrue();
        result.fold(almacen -> null, error -> {
            assertThat(error.message()).isEqualTo(message);
            assertThat(error.metadata()).containsEntry("field", field);
            return null;
        });
    }

    @Test
    void rejectsRefrigeradoWithoutTemperatureControl() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-10", "Cámara fría", TipoAlmacen.REFRIGERADO,
                true, true, true, true, false, new BigDecimal("2"), new BigDecimal("8"), NOW);

        assertRejected(result, "controlTemperatura", "Un almacén refrigerado debe controlar temperatura.");
    }

    @Test
    void rejectsTemperatureControlWithoutMinimum() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-11", "Sin mínima", TipoAlmacen.GENERAL,
                true, true, true, true, true, null, new BigDecimal("8"), NOW);

        assertRejected(result, "temperaturaMinC",
                "Indica la temperatura mínima y máxima cuando el almacén controla temperatura.");
    }

    @Test
    void rejectsTemperatureControlWithoutMaximum() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-12", "Sin máxima", TipoAlmacen.GENERAL,
                true, true, true, true, true, new BigDecimal("2"), null, NOW);

        assertRejected(result, "temperaturaMaxC",
                "Indica la temperatura mínima y máxima cuando el almacén controla temperatura.");
    }

    @Test
    void rejectsTemperatureControlWithoutBothLimitsReportingMinimum() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-15", "Sin límites", TipoAlmacen.GENERAL,
                true, true, true, true, true, null, null, NOW);

        assertRejected(result, "temperaturaMinC",
                "Indica la temperatura mínima y máxima cuando el almacén controla temperatura.");
    }

    @Test
    void rejectsMinimumGreaterThanMaximum() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-13", "Rango invertido", TipoAlmacen.GENERAL,
                true, true, true, true, true, new BigDecimal("8"), new BigDecimal("2"), NOW);

        assertRejected(result, "temperaturaMinC", "La temperatura mínima no puede ser mayor que la máxima.");
    }

    @Test
    void acceptsEqualMinimumAndMaximum() {
        var result = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-14", "Rango puntual", TipoAlmacen.GENERAL,
                true, true, true, true, true, new BigDecimal("5"), new BigDecimal("5"), NOW);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void acceptsASingleTemperatureWhenControlIsOff() {
        var onlyMaximum = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-15", "Solo máxima", TipoAlmacen.GENERAL,
                true, true, true, true, false, null, new BigDecimal("8"), NOW);
        var onlyMinimum = Almacen.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-16", "Solo mínima", TipoAlmacen.GENERAL,
                true, true, true, true, false, new BigDecimal("2"), null, NOW);

        assertThat(onlyMaximum.isSuccess()).isTrue();
        assertThat(onlyMinimum.isSuccess()).isTrue();
    }

    @Test
    void updateDetailsRejectsRefrigeradoWithoutTemperatureControl() {
        var result = baseAlmacen().updateDetails(
                "Cámara fría", TipoAlmacen.REFRIGERADO, true, true, true, true, false, null, null, NOW);

        assertRejected(result, "controlTemperatura", "Un almacén refrigerado debe controlar temperatura.");
    }

    @Test
    void updateDetailsAcceptsACoherentTemperatureRange() {
        var result = baseAlmacen().updateDetails(
                "Cámara fría", TipoAlmacen.REFRIGERADO, true, true, true, true, true,
                new BigDecimal("2"), new BigDecimal("8"), NOW);

        assertThat(result.isSuccess()).isTrue();
    }
}
