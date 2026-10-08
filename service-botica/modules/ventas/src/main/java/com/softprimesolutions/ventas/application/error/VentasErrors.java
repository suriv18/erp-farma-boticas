package com.softprimesolutions.ventas.application.error;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class VentasErrors {

    public static final String CONCURRENCIA = "VEN_MODIFICACION_CONCURRENTE";

    private static final Set<String> CONFLICTOS = Set.of(
            VentasErrorCodes.TURNO_ESTADO_INVALIDO, VentasErrorCodes.VENTA_ESTADO_INVALIDO,
            VentasErrorCodes.TURNO_NO_ABIERTO);

    private VentasErrors() {
    }

    public static ApplicationError fromDomain(ErrorDetail error) {
        var category = CONFLICTOS.contains(error.code()) ? ErrorCategory.CONFLICT : ErrorCategory.VALIDATION;
        return new StandardApplicationError(error.code(), error.message(), category, error.metadata());
    }

    public static <T> Result<T, ApplicationError> fromDomain(Result<T, ErrorDetail> result) {
        return result.mapError(VentasErrors::fromDomain);
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

    public static ApplicationError noHayTurnoAbierto() {
        return conflict("VEN_TURNO_NO_ABIERTO", "La terminal no tiene un turno abierto.");
    }

    public static ApplicationError almacenNoEncontrado() {
        return notFound("VEN_ALMACEN_NO_ENCONTRADO", "El almacen indicado no existe.");
    }

    public static ApplicationError almacenNoOperable() {
        return conflict("VEN_ALMACEN_NO_OPERABLE", "El almacen debe estar activo, controlar lotes y permitir venta.");
    }

    public static ApplicationError almacenDeOtroEstablecimiento() {
        return conflict("VEN_ALMACEN_DE_OTRO_ESTABLECIMIENTO",
                "El almacen no pertenece al establecimiento de la terminal.");
    }

    public static ApplicationError skuNoEncontrado(UUID skuId) {
        return new StandardApplicationError(
                "VEN_SKU_NO_ENCONTRADO", "El SKU indicado no existe.", ErrorCategory.NOT_FOUND,
                Map.of("skuId", skuId));
    }

    public static ApplicationError skuNoOperable(UUID skuId) {
        return new StandardApplicationError(
                "VEN_SKU_NO_OPERABLE", "El SKU no esta activo comercialmente.", ErrorCategory.CONFLICT,
                Map.of("skuId", skuId));
    }

    public static ApplicationError claveIdempotenciaInvalida() {
        return new StandardApplicationError(
                "VEN_IDEMPOTENCY_KEY_INVALID", "La cabecera Idempotency-Key debe tener entre 1 y 160 caracteres.",
                ErrorCategory.VALIDATION);
    }

    public static ApplicationError conflictoIdempotencia() {
        return conflict("VEN_IDEMPOTENCY_CONFLICT", "La clave de idempotencia ya se uso con una solicitud distinta.");
    }

    public static ApplicationError ventaNoEncontrada() {
        return notFound("VEN_VENTA_NO_ENCONTRADA", "La venta indicada no existe.");
    }

    public static ApplicationError paginacionInvalida(int page, int size) {
        return new StandardApplicationError(
                "VEN_PAGINACION_INVALIDA", "page debe ser mayor o igual a 0 y size debe estar entre 1 y 100.",
                ErrorCategory.VALIDATION, Map.of("page", page, "size", size));
    }

    private static ApplicationError notFound(String code, String message) {
        return new StandardApplicationError(code, message, ErrorCategory.NOT_FOUND);
    }

    private static ApplicationError conflict(String code, String message) {
        return new StandardApplicationError(code, message, ErrorCategory.CONFLICT);
    }
}
