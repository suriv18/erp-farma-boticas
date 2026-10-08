package com.softprimesolutions.inventario.application.mapper;

import static com.softprimesolutions.inventario.InventarioFixtures.ACTOR;
import static com.softprimesolutions.inventario.InventarioFixtures.ALMACEN;
import static com.softprimesolutions.inventario.InventarioFixtures.AHORA;
import static com.softprimesolutions.inventario.InventarioFixtures.HOY;
import static com.softprimesolutions.inventario.InventarioFixtures.LOTE;
import static com.softprimesolutions.inventario.InventarioFixtures.POSICION;
import static com.softprimesolutions.inventario.InventarioFixtures.SKU;
import static com.softprimesolutions.inventario.InventarioFixtures.TENANT;
import static com.softprimesolutions.inventario.InventarioFixtures.VENCIMIENTO;
import static com.softprimesolutions.inventario.InventarioFixtures.loteHabilitado;
import static com.softprimesolutions.inventario.InventarioFixtures.posicion;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.inventario.application.port.out.RegistroMovimiento;
import com.softprimesolutions.inventario.domain.model.TipoMovimiento;
import com.softprimesolutions.inventario.domain.valueobject.TenantId;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventarioApplicationMapperTest {

    @Test
    void mapsALoteWithItsSellability() {
        var result = InventarioApplicationMapper.toResult(loteHabilitado(), HOY);

        assertThat(result.id()).isEqualTo(LOTE);
        assertThat(result.skuId()).isEqualTo(SKU);
        assertThat(result.numeroLote()).isEqualTo("L-001");
        assertThat(result.fechaVencimiento()).isEqualTo(VENCIMIENTO);
        assertThat(result.estado()).isEqualTo("HABILITADO");
        assertThat(result.motivoEstado()).isNull();
        assertThat(result.bloqueadoAt()).isNull();
        assertThat(result.vendible()).isTrue();
    }

    @Test
    void mapsAMovementRegistrationIntoItsResult() {
        var movimientoId = UUID.randomUUID();
        var businessUuid = UUID.randomUUID();
        var registro = new RegistroMovimiento(
                movimientoId, new TenantId(TENANT), ALMACEN, SKU, loteHabilitado(), false, posicion("15", "0", 2),
                false, TipoMovimiento.AJUSTE_INGRESO, new BigDecimal("5"), new BigDecimal("10"),
                new BigDecimal("15"), "inventario inicial", ACTOR, AHORA, businessUuid, "huella-1");

        var result = InventarioApplicationMapper.toResult(registro);

        assertThat(result.id()).isEqualTo(movimientoId);
        assertThat(result.posicionId()).isEqualTo(POSICION);
        assertThat(result.loteId()).isEqualTo(LOTE);
        assertThat(result.tipo()).isEqualTo("AJUSTE_INGRESO");
        assertThat(result.naturaleza()).isEqualTo("E");
        assertThat(result.cantidad()).isEqualByComparingTo("5");
        assertThat(result.stockAnterior()).isEqualByComparingTo("10");
        assertThat(result.stockPosterior()).isEqualByComparingTo("15");
        assertThat(result.fechaNegocio()).isEqualTo(AHORA);
        assertThat(registro.tenantId().value()).isEqualTo(TENANT);
        assertThat(registro.almacenId()).isEqualTo(ALMACEN);
        assertThat(registro.skuId()).isEqualTo(SKU);
        assertThat(registro.loteNuevo()).isFalse();
        assertThat(registro.posicionNueva()).isFalse();
        assertThat(registro.motivo()).isEqualTo("inventario inicial");
        assertThat(registro.actor()).isEqualTo(ACTOR);
        assertThat(registro.businessUuid()).isEqualTo(businessUuid);
        assertThat(registro.huella()).isEqualTo("huella-1");
    }
}
