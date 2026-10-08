package com.softprimesolutions.ventas.application.port.out;

import com.softprimesolutions.ventas.application.dto.query.ListarVentasQuery;
import com.softprimesolutions.ventas.application.dto.result.PaginaResult;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResumenResult;
import java.util.Optional;
import java.util.UUID;

public interface VentasReadPort {

    Optional<TurnoResult> findTurno(UUID tenantId, UUID turnoId);

    Optional<TurnoResult> findTurnoAbierto(UUID tenantId, UUID terminalId);

    Optional<VentaResult> findVenta(UUID tenantId, UUID ventaId);

    PaginaResult<VentaResumenResult> listarVentas(ListarVentasQuery query);
}
