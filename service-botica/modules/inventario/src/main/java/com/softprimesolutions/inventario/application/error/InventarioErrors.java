package com.softprimesolutions.inventario.application.error;

import com.softprimesolutions.inventario.domain.exception.InventarioErrorCodes;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import java.util.Map;
import java.util.Set;

public final class InventarioErrors {

    public static final String CONCURRENCIA = "INV_MODIFICACION_CONCURRENTE";

    private static final Set<String> CONFLICTOS = Set.of(
            InventarioErrorCodes.LOTE_ESTADO_INVALIDO,
            InventarioErrorCodes.LOTE_NO_HABILITABLE,
            InventarioErrorCodes.STOCK_INSUFICIENTE);

    private InventarioErrors() {
    }

    public static ApplicationError fromDomain(ErrorDetail error) {
        var category = CONFLICTOS.contains(error.code()) ? ErrorCategory.CONFLICT : ErrorCategory.VALIDATION;
        return new StandardApplicationError(error.code(), error.message(), category, error.metadata());
    }

    public static ApplicationError loteNoEncontrado() {
        return notFound("INV_LOTE_NO_ENCONTRADO", "El lote indicado no existe.");
    }

    public static ApplicationError almacenNoEncontrado() {
        return notFound("INV_ALMACEN_NO_ENCONTRADO", "El almacén indicado no existe.");
    }

    public static ApplicationError skuNoEncontrado() {
        return notFound("INV_SKU_NO_ENCONTRADO", "El SKU indicado no existe.");
    }

    public static ApplicationError almacenNoOperable() {
        return conflict("INV_ALMACEN_NO_OPERABLE",
                "El almacén no está operativo: debe estar activo y controlar lotes.");
    }

    public static ApplicationError skuNoOperable() {
        return conflict("INV_SKU_NO_OPERABLE", "El SKU no está activo comercialmente.");
    }

    public static ApplicationError loteNoAdmiteIngreso() {
        return conflict("INV_LOTE_NO_ADMITE_INGRESO", "El estado del lote no admite ingresos de stock.");
    }

    public static ApplicationError loteNoVendible() {
        return conflict("INV_LOTE_NO_VENDIBLE", "El estado del lote no permite venderlo.");
    }

    public static ApplicationError stockInsuficiente() {
        return conflict(InventarioErrorCodes.STOCK_INSUFICIENTE,
                "El stock disponible no alcanza para la salida solicitada.");
    }

    public static ApplicationError cantidadInvalida() {
        return validation(InventarioErrorCodes.CANTIDAD_INVALIDA,
                "La cantidad debe ser mayor que cero y admite hasta 4 decimales.");
    }

    public static ApplicationError loteSkuDistinto() {
        return validation("INV_LOTE_SKU_DISTINTO", "El lote indicado pertenece a otro SKU.");
    }

    public static ApplicationError tipoMovimientoInvalido() {
        return validation("INV_TIPO_MOVIMIENTO_INVALIDO", "El tipo de movimiento debe ser AJUSTE_INGRESO o AJUSTE_SALIDA.");
    }

    public static ApplicationError motivoMovimientoInvalido() {
        return validation("INV_MOTIVO_INVALIDO", "El motivo del movimiento debe tener entre 1 y 1000 caracteres.");
    }

    public static ApplicationError referenciaLoteInvalida() {
        return validation("INV_LOTE_REFERENCIA_INVALIDA",
                "Indica el lote existente; un ingreso también admite número de lote y fecha de vencimiento.");
    }

    public static ApplicationError modificacionConcurrente() {
        return conflict(CONCURRENCIA, "El stock fue modificado por otra operación; reintenta.");
    }

    public static ApplicationError claveIdempotenciaInvalida() {
        return validation("INV_IDEMPOTENCY_KEY_INVALID",
                "La cabecera Idempotency-Key debe tener entre 1 y 200 caracteres.");
    }

    public static ApplicationError conflictoIdempotencia() {
        return conflict("INV_IDEMPOTENCY_CONFLICT",
                "La clave de idempotencia ya se usó con una solicitud distinta.");
    }

    public static ApplicationError paginacionInvalida(int page, int size) {
        return new StandardApplicationError(
                "INV_PAGINACION_INVALIDA",
                "page debe ser mayor o igual a 0 y size debe estar entre 1 y 100.",
                ErrorCategory.VALIDATION,
                Map.of("page", page, "size", size));
    }

    private static ApplicationError notFound(String code, String message) {
        return new StandardApplicationError(code, message, ErrorCategory.NOT_FOUND);
    }

    private static ApplicationError conflict(String code, String message) {
        return new StandardApplicationError(code, message, ErrorCategory.CONFLICT);
    }

    private static ApplicationError validation(String code, String message) {
        return new StandardApplicationError(code, message, ErrorCategory.VALIDATION);
    }
}
