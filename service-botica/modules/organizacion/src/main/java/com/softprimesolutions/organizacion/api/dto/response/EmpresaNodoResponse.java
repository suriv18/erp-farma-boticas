package com.softprimesolutions.organizacion.api.dto.response;

import java.util.List;
import java.util.UUID;

public record EmpresaNodoResponse(
        UUID id,
        String legalName,
        String tradeName,
        String status,
        List<EstablecimientoNodoResponse> establishments) {

    public EmpresaNodoResponse {
        establishments = List.copyOf(establishments);
    }
}
