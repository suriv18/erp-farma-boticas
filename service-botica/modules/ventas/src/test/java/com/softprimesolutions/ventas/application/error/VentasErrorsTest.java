package com.softprimesolutions.ventas.application.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import java.util.Map;
import java.util.UUID;
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

    @Test
    void exposesTheVentaErrors() {
        var id = UUID.fromString("88888888-8888-4888-8888-888888888888");

        assertThat(VentasErrors.noHayTurnoAbierto().code()).isEqualTo("VEN_TURNO_NO_ABIERTO");
        assertThat(VentasErrors.noHayTurnoAbierto().category()).isEqualTo(ErrorCategory.CONFLICT);
        assertThat(VentasErrors.almacenNoEncontrado().code()).isEqualTo("VEN_ALMACEN_NO_ENCONTRADO");
        assertThat(VentasErrors.almacenNoEncontrado().category()).isEqualTo(ErrorCategory.NOT_FOUND);
        assertThat(VentasErrors.almacenNoOperable().code()).isEqualTo("VEN_ALMACEN_NO_OPERABLE");
        assertThat(VentasErrors.almacenNoOperable().category()).isEqualTo(ErrorCategory.CONFLICT);
        assertThat(VentasErrors.almacenDeOtroEstablecimiento().code())
                .isEqualTo("VEN_ALMACEN_DE_OTRO_ESTABLECIMIENTO");
        assertThat(VentasErrors.skuNoEncontrado(id).code()).isEqualTo("VEN_SKU_NO_ENCONTRADO");
        assertThat(VentasErrors.skuNoEncontrado(id).category()).isEqualTo(ErrorCategory.NOT_FOUND);
        assertThat(VentasErrors.skuNoEncontrado(id).metadata()).containsEntry("skuId", id);
        assertThat(VentasErrors.skuNoOperable(id).code()).isEqualTo("VEN_SKU_NO_OPERABLE");
        assertThat(VentasErrors.skuNoOperable(id).category()).isEqualTo(ErrorCategory.CONFLICT);
        assertThat(VentasErrors.skuNoOperable(id).metadata()).containsEntry("skuId", id);
        assertThat(VentasErrors.claveIdempotenciaInvalida().code()).isEqualTo("VEN_IDEMPOTENCY_KEY_INVALID");
        assertThat(VentasErrors.claveIdempotenciaInvalida().category()).isEqualTo(ErrorCategory.VALIDATION);
        assertThat(VentasErrors.conflictoIdempotencia().code()).isEqualTo("VEN_IDEMPOTENCY_CONFLICT");
        assertThat(VentasErrors.conflictoIdempotencia().category()).isEqualTo(ErrorCategory.CONFLICT);
        assertThat(VentasErrors.ventaNoEncontrada().code()).isEqualTo("VEN_VENTA_NO_ENCONTRADA");
        assertThat(VentasErrors.ventaNoEncontrada().category()).isEqualTo(ErrorCategory.NOT_FOUND);
        assertThat(VentasErrors.paginacionInvalida(-1, 500).code()).isEqualTo("VEN_PAGINACION_INVALIDA");
        assertThat(VentasErrors.paginacionInvalida(-1, 500).category()).isEqualTo(ErrorCategory.VALIDATION);
        assertThat(VentasErrors.paginacionInvalida(-1, 500).metadata())
                .containsEntry("page", -1).containsEntry("size", 500);
    }

    @Test
    void translatesADomainResultKeepingSuccessesAndMappingFailures() {
        Result<String, ApplicationError> ok = VentasErrors.fromDomain(Result.<String, ErrorDetail>success("x"));
        Result<String, ApplicationError> falla = VentasErrors.fromDomain(
                Result.<String, ErrorDetail>failure(new ErrorDetail(VentasErrorCodes.MONTO_INVALIDO, "m", Map.of())));

        assertThat(ok.<String>fold(value -> value, error -> null)).isEqualTo("x");
        assertThat(falla.<String>fold(value -> null, error -> error.code())).isEqualTo("VEN_MONTO_INVALIDO");
    }
}
