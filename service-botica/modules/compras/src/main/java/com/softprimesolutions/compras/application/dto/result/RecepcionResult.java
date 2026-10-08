package com.softprimesolutions.compras.application.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecepcionResult(
        UUID id,
        String numero,
        UUID ordenCompraId,
        UUID proveedorId,
        UUID establecimientoId,
        UUID almacenId,
        String documentoProveedorTipo,
        String documentoProveedorSerie,
        String documentoProveedorNumero,
        String guiaRemisionRemitente,
        String guiaRemisionTransportista,
        Instant fechaRecepcion,
        BigDecimal temperaturaRecepcionC,
        BigDecimal humedadRelativaPct,
        String estado,
        String observacion,
        List<LineaRecepcionResult> lineas) {

    public RecepcionResult {
        lineas = List.copyOf(lineas);
    }

    public RecepcionResult conLineas(List<LineaRecepcionResult> nuevas) {
        return new RecepcionResult(
                id, numero, ordenCompraId, proveedorId, establecimientoId, almacenId, documentoProveedorTipo,
                documentoProveedorSerie, documentoProveedorNumero, guiaRemisionRemitente,
                guiaRemisionTransportista, fechaRecepcion, temperaturaRecepcionC, humedadRelativaPct, estado,
                observacion, nuevas);
    }
}
