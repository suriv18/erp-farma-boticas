package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoTenantRequest;
import com.softprimesolutions.catalogo.api.dto.request.MarcaRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.query.ListarMarcasQuery;
import com.softprimesolutions.catalogo.application.port.in.ActualizarMarcaUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearMarcaUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarMarcasUseCase;
import jakarta.validation.Valid;
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
    private final ListarMarcasUseCase listMarcas;
    private final CatalogoControlUseCase control;

    public MarcaController(
            CrearMarcaUseCase createMarca, ActualizarMarcaUseCase updateMarca, ListarMarcasUseCase listMarcas,
            CatalogoControlUseCase control) {
        this.createMarca = createMarca;
        this.updateMarca = updateMarca;
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

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.marcas.consultar')")
    public ResponseEntity<?> list(@RequestParam UUID tenantId, @RequestParam(required = false) String estado) {
        return listMarcas.execute(new ListarMarcasQuery(tenantId, estado)).fold(
                result -> ResponseEntity.ok(result.stream().map(CatalogoApiMapper::toResponse).toList()),
                CatalogoControllerSupport::problem);
    }
}
