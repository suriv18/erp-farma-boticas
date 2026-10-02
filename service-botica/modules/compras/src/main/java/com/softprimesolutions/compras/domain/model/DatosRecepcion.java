package com.softprimesolutions.compras.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record DatosRecepcion(
        String numero,
        UUID almacenId,
        String documentoProveedorTipo,
        String documentoProveedorSerie,
        String documentoProveedorNumero,
        String guiaRemisionRemitente,
        String guiaRemisionTransportista,
        BigDecimal temperaturaRecepcionC,
        BigDecimal humedadRelativaPct,
        String observacion) {
}
