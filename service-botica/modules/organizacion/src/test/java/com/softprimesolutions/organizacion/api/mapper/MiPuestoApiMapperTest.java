package com.softprimesolutions.organizacion.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.organizacion.application.dto.result.AlmacenPuestoResult;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoPuestoResult;
import com.softprimesolutions.organizacion.application.dto.result.MiPuestoResult;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPuestoResult;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MiPuestoApiMapperTest {

    @Test
    void mapsTheWorkstationWithoutExposingTheCompany() {
        var establecimiento = UUID.randomUUID();
        var almacen = UUID.randomUUID();
        var terminal = UUID.randomUUID();
        var result = new MiPuestoResult(List.of(new EstablecimientoPuestoResult(
                establecimiento, UUID.randomUUID(), "E1", "Botica Central",
                List.of(new AlmacenPuestoResult(almacen, "ALM", "Almacen", "GENERAL", true)),
                List.of(new TerminalPuestoResult(terminal, "T1", "Caja 1", "MANTENIMIENTO", false)))));

        var response = MiPuestoApiMapper.toResponse(result);

        assertThat(response.establecimientos()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(establecimiento);
            assertThat(item.codigo()).isEqualTo("E1");
            assertThat(item.nombre()).isEqualTo("Botica Central");
            assertThat(item.almacenes()).singleElement().satisfies(a -> {
                assertThat(a.id()).isEqualTo(almacen);
                assertThat(a.tipo()).isEqualTo("GENERAL");
                assertThat(a.permiteVenta()).isTrue();
            });
            assertThat(item.terminales()).singleElement().satisfies(t -> {
                assertThat(t.id()).isEqualTo(terminal);
                assertThat(t.estado()).isEqualTo("MANTENIMIENTO");
                assertThat(t.operable()).isFalse();
            });
        });
    }
}
