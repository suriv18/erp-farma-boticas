package com.softprimesolutions.compras.infrastructure.persistence.write.adapter;

import static com.softprimesolutions.compras.ComprasFixtures.ALMACEN;
import static com.softprimesolutions.compras.ComprasFixtures.EMPRESA;
import static com.softprimesolutions.compras.ComprasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.compras.ComprasFixtures.SKU;
import static com.softprimesolutions.compras.ComprasFixtures.TENANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.compras.infrastructure.persistence.JdbcClientStub;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class ComprasReferenciasJdbcAdapterTest {

    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final ComprasReferenciasJdbcAdapter adapter = new ComprasReferenciasJdbcAdapter(jdbc.client());

    @Test
    void findsAnEstablishmentWithItsCompanyAndOperability() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", ESTABLECIMIENTO);
        row.put("empresa_uuid", EMPRESA);
        row.put("operable", Boolean.TRUE);
        jdbc.rows("es.estado_operativo = 'ACTIVO'", row);

        var referencia = adapter.establecimiento(TENANT, ESTABLECIMIENTO);

        assertThat(referencia).hasValueSatisfying(found -> {
            assertThat(found.id()).isEqualTo(ESTABLECIMIENTO);
            assertThat(found.empresaId()).isEqualTo(EMPRESA);
            assertThat(found.operable()).isTrue();
        });
        assertThat(jdbc.statementContaining("es.estado_operativo = 'ACTIVO'").params())
                .containsEntry("tenantId", TENANT).containsEntry("referenciaId", ESTABLECIMIENTO);
    }

    @Test
    void findsNoEstablishmentWhenItDoesNotExist() {
        assertThat(adapter.establecimiento(TENANT, ESTABLECIMIENTO)).isEmpty();
    }

    @Test
    void findsAWarehouseWithItsEstablishmentAndOperability() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", ALMACEN);
        row.put("establecimiento_uuid", ESTABLECIMIENTO);
        row.put("operable", Boolean.FALSE);
        jdbc.rows("a.permite_lotes", row);

        var referencia = adapter.almacen(TENANT, ALMACEN);

        assertThat(referencia).hasValueSatisfying(found -> {
            assertThat(found.id()).isEqualTo(ALMACEN);
            assertThat(found.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
            assertThat(found.operable()).isFalse();
        });
    }

    @Test
    void findsNoWarehouseWhenItDoesNotExist() {
        assertThat(adapter.almacen(TENANT, ALMACEN)).isEmpty();
    }

    @Test
    void findsTheSkusByTheirPublicIds() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", SKU);
        row.put("descripcion_comercial", "Paracetamol 500 mg");
        row.put("operable", Boolean.TRUE);
        jdbc.rows("k.uuid_publico IN (:skuIds)", row);

        var skus = adapter.skus(TENANT, List.of(SKU));

        assertThat(skus).containsOnlyKeys(SKU);
        assertThat(skus.get(SKU).descripcion()).isEqualTo("Paracetamol 500 mg");
        assertThat(skus.get(SKU).operable()).isTrue();
        assertThat(jdbc.statementContaining("k.uuid_publico IN (:skuIds)").params())
                .containsEntry("tenantId", TENANT).containsEntry("skuIds", List.of(SKU));
    }

    @Test
    void doesNotQueryWhenThereAreNoSkusToLookFor() {
        assertThat(adapter.skus(TENANT, List.of())).isEmpty();
        assertThat(jdbc.statements()).isEmpty();
    }

    @Test
    void checksWhetherAMeasurementUnitExists() {
        jdbc.scalar("unidad_medida WHERE codigo", true);
        assertThat(adapter.existeUnidadMedida("UND")).isTrue();
        assertThat(jdbc.statementContaining("unidad_medida WHERE codigo").params()).containsEntry("codigo", "UND");

        var otro = new JdbcClientStub().scalar("unidad_medida WHERE codigo", false);
        assertThat(new ComprasReferenciasJdbcAdapter(otro.client()).existeUnidadMedida("XXX")).isFalse();
    }
}
