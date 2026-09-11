package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.AsociarPrincipioActivoRequest;
import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoGlobalRequest;
import com.softprimesolutions.catalogo.api.dto.request.ProductoReguladoRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.command.DesasociarPrincipioActivoCommand;
import com.softprimesolutions.catalogo.application.dto.query.ConsultarProductoReguladoQuery;
import com.softprimesolutions.catalogo.application.dto.query.ListarProductosReguladosQuery;
import com.softprimesolutions.catalogo.application.port.in.ActualizarProductoReguladoUseCase;
import com.softprimesolutions.catalogo.application.port.in.AsociarPrincipioActivoUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.ConsultarProductoReguladoUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearProductoReguladoUseCase;
import com.softprimesolutions.catalogo.application.port.in.DesasociarPrincipioActivoUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarProductosReguladosUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/catalogo/productos-regulados")
public class ProductoReguladoController {

    private final CrearProductoReguladoUseCase createProductoRegulado;
    private final ActualizarProductoReguladoUseCase updateProductoRegulado;
    private final ConsultarProductoReguladoUseCase getProductoRegulado;
    private final ListarProductosReguladosUseCase listProductosRegulados;
    private final AsociarPrincipioActivoUseCase asociarPrincipioActivo;
    private final DesasociarPrincipioActivoUseCase desasociarPrincipioActivo;
    private final CatalogoControlUseCase control;

    public ProductoReguladoController(
            CrearProductoReguladoUseCase createProductoRegulado,
            ActualizarProductoReguladoUseCase updateProductoRegulado,
            ConsultarProductoReguladoUseCase getProductoRegulado,
            ListarProductosReguladosUseCase listProductosRegulados,
            AsociarPrincipioActivoUseCase asociarPrincipioActivo,
            DesasociarPrincipioActivoUseCase desasociarPrincipioActivo,
            CatalogoControlUseCase control) {
        this.createProductoRegulado = createProductoRegulado;
        this.updateProductoRegulado = updateProductoRegulado;
        this.getProductoRegulado = getProductoRegulado;
        this.listProductosRegulados = listProductosRegulados;
        this.asociarPrincipioActivo = asociarPrincipioActivo;
        this.desasociarPrincipioActivo = desasociarPrincipioActivo;
        this.control = control;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalogo.productos-regulados.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody ProductoReguladoRequest request) {
        return createProductoRegulado.execute(CatalogoApiMapper.toCreateCommand(request)).fold(
                result -> ResponseEntity.status(201).body(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PutMapping("/{productoReguladoId}")
    @PreAuthorize("hasAuthority('catalogo.productos-regulados.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable UUID productoReguladoId, @Valid @RequestBody ProductoReguladoRequest request) {
        return updateProductoRegulado.execute(CatalogoApiMapper.toUpdateCommand(productoReguladoId, request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{productoReguladoId}/estado")
    @PreAuthorize("hasAuthority('catalogo.productos-regulados.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable UUID productoReguladoId, @Valid @RequestBody CambiarEstadoGlobalRequest request) {
        return control.changeProductoReguladoStatus(productoReguladoId, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), CatalogoControllerSupport::problem);
    }

    @GetMapping("/{productoReguladoId}")
    @PreAuthorize("hasAuthority('catalogo.productos-regulados.consultar')")
    public ResponseEntity<?> get(@PathVariable UUID productoReguladoId) {
        return getProductoRegulado.execute(new ConsultarProductoReguladoQuery(productoReguladoId)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.productos-regulados.consultar')")
    public ResponseEntity<?> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String condicionVentaCodigo,
            @RequestParam(required = false) String estadoRegulatorio,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return listProductosRegulados.execute(
                        new ListarProductosReguladosQuery(q, condicionVentaCodigo, estadoRegulatorio, page, size))
                .fold(result -> ResponseEntity.ok(CatalogoApiMapper.toProductoReguladoPage(result)),
                        CatalogoControllerSupport::problem);
    }

    @PostMapping("/{productoReguladoId}/principios-activos")
    @PreAuthorize("hasAuthority('catalogo.productos-regulados.gestionar')")
    public ResponseEntity<?> asociarPrincipioActivo(
            @PathVariable UUID productoReguladoId, @Valid @RequestBody AsociarPrincipioActivoRequest request) {
        return asociarPrincipioActivo.execute(CatalogoApiMapper.toCommand(productoReguladoId, request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @DeleteMapping("/{productoReguladoId}/principios-activos/{principioActivoId}")
    @PreAuthorize("hasAuthority('catalogo.productos-regulados.gestionar')")
    public ResponseEntity<?> desasociarPrincipioActivo(
            @PathVariable UUID productoReguladoId, @PathVariable UUID principioActivoId) {
        return desasociarPrincipioActivo.execute(
                        new DesasociarPrincipioActivoCommand(productoReguladoId, principioActivoId))
                .fold(result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                        CatalogoControllerSupport::problem);
    }
}
