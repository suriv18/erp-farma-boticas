package com.softprimesolutions.compras.application.usecase.query;

import com.softprimesolutions.compras.application.dto.query.ListarOrdenesCompraQuery;
import com.softprimesolutions.compras.application.dto.query.ObtenerOrdenCompraQuery;
import com.softprimesolutions.compras.application.dto.result.OrdenCompraResult;
import com.softprimesolutions.compras.application.dto.result.OrdenCompraResumenResult;
import com.softprimesolutions.compras.application.dto.result.PaginaResult;
import com.softprimesolutions.compras.application.error.ComprasErrors;
import com.softprimesolutions.compras.application.port.in.ConsultarOrdenesCompraUseCase;
import com.softprimesolutions.compras.application.port.out.ComprasReadPort;
import com.softprimesolutions.compras.domain.model.EstadoOrdenCompra;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;
import java.util.Optional;

public final class ConsultarOrdenesCompraHandler implements ConsultarOrdenesCompraUseCase {

    private final ComprasReadPort readPort;

    public ConsultarOrdenesCompraHandler(ComprasReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<OrdenCompraResult, ApplicationError> obtener(ObtenerOrdenCompraQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return readPort.findOrden(query.tenantId(), query.ordenId())
                .<Result<OrdenCompraResult, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(ComprasErrors.ordenNoEncontrada()));
    }

    @Override
    public Result<PaginaResult<OrdenCompraResumenResult>, ApplicationError> listar(ListarOrdenesCompraQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        var error = Paginacion.invalida(query.page(), query.size()).or(() -> estadoInvalido(query.estado()));
        if (error.isPresent()) return Result.failure(error.get());
        return Result.success(readPort.findOrdenes(
                query.tenantId(), query.proveedorId(), query.estado(), query.page(), query.size()));
    }

    private static Optional<ApplicationError> estadoInvalido(String estado) {
        if (estado != null && EstadoOrdenCompra.desde(estado).isEmpty()) {
            return Optional.of(ComprasErrors.filtroEstadoInvalido(estado));
        }
        return Optional.empty();
    }
}
