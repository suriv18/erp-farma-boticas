package com.softprimesolutions.ventas.application.port.out;

import com.softprimesolutions.ventas.domain.model.TurnoCaja;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface TurnoWritePort {

    GuardadoOutcome insertar(TurnoCaja turno);

    Optional<TurnoCaja> findPorIdParaActualizar(UUID tenantId, UUID turnoId);

    BigDecimal totalVentasEfectivo(UUID tenantId, UUID turnoId);

    boolean actualizarCierre(TurnoCaja turno, Actor actor);
}
