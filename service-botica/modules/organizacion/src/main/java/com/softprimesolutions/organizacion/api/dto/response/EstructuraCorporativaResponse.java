package com.softprimesolutions.organizacion.api.dto.response;

import java.time.Instant;
import java.util.List;

public record EstructuraCorporativaResponse(Instant asOf, List<EmpresaNodoResponse> companies) {

    public EstructuraCorporativaResponse {
        companies = List.copyOf(companies);
    }
}
