package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoGlobalRequest;
import com.softprimesolutions.catalogo.api.dto.request.CondicionVentaRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.query.ListarCondicionesVentaQuery;
import com.softprimesolutions.catalogo.application.port.in.ActualizarCondicionVentaUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearCondicionVentaUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarCondicionesVentaUseCase;
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
@RequestMapping("/api/v1/catalogo/condiciones-venta")
public class CondicionVentaController {

    private final CrearCondicionVentaUseCase crearCondicionVenta;
    private final ActualizarCondicionVentaUseCase actualizarCondicionVenta;
    private final ListarCondicionesVentaUseCase listarCondicionesVenta;
    private final CatalogoControlUseCase control;

    public CondicionVentaController(
            CrearCondicionVentaUseCase crearCondicionVenta,
            ActualizarCondicionVentaUseCase actualizarCondicionVenta,
            ListarCondicionesVentaUseCase listarCondicionesVenta,
            CatalogoControlUseCase control) {
        this.crearCondicionVenta = crearCondicionVenta;
        this.actualizarCondicionVenta = actualizarCondicionVenta;
        this.listarCondicionesVenta = listarCondicionesVenta;
        this.control = control;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody CondicionVentaRequest request) {
        return crearCondicionVenta.execute(CatalogoApiMapper.toCreateCommand(request)).fold(
                result -> ResponseEntity.status(201).body(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> update(@PathVariable String codigo, @Valid @RequestBody CondicionVentaRequest request) {
        return actualizarCondicionVenta.execute(CatalogoApiMapper.toUpdateCommand(request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{codigo}/estado")
    @PreAuthorize("hasAuthority('catalogo.soporte.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable String codigo, @Valid @RequestBody CambiarEstadoGlobalRequest request) {
        return control.changeCondicionVentaStatus(codigo, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.soporte.consultar')")
    public ResponseEntity<?> list(@RequestParam(required = false) String estado) {
        return listarCondicionesVenta.execute(new ListarCondicionesVentaQuery(estado)).fold(
                result -> ResponseEntity.ok(result.stream().map(CatalogoApiMapper::toResponse).toList()),
                CatalogoControllerSupport::problem);
    }
}
