package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoGlobalRequest;
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
    public ResponseEntity<?> create(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MarcaRequest request) {
        return createMarca.execute(
                        CatalogoApiMapper.toCreateCommand(CatalogoControllerSupport.tenantOf(jwt), request)).fold(
                result -> ResponseEntity.status(201).body(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PutMapping("/{marcaId}")
    @PreAuthorize("hasAuthority('catalogo.marcas.gestionar')")
    public ResponseEntity<?> update(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID marcaId, @Valid @RequestBody MarcaRequest request) {
        return updateMarca.execute(
                        CatalogoApiMapper.toUpdateCommand(CatalogoControllerSupport.tenantOf(jwt), marcaId, request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{marcaId}/estado")
    @PreAuthorize("hasAuthority('catalogo.marcas.gestionar')")
    public ResponseEntity<?> changeStatus(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID marcaId, @Valid @RequestBody CambiarEstadoGlobalRequest request) {
        return control.changeMarcaStatus(CatalogoControllerSupport.tenantOf(jwt), marcaId, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), CatalogoControllerSupport::problem);
    }

    @GetMapping("/{marcaId}")
    @PreAuthorize("hasAuthority('catalogo.marcas.consultar')")
    public ResponseEntity<?> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID marcaId) {
        return getMarca.execute(
                        new ConsultarMarcaQuery(CatalogoControllerSupport.tenantOf(jwt), marcaId)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.marcas.consultar')")
    public ResponseEntity<?> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return listMarcas.execute(new ListarMarcasQuery(CatalogoControllerSupport.tenantOf(jwt), q, estado, page, size)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toMarcaPage(result)),
                CatalogoControllerSupport::problem);
    }
}
