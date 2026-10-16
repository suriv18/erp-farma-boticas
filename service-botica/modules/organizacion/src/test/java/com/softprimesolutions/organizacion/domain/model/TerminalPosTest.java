package com.softprimesolutions.organizacion.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.organizacion.domain.valueobject.EstablecimientoId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import com.softprimesolutions.organizacion.domain.valueobject.TerminalPosId;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TerminalPosTest {

    private static final TerminalPosId ID = new TerminalPosId(UUID.randomUUID());
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final EstablecimientoId ESTABLECIMIENTO_ID = new EstablecimientoId(UUID.randomUUID());
    private static final Instant NOW = Instant.parse("2026-09-27T00:00:00Z");

    @Test
    void createsWithValidData() {
        var result = TerminalPos.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", "B001", "F001",
                "SN-12345", "pos-01", "192.168.1.10", "PR-01", true, NOW);

        assertThat(result.isSuccess()).isTrue();
        var terminal = result.fold(t -> t, error -> { throw new AssertionError(error.message()); });
        assertThat(terminal.id()).isEqualTo(ID);
        assertThat(terminal.tenantId()).isEqualTo(TENANT_ID);
        assertThat(terminal.establecimientoId()).isEqualTo(ESTABLECIMIENTO_ID);
        assertThat(terminal.codigo()).isEqualTo("CR-01");
        assertThat(terminal.nombre()).isEqualTo("Caja 1");
        assertThat(terminal.serieBoletaDefecto()).isEqualTo("B001");
        assertThat(terminal.serieFacturaDefecto()).isEqualTo("F001");
        assertThat(terminal.numeroSerieEquipo()).isEqualTo("SN-12345");
        assertThat(terminal.hostname()).isEqualTo("pos-01");
        assertThat(terminal.ipEquipo()).isEqualTo("192.168.1.10");
        assertThat(terminal.impresoraCodigo()).isEqualTo("PR-01");
        assertThat(terminal.storeEdgeHabilitado()).isTrue();
        assertThat(terminal.estado()).isEqualTo(EstadoTerminalPos.ACTIVO);
        assertThat(terminal.createdAt()).isEqualTo(NOW);
        assertThat(terminal.updatedAt()).isNull();
    }

    @Test
    void createsWithOnlyRequiredFields() {
        var result = TerminalPos.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-02", "Caja 2", null, null,
                null, null, null, null, false, NOW);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void rejectsNullId() {
        var result = TerminalPos.create(
                null, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", null, null,
                null, null, null, null, false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullTenantId() {
        var result = TerminalPos.create(
                ID, null, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", null, null,
                null, null, null, null, false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullEstablecimientoId() {
        var result = TerminalPos.create(
                ID, TENANT_ID, null, "CR-01", "Caja 1", null, null,
                null, null, null, null, false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsBlankCodigo() {
        var result = TerminalPos.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, " ", "Caja 1", null, null,
                null, null, null, null, false, NOW);
        assertThat(result.isFailure()).isTrue();
        result.fold(t -> null, error -> {
            assertThat(error.code()).isEqualTo("ORG_TERMINAL_INVALIDO");
            return null;
        });
    }

    @Test
    void rejectsCodigoTooLong() {
        var result = TerminalPos.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "C".repeat(41), "Caja 1", null, null,
                null, null, null, null, false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullNombre() {
        var result = TerminalPos.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", null, null, null,
                null, null, null, null, false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsBlankNombre() {
        var result = TerminalPos.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", " ", null, null,
                null, null, null, null, false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNombreTooShort() {
        var result = TerminalPos.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "A", null, null,
                null, null, null, null, false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNombreTooLong() {
        var result = TerminalPos.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "A".repeat(121), null, null,
                null, null, null, null, false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullCreatedAt() {
        var result = TerminalPos.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", null, null,
                null, null, null, null, false, null);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void restoresWithoutValidation() {
        var terminal = TerminalPos.restore(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", null, null,
                null, null, null, null, false, EstadoTerminalPos.MANTENIMIENTO, NOW, NOW.plusSeconds(60));

        assertThat(terminal.estado()).isEqualTo(EstadoTerminalPos.MANTENIMIENTO);
        assertThat(terminal.updatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void updatesDetails() {
        var terminal = TerminalPos.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", null, null,
                        null, null, null, null, false, NOW)
                .fold(t -> t, error -> { throw new AssertionError(error.message()); });

        var updated = terminal.updateDetails(
                        "Caja principal", "B002", "F002", "SN-99999", "pos-02", "192.168.1.20",
                        "PR-02", true, NOW.plusSeconds(120))
                .fold(t -> t, error -> { throw new AssertionError(error.message()); });

        assertThat(updated.nombre()).isEqualTo("Caja principal");
        assertThat(updated.storeEdgeHabilitado()).isTrue();
        assertThat(updated.updatedAt()).isEqualTo(NOW.plusSeconds(120));
    }

    @Test
    void rejectsUpdateWithNullNombre() {
        var terminal = TerminalPos.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", null, null,
                        null, null, null, null, false, NOW)
                .fold(t -> t, error -> { throw new AssertionError(error.message()); });

        var result = terminal.updateDetails(
                null, null, null, null, null, null, null, false, NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithBlankNombre() {
        var terminal = TerminalPos.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", null, null,
                        null, null, null, null, false, NOW)
                .fold(t -> t, error -> { throw new AssertionError(error.message()); });

        var result = terminal.updateDetails(
                " ", null, null, null, null, null, null, false, NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithNombreTooShort() {
        var terminal = TerminalPos.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", null, null,
                        null, null, null, null, false, NOW)
                .fold(t -> t, error -> { throw new AssertionError(error.message()); });

        var result = terminal.updateDetails(
                "A", null, null, null, null, null, null, false, NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithNombreTooLong() {
        var terminal = TerminalPos.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", null, null,
                        null, null, null, null, false, NOW)
                .fold(t -> t, error -> { throw new AssertionError(error.message()); });

        var result = terminal.updateDetails(
                "A".repeat(121), null, null, null, null, null, null, false, NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithNullUpdatedAt() {
        var terminal = TerminalPos.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", null, null,
                        null, null, null, null, false, NOW)
                .fold(t -> t, error -> { throw new AssertionError(error.message()); });

        var result = terminal.updateDetails(
                "Caja 1", null, null, null, null, null, null, false, null);

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void changesEstado() {
        var terminal = TerminalPos.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", null, null,
                        null, null, null, null, false, NOW)
                .fold(t -> t, error -> { throw new AssertionError(error.message()); });

        var result = terminal.cambiarEstado(EstadoTerminalPos.BLOQUEADO, NOW.plusSeconds(60));

        assertThat(result.isSuccess()).isTrue();
        result.fold(t -> {
            assertThat(t.estado()).isEqualTo(EstadoTerminalPos.BLOQUEADO);
            return null;
        }, error -> { throw new AssertionError(error.message()); });
    }

    @Test
    void rejectsEstadoChangeWithNullValue() {
        var terminal = TerminalPos.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", null, null,
                        null, null, null, null, false, NOW)
                .fold(t -> t, error -> { throw new AssertionError(error.message()); });

        assertThat(terminal.cambiarEstado(null, NOW.plusSeconds(60)).isFailure()).isTrue();
    }

    @Test
    void rejectsEstadoChangeWithNullUpdatedAt() {
        var terminal = TerminalPos.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", null, null,
                        null, null, null, null, false, NOW)
                .fold(t -> t, error -> { throw new AssertionError(error.message()); });

        assertThat(terminal.cambiarEstado(EstadoTerminalPos.BLOQUEADO, null).isFailure()).isTrue();
    }

    private static final String BOLETA_MESSAGE =
            "La serie de boleta debe iniciar con B y tener 4 caracteres alfanuméricos en mayúscula.";
    private static final String FACTURA_MESSAGE =
            "La serie de factura debe iniciar con F y tener 4 caracteres alfanuméricos en mayúscula.";

    private static TerminalPos baseTerminal() {
        return TerminalPos.create(
                        ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", null, null,
                        null, null, null, null, false, NOW)
                .fold(t -> t, error -> { throw new AssertionError(error.message()); });
    }

    private static void assertRejected(
            Result<TerminalPos, ErrorDetail> result,
            String field, String message) {
        assertThat(result.isFailure()).isTrue();
        result.fold(t -> null, error -> {
            assertThat(error.code()).isEqualTo("ORG_TERMINAL_INVALIDO");
            assertThat(error.message()).isEqualTo(message);
            assertThat(error.metadata()).containsEntry("field", field);
            return null;
        });
    }

    private static Result<TerminalPos, ErrorDetail> createWithSeries(
            String boleta, String factura) {
        return TerminalPos.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "Caja 1", boleta, factura,
                null, null, null, null, false, NOW);
    }

    private static Result<TerminalPos, ErrorDetail> updateWithSeries(
            String boleta, String factura) {
        return baseTerminal().updateDetails(
                "Caja 1", boleta, factura, null, null, null, null, false, NOW.plusSeconds(60));
    }

    @Test
    void acceptsAlphanumericSeries() {
        assertThat(createWithSeries("B001", "FA1Z").isSuccess()).isTrue();
        assertThat(updateWithSeries("BZ9A", "F001").isSuccess()).isTrue();
    }

    @Test
    void acceptsNullAndBlankSeries() {
        assertThat(createWithSeries(null, null).isSuccess()).isTrue();
        assertThat(createWithSeries(" ", " ").isSuccess()).isTrue();
        assertThat(updateWithSeries(null, null).isSuccess()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"b001", "F001", "B01", "B0011", "B0 1"})
    void rejectsInvalidBoletaSeries(String serie) {
        assertRejected(createWithSeries(serie, null), "serieBoletaDefecto", BOLETA_MESSAGE);
        assertRejected(updateWithSeries(serie, null), "serieBoletaDefecto", BOLETA_MESSAGE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"f001", "B001", "F01", "F0011", "F0 1"})
    void rejectsInvalidFacturaSeries(String serie) {
        assertRejected(createWithSeries(null, serie), "serieFacturaDefecto", FACTURA_MESSAGE);
        assertRejected(updateWithSeries(null, serie), "serieFacturaDefecto", FACTURA_MESSAGE);
    }

    @Test
    void acceptsNombreOfMaximumLength() {
        assertThat(TerminalPos.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "A".repeat(120), null, null,
                null, null, null, null, false, NOW).isSuccess()).isTrue();
        assertThat(baseTerminal().updateDetails(
                "A".repeat(120), null, null, null, null, null, null, false, NOW.plusSeconds(60)).isSuccess())
                .isTrue();
    }

    @Test
    void rejectsNombreLongerThanMaximumWithDatabaseAlignedMessage() {
        assertRejected(TerminalPos.create(
                ID, TENANT_ID, ESTABLECIMIENTO_ID, "CR-01", "A".repeat(121), null, null,
                null, null, null, null, false, NOW),
                "nombre", "El nombre debe tener entre 2 y 120 caracteres.");
        assertRejected(baseTerminal().updateDetails(
                "A".repeat(121), null, null, null, null, null, null, false, NOW.plusSeconds(60)),
                "nombre", "El nombre debe tener entre 2 y 120 caracteres.");
    }
}
