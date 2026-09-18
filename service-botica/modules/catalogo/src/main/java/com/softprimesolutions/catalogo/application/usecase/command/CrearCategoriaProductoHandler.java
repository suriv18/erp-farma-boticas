package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.CrearCategoriaProductoCommand;
import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.CrearCategoriaProductoUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.catalogo.domain.model.CategoriaProducto;
import com.softprimesolutions.catalogo.domain.valueobject.CategoriaProductoId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearCategoriaProductoHandler implements CrearCategoriaProductoUseCase {

    private final CatalogoComercialPort writePort;
    private final IdentifierGenerator identifierGenerator;

    public CrearCategoriaProductoHandler(CatalogoComercialPort writePort, IdentifierGenerator identifierGenerator) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.identifierGenerator = Objects.requireNonNull(identifierGenerator, "identifierGenerator es obligatorio");
    }

    @Override
    public Result<CategoriaProductoResult, ApplicationError> execute(CrearCategoriaProductoCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var categoria = CategoriaProducto.create(
                new CategoriaProductoId(identifierGenerator.next()),
                command.tenantId() == null ? null : new TenantId(command.tenantId()),
                command.categoriaPadreId() == null ? null : new CategoriaProductoId(command.categoriaPadreId()),
                command.codigo(), command.nombre(), command.descripcion(), command.nivel(), command.orden());
        return categoria.fold(this::persist, this::validationFailure);
    }

    private Result<CategoriaProductoResult, ApplicationError> persist(CategoriaProducto categoria) {
        var outcome = writePort.save(categoria);
        if (outcome == CatalogoComercialPort.SaveCategoriaOutcome.TENANT_NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_TENANT_NO_ENCONTRADO", "El tenant indicado no existe.", ErrorCategory.NOT_FOUND));
        }
        if (outcome == CatalogoComercialPort.SaveCategoriaOutcome.CATEGORIA_PADRE_NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_CATEGORIA_PADRE_NO_ENCONTRADA", "La categoría padre indicada no existe.",
                    ErrorCategory.NOT_FOUND));
        }
        if (outcome == CatalogoComercialPort.SaveCategoriaOutcome.DUPLICATE_CODIGO) {
            return Result.failure(new StandardApplicationError(
                    "CAT_CATEGORIA_PRODUCTO_DUPLICADA", "Ya existe una categoría con el código indicado.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(CatalogoApplicationMapper.toResult(categoria));
    }

    private Result<CategoriaProductoResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
