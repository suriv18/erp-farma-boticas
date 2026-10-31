package com.softprimesolutions.clientes.application.mapper;

import com.softprimesolutions.clientes.application.dto.result.ClienteResult;
import com.softprimesolutions.clientes.domain.model.Cliente;

public final class ClientesApplicationMapper {

    private ClientesApplicationMapper() {
    }

    public static ClienteResult toResult(Cliente cliente) {
        return new ClienteResult(
                cliente.id().value(), cliente.tenantId().value(), cliente.tipoCliente().name(),
                cliente.tipoDocumento(), cliente.numeroDocumento(), cliente.nombres(), cliente.apellidos(),
                cliente.razonSocial(), cliente.email(), cliente.telefono(), cliente.fechaNacimiento(),
                cliente.estado().name(), cliente.createdAt());
    }
}
