package com.softprimesolutions.organizacion.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrganizacionApplicationMapperTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-02-01T00:00:00Z");

    @Test
    void restoresAnEmpresaThatMapsBackToTheSameResult() {
        var result = new EmpresaOperadoraResult(
                UUID.randomUUID(), UUID.randomUUID(), "20123456786", "Boticas SAC", "Boticas", "Av. 1",
                "150101", "01444", "a@b.pe", "https://b.pe", "PEN", "America/Lima", true, "SUSPENDIDO",
                CREATED_AT, UPDATED_AT);

        var restored = OrganizacionApplicationMapper.toDomain(result);

        assertThat(OrganizacionApplicationMapper.toResult(restored)).isEqualTo(result);
    }

    @Test
    void restoresAnEstablecimientoThatMapsBackToTheSameResult() {
        var result = new EstablecimientoResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "EST001", "Botica Central", "BOTICA",
                "CAT", "0001", "DIG001", "Av. 2", "150101", "Frente al parque", BigDecimal.ONE, BigDecimal.TEN,
                "01444", "e@b.pe", true, true, true, "STORE_EDGE", "America/Lima", "CLAUSURADO", CREATED_AT,
                UPDATED_AT);

        var restored = OrganizacionApplicationMapper.toDomain(result);

        assertThat(OrganizacionApplicationMapper.toResult(restored)).isEqualTo(result);
    }
}
