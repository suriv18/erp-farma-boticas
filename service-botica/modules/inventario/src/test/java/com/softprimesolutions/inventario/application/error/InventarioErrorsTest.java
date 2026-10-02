package com.softprimesolutions.inventario.application.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.inventario.domain.exception.InventarioErrorCodes;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InventarioErrorsTest {

    private static void assertError(ApplicationError error, String code, ErrorCategory category) {
        assertThat(error.code()).isEqualTo(code);
        assertThat(error.category()).isEqualTo(category);
        assertThat(error.message()).isNotBlank();
    }

    @Test
    void mapsStateAndStockRulesFromTheDomainToConflicts() {
        for (var code : new String[] {
                InventarioErrorCodes.LOTE_ESTADO_INVALIDO, InventarioErrorCodes.LOTE_NO_HABILITABLE,
                InventarioErrorCodes.STOCK_INSUFICIENTE}) {
            assertError(InventarioErrors.fromDomain(new ErrorDetail(code, "m")), code, ErrorCategory.CONFLICT);
        }
    }

    @Test
    void mapsAnyOtherDomainErrorToAValidationFailureKeepingItsMetadata() {
        var error = InventarioErrors.fromDomain(
                new ErrorDetail(InventarioErrorCodes.CANTIDAD_INVALIDA, "m", Map.of("field", "cantidad")));

        assertError(error, InventarioErrorCodes.CANTIDAD_INVALIDA, ErrorCategory.VALIDATION);
        assertThat(error.metadata()).containsEntry("field", "cantidad");
    }

    @Test
    void exposesTheApplicationErrorsWithTheirCategory() {
        assertError(InventarioErrors.loteNoEncontrado(), "INV_LOTE_NO_ENCONTRADO", ErrorCategory.NOT_FOUND);
        assertError(InventarioErrors.almacenNoEncontrado(), "INV_ALMACEN_NO_ENCONTRADO", ErrorCategory.NOT_FOUND);
        assertError(InventarioErrors.skuNoEncontrado(), "INV_SKU_NO_ENCONTRADO", ErrorCategory.NOT_FOUND);
        assertError(InventarioErrors.almacenNoOperable(), "INV_ALMACEN_NO_OPERABLE", ErrorCategory.CONFLICT);
        assertError(InventarioErrors.skuNoOperable(), "INV_SKU_NO_OPERABLE", ErrorCategory.CONFLICT);
        assertError(InventarioErrors.loteNoAdmiteIngreso(), "INV_LOTE_NO_ADMITE_INGRESO", ErrorCategory.CONFLICT);
        assertError(InventarioErrors.loteSkuDistinto(), "INV_LOTE_SKU_DISTINTO", ErrorCategory.VALIDATION);
        assertError(InventarioErrors.tipoMovimientoInvalido(), "INV_TIPO_MOVIMIENTO_INVALIDO",
                ErrorCategory.VALIDATION);
        assertError(InventarioErrors.motivoMovimientoInvalido(), "INV_MOTIVO_INVALIDO", ErrorCategory.VALIDATION);
        assertError(InventarioErrors.referenciaLoteInvalida(), "INV_LOTE_REFERENCIA_INVALIDA",
                ErrorCategory.VALIDATION);
        assertError(InventarioErrors.claveIdempotenciaInvalida(), "INV_IDEMPOTENCY_KEY_INVALID",
                ErrorCategory.VALIDATION);
        assertError(InventarioErrors.conflictoIdempotencia(), "INV_IDEMPOTENCY_CONFLICT", ErrorCategory.CONFLICT);
        assertError(InventarioErrors.modificacionConcurrente(), InventarioErrors.CONCURRENCIA,
                ErrorCategory.CONFLICT);
    }

    @Test
    void reportsTheInvalidPaginationValues() {
        var error = InventarioErrors.paginacionInvalida(-1, 500);

        assertError(error, "INV_PAGINACION_INVALIDA", ErrorCategory.VALIDATION);
        assertThat(error.metadata()).containsEntry("page", -1).containsEntry("size", 500);
    }
}
