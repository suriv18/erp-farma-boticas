package com.softprimesolutions.ventas.application.port.in;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.query.ObtenerTurnoQuery;
import com.softprimesolutions.ventas.application.dto.query.TurnoActualQuery;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;

public interface ConsultarTurnosUseCase {

    Result<TurnoResult, ApplicationError> obtener(ObtenerTurnoQuery query);

    Result<TurnoResult, ApplicationError> actual(TurnoActualQuery query);
}
