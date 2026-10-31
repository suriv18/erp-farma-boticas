package com.softprimesolutions.inventario.application.port.out;

import com.softprimesolutions.inventario.application.dto.command.DocumentoOrigen;
import com.softprimesolutions.inventario.domain.model.Lote;
import com.softprimesolutions.inventario.domain.model.PosicionInventario;
import com.softprimesolutions.inventario.domain.model.TipoMovimiento;
import com.softprimesolutions.inventario.domain.valueobject.Actor;
import com.softprimesolutions.inventario.domain.valueobject.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RegistroMovimiento(
        UUID movimientoId,
        TenantId tenantId,
        UUID almacenId,
        UUID skuId,
        Lote lote,
        boolean loteNuevo,
        PosicionInventario posicion,
        boolean posicionNueva,
        TipoMovimiento tipo,
        BigDecimal cantidad,
        BigDecimal stockAnterior,
        BigDecimal stockPosterior,
        String motivo,
        Actor actor,
        Instant fechaNegocio,
        UUID businessUuid,
        String huella,
        DocumentoOrigen origen) {

    public RegistroMovimiento(
            UUID movimientoId, TenantId tenantId, UUID almacenId, UUID skuId, Lote lote, boolean loteNuevo,
            PosicionInventario posicion, boolean posicionNueva, TipoMovimiento tipo, BigDecimal cantidad,
            BigDecimal stockAnterior, BigDecimal stockPosterior, String motivo, Actor actor, Instant fechaNegocio,
            UUID businessUuid, String huella) {
        this(movimientoId, tenantId, almacenId, skuId, lote, loteNuevo, posicion, posicionNueva, tipo, cantidad,
                stockAnterior, stockPosterior, motivo, actor, fechaNegocio, businessUuid, huella, null);
    }
}
