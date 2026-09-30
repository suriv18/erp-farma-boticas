package com.softprimesolutions.organizacion.api.controller;

import com.softprimesolutions.organizacion.api.dto.request.ActualizarEmpresaOperadoraRequest;
import com.softprimesolutions.organizacion.api.dto.request.CrearEmpresaOperadoraRequest;
import com.softprimesolutions.organizacion.api.mapper.OrganizacionApiMapper;
import com.softprimesolutions.organizacion.application.dto.query.ListarEmpresasQuery;
import com.softprimesolutions.organizacion.application.dto.query.ObtenerEmpresaQuery;
import com.softprimesolutions.organizacion.application.port.in.ActualizarEmpresaOperadoraUseCase;
import com.softprimesolutions.organizacion.application.port.in.CrearEmpresaOperadoraUseCase;
import com.softprimesolutions.organizacion.application.port.in.ListarEmpresasUseCase;
import com.softprimesolutions.organizacion.application.port.in.ObtenerEmpresaUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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

    public EmpresaOperadoraController(
            CrearEmpresaOperadoraUseCase createEmpresa,
            ActualizarEmpresaOperadoraUseCase updateEmpresa,
            ListarEmpresasUseCase listEmpresas,
            ObtenerEmpresaUseCase getEmpresa) {
        this.createEmpresa = createEmpresa;
        this.updateEmpresa = updateEmpresa;
        this.listEmpresas = listEmpresas;
        this.getEmpresa = getEmpresa;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('organizacion.empresas.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody CrearEmpresaOperadoraRequest request) {
        return createEmpresa.execute(OrganizacionApiMapper.toCommand(request)).fold(
                result -> OrganizacionControllerSupport.created(
                        BASE_PATH, result.id(), OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('organizacion.empresas.consultar')")
    public ResponseEntity<?> list(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return listEmpresas.execute(new ListarEmpresasQuery(tenantId, search, page, size)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toEmpresaPage(result)),
                OrganizacionControllerSupport::problem);
    }

    @GetMapping("/{empresaId}")
    @PreAuthorize("hasAuthority('organizacion.empresas.consultar')")
    public ResponseEntity<?> getById(@PathVariable UUID empresaId, @RequestParam UUID tenantId) {
        return getEmpresa.execute(new ObtenerEmpresaQuery(tenantId, empresaId)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }

    @PutMapping("/{empresaId}")
    @PreAuthorize("hasAuthority('organizacion.empresas.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable UUID empresaId,
            @RequestParam UUID tenantId,
            @Valid @RequestBody ActualizarEmpresaOperadoraRequest request) {
        return updateEmpresa.execute(OrganizacionApiMapper.toCommand(empresaId, tenantId, request)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }
}
