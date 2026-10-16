package com.softprimesolutions.inventario.api.controller;

import com.softprimesolutions.inventario.api.mapper.InventarioApiMapper;
import com.softprimesolutions.inventario.application.dto.query.ListarPosicionesQuery;
import com.softprimesolutions.inventario.application.port.in.ListarPosicionesUseCase;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(PosicionInventarioController.BASE_PATH)
public class PosicionInventarioController {

    static final String BASE_PATH = "/api/v1/inventario/posiciones";

    private final ListarPosicionesUseCase listPosiciones;

    public PosicionInventarioController(ListarPosicionesUseCase listPosiciones) {
        this.listPosiciones = listPosiciones;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventario.posiciones.consultar')")
    public ResponseEntity<?> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID establecimientoId,
            @RequestParam(required = false) UUID almacenId,
            @RequestParam(required = false) UUID skuId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var query = new ListarPosicionesQuery(
                InventarioControllerSupport.tenantOf(jwt), establecimientoId, almacenId, skuId, page, size);
        return listPosiciones.execute(query).fold(
                result -> ResponseEntity.ok(InventarioApiMapper.toPosicionPage(result)),
                InventarioControllerSupport::problem);
    }
}
