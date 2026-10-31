package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoGlobalRequest;
import com.softprimesolutions.catalogo.api.dto.request.TipoDocumentoIdentidadRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.query.ConsultarTipoDocumentoIdentidadQuery;
import com.softprimesolutions.catalogo.application.dto.query.ListarTiposDocumentoIdentidadQuery;
import com.softprimesolutions.catalogo.application.port.in.ActualizarTipoDocumentoIdentidadUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.ConsultarTipoDocumentoIdentidadUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearTipoDocumentoIdentidadUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarTiposDocumentoIdentidadUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@RequestMapping("/api/v1/catalogo/tipos-documento-identidad")
public class TipoDocumentoIdentidadController {

    private final CrearTipoDocumentoIdentidadUseCase crearTipoDocumentoIdentidad;
    private final ActualizarTipoDocumentoIdentidadUseCase actualizarTipoDocumentoIdentidad;
    private final ConsultarTipoDocumentoIdentidadUseCase consultarTipoDocumentoIdentidad;
    private final ListarTiposDocumentoIdentidadUseCase listarTiposDocumentoIdentidad;
    private final CatalogoControlUseCase control;

    public TipoDocumentoIdentidadController(
            CrearTipoDocumentoIdentidadUseCase crearTipoDocumentoIdentidad,
            ActualizarTipoDocumentoIdentidadUseCase actualizarTipoDocumentoIdentidad,
            ConsultarTipoDocumentoIdentidadUseCase consultarTipoDocumentoIdentidad,
            ListarTiposDocumentoIdentidadUseCase listarTiposDocumentoIdentidad,
            CatalogoControlUseCase control) {
        this.crearTipoDocumentoIdentidad = crearTipoDocumentoIdentidad;
        this.actualizarTipoDocumentoIdentidad = actualizarTipoDocumentoIdentidad;
        this.consultarTipoDocumentoIdentidad = consultarTipoDocumentoIdentidad;
        this.listarTiposDocumentoIdentidad = listarTiposDocumentoIdentidad;
        this.control = control;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody TipoDocumentoIdentidadRequest request) {
        return crearTipoDocumentoIdentidad.execute(CatalogoApiMapper.toCreateCommand(request)).fold(
                result -> ResponseEntity.status(201).body(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable String codigo, @Valid @RequestBody TipoDocumentoIdentidadRequest request) {
        return actualizarTipoDocumentoIdentidad.execute(CatalogoApiMapper.toUpdateCommand(request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{codigo}/estado")
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable String codigo, @Valid @RequestBody CambiarEstadoGlobalRequest request) {
        return control.changeTipoDocumentoIdentidadStatus(codigo, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), CatalogoControllerSupport::problem);
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("hasAuthority('catalogo.soporte.consultar')")
    public ResponseEntity<?> get(@PathVariable String codigo) {
        return consultarTipoDocumentoIdentidad.execute(new ConsultarTipoDocumentoIdentidadQuery(codigo)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.soporte.consultar')")
    public ResponseEntity<?> list(
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return listarTiposDocumentoIdentidad.execute(new ListarTiposDocumentoIdentidadQuery(estado, page, size))
                .fold(result -> ResponseEntity.ok(CatalogoApiMapper.toTipoDocumentoIdentidadPage(result)),
                        CatalogoControllerSupport::problem);
    }
}
