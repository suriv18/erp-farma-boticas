package com.softprimesolutions.organizacion.application.dto.result;

import java.time.Instant;
import java.util.List;

public record EstructuraCorporativaResult(Instant asOf, List<EmpresaNodoResult> companies) {

    public EstructuraCorporativaResult {
        companies = List.copyOf(companies);
    }
}
