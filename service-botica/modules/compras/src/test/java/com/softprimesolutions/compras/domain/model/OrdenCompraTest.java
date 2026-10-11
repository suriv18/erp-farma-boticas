package com.softprimesolutions.compras.domain.model;

import static com.softprimesolutions.compras.ComprasFixtures.ACTOR;
import static com.softprimesolutions.compras.ComprasFixtures.ACTOR_ID;
import static com.softprimesolutions.compras.ComprasFixtures.AHORA;
import static com.softprimesolutions.compras.ComprasFixtures.EMPRESA;
import static com.softprimesolutions.compras.ComprasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.compras.ComprasFixtures.HOY;
import static com.softprimesolutions.compras.ComprasFixtures.ORDEN;
import static com.softprimesolutions.compras.ComprasFixtures.PROVEEDOR;
import static com.softprimesolutions.compras.ComprasFixtures.SKU;
import static com.softprimesolutions.compras.ComprasFixtures.TENANT;
import static com.softprimesolutions.compras.ComprasFixtures.condiciones;
import static com.softprimesolutions.compras.ComprasFixtures.dec;
import static com.softprimesolutions.compras.ComprasFixtures.domainError;
import static com.softprimesolutions.compras.ComprasFixtures.domainValue;
import static com.softprimesolutions.compras.ComprasFixtures.lineaNueva;
import static com.softprimesolutions.compras.ComprasFixtures.lineaOrden;
import static com.softprimesolutions.compras.ComprasFixtures.orden;
import static com.softprimesolutions.compras.ComprasFixtures.ordenEmitida;
import static com.softprimesolutions.compras.ComprasFixtures.ordenNueva;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.compras.domain.exception.ComprasErrorCodes;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OrdenCompraTest {

    private static final Instant DESPUES = AHORA.plusSeconds(60);

    private static Result<OrdenCompra, ErrorDetail> crear(CondicionesOrden condiciones, List<LineaNueva> lineas) {
        return OrdenCompra.crear(
                ORDEN, TENANT, EMPRESA, PROVEEDOR, ESTABLECIMIENTO, "OC-2026-000001", HOY, condiciones, lineas, ACTOR,
                AHORA);
    }

    private static CondicionesOrden condicionesDe(
            LocalDate entrega, String moneda, String tipoCambio, String condicionPago, int dias, String observacion) {
        return new CondicionesOrden(entrega, moneda, dec(tipoCambio), condicionPago, dias, observacion);
    }

    private static void assertInvalidOrder(Result<OrdenCompra, ErrorDetail> result) {
        assertThat(domainError(result).code()).isEqualTo(ComprasErrorCodes.ORDEN_INVALIDA);
    }

    private static void assertInvalidLine(LineaNueva linea) {
        var error = domainError(crear(condiciones(), List.of(linea)));
        assertThat(error.code()).isEqualTo(ComprasErrorCodes.ORDEN_INVALIDA);
        assertThat(error.message()).startsWith("Linea 1: ");
    }

    @Test
    void aNewOrderIsADraftWithNumberedLinesAndTheTotalsOfItsLines() {
        var orden = domainValue(crear(condiciones(), List.of(
                lineaNueva("10", "5.5", "0", "9.9", "10", "5"),
                lineaNueva("2", "1.005", "0.5", "0.1", "0", "0"))));

        assertThat(orden.estado()).isEqualTo(EstadoOrdenCompra.BORRADOR);
        assertThat(orden.id()).isEqualTo(ORDEN);
        assertThat(orden.tenantId()).isEqualTo(TENANT);
        assertThat(orden.empresaId()).isEqualTo(EMPRESA);
        assertThat(orden.proveedorId()).isEqualTo(PROVEEDOR);
        assertThat(orden.establecimientoDestinoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(orden.numero()).isEqualTo("OC-2026-000001");
        assertThat(orden.fechaEmision()).isEqualTo(HOY);
        assertThat(orden.observacion()).isEqualTo("Reposicion");
        assertThat(orden.createdAt()).isEqualTo(AHORA);
        assertThat(orden.createdBy()).isEqualTo(ACTOR_ID.toString());
        assertThat(orden.updatedAt()).isNull();
        assertThat(orden.updatedBy()).isNull();
        assertThat(orden.aprobadoPor()).isNull();
        assertThat(orden.aprobadoAt()).isNull();
        assertThat(orden.lineas()).extracting(LineaOrdenCompra::numeroLinea).containsExactly(1, 2);
        assertThat(orden.lineas().get(0).totalLinea()).isEqualByComparingTo("64.90");
        assertThat(orden.lineas().get(0).cantidadRecibida()).isEqualByComparingTo("0");
        assertThat(orden.lineas().get(1).totalLinea()).isEqualByComparingTo("1.61");
        assertThat(orden.subtotal()).isEqualByComparingTo("57.01");
        assertThat(orden.descuentoTotal()).isEqualByComparingTo("0.50");
        assertThat(orden.impuestoTotal()).isEqualByComparingTo("10.00");
        assertThat(orden.total()).isEqualByComparingTo("66.51");
        assertThat(orden.pullDomainEvents()).isEmpty();
    }

    @Test
    void rejectsInvalidOrderConditions() {
        assertInvalidOrder(crear(condicionesDe(null, "pen", "1", "CONTADO", 0, null), List.of(lineaNueva())));
        assertInvalidOrder(crear(condicionesDe(null, "PEN", "0", "CONTADO", 0, null), List.of(lineaNueva())));
        assertInvalidOrder(crear(condicionesDe(null, "PEN", "1.0000001", "CONTADO", 0, null), List.of(lineaNueva())));
        assertInvalidOrder(crear(condicionesDe(HOY.minusDays(1), "PEN", "1", "CONTADO", 0, null), List.of(lineaNueva())));
        assertInvalidOrder(crear(condicionesDe(null, "PEN", "1", "CONTADO", -1, null), List.of(lineaNueva())));
        assertInvalidOrder(crear(condicionesDe(null, "PEN", "1", "x".repeat(81), 0, null), List.of(lineaNueva())));
        assertInvalidOrder(crear(condicionesDe(null, "PEN", "1", "CONTADO", 0, "x".repeat(1501)), List.of(lineaNueva())));
    }

    @Test
    void acceptsADeliveryDateOnTheIssueDate() {
        var orden = domainValue(crear(condicionesDe(HOY, "USD", "3.75", "CONTADO", 0, null), List.of(lineaNueva())));

        assertThat(orden.condiciones().fechaEntregaEstimada()).isEqualTo(HOY);
        assertThat(orden.condiciones().moneda()).isEqualTo("USD");
    }

    @Test
    void requiresBetweenOneAndTwoHundredLines() {
        assertInvalidOrder(crear(condiciones(), null));
        assertInvalidOrder(crear(condiciones(), List.of()));
        assertInvalidOrder(crear(condiciones(), Collections.nCopies(201, lineaNueva())));
        assertThat(domainValue(crear(condiciones(), Collections.nCopies(200, lineaNueva()))).lineas()).hasSize(200);
    }

    @Test
    void reportsTheFirstInvalidLineWithItsNumber() {
        var error = domainError(crear(condiciones(), List.of(lineaNueva(), lineaNueva("0", "5", "0", "0", "0", "0"))));

        assertThat(error.code()).isEqualTo(ComprasErrorCodes.ORDEN_INVALIDA);
        assertThat(error.message()).startsWith("Linea 2: ");
    }

    @Test
    void stopsAtTheFirstInvalidLineWithoutValidatingTheFollowingOnes() {
        var error = domainError(crear(condiciones(), List.of(
                lineaNueva("0", "5", "0", "0", "0", "0"), lineaNueva(), lineaNueva())));

        assertThat(error.message()).startsWith("Linea 1: ");
    }

    @Test
    void rejectsInvalidQuantities() {
        assertInvalidLine(lineaNueva("0", "5", "0", "0", "0", "0"));
        assertInvalidLine(lineaNueva("-1", "5", "0", "0", "0", "0"));
        assertInvalidLine(lineaNueva("1.00001", "5", "0", "0", "0", "0"));
        assertInvalidLine(lineaNueva("1000000001", "5", "0", "0", "0", "0"));
    }

    @Test
    void rejectsInvalidPrices() {
        assertInvalidLine(lineaNueva("1", "-1", "0", "0", "0", "0"));
        assertInvalidLine(lineaNueva("1", "1.0000001", "0", "0", "0", "0"));
    }

    @Test
    void rejectsInvalidDiscountsAndTaxes() {
        assertInvalidLine(lineaNueva("1", "5", "-1", "0", "0", "0"));
        assertInvalidLine(lineaNueva("1", "5", "0.001", "0", "0", "0"));
        assertInvalidLine(lineaNueva("1", "5", "0", "-1", "0", "0"));
        assertInvalidLine(lineaNueva("1", "5", "0", "0.001", "0", "0"));
    }

    @Test
    void rejectsInvalidTolerances() {
        assertInvalidLine(lineaNueva("1", "5", "0", "0", "-1", "0"));
        assertInvalidLine(lineaNueva("1", "5", "0", "0", "100.1", "0"));
        assertInvalidLine(lineaNueva("1", "5", "0", "0", "0", "-1"));
        assertInvalidLine(lineaNueva("1", "5", "0", "0", "0", "0.00001"));
    }

    @Test
    void rejectsAGrossAmountAboveTheLimitOrANegativeLineTotal() {
        assertInvalidLine(lineaNueva("1000000", "10001", "0", "0", "0", "0"));
        assertInvalidLine(lineaNueva("1", "5", "6", "0", "0", "0"));
    }

    @Test
    void approvesADraftOrAnOrderInApprovalRecordingWhoAndWhen() {
        var aprobada = domainValue(ordenNueva().aprobar(ACTOR, DESPUES));

        assertThat(aprobada.estado()).isEqualTo(EstadoOrdenCompra.APROBADA);
        assertThat(aprobada.aprobadoPor()).isEqualTo(ACTOR_ID.toString());
        assertThat(aprobada.aprobadoAt()).isEqualTo(DESPUES);
        assertThat(aprobada.updatedAt()).isEqualTo(DESPUES);
        assertThat(aprobada.updatedBy()).isEqualTo(ACTOR_ID.toString());
        assertThat(aprobada.createdAt()).isEqualTo(AHORA);
        assertThat(domainValue(orden(EstadoOrdenCompra.EN_APROBACION, lineaOrden("1", "0", "0", "0"))
                .aprobar(ACTOR, DESPUES)).estado()).isEqualTo(EstadoOrdenCompra.APROBADA);
        assertThat(domainError(aprobada.aprobar(ACTOR, DESPUES)).code())
                .isEqualTo(ComprasErrorCodes.ORDEN_ESTADO_INVALIDO);
    }

    @Test
    void issuesOnlyAnApprovedOrderKeepingTheApproval() {
        var aprobada = domainValue(ordenNueva().aprobar(ACTOR, DESPUES));

        var emitida = domainValue(aprobada.emitir(ACTOR, DESPUES.plusSeconds(1)));

        assertThat(emitida.estado()).isEqualTo(EstadoOrdenCompra.EMITIDA);
        assertThat(emitida.aprobadoAt()).isEqualTo(DESPUES);
        assertThat(emitida.aprobadoPor()).isEqualTo(ACTOR_ID.toString());
        assertThat(domainError(ordenNueva().emitir(ACTOR, DESPUES)).code())
                .isEqualTo(ComprasErrorCodes.ORDEN_ESTADO_INVALIDO);
    }

    @Test
    void cancelsAnOrderThatHasNoReceptionsKeepingTheOriginalObservation() {
        var anulada = domainValue(ordenNueva().anular("  Proveedor sin stock ", ACTOR, DESPUES));

        assertThat(anulada.estado()).isEqualTo(EstadoOrdenCompra.CANCELADA);
        assertThat(anulada.observacion()).isEqualTo("Anulada: Proveedor sin stock | Reposicion");
        assertThat(anulada.updatedAt()).isEqualTo(DESPUES);
    }

    @Test
    void cancellingAnOrderWithoutObservationOnlyRecordsTheReason() {
        var sinObservacion = orden(EstadoOrdenCompra.APROBADA, lineaOrden("1", "0", "0", "0"));
        var condiciones = new CondicionesOrden(null, "PEN", BigDecimal.ONE, "CONTADO", 0, null);
        var builder = new OrdenCompra.Builder();
        builder.id = sinObservacion.id();
        builder.tenantId = sinObservacion.tenantId();
        builder.empresaId = sinObservacion.empresaId();
        builder.proveedorId = sinObservacion.proveedorId();
        builder.establecimientoDestinoId = sinObservacion.establecimientoDestinoId();
        builder.numero = sinObservacion.numero();
        builder.fechaEmision = sinObservacion.fechaEmision();
        builder.condiciones = condiciones;
        builder.estado = EstadoOrdenCompra.APROBADA;
        builder.lineas = sinObservacion.lineas();

        var anulada = domainValue(OrdenCompra.restore(builder).anular("Error de digitacion", ACTOR, DESPUES));

        assertThat(anulada.observacion()).isEqualTo("Anulada: Error de digitacion");
    }

    @Test
    void theCancellationNeverExceedsTheObservationColumn() {
        var larga = "x".repeat(300);
        var condiciones = new CondicionesOrden(null, "PEN", BigDecimal.ONE, "CONTADO", 0, "y".repeat(1500));
        var original = domainValue(crear(condiciones, List.of(lineaNueva())));

        var anulada = domainValue(original.anular(larga, ACTOR, DESPUES));

        assertThat(anulada.observacion()).hasSize(1500).startsWith("Anulada: " + larga + " | y");
    }

    @Test
    void rejectsCancellingWhenTheStateDoesNotAllowItOrTheReasonIsInvalid() {
        var parcial = orden(EstadoOrdenCompra.PARCIALMENTE_RECIBIDA, lineaOrden("10", "0", "0", "4"));

        assertThat(domainError(parcial.anular("motivo", ACTOR, DESPUES)).code())
                .isEqualTo(ComprasErrorCodes.ORDEN_ESTADO_INVALIDO);
        assertThat(domainError(ordenNueva().anular(null, ACTOR, DESPUES)).code())
                .isEqualTo(ComprasErrorCodes.ORDEN_MOTIVO_INVALIDO);
        assertThat(domainError(ordenNueva().anular("   ", ACTOR, DESPUES)).code())
                .isEqualTo(ComprasErrorCodes.ORDEN_MOTIVO_INVALIDO);
        assertThat(domainError(ordenNueva().anular("x".repeat(301), ACTOR, DESPUES)).code())
                .isEqualTo(ComprasErrorCodes.ORDEN_MOTIVO_INVALIDO);
    }

    @Test
    void aPartialReceptionLeavesTheOrderPartiallyReceived() {
        var parcial = ordenEmitida().conRecepcion(Map.of(1, dec("4")), ACTOR, DESPUES);

        assertThat(parcial.estado()).isEqualTo(EstadoOrdenCompra.PARCIALMENTE_RECIBIDA);
        assertThat(parcial.lineas().get(0).cantidadRecibida()).isEqualByComparingTo("4");
        assertThat(parcial.updatedAt()).isEqualTo(DESPUES);
        assertThat(parcial.updatedBy()).isEqualTo(ACTOR_ID.toString());
    }

    @Test
    void receivingTheOrderedQuantityCompletesTheOrderAndOtherLinesAreUntouched() {
        var dosLineas = orden(
                EstadoOrdenCompra.EMITIDA, lineaOrden("10", "0", "0", "0"),
                new LineaOrdenCompra(
                        2, SKU, "Otro", dec("5"), "UND", dec("1"), dec("0"), dec("0"), dec("5.00"), dec("0"),
                        dec("0"), dec("0")));

        var primera = dosLineas.conRecepcion(Map.of(1, dec("10")), ACTOR, DESPUES);
        var completa = primera.conRecepcion(Map.of(2, dec("5")), ACTOR, DESPUES);

        assertThat(primera.estado()).isEqualTo(EstadoOrdenCompra.PARCIALMENTE_RECIBIDA);
        assertThat(completa.estado()).isEqualTo(EstadoOrdenCompra.RECIBIDA);
        assertThat(completa.lineas()).extracting(linea -> linea.cantidadRecibida().intValue()).containsExactly(10, 5);
    }

    @Test
    void findsALineByItsNumber() {
        assertThat(ordenEmitida().linea(1)).isPresent();
        assertThat(ordenEmitida().linea(2)).isEmpty();
    }
}
