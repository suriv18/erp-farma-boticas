package com.softprimesolutions.clientes.application.port.out;

import com.softprimesolutions.clientes.domain.model.Cliente;

public interface ClientesWritePort {

    SaveClienteOutcome save(Cliente cliente);

    UpdateClienteOutcome update(Cliente cliente);

    enum SaveClienteOutcome { CREATED, TENANT_NOT_FOUND, DUPLICATE_DOCUMENTO, TIPO_DOCUMENTO_INVALIDO }

    enum UpdateClienteOutcome { UPDATED, NOT_FOUND }
}
