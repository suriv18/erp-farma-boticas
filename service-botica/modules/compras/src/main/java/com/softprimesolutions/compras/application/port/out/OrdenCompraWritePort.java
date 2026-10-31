package com.softprimesolutions.compras.application.port.out;

import com.softprimesolutions.compras.domain.model.EstadoOrdenCompra;
import com.softprimesolutions.compras.domain.model.OrdenCompra;
import java.util.Optional;
import java.util.UUID;

public interface OrdenCompraWritePort {

    Optional<OrdenCompra> findById(UUID tenantId, UUID ordenId);

    Optional<OrdenCompra> findByIdParaActualizar(UUID tenantId, UUID ordenId);

    GuardadoOutcome insertar(OrdenCompra orden);

    boolean actualizarEstado(OrdenCompra orden, EstadoOrdenCompra estadoPrevio);
}
