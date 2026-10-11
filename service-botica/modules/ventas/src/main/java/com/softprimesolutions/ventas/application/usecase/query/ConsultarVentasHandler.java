package com.softprimesolutions.ventas.application.usecase.query;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.query.ListarVentasQuery;
import com.softprimesolutions.ventas.application.dto.query.ObtenerVentaQuery;
import com.softprimesolutions.ventas.application.dto.result.PaginaResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResumenResult;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.application.port.in.ConsultarVentasUseCase;
import com.softprimesolutions.ventas.application.port.out.VentasReadPort;
import java.util.Objects;

public final class ConsultarVentasHandler implements ConsultarVentasUseCase {

    private static final int TAMANO_MAXIMO = 100;

    private final VentasReadPort readPort;

    public ConsultarVentasHandler(VentasReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<VentaResult, ApplicationError> obtener(ObtenerVentaQuery query) {
        Objects.requireNonNull(query, "query es obligatoria");
        return readPort.findVenta(query.tenantId(), query.ventaId())
                .<Result<VentaResult, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(VentasErrors.ventaNoEncontrada()));
    }

    @Override
    public Result<PaginaResult<VentaResumenResult>, ApplicationError> listar(ListarVentasQuery query) {
        Objects.requireNonNull(query, "query es obligatoria");
        if (query.page() < 0 || query.size() < 1 || query.size() > TAMANO_MAXIMO) {
            return Result.failure(VentasErrors.paginacionInvalida(query.page(), query.size()));
        }
        return Result.success(readPort.listarVentas(query));
    }
}
