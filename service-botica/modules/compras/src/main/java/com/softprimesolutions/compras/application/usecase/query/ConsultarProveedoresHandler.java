package com.softprimesolutions.compras.application.usecase.query;

import com.softprimesolutions.compras.application.dto.query.ListarProveedoresQuery;
import com.softprimesolutions.compras.application.dto.query.ObtenerProveedorQuery;
import com.softprimesolutions.compras.application.dto.result.PaginaResult;
import com.softprimesolutions.compras.application.dto.result.ProveedorResult;
import com.softprimesolutions.compras.application.error.ComprasErrors;
import com.softprimesolutions.compras.application.port.in.ConsultarProveedoresUseCase;
import com.softprimesolutions.compras.application.port.out.ComprasReadPort;
import com.softprimesolutions.compras.domain.model.EstadoProveedor;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;
import java.util.Optional;

public final class ConsultarProveedoresHandler implements ConsultarProveedoresUseCase {

    private final ComprasReadPort readPort;

    public ConsultarProveedoresHandler(ComprasReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<ProveedorResult, ApplicationError> obtener(ObtenerProveedorQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return readPort.findProveedor(query.tenantId(), query.proveedorId())
                .<Result<ProveedorResult, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(ComprasErrors.proveedorNoEncontrado()));
    }

    @Override
    public Result<PaginaResult<ProveedorResult>, ApplicationError> listar(ListarProveedoresQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        var error = Paginacion.invalida(query.page(), query.size()).or(() -> estadoInvalido(query.estado()));
        if (error.isPresent()) return Result.failure(error.get());
        var texto = Optional.ofNullable(query.texto()).map(String::trim).filter(valor -> !valor.isEmpty()).orElse(null);
        return Result.success(readPort.findProveedores(
                query.tenantId(), query.estado(), texto, query.page(), query.size()));
    }

    private static Optional<ApplicationError> estadoInvalido(String estado) {
        if (estado != null && EstadoProveedor.desde(estado).isEmpty()) {
            return Optional.of(ComprasErrors.filtroEstadoInvalido(estado));
        }
        return Optional.empty();
    }
}
