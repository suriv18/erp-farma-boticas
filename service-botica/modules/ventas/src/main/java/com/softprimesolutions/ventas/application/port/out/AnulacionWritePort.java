package com.softprimesolutions.ventas.application.port.out;

import com.softprimesolutions.ventas.domain.model.EstadoTurno;
import com.softprimesolutions.ventas.domain.model.EstadoVenta;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AnulacionWritePort {

    Optional<VentaParaAnular> bloquearVenta(UUID tenantId, UUID ventaId);

    boolean marcarAnulada(UUID tenantId, UUID ventaId, Actor actor, String motivo, Instant ahora);

    record VentaParaAnular(UUID id, UUID turnoId, EstadoVenta estado, EstadoTurno estadoTurno) {
    }
}
