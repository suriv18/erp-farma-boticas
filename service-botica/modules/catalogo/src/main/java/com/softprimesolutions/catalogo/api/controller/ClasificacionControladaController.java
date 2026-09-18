package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoGlobalRequest;
import com.softprimesolutions.catalogo.api.dto.request.ClasificacionControladaRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.query.ListarClasificacionesControladasQuery;
import com.softprimesolutions.catalogo.application.port.in.ActualizarClasificacionControladaUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearClasificacionControladaUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarClasificacionesControladasUseCase;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/catalogo/clasificaciones-controladas")
public class ClasificacionControladaController {

    private final CrearClasificacionControladaUseCase crearClasificacionControlada;
    private final ActualizarClasificacionControladaUseCase actualizarClasificacionControlada;
    private final ListarClasificacionesControladasUseCase listarClasificacionesControladas;
    private final CatalogoControlUseCase control;

    public ClasificacionControladaController(
            CrearClasificacionControladaUseCase crearClasificacionControlada,
            ActualizarClasificacionControladaUseCase actualizarClasificacionControlada,
            ListarClasificacionesControladasUseCase listarClasificacionesControladas,
            CatalogoControlUseCase control) {
        this.crearClasificacionControlada = crearClasificacionControlada;
        this.actualizarClasificacionControlada = actualizarClasificacionControlada;
        this.listarClasificacionesControladas = listarClasificacionesControladas;
        this.control = control;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody ClasificacionControladaRequest request) {
        return crearClasificacionControlada.execute(CatalogoApiMapper.toCreateCommand(request)).fold(
                result -> ResponseEntity.status(201).body(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable String codigo, @Valid @RequestBody ClasificacionControladaRequest request) {
        return actualizarClasificacionControlada.execute(CatalogoApiMapper.toUpdateCommand(request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{codigo}/estado")
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable String codigo, @Valid @RequestBody CambiarEstadoGlobalRequest request) {
        return control.changeClasificacionControladaStatus(codigo, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.soporte.consultar')")
    public ResponseEntity<?> list(@RequestParam(required = false) String estado) {
        return listarClasificacionesControladas.execute(new ListarClasificacionesControladasQuery(estado)).fold(
                result -> ResponseEntity.ok(result.stream().map(CatalogoApiMapper::toResponse).toList()),
                CatalogoControllerSupport::problem);
    }
}
