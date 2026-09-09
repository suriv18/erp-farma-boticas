package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.CrearSkuCommand;
import com.softprimesolutions.catalogo.application.dto.result.SkuResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.CrearSkuUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.catalogo.domain.model.SKUComercial;
import com.softprimesolutions.catalogo.domain.model.TipoSku;
import com.softprimesolutions.catalogo.domain.valueobject.CategoriaProductoId;
import com.softprimesolutions.catalogo.domain.valueobject.MarcaId;
import com.softprimesolutions.catalogo.domain.valueobject.ProductoReguladoId;
import com.softprimesolutions.catalogo.domain.valueobject.SkuId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class CrearSkuHandler implements CrearSkuUseCase {

    private final CatalogoComercialPort writePort;
    private final IdentifierGenerator identifierGenerator;
    private final ClockPort clock;

    public CrearSkuHandler(
            CatalogoComercialPort writePort, IdentifierGenerator identifierGenerator, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.identifierGenerator = Objects.requireNonNull(identifierGenerator, "identifierGenerator es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<SkuResult, ApplicationError> execute(CrearSkuCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");

        final TipoSku tipoSku;
        try {
            tipoSku = TipoSku.valueOf(command.tipoSku() == null ? "" : command.tipoSku().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Result.failure(new StandardApplicationError(
                    "CAT_SKU_INVALIDO", "El tipo de SKU no es válido.", ErrorCategory.VALIDATION,
                    Map.of("field", "tipoSku")));
        }

        var sku = SKUComercial.create(
                new SkuId(identifierGenerator.next()),
                command.tenantId() == null ? null : new TenantId(command.tenantId()),
                command.productoReguladoId() == null ? null : new ProductoReguladoId(command.productoReguladoId()),
                command.categoriaId() == null ? null : new CategoriaProductoId(command.categoriaId()),
                command.marcaId() == null ? null : new MarcaId(command.marcaId()),
                tipoSku, command.codigoInterno(), command.descripcionComercial(), command.nombreCorto(),
                command.presentacionComercial(), command.unidadVentaCodigo(), command.contenido(),
                command.unidadContenidoCodigo(), command.pesoGramos(), command.altoCm(), command.anchoCm(),
                command.largoCm(), command.permiteVentaFraccion(), command.factorFraccion(),
                command.requiereLote(), command.requiereVencimiento(), command.afectoIgv(),
                command.stockMinimoDefault(), command.stockMaximoDefault(), command.imagenUri(),
                command.createdBy(), clock.now());
        return sku.fold(this::persist, this::validationFailure);
    }

    private Result<SkuResult, ApplicationError> persist(SKUComercial sku) {
        var outcome = writePort.save(sku);
        var error = mapOutcomeToError(outcome);
        if (error != null) return Result.failure(error);
        return Result.success(CatalogoApplicationMapper.toResult(sku));
    }

    private static ApplicationError mapOutcomeToError(CatalogoComercialPort.SaveSkuOutcome outcome) {
        return switch (outcome) {
            case TENANT_NOT_FOUND -> new StandardApplicationError(
                    "CAT_TENANT_NO_ENCONTRADO", "El tenant indicado no existe.", ErrorCategory.NOT_FOUND);
            case PRODUCTO_REGULADO_NOT_FOUND -> new StandardApplicationError(
                    "CAT_PRODUCTO_REGULADO_NO_ENCONTRADO", "El producto regulado indicado no existe.",
                    ErrorCategory.NOT_FOUND);
            case CATEGORIA_NOT_FOUND -> new StandardApplicationError(
                    "CAT_CATEGORIA_PRODUCTO_NO_ENCONTRADA", "La categoría indicada no existe.",
                    ErrorCategory.NOT_FOUND);
            case MARCA_NOT_FOUND -> new StandardApplicationError(
                    "CAT_MARCA_NO_ENCONTRADA", "La marca indicada no existe.", ErrorCategory.NOT_FOUND);
            case NOT_FOUND -> new StandardApplicationError(
                    "CAT_SKU_NO_ENCONTRADO", "El SKU indicado no existe.", ErrorCategory.NOT_FOUND);
            case DUPLICATE_CODIGO_INTERNO -> new StandardApplicationError(
                    "CAT_SKU_CODIGO_INTERNO_DUPLICADO", "Ya existe un SKU con el código interno indicado.",
                    ErrorCategory.CONFLICT);
            case DUPLICATE_CODIGO_BARRA -> new StandardApplicationError(
                    "CAT_SKU_CODIGO_BARRA_DUPLICADO", "Ya existe un SKU con el código de barras indicado.",
                    ErrorCategory.CONFLICT);
            case CREATED, UPDATED -> null;
        };
    }

    private Result<SkuResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
