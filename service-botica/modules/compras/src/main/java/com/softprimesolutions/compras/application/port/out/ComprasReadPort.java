package com.softprimesolutions.compras.application.port.out;

import com.softprimesolutions.compras.application.dto.result.OrdenCompraResult;
import com.softprimesolutions.compras.application.dto.result.OrdenCompraResumenResult;
import com.softprimesolutions.compras.application.dto.result.PaginaResult;
import com.softprimesolutions.compras.application.dto.result.ProveedorResult;
import com.softprimesolutions.compras.application.dto.result.RecepcionResult;
import java.util.Optional;
import java.util.UUID;

public interface ComprasReadPort {

    Optional<ProveedorResult> findProveedor(UUID tenantId, UUID proveedorId);

    PaginaResult<ProveedorResult> findProveedores(UUID tenantId, String estado, String texto, int page, int size);

    Optional<OrdenCompraResult> findOrden(UUID tenantId, UUID ordenId);

    PaginaResult<OrdenCompraResumenResult> findOrdenes(
            UUID tenantId, UUID proveedorId, String estado, int page, int size);

    Optional<RecepcionResult> findRecepcion(UUID tenantId, UUID recepcionId);

    PaginaResult<RecepcionResult> findRecepcionesDeOrden(UUID tenantId, UUID ordenId, int page, int size);
}
