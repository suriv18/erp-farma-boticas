package com.softprimesolutions.inventario.application.port.in;

import com.softprimesolutions.inventario.application.dto.command.ReintegrarSalidasDeVentaCommand;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;

@FunctionalInterface
public interface ReintegrarSalidasDeVentaUseCase {
    Result<List<MovimientoResult>, ApplicationError> execute(ReintegrarSalidasDeVentaCommand command);
}
