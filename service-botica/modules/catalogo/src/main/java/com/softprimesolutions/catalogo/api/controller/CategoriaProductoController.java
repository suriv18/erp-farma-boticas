package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.catalogo.api.dto.request.CambiarEstadoTenantRequest;
import com.softprimesolutions.catalogo.api.dto.request.CategoriaProductoRequest;
import com.softprimesolutions.catalogo.api.mapper.CatalogoApiMapper;
import com.softprimesolutions.catalogo.application.dto.query.ListarCategoriasProductoQuery;
import com.softprimesolutions.catalogo.application.port.in.ActualizarCategoriaProductoUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearCategoriaProductoUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarCategoriasProductoUseCase;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/catalogo/categorias")
public class CategoriaProductoController {

    private final CrearCategoriaProductoUseCase createCategoria;
    private final ActualizarCategoriaProductoUseCase updateCategoria;
    private final ListarCategoriasProductoUseCase listCategorias;
    private final CatalogoControlUseCase control;

    public CategoriaProductoController(
            CrearCategoriaProductoUseCase createCategoria,
            ActualizarCategoriaProductoUseCase updateCategoria,
            ListarCategoriasProductoUseCase listCategorias,
            CatalogoControlUseCase control) {
        this.createCategoria = createCategoria;
        this.updateCategoria = updateCategoria;
        this.listCategorias = listCategorias;
        this.control = control;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('catalogo.categorias.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody CategoriaProductoRequest request) {
        return createCategoria.execute(CatalogoApiMapper.toCreateCommand(request)).fold(
                result -> ResponseEntity.status(201).body(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PutMapping("/{categoriaId}")
    @PreAuthorize("hasAuthority('catalogo.categorias.gestionar')")
    public ResponseEntity<?> update(
            @PathVariable UUID categoriaId, @Valid @RequestBody CategoriaProductoRequest request) {
        return updateCategoria.execute(CatalogoApiMapper.toUpdateCommand(categoriaId, request)).fold(
                result -> ResponseEntity.ok(CatalogoApiMapper.toResponse(result)),
                CatalogoControllerSupport::problem);
    }

    @PatchMapping("/{categoriaId}/estado")
    @PreAuthorize("hasAuthority('catalogo.categorias.gestionar')")
    public ResponseEntity<?> changeStatus(
            @PathVariable UUID categoriaId, @Valid @RequestBody CambiarEstadoTenantRequest request) {
        return control.changeCategoriaProductoStatus(request.tenantId(), categoriaId, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), CatalogoControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalogo.categorias.consultar')")
    public ResponseEntity<?> list(
            @RequestParam UUID tenantId, @RequestParam(required = false) UUID categoriaPadreId,
            @RequestParam(required = false) String estado) {
        return listCategorias.execute(new ListarCategoriasProductoQuery(tenantId, categoriaPadreId, estado)).fold(
                result -> ResponseEntity.ok(result.stream().map(CatalogoApiMapper::toResponse).toList()),
                CatalogoControllerSupport::problem);
    }
}
