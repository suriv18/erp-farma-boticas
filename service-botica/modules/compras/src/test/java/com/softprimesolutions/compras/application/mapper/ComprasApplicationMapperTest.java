package com.softprimesolutions.compras.application.mapper;

import static com.softprimesolutions.compras.ComprasFixtures.ACTOR;
import static com.softprimesolutions.compras.ComprasFixtures.AHORA;
import static com.softprimesolutions.compras.ComprasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.compras.ComprasFixtures.HOY;
import static com.softprimesolutions.compras.ComprasFixtures.ORDEN;
import static com.softprimesolutions.compras.ComprasFixtures.PROVEEDOR;
import static com.softprimesolutions.compras.ComprasFixtures.SKU;
import static com.softprimesolutions.compras.ComprasFixtures.domainError;
import static com.softprimesolutions.compras.ComprasFixtures.domainValue;
import static com.softprimesolutions.compras.ComprasFixtures.ordenEmitida;
import static com.softprimesolutions.compras.ComprasFixtures.proveedor;
import static com.softprimesolutions.compras.ComprasFixtures.proveedorInput;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.compras.application.dto.command.ProveedorInput;
import com.softprimesolutions.compras.domain.exception.ComprasErrorCodes;
import com.softprimesolutions.compras.domain.model.EstadoProveedor;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ComprasApplicationMapperTest {

    @Test
    void buildsTheProveedorDataFromTheInputApplyingTheDefaults() {
        var datos = domainValue(ComprasApplicationMapper.toDatos(proveedorInput()));

        assertThat(datos.numeroDocumento()).isEqualTo("20100070970");
        assertThat(datos.tipoDocumento()).isEqualTo("6");
    }

    @Test
    void reportsTheDomainErrorOfAnInvalidInput() {
        var input = new ProveedorInput(
                "6", "123", "Laboratorios", null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null);

        assertThat(domainError(ComprasApplicationMapper.toDatos(input)).code())
                .isEqualTo(ComprasErrorCodes.PROVEEDOR_INVALIDO);
    }

    @Test
    void mapsAProveedorToItsResult() {
        var result = ComprasApplicationMapper.toResult(proveedor(EstadoProveedor.BLOQUEADO));

        assertThat(result.id()).isEqualTo(PROVEEDOR);
        assertThat(result.numeroDocumento()).isEqualTo("20100070970");
        assertThat(result.razonSocial()).isEqualTo("Laboratorios SAC");
        assertThat(result.condicionPagoDefault()).isEqualTo("CONTADO");
        assertThat(result.diasCreditoDefault()).isZero();
        assertThat(result.monedaDefault()).isEqualTo("PEN");
        assertThat(result.esDistribuidor()).isTrue();
        assertThat(result.estado()).isEqualTo("BLOQUEADO");
    }

    @Test
    void mapsAnOrderToItsResultWithTheReceivedAndOutstandingQuantities() {
        var orden = ordenEmitida().conRecepcion(Map.of(1, new BigDecimal("4")), ACTOR, AHORA);

        var result = ComprasApplicationMapper.toResult(orden);

        assertThat(result.id()).isEqualTo(ORDEN);
        assertThat(result.numero()).isEqualTo("OC-2026-000001");
        assertThat(result.proveedorId()).isEqualTo(PROVEEDOR);
        assertThat(result.establecimientoDestinoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(result.fechaEmision()).isEqualTo(HOY);
        assertThat(result.moneda()).isEqualTo("PEN");
        assertThat(result.condicionPago()).isEqualTo("CONTADO");
        assertThat(result.estado()).isEqualTo("PARCIALMENTE_RECIBIDA");
        assertThat(result.observacion()).isEqualTo("Reposicion");
        assertThat(result.lineas()).singleElement().satisfies(linea -> {
            assertThat(linea.skuId()).isEqualTo(SKU);
            assertThat(linea.descripcion()).isEqualTo("Paracetamol 500 mg");
            assertThat(linea.cantidadRecibida()).isEqualByComparingTo("4");
            assertThat(linea.cantidadPendiente()).isEqualByComparingTo("6");
        });
    }
}
