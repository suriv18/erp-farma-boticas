package com.softprimesolutions.organizacion.api.controller;

import com.softprimesolutions.organizacion.api.dto.request.ActualizarEstablecimientoRequest;
import com.softprimesolutions.organizacion.api.dto.request.CrearEstablecimientoRequest;
import com.softprimesolutions.organizacion.api.mapper.OrganizacionApiMapper;
import com.softprimesolutions.organizacion.application.dto.query.ListarEstablecimientosQuery;
import com.softprimesolutions.organizacion.application.dto.query.ObtenerEstablecimientoQuery;
import com.softprimesolutions.organizacion.application.port.in.ActualizarEstablecimientoUseCase;
import com.softprimesolutions.organizacion.application.port.in.CrearEstablecimientoUseCase;
import com.softprimesolutions.organizacion.application.port.in.ListarEstablecimientosUseCase;
import com.softprimesolutions.organizacion.application.port.in.ObtenerEstablecimientoUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping(EstablecimientoController.BASE_PATH)
public class EstablecimientoController {

    static final String BASE_PATH = "/api/v1/organizacion/establecimientos";

    private final CrearEstablecimientoUseCase createEstablecimiento;
    private final ActualizarEstablecimientoUseCase updateEstablecimiento;
    private final ListarEstablecimientosUseCase listEstablecimientos;
    private final ObtenerEstablecimientoUseCase getEstablecimiento;

    public EstablecimientoController(
            CrearEstablecimientoUseCase createEstablecimiento,
            ActualizarEstablecimientoUseCase updateEstablecimiento,
            ListarEstablecimientosUseCase listEstablecimientos,
            ObtenerEstablecimientoUseCase getEstablecimiento) {
        this.createEstablecimiento = createEstablecimiento;
        this.updateEstablecimiento = updateEstablecimiento;
        this.listEstablecimientos = listEstablecimientos;
        this.getEstablecimiento = getEstablecimiento;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('organizacion.establecimientos.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody CrearEstablecimientoRequest request) {
        return createEstablecimiento.execute(OrganizacionApiMapper.toCommand(request)).fold(
                result -> OrganizacionControllerSupport.created(
                        BASE_PATH, result.id(), OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('organizacion.establecimientos.consultar')")
    public ResponseEntity<?> list(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) UUID empresaId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return listEstablecimientos.execute(
                new ListarEstablecimientosQuery(tenantId, empresaId, search, page, size)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toEstablecimientoPage(result)),
                OrganizacionControllerSupport::problem);
    }

    @GetMapping("/{establecimientoId}")
    @PreAuthorize("hasAuthority('organizacion.establecimientos.consultar')")
    public ResponseEntity<?> getById(@PathVariable UUID establecimientoId, @RequestParam UUID tenantId) {
        return getEstablecimiento.execute(new ObtenerEstablecimientoQuery(tenantId, establecimientoId)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }

    @PutMapping("/{establecimientoId}")
    @PreAuthorize("hasAuthority('organizacion.establecimientos.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable UUID establecimientoId,
            @RequestParam UUID tenantId,
            @Valid @RequestBody ActualizarEstablecimientoRequest request) {
        return updateEstablecimiento.execute(
                OrganizacionApiMapper.toCommand(establecimientoId, tenantId, request)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }
}
