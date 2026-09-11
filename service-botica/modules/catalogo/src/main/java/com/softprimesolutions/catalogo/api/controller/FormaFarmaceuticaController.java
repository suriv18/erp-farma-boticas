package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoGlobalRequest;
import com.softprimesolutions.catalogo.api.dto.request.FormaFarmaceuticaRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.query.ListarFormasFarmaceuticasQuery;
import com.softprimesolutions.catalogo.application.port.in.ActualizarFormaFarmaceuticaUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearFormaFarmaceuticaUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarFormasFarmaceuticasUseCase;
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
@RequestMapping("/api/v1/catalogo/formas-farmaceuticas")
public class FormaFarmaceuticaController {

    private final CrearFormaFarmaceuticaUseCase crearFormaFarmaceutica;
    private final ActualizarFormaFarmaceuticaUseCase actualizarFormaFarmaceutica;
    private final ListarFormasFarmaceuticasUseCase listarFormasFarmaceuticas;
    private final CatalogoControlUseCase control;

    public FormaFarmaceuticaController(
            CrearFormaFarmaceuticaUseCase crearFormaFarmaceutica,
            ActualizarFormaFarmaceuticaUseCase actualizarFormaFarmaceutica,
            ListarFormasFarmaceuticasUseCase listarFormasFarmaceuticas,
            CatalogoControlUseCase control) {
        this.crearFormaFarmaceutica = crearFormaFarmaceutica;
        this.actualizarFormaFarmaceutica = actualizarFormaFarmaceutica;
        this.listarFormasFarmaceuticas = listarFormasFarmaceuticas;
        this.control = control;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody FormaFarmaceuticaRequest request) {
        return crearFormaFarmaceutica.execute(CatalogoApiMapper.toCreateCommand(request)).fold(
                result -> ResponseEntity.status(201).body(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable String codigo, @Valid @RequestBody FormaFarmaceuticaRequest request) {
        return actualizarFormaFarmaceutica.execute(CatalogoApiMapper.toUpdateCommand(request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{codigo}/estado")
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable String codigo, @Valid @RequestBody CambiarEstadoGlobalRequest request) {
        return control.changeFormaFarmaceuticaStatus(codigo, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.soporte.consultar')")
    public ResponseEntity<?> list(@RequestParam(required = false) String estado) {
        return listarFormasFarmaceuticas.execute(new ListarFormasFarmaceuticasQuery(estado)).fold(
                result -> ResponseEntity.ok(result.stream().map(CatalogoApiMapper::toResponse).toList()),
                CatalogoControllerSupport::problem);
    }
}
