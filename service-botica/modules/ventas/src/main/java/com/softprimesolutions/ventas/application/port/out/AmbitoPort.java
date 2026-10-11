package com.softprimesolutions.ventas.application.port.out;

import java.util.UUID;

public interface AmbitoPort {

    boolean permite(Ambito ambito);

    record Ambito(
            UUID tenantId, UUID actorId, String permiso, UUID establecimientoId, UUID almacenId, UUID terminalId) {
    }
}
