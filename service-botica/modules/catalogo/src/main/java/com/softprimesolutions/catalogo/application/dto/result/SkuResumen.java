package com.softprimesolutions.catalogo.application.dto.result;

import java.util.UUID;

public record SkuResumen(
        UUID id, String codigoInterno, String descripcionComercial, String tipoSku, String estado) {
}
