package com.softprimesolutions.inventario.application.port.out;

import java.util.UUID;

public interface ReferenciasInventarioPort {

    EstadoReferencia estadoAlmacen(UUID tenantId, UUID almacenId);

    EstadoReferencia estadoSku(UUID tenantId, UUID skuId);

    enum EstadoReferencia {
        INEXISTENTE,
        NO_OPERABLE,
        OPERABLE
    }
}
