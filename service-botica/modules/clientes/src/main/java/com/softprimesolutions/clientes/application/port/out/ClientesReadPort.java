package com.softprimesolutions.clientes.application.port.out;

import com.softprimesolutions.clientes.application.dto.result.ClienteResult;
import com.softprimesolutions.clientes.application.dto.result.PaginaResult;
import java.util.Optional;
import java.util.UUID;

public interface ClientesReadPort {

    PaginaResult<ClienteResult> findClientes(UUID tenantId, String search, int page, int size);

    Optional<ClienteResult> findClienteById(UUID tenantId, UUID clienteId);

    Optional<ClienteResult> findClienteByDocumento(UUID tenantId, String tipoDocumento, String numeroDocumento);
}
