package com.softprimesolutions.inventario.infrastructure.persistence.write.adapter;

import static com.softprimesolutions.inventario.InventarioFixtures.ALMACEN;
import static com.softprimesolutions.inventario.InventarioFixtures.SKU;
import static com.softprimesolutions.inventario.InventarioFixtures.TENANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.inventario.application.port.out.ReferenciasInventarioPort.EstadoReferencia;
import com.softprimesolutions.inventario.infrastructure.persistence.JdbcClientStub;
import org.junit.jupiter.api.Test;

class InventarioReferenciasJdbcAdapterTest {

    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final InventarioReferenciasJdbcAdapter adapter = new InventarioReferenciasJdbcAdapter(jdbc.client());

    @Test
    void reportsAnOperableWarehouseAndSku() {
        jdbc.scalar("sch_organizacion.almacen", "OPERABLE").scalar("sch_catalogo.sku_comercial", "OPERABLE");

        assertThat(adapter.estadoAlmacen(TENANT, ALMACEN)).isEqualTo(EstadoReferencia.OPERABLE);
        assertThat(adapter.estadoSku(TENANT, SKU)).isEqualTo(EstadoReferencia.OPERABLE);
        assertThat(jdbc.statementContaining("sch_organizacion.almacen").params())
                .containsEntry("tenantId", TENANT).containsEntry("referenciaId", ALMACEN);
        assertThat(jdbc.statementContaining("sch_catalogo.sku_comercial").params())
                .containsEntry("referenciaId", SKU);
    }

    @Test
    void reportsANonOperableReference() {
        jdbc.scalar("sch_organizacion.almacen", "NO_OPERABLE");

        assertThat(adapter.estadoAlmacen(TENANT, ALMACEN)).isEqualTo(EstadoReferencia.NO_OPERABLE);
    }

    @Test
    void reportsAMissingReferenceAsNonexistent() {
        assertThat(adapter.estadoAlmacen(TENANT, ALMACEN)).isEqualTo(EstadoReferencia.INEXISTENTE);
        assertThat(adapter.estadoSku(TENANT, SKU)).isEqualTo(EstadoReferencia.INEXISTENTE);
    }
}
