package com.softprimesolutions.organizacion.api.controller;

import static com.softprimesolutions.organizacion.api.controller.OrganizacionControllerSupport.tenantOf;

import com.softprimesolutions.organizacion.api.dto.request.ActualizarAlmacenRequest;
import com.softprimesolutions.organizacion.api.dto.request.CrearAlmacenRequest;
import com.softprimesolutions.organizacion.api.mapper.OrganizacionApiMapper;
import com.softprimesolutions.organizacion.application.dto.query.ListarAlmacenesQuery;
import com.softprimesolutions.organizacion.application.dto.query.ObtenerAlmacenQuery;
import com.softprimesolutions.organizacion.application.port.in.ActualizarAlmacenUseCase;
import com.softprimesolutions.organizacion.application.port.in.CrearAlmacenUseCase;
import com.softprimesolutions.organizacion.application.port.in.ListarAlmacenesUseCase;
import com.softprimesolutions.organizacion.application.port.in.ObtenerAlmacenUseCase;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(AlmacenController.BASE_PATH)
public class AlmacenController {

    static final String BASE_PATH = "/api/v1/organizacion/almacenes";

    private final CrearAlmacenUseCase createAlmacen;
    private final ActualizarAlmacenUseCase updateAlmacen;
    private final ListarAlmacenesUseCase listAlmacenes;
    private final ObtenerAlmacenUseCase getAlmacen;

    public AlmacenController(
            CrearAlmacenUseCase createAlmacen,
            ActualizarAlmacenUseCase updateAlmacen,
            ListarAlmacenesUseCase listAlmacenes,
            ObtenerAlmacenUseCase getAlmacen) {
        this.createAlmacen = createAlmacen;
        this.updateAlmacen = updateAlmacen;
        this.listAlmacenes = listAlmacenes;
        this.getAlmacen = getAlmacen;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('organizacion.almacenes.gestionar')")
    public ResponseEntity<?> create(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CrearAlmacenRequest request) {
        return createAlmacen.execute(OrganizacionApiMapper.toCommand(tenantOf(jwt), request)).fold(
                result -> OrganizacionControllerSupport.created(
                        BASE_PATH, result.id(), OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('organizacion.almacenes.consultar')")
    public ResponseEntity<?> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID establecimientoId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return listAlmacenes.execute(
                new ListarAlmacenesQuery(tenantOf(jwt), establecimientoId, search, page, size)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toAlmacenPage(result)),
                OrganizacionControllerSupport::problem);
    }

    @GetMapping("/{almacenId}")
    @PreAuthorize("hasAuthority('organizacion.almacenes.consultar')")
    public ResponseEntity<?> getById(@PathVariable UUID almacenId, @AuthenticationPrincipal Jwt jwt) {
        return getAlmacen.execute(new ObtenerAlmacenQuery(tenantOf(jwt), almacenId)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }

    @PutMapping("/{almacenId}")
    @PreAuthorize("hasAuthority('organizacion.almacenes.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable UUID almacenId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ActualizarAlmacenRequest request) {
        return updateAlmacen.execute(OrganizacionApiMapper.toCommand(almacenId, tenantOf(jwt), request)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }
}
