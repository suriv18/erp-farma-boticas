package com.softprimesolutions.organizacion.application.usecase.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort.SaveTerminalOutcome;
import com.softprimesolutions.organizacion.domain.model.TerminalPos;
import com.softprimesolutions.organizacion.domain.valueobject.EstablecimientoId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import com.softprimesolutions.organizacion.domain.valueobject.TerminalPosId;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerminalSaveErrorsTest {

    private static final TerminalPos TERMINAL = TerminalPos.create(
                    new TerminalPosId(UUID.randomUUID()), new TenantId(UUID.randomUUID()),
                    new EstablecimientoId(UUID.randomUUID()), "POS-01", "Caja 1", "B001", "F002",
                    "SN-0001", "host-01", "192.168.0.10", "PRN-01", true, Instant.parse("2026-09-27T00:00:00Z"))
            .fold(terminal -> terminal, error -> { throw new AssertionError(error.message()); });

    @Test
    void translatesADuplicateBoletaSerie() {
        var error = TerminalSaveErrors.seriesConflict(SaveTerminalOutcome.DUPLICATE_SERIE_BOLETA, TERMINAL);

        assertThat(error).isPresent();
        assertThat(error.get().code()).isEqualTo("ORG_TERMINAL_SERIE_BOLETA_DUPLICADA");
        assertThat(error.get().message()).isEqualTo("La serie B001 ya está asignada a otra caja de esta empresa.");
        assertThat(error.get().category()).isEqualTo(ErrorCategory.CONFLICT);
    }

    @Test
    void translatesADuplicateFacturaSerie() {
        var error = TerminalSaveErrors.seriesConflict(SaveTerminalOutcome.DUPLICATE_SERIE_FACTURA, TERMINAL);

        assertThat(error).isPresent();
        assertThat(error.get().code()).isEqualTo("ORG_TERMINAL_SERIE_FACTURA_DUPLICADA");
        assertThat(error.get().message()).isEqualTo("La serie F002 ya está asignada a otra caja de esta empresa.");
        assertThat(error.get().category()).isEqualTo(ErrorCategory.CONFLICT);
    }

    @Test
    void ignoresOutcomesThatAreNotSerieConflicts() {
        assertThat(TerminalSaveErrors.seriesConflict(SaveTerminalOutcome.CREATED, TERMINAL)).isEmpty();
    }
}
