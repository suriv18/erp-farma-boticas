package com.softprimesolutions.ventas.application.mapper;

import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.domain.model.TurnoCaja;

public final class VentasApplicationMapper {

    private VentasApplicationMapper() {
    }

    public static TurnoResult toResult(TurnoCaja turno) {
        return new TurnoResult(
                turno.id(), turno.terminalId(), turno.establecimientoId(), turno.cajero().id(), turno.aperturaAt(),
                turno.fondoInicial(), turno.estado().name(), turno.cierreAt(), turno.totalVentasSistema(),
                turno.totalSistema(), turno.totalDeclarado(), turno.diferencia(), turno.observacionCierre());
    }
}
