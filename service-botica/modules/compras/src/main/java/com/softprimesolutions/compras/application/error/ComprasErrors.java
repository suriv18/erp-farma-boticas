package com.softprimesolutions.compras.application.error;

import com.softprimesolutions.compras.domain.exception.ComprasErrorCodes;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ComprasErrors {

    public static final String CONCURRENCIA = "COM_MODIFICACION_CONCURRENTE";

    private static final Set<String> CONFLICTOS = Set.of(
            ComprasErrorCodes.ORDEN_ESTADO_INVALIDO,
            ComprasErrorCodes.RECEPCION_ORDEN_NO_RECEPCIONABLE,
            ComprasErrorCodes.RECEPCION_EXCEDE_PENDIENTE);

    private ComprasErrors() {
    }

    public static ApplicationError fromDomain(ErrorDetail error) {
        var category = CONFLICTOS.contains(error.code()) ? ErrorCategory.CONFLICT : ErrorCategory.VALIDATION;
        return new StandardApplicationError(error.code(), error.message(), category, error.metadata());
    }

    public static ApplicationError proveedorNoEncontrado() {
        return notFound("COM_PROVEEDOR_NO_ENCONTRADO", "El proveedor indicado no existe.");
    }

    public static ApplicationError ordenNoEncontrada() {
        return notFound("COM_ORDEN_NO_ENCONTRADA", "La orden de compra indicada no existe.");
    }

    public static ApplicationError recepcionNoEncontrada() {
        return notFound("COM_RECEPCION_NO_ENCONTRADA", "La recepcion indicada no existe.");
    }

    public static ApplicationError establecimientoNoEncontrado() {
        return notFound("COM_ESTABLECIMIENTO_NO_ENCONTRADO", "El establecimiento indicado no existe.");
    }

    public static ApplicationError almacenNoEncontrado() {
        return notFound("COM_ALMACEN_NO_ENCONTRADO", "El almacen indicado no existe.");
    }

    public static ApplicationError skuNoEncontrado(UUID skuId) {
        return new StandardApplicationError(
                "COM_SKU_NO_ENCONTRADO", "El SKU indicado no existe.", ErrorCategory.NOT_FOUND,
                Map.of("skuId", skuId));
    }

    public static ApplicationError unidadMedidaNoEncontrada(String codigo) {
        return new StandardApplicationError(
                "COM_UNIDAD_MEDIDA_NO_ENCONTRADA", "La unidad de medida indicada no existe.",
                ErrorCategory.VALIDATION, Map.of("unidadMedidaCodigo", codigo));
    }

    public static ApplicationError proveedorDuplicado() {
        return conflict("COM_PROVEEDOR_DUPLICADO", "Ya existe un proveedor con ese documento.");
    }

    public static ApplicationError proveedorNoOperable() {
        return conflict("COM_PROVEEDOR_NO_OPERABLE", "El proveedor debe estar activo para emitir ordenes de compra.");
    }

    public static ApplicationError establecimientoNoOperable() {
        return conflict("COM_ESTABLECIMIENTO_NO_OPERABLE", "El establecimiento destino debe estar activo.");
    }

    public static ApplicationError almacenNoOperable() {
        return conflict("COM_ALMACEN_NO_OPERABLE", "El almacen debe estar activo y controlar lotes.");
    }

    public static ApplicationError almacenDeOtroEstablecimiento() {
        return conflict("COM_ALMACEN_DE_OTRO_ESTABLECIMIENTO",
                "El almacen no pertenece al establecimiento destino de la orden.");
    }

    public static ApplicationError skuNoOperable(UUID skuId) {
        return new StandardApplicationError(
                "COM_SKU_NO_OPERABLE", "El SKU no esta activo comercialmente.", ErrorCategory.CONFLICT,
                Map.of("skuId", skuId));
    }

    public static ApplicationError estadoInvalido() {
        return validation("COM_ESTADO_INVALIDO", "El estado debe ser ACTIVO, BLOQUEADO o SUSPENDIDO.");
    }

    public static ApplicationError modificacionConcurrente() {
        return conflict(CONCURRENCIA, "El documento fue modificado por otra operacion; reintenta.");
    }

    public static ApplicationError claveIdempotenciaInvalida() {
        return validation("COM_IDEMPOTENCY_KEY_INVALID",
                "La cabecera Idempotency-Key debe tener entre 1 y 200 caracteres.");
    }

    public static ApplicationError conflictoIdempotencia() {
        return conflict("COM_IDEMPOTENCY_CONFLICT",
                "La clave de idempotencia ya se uso con una solicitud distinta.");
    }

    public static ApplicationError paginacionInvalida(int page, int size) {
        return new StandardApplicationError(
                "COM_PAGINACION_INVALIDA",
                "page debe ser mayor o igual a 0 y size debe estar entre 1 y 100.",
                ErrorCategory.VALIDATION,
                Map.of("page", page, "size", size));
    }

    public static ApplicationError filtroEstadoInvalido(String estado) {
        return new StandardApplicationError(
                "COM_FILTRO_ESTADO_INVALIDO", "El estado de filtro no es valido.", ErrorCategory.VALIDATION,
                Map.of("estado", estado));
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
