package com.softprimesolutions.organizacion.api.controller;

import com.softprimesolutions.organizacion.api.dto.request.ActualizarTerminalPosRequest;
import com.softprimesolutions.organizacion.api.dto.request.CrearTerminalPosRequest;
import com.softprimesolutions.organizacion.api.mapper.OrganizacionApiMapper;
import com.softprimesolutions.organizacion.application.dto.query.ListarTerminalesQuery;
import com.softprimesolutions.organizacion.application.dto.query.ObtenerTerminalQuery;
import com.softprimesolutions.organizacion.application.port.in.ActualizarTerminalPosUseCase;
import com.softprimesolutions.organizacion.application.port.in.CrearTerminalPosUseCase;
import com.softprimesolutions.organizacion.application.port.in.ListarTerminalesUseCase;
import com.softprimesolutions.organizacion.application.port.in.ObtenerTerminalUseCase;
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
@RequestMapping(TerminalPosController.BASE_PATH)
public class TerminalPosController {

    static final String BASE_PATH = "/api/v1/organizacion/terminales-pos";

    private final CrearTerminalPosUseCase createTerminal;
    private final ActualizarTerminalPosUseCase updateTerminal;
    private final ListarTerminalesUseCase listTerminales;
    private final ObtenerTerminalUseCase getTerminal;

    public TerminalPosController(
            CrearTerminalPosUseCase createTerminal,
            ActualizarTerminalPosUseCase updateTerminal,
            ListarTerminalesUseCase listTerminales,
            ObtenerTerminalUseCase getTerminal) {
        this.createTerminal = createTerminal;
        this.updateTerminal = updateTerminal;
        this.listTerminales = listTerminales;
        this.getTerminal = getTerminal;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('organizacion.terminales-pos.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody CrearTerminalPosRequest request) {
        return createTerminal.execute(OrganizacionApiMapper.toCommand(request)).fold(
                result -> OrganizacionControllerSupport.created(
                        BASE_PATH, result.id(), OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('organizacion.terminales-pos.consultar')")
    public ResponseEntity<?> list(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) UUID establecimientoId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return listTerminales.execute(new ListarTerminalesQuery(tenantId, establecimientoId, search, page, size)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toTerminalPage(result)),
                OrganizacionControllerSupport::problem);
    }

    @GetMapping("/{terminalId}")
    @PreAuthorize("hasAuthority('organizacion.terminales-pos.consultar')")
    public ResponseEntity<?> getById(@PathVariable UUID terminalId, @RequestParam UUID tenantId) {
        return getTerminal.execute(new ObtenerTerminalQuery(tenantId, terminalId)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }

    @PutMapping("/{terminalId}")
    @PreAuthorize("hasAuthority('organizacion.terminales-pos.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable UUID terminalId,
            @RequestParam UUID tenantId,
            @Valid @RequestBody ActualizarTerminalPosRequest request) {
        return updateTerminal.execute(OrganizacionApiMapper.toCommand(terminalId, tenantId, request)).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }
}
