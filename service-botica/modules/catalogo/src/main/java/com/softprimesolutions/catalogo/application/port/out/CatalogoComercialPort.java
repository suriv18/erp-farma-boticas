package com.softprimesolutions.catalogo.application.port.out;

import com.softprimesolutions.catalogo.domain.model.CategoriaProducto;
import com.softprimesolutions.catalogo.domain.model.Marca;
import com.softprimesolutions.catalogo.domain.model.SKUComercial;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CatalogoComercialPort {

    SaveMarcaOutcome save(Marca marca);

    SaveCategoriaOutcome save(CategoriaProducto categoria);

    SaveSkuOutcome save(SKUComercial sku);

    Optional<SKUComercial> findSkuById(UUID tenantId, UUID skuId);

    boolean categoriaExists(UUID tenantId, UUID categoriaId);

    boolean marcaExists(UUID tenantId, UUID marcaId);

    boolean changeMarcaStatus(UUID tenantId, UUID marcaId, String status, Instant changedAt);

    boolean changeCategoriaStatus(UUID tenantId, UUID categoriaId, String status, Instant changedAt);

    boolean changeSkuStatus(UUID tenantId, UUID skuId, String status, Instant changedAt);

    enum SaveMarcaOutcome { CREATED, UPDATED, DUPLICATE_CODIGO, TENANT_NOT_FOUND, NOT_FOUND }

    enum SaveCategoriaOutcome { CREATED, UPDATED, DUPLICATE_CODIGO, TENANT_NOT_FOUND, CATEGORIA_PADRE_NOT_FOUND, NOT_FOUND }

    enum SaveSkuOutcome {
        CREATED, UPDATED, DUPLICATE_CODIGO_INTERNO, DUPLICATE_CODIGO_BARRA, TENANT_NOT_FOUND,
        PRODUCTO_REGULADO_NOT_FOUND, CATEGORIA_NOT_FOUND, MARCA_NOT_FOUND, NOT_FOUND
    }
}
