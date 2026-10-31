package com.softprimesolutions.clientes.api.mapper;

import com.softprimesolutions.clientes.api.dto.request.ActualizarClienteRequest;
import com.softprimesolutions.clientes.api.dto.request.CambiarEstadoClienteRequest;
import com.softprimesolutions.clientes.api.dto.request.RegistrarClienteRequest;
import com.softprimesolutions.clientes.api.dto.response.ClienteResponse;
import com.softprimesolutions.clientes.api.dto.response.PaginaResponse;
import com.softprimesolutions.clientes.application.dto.command.ActualizarClienteCommand;
import com.softprimesolutions.clientes.application.dto.command.CambiarEstadoClienteCommand;
import com.softprimesolutions.clientes.application.dto.command.RegistrarClienteCommand;
import com.softprimesolutions.clientes.application.dto.result.ClienteResult;
import com.softprimesolutions.clientes.application.dto.result.PaginaResult;
import java.util.UUID;

public final class ClientesApiMapper {

    private ClientesApiMapper() {
    }

    public static RegistrarClienteCommand toCommand(UUID tenantId, RegistrarClienteRequest request) {
        return new RegistrarClienteCommand(
                tenantId, request.tipoCliente(), request.tipoDocumento(), request.numeroDocumento(),
                request.nombres(), request.apellidos(), request.razonSocial(), request.email(),
                request.telefono(), request.fechaNacimiento());
    }

    public static ActualizarClienteCommand toCommand(
            UUID tenantId, UUID clienteId, ActualizarClienteRequest request) {
        return new ActualizarClienteCommand(
                tenantId, clienteId, request.nombres(), request.apellidos(), request.razonSocial(),
                request.email(), request.telefono(), request.fechaNacimiento());
    }

    public static CambiarEstadoClienteCommand toCommand(
            UUID tenantId, UUID clienteId, CambiarEstadoClienteRequest request) {
        return new CambiarEstadoClienteCommand(tenantId, clienteId, request.estado(), request.motivo());
    }

    public static ClienteResponse toResponse(ClienteResult result) {
        return new ClienteResponse(
                result.id(), result.tipoCliente(), result.tipoDocumento(), result.numeroDocumento(),
                result.nombres(), result.apellidos(), result.razonSocial(), result.email(), result.telefono(),
                result.fechaNacimiento(), result.estado(), result.motivoBloqueo(), result.createdAt(),
                result.updatedAt());
    }

    public static PaginaResponse<ClienteResponse> toPage(PaginaResult<ClienteResult> result) {
        return new PaginaResponse<>(
                result.items().stream().map(ClientesApiMapper::toResponse).toList(),
                result.page(), result.size(), result.totalElements());
    }
}
