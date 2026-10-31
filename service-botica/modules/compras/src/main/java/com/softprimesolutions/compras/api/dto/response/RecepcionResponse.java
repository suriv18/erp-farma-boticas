package com.softprimesolutions.compras.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecepcionResponse(
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
        List<LineaRecepcionResponse> lineas) {

    public RecepcionResponse {
        lineas = List.copyOf(lineas);
    }
}
