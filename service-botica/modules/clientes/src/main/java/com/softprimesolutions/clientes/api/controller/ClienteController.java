package com.softprimesolutions.clientes.api.controller;

import static com.softprimesolutions.clientes.api.controller.ClientesControllerSupport.tenantOf;

import com.softprimesolutions.clientes.api.dto.request.RegistrarClienteRequest;
import com.softprimesolutions.clientes.api.mapper.ClientesApiMapper;
import com.softprimesolutions.clientes.application.dto.query.ListarClientesQuery;
import com.softprimesolutions.clientes.application.dto.query.ObtenerClienteQuery;
import com.softprimesolutions.clientes.application.port.in.ListarClientesUseCase;
import com.softprimesolutions.clientes.application.port.in.ObtenerClienteUseCase;
import com.softprimesolutions.clientes.application.port.in.RegistrarClienteUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(ClienteController.BASE_PATH)
public class ClienteController {

    static final String BASE_PATH = "/api/v1/clientes/clientes";

    private final RegistrarClienteUseCase registrarCliente;
    private final ObtenerClienteUseCase obtenerCliente;
    private final ListarClientesUseCase listarClientes;

    public ClienteController(
            RegistrarClienteUseCase registrarCliente,
            ObtenerClienteUseCase obtenerCliente,
            ListarClientesUseCase listarClientes) {
        this.registrarCliente = registrarCliente;
        this.obtenerCliente = obtenerCliente;
        this.listarClientes = listarClientes;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('clientes.clientes.registrar')")
    public ResponseEntity<?> register(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RegistrarClienteRequest request) {
        return registrarCliente.execute(ClientesApiMapper.toCommand(tenantOf(jwt), request)).fold(
                registro -> registro.creado()
                        ? ClientesControllerSupport.created(
                                BASE_PATH, registro.cliente().id(), ClientesApiMapper.toResponse(registro.cliente()))
                        : ResponseEntity.ok(ClientesApiMapper.toResponse(registro.cliente())),
                ClientesControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('clientes.clientes.consultar')")
    public ResponseEntity<?> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return listarClientes.execute(new ListarClientesQuery(tenantOf(jwt), search, page, size)).fold(
                result -> ResponseEntity.ok(ClientesApiMapper.toPage(result)),
                ClientesControllerSupport::problem);
    }

    @GetMapping("/{clienteId}")
    @PreAuthorize("hasAuthority('clientes.clientes.consultar')")
    public ResponseEntity<?> getById(@PathVariable UUID clienteId, @AuthenticationPrincipal Jwt jwt) {
        return obtenerCliente.execute(new ObtenerClienteQuery(tenantOf(jwt), clienteId)).fold(
                result -> ResponseEntity.ok(ClientesApiMapper.toResponse(result)),
                ClientesControllerSupport::problem);
    }
}
