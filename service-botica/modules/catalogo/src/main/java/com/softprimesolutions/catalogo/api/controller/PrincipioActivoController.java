package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoGlobalRequest;
import com.softprimesolutions.catalogo.api.dto.request.PrincipioActivoRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.query.ListarPrincipioActivoQuery;
import com.softprimesolutions.catalogo.application.port.in.ActualizarPrincipioActivoUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearPrincipioActivoUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarPrincipioActivoUseCase;
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
@RequestMapping("/api/v1/catalogo/principios-activos")
public class PrincipioActivoController {

    private final CrearPrincipioActivoUseCase createPrincipioActivo;
    private final ActualizarPrincipioActivoUseCase updatePrincipioActivo;
    private final ListarPrincipioActivoUseCase listPrincipiosActivos;
    private final CatalogoControlUseCase control;

    public PrincipioActivoController(
            CrearPrincipioActivoUseCase createPrincipioActivo,
            ActualizarPrincipioActivoUseCase updatePrincipioActivo,
            ListarPrincipioActivoUseCase listPrincipiosActivos,
            CatalogoControlUseCase control) {
        this.createPrincipioActivo = createPrincipioActivo;
        this.updatePrincipioActivo = updatePrincipioActivo;
        this.listPrincipiosActivos = listPrincipiosActivos;
        this.control = control;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalogo.principios-activos.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody PrincipioActivoRequest request) {
        return createPrincipioActivo.execute(CatalogoApiMapper.toCreateCommand(request)).fold(
                result -> ResponseEntity.status(201).body(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PutMapping("/{principioActivoId}")
    @PreAuthorize("hasAuthority('catalogo.principios-activos.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable UUID principioActivoId, @Valid @RequestBody PrincipioActivoRequest request) {
        return updatePrincipioActivo.execute(CatalogoApiMapper.toUpdateCommand(principioActivoId, request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{principioActivoId}/estado")
    @PreAuthorize("hasAuthority('catalogo.principios-activos.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable UUID principioActivoId, @Valid @RequestBody CambiarEstadoGlobalRequest request) {
        return control.changePrincipioActivoStatus(principioActivoId, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.principios-activos.consultar')")
    public ResponseEntity<?> list(
            @RequestParam(required = false) String texto, @RequestParam(required = false) String estado) {
        return listPrincipiosActivos.execute(new ListarPrincipioActivoQuery(texto, estado)).fold(
                result -> ResponseEntity.ok(result.stream().map(CatalogoApiMapper::toResponse).toList()),
                CatalogoControllerSupport::problem);
    }
}
