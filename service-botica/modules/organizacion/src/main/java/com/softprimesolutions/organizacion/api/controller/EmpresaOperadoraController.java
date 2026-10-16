package com.softprimesolutions.organizacion.api.controller;

import static com.softprimesolutions.organizacion.api.controller.OrganizacionControllerSupport.tenantOf;

import com.softprimesolutions.organizacion.api.dto.request.ActualizarEmpresaOperadoraRequest;
import com.softprimesolutions.organizacion.api.dto.request.CambiarEstadoRequest;
import com.softprimesolutions.organizacion.api.dto.request.CrearEmpresaOperadoraRequest;
import com.softprimesolutions.organizacion.api.mapper.OrganizacionApiMapper;
import com.softprimesolutions.organizacion.application.dto.query.ListarEmpresasQuery;
import com.softprimesolutions.organizacion.application.dto.query.ObtenerEmpresaQuery;
import com.softprimesolutions.organizacion.application.port.in.ActualizarEmpresaOperadoraUseCase;
import com.softprimesolutions.organizacion.application.port.in.CambiarEstadoEmpresaUseCase;
import com.softprimesolutions.organizacion.application.port.in.CrearEmpresaOperadoraUseCase;
import com.softprimesolutions.organizacion.application.port.in.ListarEmpresasUseCase;
import com.softprimesolutions.organizacion.application.port.in.ObtenerEmpresaUseCase;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(EmpresaOperadoraController.BASE_PATH)
public class EmpresaOperadoraController {

    static final String BASE_PATH = "/api/v1/organizacion/empresas";

    private final CrearEmpresaOperadoraUseCase createEmpresa;
    private final ActualizarEmpresaOperadoraUseCase updateEmpresa;
    private final ListarEmpresasUseCase listEmpresas;
    private final ObtenerEmpresaUseCase getEmpresa;
    private final CambiarEstadoEmpresaUseCase changeEmpresaStatus;

    public EmpresaOperadoraController(
            CrearEmpresaOperadoraUseCase createEmpresa,
            ActualizarEmpresaOperadoraUseCase updateEmpresa,
            ListarEmpresasUseCase listEmpresas,
            ObtenerEmpresaUseCase getEmpresa,
            CambiarEstadoEmpresaUseCase changeEmpresaStatus) {
        this.createEmpresa = createEmpresa;
        this.updateEmpresa = updateEmpresa;
        this.listEmpresas = listEmpresas;
        this.getEmpresa = getEmpresa;
        this.changeEmpresaStatus = changeEmpresaStatus;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('organizacion.empresas.gestionar')")
    public ResponseEntity<?> create(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CrearEmpresaOperadoraRequest request) {
        return createEmpresa.execute(OrganizacionApiMapper.toCommand(tenantOf(jwt), request)).fold(
                result -> OrganizacionControllerSupport.created(
                        BASE_PATH, result.id(), OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('organizacion.empresas.consultar')")
    public ResponseEntity<?> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return listEmpresas.execute(new ListarEmpresasQuery(tenantOf(jwt), search, page, size)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toEmpresaPage(result)),
                OrganizacionControllerSupport::problem);
    }

    @GetMapping("/{empresaId}")
    @PreAuthorize("hasAuthority('organizacion.empresas.consultar')")
    public ResponseEntity<?> getById(@PathVariable UUID empresaId, @AuthenticationPrincipal Jwt jwt) {
        return getEmpresa.execute(new ObtenerEmpresaQuery(tenantOf(jwt), empresaId)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }

    @PutMapping("/{empresaId}")
    @PreAuthorize("hasAuthority('organizacion.empresas.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable UUID empresaId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ActualizarEmpresaOperadoraRequest request) {
        return updateEmpresa.execute(OrganizacionApiMapper.toCommand(empresaId, tenantOf(jwt), request)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }

    @PatchMapping("/{empresaId}/estado")
    @PreAuthorize("hasAuthority('organizacion.empresas.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable UUID empresaId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CambiarEstadoRequest request) {
        return changeEmpresaStatus.execute(
                OrganizacionApiMapper.toCambiarEstadoEmpresaCommand(empresaId, tenantOf(jwt), request)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }
}
