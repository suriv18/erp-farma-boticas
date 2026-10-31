package com.softprimesolutions.clientes.application.mapper;

import com.softprimesolutions.clientes.application.dto.result.ClienteResult;
import com.softprimesolutions.clientes.domain.model.Cliente;
import com.softprimesolutions.clientes.domain.model.EstadoCliente;
import com.softprimesolutions.clientes.domain.model.TipoCliente;
import com.softprimesolutions.clientes.domain.valueobject.ClienteId;
import com.softprimesolutions.clientes.domain.valueobject.TenantId;

public final class ClientesApplicationMapper {

    private ClientesApplicationMapper() {
    }

    public static ClienteResult toResult(Cliente cliente) {
        return new ClienteResult(
                cliente.id().value(), cliente.tenantId().value(), cliente.tipoCliente().name(),
                cliente.tipoDocumento(), cliente.numeroDocumento(), cliente.nombres(), cliente.apellidos(),
                cliente.razonSocial(), cliente.email(), cliente.telefono(), cliente.fechaNacimiento(),
                cliente.estado().name(), cliente.motivoBloqueo(), cliente.createdAt(), cliente.updatedAt());
    }

    public static Cliente toDomain(ClienteResult result) {
        return Cliente.restore(
                new ClienteId(result.id()), new TenantId(result.tenantId()),
                TipoCliente.valueOf(result.tipoCliente()), result.tipoDocumento(), result.numeroDocumento(),
                result.nombres(), result.apellidos(), result.razonSocial(), result.email(), result.telefono(),
                result.fechaNacimiento(), EstadoCliente.valueOf(result.estado()), result.motivoBloqueo(),
                result.createdAt(), result.updatedAt());
    }
}
