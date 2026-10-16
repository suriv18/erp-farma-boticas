package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarProductoReguladoCommand;
import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ActualizarProductoReguladoUseCase;
import com.softprimesolutions.catalogo.application.port.out.ProductoReguladoPort;
import com.softprimesolutions.catalogo.domain.model.ProductoRegulado;
import com.softprimesolutions.catalogo.domain.valueobject.ProductoReguladoId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarProductoReguladoHandler implements ActualizarProductoReguladoUseCase {

    private final ProductoReguladoPort writePort;
    private final ClockPort clock;

    public ActualizarProductoReguladoHandler(ProductoReguladoPort writePort, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<ProductoReguladoResult, ApplicationError> execute(ActualizarProductoReguladoCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var producto = ProductoRegulado.create(
                new ProductoReguladoId(command.productoReguladoId()), command.tipoProducto(),
                command.rubroCodigo(), command.tipoRegistro(), command.numeroRegistro(), command.denominacion(),
                command.concentracionTexto(), command.presentacionRegulatoria(), command.formaFarmaceuticaCodigo(),
                command.viaAdministracionCodigo(), command.unidadMedidaCodigo(), command.condicionVentaCodigo(),
                command.clasificacionAtc(), command.clasificacionControladaCodigo(), command.tipoLiberacion(),
                command.origenFabricacion(), command.paisOrigen(), command.subpartidaNacional(),
                command.titularRegistro(), command.fabricante(), command.importador(),
                command.establecimientoExpendio(), command.vigenteDesde(), command.vigenteHasta(),
                command.fuente(), command.versionFuente(), clock.now());
        return producto.fold(this::persist, this::validationFailure);
    }

    private Result<ProductoReguladoResult, ApplicationError> persist(ProductoRegulado producto) {
        var outcome = writePort.save(producto);
        var error = ActualizarProductoReguladoHandler.mapOutcomeToError(outcome);
        if (error != null) return Result.failure(error);
        return Result.success(CatalogoApplicationMapper.toResult(producto));
    }

    private static ApplicationError mapOutcomeToError(ProductoReguladoPort.SaveOutcome outcome) {
        return switch (outcome) {
            case NOT_FOUND -> new StandardApplicationError(
                    "CAT_PRODUCTO_REGULADO_NO_ENCONTRADO", "El producto regulado indicado no existe.",
                    ErrorCategory.NOT_FOUND);
            case FORMA_FARMACEUTICA_NOT_FOUND -> new StandardApplicationError(
                    "CAT_FORMA_FARMACEUTICA_NO_ENCONTRADA", "La forma farmacéutica indicada no existe.",
                    ErrorCategory.NOT_FOUND);
            case VIA_ADMINISTRACION_NOT_FOUND -> new StandardApplicationError(
                    "CAT_VIA_ADMINISTRACION_NO_ENCONTRADA", "La vía de administración indicada no existe.",
                    ErrorCategory.NOT_FOUND);
            case UNIDAD_MEDIDA_NOT_FOUND -> new StandardApplicationError(
                    "CAT_UNIDAD_MEDIDA_NO_ENCONTRADA", "La unidad de medida indicada no existe.",
                    ErrorCategory.NOT_FOUND);
            case CONDICION_VENTA_NOT_FOUND -> new StandardApplicationError(
                    "CAT_CONDICION_VENTA_NO_ENCONTRADA", "La condición de venta indicada no existe.",
                    ErrorCategory.NOT_FOUND);
            case CLASIFICACION_CONTROLADA_NOT_FOUND -> new StandardApplicationError(
                    "CAT_CLASIFICACION_CONTROLADA_NO_ENCONTRADA", "La clasificación controlada indicada no existe.",
                    ErrorCategory.NOT_FOUND);
            case PRINCIPIO_ACTIVO_NOT_FOUND -> new StandardApplicationError(
                    "CAT_PRINCIPIO_ACTIVO_NO_ENCONTRADO", "El principio activo indicado no existe.",
                    ErrorCategory.NOT_FOUND);
            case CREATED, UPDATED -> null;
        };
    }

    private Result<ProductoReguladoResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
