package com.softprimesolutions.compras.application.port.out;

import com.softprimesolutions.compras.domain.model.Proveedor;
import java.util.Optional;
import java.util.UUID;

public interface ProveedorWritePort {

    Optional<Proveedor> findById(UUID tenantId, UUID proveedorId);

    GuardadoOutcome insertar(Proveedor proveedor);

    GuardadoOutcome actualizar(Proveedor proveedor);
}
