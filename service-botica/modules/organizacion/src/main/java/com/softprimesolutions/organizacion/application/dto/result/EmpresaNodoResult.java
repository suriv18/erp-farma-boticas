package com.softprimesolutions.organizacion.application.dto.result;

import java.util.List;
import java.util.UUID;

public record EmpresaNodoResult(
        UUID id,
        String legalName,
        String tradeName,
        String status,
        List<EstablecimientoNodoResult> establishments) {

    public EmpresaNodoResult {
        establishments = List.copyOf(establishments);
    }
}
