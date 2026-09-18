package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.AgregarCodigoBarraRequest;
import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoTenantRequest;
import com.softprimesolutions.catalogo.api.dto.request.SkuRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.command.EliminarCodigoBarraCommand;
import com.softprimesolutions.catalogo.application.dto.command.MarcarCodigoBarraPrincipalCommand;
import com.softprimesolutions.catalogo.application.dto.query.ConsultarSkuQuery;
import com.softprimesolutions.catalogo.application.dto.query.ListarSkusQuery;
import com.softprimesolutions.catalogo.application.port.in.ActualizarSkuUseCase;
import com.softprimesolutions.catalogo.application.port.in.AgregarCodigoBarraUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.ConsultarSkuUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearSkuUseCase;
import com.softprimesolutions.catalogo.application.port.in.EliminarCodigoBarraUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarSkusUseCase;
import com.softprimesolutions.catalogo.application.port.in.MarcarCodigoBarraPrincipalUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/v1/catalogo/skus")
public class SkuController {

    private final CrearSkuUseCase createSku;
    private final ActualizarSkuUseCase updateSku;
    private final ConsultarSkuUseCase getSku;
    private final ListarSkusUseCase listSkus;
    private final AgregarCodigoBarraUseCase agregarCodigoBarra;
    private final EliminarCodigoBarraUseCase eliminarCodigoBarra;
    private final MarcarCodigoBarraPrincipalUseCase marcarCodigoBarraPrincipal;
    private final CatalogoControlUseCase control;

    public SkuController(
            CrearSkuUseCase createSku,
            ActualizarSkuUseCase updateSku,
            ConsultarSkuUseCase getSku,
            ListarSkusUseCase listSkus,
            AgregarCodigoBarraUseCase agregarCodigoBarra,
            EliminarCodigoBarraUseCase eliminarCodigoBarra,
            MarcarCodigoBarraPrincipalUseCase marcarCodigoBarraPrincipal,
            CatalogoControlUseCase control) {
        this.createSku = createSku;
        this.updateSku = updateSku;
        this.getSku = getSku;
        this.listSkus = listSkus;
        this.agregarCodigoBarra = agregarCodigoBarra;
        this.eliminarCodigoBarra = eliminarCodigoBarra;
        this.marcarCodigoBarraPrincipal = marcarCodigoBarraPrincipal;
        this.control = control;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalogo.skus.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody SkuRequest request, Authentication authentication) {
        return createSku.execute(CatalogoApiMapper.toCreateCommand(request, authentication.getName())).fold(
                result -> ResponseEntity.status(201).body(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PutMapping("/{skuId}")
    @PreAuthorize("hasAuthority('catalogo.skus.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable UUID skuId, @Valid @RequestBody SkuRequest request, Authentication authentication) {
        return updateSku.execute(CatalogoApiMapper.toUpdateCommand(skuId, request, authentication.getName())).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{skuId}/estado")
    @PreAuthorize("hasAuthority('catalogo.skus.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable UUID skuId, @Valid @RequestBody CambiarEstadoTenantRequest request) {
        return control.changeSkuStatus(request.tenantId(), skuId, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), CatalogoControllerSupport::problem);
    }

    @GetMapping("/{skuId}")
    @PreAuthorize("hasAuthority('catalogo.skus.consultar')")
    public ResponseEntity<?> get(@PathVariable UUID skuId, @RequestParam UUID tenantId) {
        return getSku.execute(new ConsultarSkuQuery(tenantId, skuId)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.skus.consultar')")
    public ResponseEntity<?> list(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) UUID marcaId,
            @RequestParam(required = false) String tipoSku,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return listSkus.execute(new ListarSkusQuery(tenantId, q, categoriaId, marcaId, tipoSku, estado, page, size))
                .fold(result -> ResponseEntity.ok(CatalogoApiMapper.toSkuPage(result)),
                        CatalogoControllerSupport::problem);
    }

    @PostMapping("/{skuId}/codigos-barra")
    @PreAuthorize("hasAuthority('catalogo.skus.gestionar')")
    public ResponseEntity<?> agregarCodigoBarra(
            @PathVariable UUID skuId, @RequestParam UUID tenantId, @Valid @RequestBody AgregarCodigoBarraRequest request) {
        return agregarCodigoBarra.execute(CatalogoApiMapper.toCommand(tenantId, skuId, request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @DeleteMapping("/{skuId}/codigos-barra/{codigoBarra}")
    @PreAuthorize("hasAuthority('catalogo.skus.gestionar')")
    public ResponseEntity<?> eliminarCodigoBarra(
            @PathVariable UUID skuId, @PathVariable String codigoBarra, @RequestParam UUID tenantId) {
        return eliminarCodigoBarra.execute(new EliminarCodigoBarraCommand(tenantId, skuId, codigoBarra)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{skuId}/codigos-barra/{codigoBarra}/principal")
    @PreAuthorize("hasAuthority('catalogo.skus.gestionar')")
    public ResponseEntity<?> marcarCodigoBarraPrincipal(
            @PathVariable UUID skuId, @PathVariable String codigoBarra, @RequestParam UUID tenantId) {
        return marcarCodigoBarraPrincipal.execute(
                        new MarcarCodigoBarraPrincipalCommand(tenantId, skuId, codigoBarra))
                .fold(result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                        CatalogoControllerSupport::problem);
    }
}
