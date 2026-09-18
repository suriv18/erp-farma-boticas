package com.softprimesolutions.catalogo.api.dto.response;

import java.util.UUID;

public record SkuResumenResponse(UUID id, String codigoInterno, String descripcionComercial, String tipoSku, String estado) {
}
