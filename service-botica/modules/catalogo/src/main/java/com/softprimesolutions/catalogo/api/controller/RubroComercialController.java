package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoTenantRequest;
import com.softprimesolutions.catalogo.api.dto.request.RubroComercialRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.query.ConsultarRubroComercialQuery;
import com.softprimesolutions.catalogo.application.dto.query.ListarRubrosComercialesQuery;
import com.softprimesolutions.catalogo.application.port.in.ActualizarRubroComercialUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.ConsultarRubroComercialUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearRubroComercialUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarRubrosComercialesUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@RequestMapping("/api/v1/catalogo/rubros-comerciales")
public class RubroComercialController {

    private final CrearRubroComercialUseCase createRubroComercial;
    private final ActualizarRubroComercialUseCase updateRubroComercial;
    private final ConsultarRubroComercialUseCase getRubroComercial;
    private final ListarRubrosComercialesUseCase listRubrosComerciales;
    private final CatalogoControlUseCase control;

    public RubroComercialController(
            CrearRubroComercialUseCase createRubroComercial,
            ActualizarRubroComercialUseCase updateRubroComercial,
            ConsultarRubroComercialUseCase getRubroComercial,
            ListarRubrosComercialesUseCase listRubrosComerciales,
            CatalogoControlUseCase control) {
        this.createRubroComercial = createRubroComercial;
        this.updateRubroComercial = updateRubroComercial;
        this.getRubroComercial = getRubroComercial;
        this.listRubrosComerciales = listRubrosComerciales;
        this.control = control;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalogo.rubros-comerciales.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody RubroComercialRequest request) {
        return createRubroComercial.execute(CatalogoApiMapper.toCreateCommand(request)).fold(
                result -> ResponseEntity.status(201).body(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PutMapping("/{rubroComercialId}")
    @PreAuthorize("hasAuthority('catalogo.rubros-comerciales.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable UUID rubroComercialId, @Valid @RequestBody RubroComercialRequest request) {
        return updateRubroComercial.execute(CatalogoApiMapper.toUpdateCommand(rubroComercialId, request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{rubroComercialId}/estado")
    @PreAuthorize("hasAuthority('catalogo.rubros-comerciales.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable UUID rubroComercialId, @Valid @RequestBody CambiarEstadoTenantRequest request) {
        return control.changeRubroComercialStatus(request.tenantId(), rubroComercialId, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), CatalogoControllerSupport::problem);
    }

    @GetMapping("/{rubroComercialId}")
    @PreAuthorize("hasAuthority('catalogo.rubros-comerciales.consultar')")
    public ResponseEntity<?> get(@PathVariable UUID rubroComercialId, @RequestParam UUID tenantId) {
        return getRubroComercial.execute(new ConsultarRubroComercialQuery(tenantId, rubroComercialId)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.rubros-comerciales.consultar')")
    public ResponseEntity<?> list(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean esFarmaceutico,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return listRubrosComerciales.execute(
                        new ListarRubrosComercialesQuery(tenantId, q, esFarmaceutico, estado, page, size))
                .fold(result -> ResponseEntity.ok(CatalogoApiMapper.toRubroComercialPage(result)),
                        CatalogoControllerSupport::problem);
    }
}
