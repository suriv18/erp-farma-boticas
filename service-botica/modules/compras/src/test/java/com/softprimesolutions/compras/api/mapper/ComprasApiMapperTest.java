package com.softprimesolutions.compras.api.mapper;

import static com.softprimesolutions.compras.ComprasFixtures.ACTOR_ID;
import static com.softprimesolutions.compras.ComprasFixtures.ALMACEN;
import static com.softprimesolutions.compras.ComprasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.compras.ComprasFixtures.ORDEN;
import static com.softprimesolutions.compras.ComprasFixtures.PROVEEDOR;
import static com.softprimesolutions.compras.ComprasFixtures.RECEPCION;
import static com.softprimesolutions.compras.ComprasFixtures.SKU;
import static com.softprimesolutions.compras.ComprasFixtures.TENANT;
import static com.softprimesolutions.compras.ComprasFixtures.VENCIMIENTO;
import static com.softprimesolutions.compras.ComprasFixtures.dec;
import static com.softprimesolutions.compras.ComprasFixtures.ordenNueva;
import static com.softprimesolutions.compras.ComprasFixtures.proveedor;
import static com.softprimesolutions.compras.ComprasResultFixtures.ordenResumen;
import static com.softprimesolutions.compras.ComprasResultFixtures.recepcionResult;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.compras.api.dto.request.ItemRecepcionRequest;
import com.softprimesolutions.compras.api.dto.request.LineaOrdenCompraRequest;
import com.softprimesolutions.compras.api.dto.request.OrdenCompraRequest;
import com.softprimesolutions.compras.api.dto.request.ProveedorRequest;
import com.softprimesolutions.compras.api.dto.request.RecepcionRequest;
import com.softprimesolutions.compras.application.dto.result.PaginaResult;
import com.softprimesolutions.compras.application.mapper.ComprasApplicationMapper;
import com.softprimesolutions.compras.domain.model.EstadoProveedor;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ComprasApiMapperTest {

    @Test
    void mapsAProveedorRequestToItsInput() {
        var request = new ProveedorRequest(
                "6", "20100070970", "Laboratorios SAC", "Labs", "Av. Lima 123", "150101", "999888777",
                "ventas@labs.example", "Ana", "999000111", "ana@labs.example", "CREDITO 30", 30, "USD", true, false,
                true, "EXCELENTE");

        var input = ComprasApiMapper.toInput(request);

        assertThat(input.tipoDocumento()).isEqualTo("6");
        assertThat(input.numeroDocumento()).isEqualTo("20100070970");
        assertThat(input.razonSocial()).isEqualTo("Laboratorios SAC");
        assertThat(input.nombreComercial()).isEqualTo("Labs");
        assertThat(input.direccion()).isEqualTo("Av. Lima 123");
        assertThat(input.ubigeo()).isEqualTo("150101");
        assertThat(input.telefono()).isEqualTo("999888777");
        assertThat(input.email()).isEqualTo("ventas@labs.example");
        assertThat(input.contactoNombre()).isEqualTo("Ana");
        assertThat(input.contactoTelefono()).isEqualTo("999000111");
        assertThat(input.contactoEmail()).isEqualTo("ana@labs.example");
        assertThat(input.condicionPagoDefault()).isEqualTo("CREDITO 30");
        assertThat(input.diasCreditoDefault()).isEqualTo(30);
        assertThat(input.monedaDefault()).isEqualTo("USD");
        assertThat(input.esLaboratorio()).isTrue();
        assertThat(input.esImportador()).isFalse();
        assertThat(input.esDistribuidor()).isTrue();
        assertThat(input.calificacion()).isEqualTo("EXCELENTE");
    }

    @Test
    void mapsAnOrderRequestToItsCommand() {
        var request = new OrdenCompraRequest(
                PROVEEDOR, ESTABLECIMIENTO, LocalDate.of(2026, 7, 1), "PEN", dec("1"), "CONTADO", 0, "Urgente",
                List.of(new LineaOrdenCompraRequest(
                        SKU, dec("10"), "UND", dec("5"), dec("1"), dec("2"), dec("10"), dec("5"))));

        var command = ComprasApiMapper.toCommand(TENANT, ACTOR_ID, request);

        assertThat(command.tenantId()).isEqualTo(TENANT);
        assertThat(command.actorId()).isEqualTo(ACTOR_ID);
        assertThat(command.proveedorId()).isEqualTo(PROVEEDOR);
        assertThat(command.establecimientoDestinoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(command.fechaEntregaEstimada()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(command.moneda()).isEqualTo("PEN");
        assertThat(command.tipoCambio()).isEqualByComparingTo("1");
        assertThat(command.condicionPago()).isEqualTo("CONTADO");
        assertThat(command.diasCredito()).isZero();
        assertThat(command.observacion()).isEqualTo("Urgente");
        assertThat(command.lineas()).singleElement().satisfies(linea -> {
            assertThat(linea.skuId()).isEqualTo(SKU);
            assertThat(linea.cantidad()).isEqualByComparingTo("10");
            assertThat(linea.unidadMedidaCodigo()).isEqualTo("UND");
            assertThat(linea.precioUnitario()).isEqualByComparingTo("5");
            assertThat(linea.descuento()).isEqualByComparingTo("1");
            assertThat(linea.impuesto()).isEqualByComparingTo("2");
            assertThat(linea.toleranciaExcesoPct()).isEqualByComparingTo("10");
            assertThat(linea.toleranciaDefectoPct()).isEqualByComparingTo("5");
        });
    }

    @Test
    void mapsAReceptionRequestToItsCommand() {
        var request = new RecepcionRequest(
                ORDEN, ALMACEN, "01", "F001", "123", "T001-45", "T002-9", dec("22.5"), dec("55"), "Recepcion",
                List.of(new ItemRecepcionRequest(
                        1, "LOTE-1", LocalDate.of(2026, 1, 1), VENCIMIENTO, dec("6"), dec("1"), "Envase danado",
                        dec("5.5"), "Caja abierta")));

        var command = ComprasApiMapper.toCommand(TENANT, ACTOR_ID, "clave-1", request);

        assertThat(command.tenantId()).isEqualTo(TENANT);
        assertThat(command.actorId()).isEqualTo(ACTOR_ID);
        assertThat(command.idempotencyKey()).isEqualTo("clave-1");
        assertThat(command.ordenCompraId()).isEqualTo(ORDEN);
        assertThat(command.almacenId()).isEqualTo(ALMACEN);
        assertThat(command.documentoProveedorTipo()).isEqualTo("01");
        assertThat(command.documentoProveedorSerie()).isEqualTo("F001");
        assertThat(command.documentoProveedorNumero()).isEqualTo("123");
        assertThat(command.guiaRemisionRemitente()).isEqualTo("T001-45");
        assertThat(command.guiaRemisionTransportista()).isEqualTo("T002-9");
        assertThat(command.temperaturaRecepcionC()).isEqualByComparingTo("22.5");
        assertThat(command.humedadRelativaPct()).isEqualByComparingTo("55");
        assertThat(command.observacion()).isEqualTo("Recepcion");
        assertThat(command.items()).singleElement().satisfies(item -> {
            assertThat(item.numeroLineaOrden()).isEqualTo(1);
            assertThat(item.numeroLote()).isEqualTo("LOTE-1");
            assertThat(item.fechaFabricacion()).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(item.fechaVencimiento()).isEqualTo(VENCIMIENTO);
            assertThat(item.cantidadRecibida()).isEqualByComparingTo("6");
            assertThat(item.cantidadRechazada()).isEqualByComparingTo("1");
            assertThat(item.motivoRechazo()).isEqualTo("Envase danado");
            assertThat(item.costoUnitario()).isEqualByComparingTo("5.5");
            assertThat(item.observacion()).isEqualTo("Caja abierta");
        });
    }

    @Test
    void mapsAProveedorResultToItsResponse() {
        var response = ComprasApiMapper.toResponse(
                ComprasApplicationMapper.toResult(proveedor(EstadoProveedor.ACTIVO)));

        assertThat(response.id()).isEqualTo(PROVEEDOR);
        assertThat(response.numeroDocumento()).isEqualTo("20100070970");
        assertThat(response.razonSocial()).isEqualTo("Laboratorios SAC");
        assertThat(response.monedaDefault()).isEqualTo("PEN");
        assertThat(response.esDistribuidor()).isTrue();
        assertThat(response.estado()).isEqualTo("ACTIVO");
        assertThat(response.calificacion()).isEqualTo("CONFIABLE");
        assertThat(response.diasCreditoDefault()).isZero();
        assertThat(response.condicionPagoDefault()).isEqualTo("CONTADO");
        assertThat(response.nombreComercial()).isNull();
    }

    @Test
    void mapsAnOrderResultToItsResponseWithItsLines() {
        var response = ComprasApiMapper.toResponse(ComprasApplicationMapper.toResult(ordenNueva()));

        assertThat(response.id()).isEqualTo(ORDEN);
        assertThat(response.numero()).isEqualTo("OC-2026-000001");
        assertThat(response.estado()).isEqualTo("BORRADOR");
        assertThat(response.moneda()).isEqualTo("PEN");
        assertThat(response.total()).isEqualByComparingTo("64.90");
        assertThat(response.observacion()).isEqualTo("Reposicion");
        assertThat(response.lineas()).singleElement().satisfies(linea -> {
            assertThat(linea.numeroLinea()).isEqualTo(1);
            assertThat(linea.skuId()).isEqualTo(SKU);
            assertThat(linea.cantidadPendiente()).isEqualByComparingTo("10");
            assertThat(linea.totalLinea()).isEqualByComparingTo("64.90");
        });
    }

    @Test
    void mapsAReceptionResultToItsResponseWithItsLines() {
        var response = ComprasApiMapper.toResponse(recepcionResult());

        assertThat(response.id()).isEqualTo(RECEPCION);
        assertThat(response.numero()).isEqualTo("REC-2026-000001");
        assertThat(response.almacenId()).isEqualTo(ALMACEN);
        assertThat(response.estado()).isEqualTo("CONFIRMADA");
        assertThat(response.lineas()).singleElement().satisfies(linea -> {
            assertThat(linea.numeroLote()).isEqualTo("LOTE-1");
            assertThat(linea.cantidadAceptada()).isEqualByComparingTo("5");
            assertThat(linea.decisionCalidad()).isEqualTo("ACEPTADO_PARCIAL");
            assertThat(linea.loteId()).isNotNull();
        });
    }

    @Test
    void mapsThePagesOfProveedoresAndOrders() {
        var proveedores = ComprasApiMapper.toProveedorPage(new PaginaResult<>(
                List.of(ComprasApplicationMapper.toResult(proveedor(EstadoProveedor.ACTIVO))), 1, 20, 21));
        var ordenes = ComprasApiMapper.toOrdenPage(new PaginaResult<>(List.of(ordenResumen()), 0, 10, 1));

        assertThat(proveedores.items()).singleElement().satisfies(item -> assertThat(item.id()).isEqualTo(PROVEEDOR));
        assertThat(proveedores.page()).isEqualTo(1);
        assertThat(proveedores.size()).isEqualTo(20);
        assertThat(proveedores.totalElements()).isEqualTo(21);
        assertThat(ordenes.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(ORDEN);
            assertThat(item.proveedorRazonSocial()).isEqualTo("Laboratorios SAC");
            assertThat(item.estado()).isEqualTo("EMITIDA");
        });
        assertThat(ordenes.totalElements()).isEqualTo(1);
    }

    @Test
    void mapsThePageOfReceptions() {
        var pagina = ComprasApiMapper.toRecepcionPage(new PaginaResult<>(List.of(recepcionResult()), 1, 5, 6));

        assertThat(pagina.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(RECEPCION);
            assertThat(item.lineas()).hasSize(1);
        });
        assertThat(pagina.page()).isEqualTo(1);
        assertThat(pagina.size()).isEqualTo(5);
        assertThat(pagina.totalElements()).isEqualTo(6);
    }
}
