package com.softprimesolutions.compras.application.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.compras.domain.exception.ComprasErrorCodes;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ComprasErrorsTest {

    private static void assertError(ApplicationError error, String code, ErrorCategory category) {
        assertThat(error.code()).isEqualTo(code);
        assertThat(error.category()).isEqualTo(category);
        assertThat(error.message()).isNotBlank();
    }

    @Test
    void mapsDomainConflictsToConflictAndTheRestToValidation() {
        var conflictos = new String[] {
                ComprasErrorCodes.ORDEN_ESTADO_INVALIDO, ComprasErrorCodes.RECEPCION_ORDEN_NO_RECEPCIONABLE,
                ComprasErrorCodes.RECEPCION_EXCEDE_PENDIENTE};
        for (var codigo : conflictos) {
            assertError(ComprasErrors.fromDomain(new ErrorDetail(codigo, "m")), codigo, ErrorCategory.CONFLICT);
        }
        var validacion = ComprasErrors.fromDomain(
                new ErrorDetail(ComprasErrorCodes.ORDEN_INVALIDA, "m", Map.of("campo", "x")));
        assertError(validacion, ComprasErrorCodes.ORDEN_INVALIDA, ErrorCategory.VALIDATION);
        assertThat(validacion.metadata()).containsEntry("campo", "x");
    }

    @Test
    void notFoundErrors() {
        assertError(ComprasErrors.proveedorNoEncontrado(), "COM_PROVEEDOR_NO_ENCONTRADO", ErrorCategory.NOT_FOUND);
        assertError(ComprasErrors.ordenNoEncontrada(), "COM_ORDEN_NO_ENCONTRADA", ErrorCategory.NOT_FOUND);
        assertError(ComprasErrors.recepcionNoEncontrada(), "COM_RECEPCION_NO_ENCONTRADA", ErrorCategory.NOT_FOUND);
        assertError(ComprasErrors.establecimientoNoEncontrado(), "COM_ESTABLECIMIENTO_NO_ENCONTRADO",
                ErrorCategory.NOT_FOUND);
        assertError(ComprasErrors.almacenNoEncontrado(), "COM_ALMACEN_NO_ENCONTRADO", ErrorCategory.NOT_FOUND);
        var sku = UUID.randomUUID();
        var skuError = ComprasErrors.skuNoEncontrado(sku);
        assertError(skuError, "COM_SKU_NO_ENCONTRADO", ErrorCategory.NOT_FOUND);
        assertThat(skuError.metadata()).containsEntry("skuId", sku);
    }

    @Test
    void conflictErrors() {
        assertError(ComprasErrors.proveedorDuplicado(), "COM_PROVEEDOR_DUPLICADO", ErrorCategory.CONFLICT);
        assertError(ComprasErrors.proveedorNoOperable(), "COM_PROVEEDOR_NO_OPERABLE", ErrorCategory.CONFLICT);
        assertError(ComprasErrors.establecimientoNoOperable(), "COM_ESTABLECIMIENTO_NO_OPERABLE",
                ErrorCategory.CONFLICT);
        assertError(ComprasErrors.almacenNoOperable(), "COM_ALMACEN_NO_OPERABLE", ErrorCategory.CONFLICT);
        assertError(ComprasErrors.almacenDeOtroEstablecimiento(), "COM_ALMACEN_DE_OTRO_ESTABLECIMIENTO",
                ErrorCategory.CONFLICT);
        assertError(ComprasErrors.modificacionConcurrente(), ComprasErrors.CONCURRENCIA, ErrorCategory.CONFLICT);
        assertError(ComprasErrors.conflictoIdempotencia(), "COM_IDEMPOTENCY_CONFLICT", ErrorCategory.CONFLICT);
        var sku = UUID.randomUUID();
        var skuError = ComprasErrors.skuNoOperable(sku);
        assertError(skuError, "COM_SKU_NO_OPERABLE", ErrorCategory.CONFLICT);
        assertThat(skuError.metadata()).containsEntry("skuId", sku);
    }

    @Test
    void validationErrors() {
        assertError(ComprasErrors.estadoInvalido(), "COM_ESTADO_INVALIDO", ErrorCategory.VALIDATION);
        assertError(ComprasErrors.claveIdempotenciaInvalida(), "COM_IDEMPOTENCY_KEY_INVALID",
                ErrorCategory.VALIDATION);
        var unidad = ComprasErrors.unidadMedidaNoEncontrada("XXX");
        assertError(unidad, "COM_UNIDAD_MEDIDA_NO_ENCONTRADA", ErrorCategory.VALIDATION);
        assertThat(unidad.metadata()).containsEntry("unidadMedidaCodigo", "XXX");
        var paginacion = ComprasErrors.paginacionInvalida(-1, 500);
        assertError(paginacion, "COM_PAGINACION_INVALIDA", ErrorCategory.VALIDATION);
        assertThat(paginacion.metadata()).containsEntry("page", -1).containsEntry("size", 500);
        var filtro = ComprasErrors.filtroEstadoInvalido("XX");
        assertError(filtro, "COM_FILTRO_ESTADO_INVALIDO", ErrorCategory.VALIDATION);
        assertThat(filtro.metadata()).containsEntry("estado", "XX");
    }
}
