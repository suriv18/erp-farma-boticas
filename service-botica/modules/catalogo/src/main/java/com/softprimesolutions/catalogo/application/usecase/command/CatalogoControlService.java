package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.application.port.out.ProductoReguladoPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.shared.kernel.result.Unit;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class CatalogoControlService implements CatalogoControlUseCase {

    private static final Set<String> SOPORTE_STATUSES = Set.of("ACTIVO", "INACTIVO");
    private static final Set<String> COMERCIAL_STATUSES = Set.of("ACTIVO", "INACTIVO");
    private static final Set<String> SKU_STATUSES = Set.of("ACTIVO", "INACTIVO", "BLOQUEADO", "DESCONTINUADO");
    private static final Set<String> PRODUCTO_REGULADO_STATUSES =
            Set.of("VIGENTE", "VENCIDO", "SUSPENDIDO", "CANCELADO", "POR_VALIDAR");

    private final CatalogoSoportePort soportePort;
    private final CatalogoComercialPort comercialPort;
    private final ProductoReguladoPort productoReguladoPort;
    private final ClockPort clock;

    public CatalogoControlService(
            CatalogoSoportePort soportePort, CatalogoComercialPort comercialPort,
            ProductoReguladoPort productoReguladoPort, ClockPort clock) {
        this.soportePort = Objects.requireNonNull(soportePort, "soportePort es obligatorio");
        this.comercialPort = Objects.requireNonNull(comercialPort, "comercialPort es obligatorio");
        this.productoReguladoPort = Objects.requireNonNull(productoReguladoPort, "productoReguladoPort es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<Unit, ApplicationError> changeCondicionVentaStatus(String codigo, String status) {
        var normalized = normalizeStatus(status);
        if (!SOPORTE_STATUSES.contains(normalized)) return invalidStatus(SOPORTE_STATUSES);
        return soportePort.changeCondicionVentaStatus(codigo, normalized, clock.now())
                ? Result.success(Unit.INSTANCE)
                : notFound("CAT_CONDICION_VENTA_NO_ENCONTRADA", "La condición de venta indicada no existe.");
    }

    @Override
    public Result<Unit, ApplicationError> changeFormaFarmaceuticaStatus(String codigo, String status) {
        var normalized = normalizeStatus(status);
        if (!SOPORTE_STATUSES.contains(normalized)) return invalidStatus(SOPORTE_STATUSES);
        return soportePort.changeFormaFarmaceuticaStatus(codigo, normalized, clock.now())
                ? Result.success(Unit.INSTANCE)
                : notFound("CAT_FORMA_FARMACEUTICA_NO_ENCONTRADA", "La forma farmacéutica indicada no existe.");
    }

    @Override
    public Result<Unit, ApplicationError> changeViaAdministracionStatus(String codigo, String status) {
        var normalized = normalizeStatus(status);
        if (!SOPORTE_STATUSES.contains(normalized)) return invalidStatus(SOPORTE_STATUSES);
        return soportePort.changeViaAdministracionStatus(codigo, normalized, clock.now())
                ? Result.success(Unit.INSTANCE)
                : notFound("CAT_VIA_ADMINISTRACION_NO_ENCONTRADA", "La vía de administración indicada no existe.");
    }

    @Override
    public Result<Unit, ApplicationError> changeUnidadMedidaStatus(String codigo, String status) {
        var normalized = normalizeStatus(status);
        if (!SOPORTE_STATUSES.contains(normalized)) return invalidStatus(SOPORTE_STATUSES);
        return soportePort.changeUnidadMedidaStatus(codigo, normalized, clock.now())
                ? Result.success(Unit.INSTANCE)
                : notFound("CAT_UNIDAD_MEDIDA_NO_ENCONTRADA", "La unidad de medida indicada no existe.");
    }

    @Override
    public Result<Unit, ApplicationError> changeClasificacionControladaStatus(String codigo, String status) {
        var normalized = normalizeStatus(status);
        if (!SOPORTE_STATUSES.contains(normalized)) return invalidStatus(SOPORTE_STATUSES);
        return soportePort.changeClasificacionControladaStatus(codigo, normalized, clock.now())
                ? Result.success(Unit.INSTANCE)
                : notFound("CAT_CLASIFICACION_CONTROLADA_NO_ENCONTRADA", "La clasificación controlada indicada no existe.");
    }

    @Override
    public Result<Unit, ApplicationError> changePrincipioActivoStatus(UUID principioActivoId, String status) {
        var normalized = normalizeStatus(status);
        if (!SOPORTE_STATUSES.contains(normalized)) return invalidStatus(SOPORTE_STATUSES);
        return soportePort.changePrincipioActivoStatus(principioActivoId, normalized, clock.now())
                ? Result.success(Unit.INSTANCE)
                : notFound("CAT_PRINCIPIO_ACTIVO_NO_ENCONTRADO", "El principio activo indicado no existe.");
    }

    @Override
    public Result<Unit, ApplicationError> changeMarcaStatus(UUID tenantId, UUID marcaId, String status) {
        var normalized = normalizeStatus(status);
        if (!COMERCIAL_STATUSES.contains(normalized)) return invalidStatus(COMERCIAL_STATUSES);
        return comercialPort.changeMarcaStatus(tenantId, marcaId, normalized, clock.now())
                ? Result.success(Unit.INSTANCE)
                : notFound("CAT_MARCA_NO_ENCONTRADA", "La marca no existe en el tenant indicado.");
    }

    @Override
    public Result<Unit, ApplicationError> changeCategoriaProductoStatus(UUID tenantId, UUID categoriaId, String status) {
        var normalized = normalizeStatus(status);
        if (!COMERCIAL_STATUSES.contains(normalized)) return invalidStatus(COMERCIAL_STATUSES);
        return comercialPort.changeCategoriaStatus(tenantId, categoriaId, normalized, clock.now())
                ? Result.success(Unit.INSTANCE)
                : notFound("CAT_CATEGORIA_PRODUCTO_NO_ENCONTRADA", "La categoría no existe en el tenant indicado.");
    }

    @Override
    public Result<Unit, ApplicationError> changeProductoReguladoStatus(UUID productoReguladoId, String status) {
        var normalized = normalizeStatus(status);
        if (!PRODUCTO_REGULADO_STATUSES.contains(normalized)) return invalidStatus(PRODUCTO_REGULADO_STATUSES);
        return productoReguladoPort.changeStatus(productoReguladoId, normalized, clock.now())
                ? Result.success(Unit.INSTANCE)
                : notFound("CAT_PRODUCTO_REGULADO_NO_ENCONTRADO", "El producto regulado indicado no existe.");
    }

    @Override
    public Result<Unit, ApplicationError> changeSkuStatus(UUID tenantId, UUID skuId, String status) {
        var normalized = normalizeStatus(status);
        if (!SKU_STATUSES.contains(normalized)) return invalidStatus(SKU_STATUSES);
        return comercialPort.changeSkuStatus(tenantId, skuId, normalized, clock.now())
                ? Result.success(Unit.INSTANCE)
                : notFound("CAT_SKU_NO_ENCONTRADO", "El SKU no existe en el tenant indicado.");
    }

    private static String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }

    private static Result<Unit, ApplicationError> invalidStatus(Set<String> allowed) {
        return Result.failure(new StandardApplicationError(
                "CAT_ESTADO_INVALIDO", "El estado indicado no es válido.",
                ErrorCategory.VALIDATION, Map.of("allowed", allowed)));
    }

    private static Result<Unit, ApplicationError> notFound(String code, String message) {
        return Result.failure(new StandardApplicationError(code, message, ErrorCategory.NOT_FOUND));
    }
}
