package com.softprimesolutions.clientes.infrastructure.persistence.write.mapper;

import com.softprimesolutions.clientes.domain.model.Cliente;
import com.softprimesolutions.clientes.infrastructure.persistence.write.entity.ClienteJpaEntity;

public final class ClientesWriteMapper {

    private ClientesWriteMapper() {
    }

    public static ClienteJpaEntity toEntity(Cliente cliente, Long tenantId) {
        return new ClienteJpaEntity(
                cliente.id().value(), tenantId, cliente.tipoCliente().name(), cliente.tipoDocumento(),
                cliente.numeroDocumento(), cliente.nombres(), cliente.apellidos(), cliente.razonSocial(),
                cliente.email(), cliente.telefono(), cliente.fechaNacimiento(), cliente.estado().name(),
                cliente.createdAt());
    }
}
