package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoTenantRequest;
import com.softprimesolutions.catalogo.api.dto.request.MarcaRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.query.ConsultarMarcaQuery;
import com.softprimesolutions.catalogo.application.dto.query.ListarMarcasQuery;
import com.softprimesolutions.catalogo.application.port.in.ActualizarMarcaUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.ConsultarMarcaUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearMarcaUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarMarcasUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/catalogo/marcas")
public class MarcaController {

    private final CrearMarcaUseCase createMarca;
    private final ActualizarMarcaUseCase updateMarca;
    private final ConsultarMarcaUseCase getMarca;
    private final ListarMarcasUseCase listMarcas;
    private final CatalogoControlUseCase control;

    public MarcaController(
            CrearMarcaUseCase createMarca, ActualizarMarcaUseCase updateMarca, ConsultarMarcaUseCase getMarca,
            ListarMarcasUseCase listMarcas, CatalogoControlUseCase control) {
        this.createMarca = createMarca;
        this.updateMarca = updateMarca;
        this.getMarca = getMarca;
        this.listMarcas = listMarcas;
        this.control = control;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalogo.marcas.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody MarcaRequest request) {
        return createMarca.execute(CatalogoApiMapper.toCreateCommand(request)).fold(
                result -> ResponseEntity.status(201).body(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PutMapping("/{marcaId}")
    @PreAuthorize("hasAuthority('catalogo.marcas.gestionar')")
    public ResponseEntity<?> update(@PathVariable UUID marcaId, @Valid @RequestBody MarcaRequest request) {
        return updateMarca.execute(CatalogoApiMapper.toUpdateCommand(marcaId, request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{marcaId}/estado")
    @PreAuthorize("hasAuthority('catalogo.marcas.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable UUID marcaId, @Valid @RequestBody CambiarEstadoTenantRequest request) {
        return control.changeMarcaStatus(request.tenantId(), marcaId, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), CatalogoControllerSupport::problem);
    }

    @GetMapping("/{marcaId}")
    @PreAuthorize("hasAuthority('catalogo.marcas.consultar')")
    public ResponseEntity<?> get(@PathVariable UUID marcaId, @RequestParam UUID tenantId) {
        return getMarca.execute(new ConsultarMarcaQuery(tenantId, marcaId)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.marcas.consultar')")
    public ResponseEntity<?> list(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return listMarcas.execute(new ListarMarcasQuery(tenantId, q, estado, page, size)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toMarcaPage(result)),
                CatalogoControllerSupport::problem);
    }
}
