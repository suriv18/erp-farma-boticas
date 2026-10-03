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
