package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoGlobalRequest;
import com.softprimesolutions.catalogo.api.dto.request.UnidadMedidaRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.query.ListarUnidadesMedidaQuery;
import com.softprimesolutions.catalogo.application.port.in.ActualizarUnidadMedidaUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearUnidadMedidaUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarUnidadesMedidaUseCase;
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
@RequestMapping("/api/v1/catalogo/unidades-medida")
public class UnidadMedidaController {

    private final CrearUnidadMedidaUseCase crearUnidadMedida;
    private final ActualizarUnidadMedidaUseCase actualizarUnidadMedida;
    private final ListarUnidadesMedidaUseCase listarUnidadesMedida;
    private final CatalogoControlUseCase control;

    public UnidadMedidaController(
            CrearUnidadMedidaUseCase crearUnidadMedida,
            ActualizarUnidadMedidaUseCase actualizarUnidadMedida,
            ListarUnidadesMedidaUseCase listarUnidadesMedida,
            CatalogoControlUseCase control) {
        this.crearUnidadMedida = crearUnidadMedida;
        this.actualizarUnidadMedida = actualizarUnidadMedida;
        this.listarUnidadesMedida = listarUnidadesMedida;
        this.control = control;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody UnidadMedidaRequest request) {
        return crearUnidadMedida.execute(CatalogoApiMapper.toCreateCommand(request)).fold(
                result -> ResponseEntity.status(201).body(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> update(@PathVariable String codigo, @Valid @RequestBody UnidadMedidaRequest request) {
        return actualizarUnidadMedida.execute(CatalogoApiMapper.toUpdateCommand(request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{codigo}/estado")
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable String codigo, @Valid @RequestBody CambiarEstadoGlobalRequest request) {
        return control.changeUnidadMedidaStatus(codigo, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.soporte.consultar')")
    public ResponseEntity<?> list(@RequestParam(required = false) String estado) {
        return listarUnidadesMedida.execute(new ListarUnidadesMedidaQuery(estado)).fold(
                result -> ResponseEntity.ok(result.stream().map(CatalogoApiMapper::toResponse).toList()),
                CatalogoControllerSupport::problem);
    }
}
