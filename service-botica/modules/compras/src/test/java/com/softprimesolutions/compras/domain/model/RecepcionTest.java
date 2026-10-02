package com.softprimesolutions.compras.domain.model;

import static com.softprimesolutions.compras.ComprasFixtures.ACTOR;
import static com.softprimesolutions.compras.ComprasFixtures.ACTOR_ID;
import static com.softprimesolutions.compras.ComprasFixtures.AHORA;
import static com.softprimesolutions.compras.ComprasFixtures.ALMACEN;
import static com.softprimesolutions.compras.ComprasFixtures.EMPRESA;
import static com.softprimesolutions.compras.ComprasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.compras.ComprasFixtures.LINEA_RECEPCION;
import static com.softprimesolutions.compras.ComprasFixtures.ORDEN;
import static com.softprimesolutions.compras.ComprasFixtures.PROVEEDOR;
import static com.softprimesolutions.compras.ComprasFixtures.RECEPCION;
import static com.softprimesolutions.compras.ComprasFixtures.SKU;
import static com.softprimesolutions.compras.ComprasFixtures.TENANT;
import static com.softprimesolutions.compras.ComprasFixtures.VENCIMIENTO;
import static com.softprimesolutions.compras.ComprasFixtures.datosRecepcion;
import static com.softprimesolutions.compras.ComprasFixtures.dec;
import static com.softprimesolutions.compras.ComprasFixtures.domainError;
import static com.softprimesolutions.compras.ComprasFixtures.domainValue;
import static com.softprimesolutions.compras.ComprasFixtures.itemRecepcion;
import static com.softprimesolutions.compras.ComprasFixtures.lineaOrden;
import static com.softprimesolutions.compras.ComprasFixtures.orden;
import static com.softprimesolutions.compras.ComprasFixtures.ordenEmitida;
import static com.softprimesolutions.compras.ComprasFixtures.recepcion;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.compras.domain.exception.ComprasErrorCodes;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecepcionTest {

    private static Result<Recepcion, ErrorDetail> registrar(DatosRecepcion datos, List<ItemRecepcion> items) {
        return Recepcion.registrar(
                RECEPCION, ESTABLECIMIENTO, datos, ordenEmitida(), items, ACTOR, AHORA, () -> LINEA_RECEPCION);
    }

    private static Result<Recepcion, ErrorDetail> registrar(List<ItemRecepcion> items) {
        return registrar(datosRecepcion(), items);
    }

    private static DatosRecepcion datos(
            String tipo, String serie, String numero, String remitente, String transportista, String temperatura,
            String humedad, String observacion) {
        return new DatosRecepcion(
                "REC-1", ALMACEN, tipo, serie, numero, remitente, transportista,
                temperatura == null ? null : dec(temperatura), humedad == null ? null : dec(humedad), observacion);
    }

    private static ItemRecepcion item(
            String lote, LocalDate fabricacion, LocalDate vencimiento, String recibida, String rechazada,
            String motivo, String costo, String observacion) {
        return new ItemRecepcion(
                1, lote, fabricacion, vencimiento, dec(recibida), dec(rechazada), motivo,
                costo == null ? null : dec(costo), observacion);
    }

    private static void assertInvalid(Result<Recepcion, ErrorDetail> result) {
        assertThat(domainError(result).code()).isEqualTo(ComprasErrorCodes.RECEPCION_INVALIDA);
    }

    @Test
    void aReceptionRecordsTheAcceptedQuantityAndTheQualityDecisionOfEachLine() {
        var recepcion = domainValue(registrar(List.of(
                item("  LOTE-1 ", null, VENCIMIENTO, "6", "1", " Envase danado ", "5.5", " Caja abierta "))));

        assertThat(recepcion.id()).isEqualTo(RECEPCION);
        assertThat(recepcion.tenantId()).isEqualTo(TENANT);
        assertThat(recepcion.empresaId()).isEqualTo(EMPRESA);
        assertThat(recepcion.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(recepcion.ordenCompraId()).isEqualTo(ORDEN);
        assertThat(recepcion.proveedorId()).isEqualTo(PROVEEDOR);
        assertThat(recepcion.datos()).isEqualTo(datosRecepcion());
        assertThat(recepcion.fechaRecepcion()).isEqualTo(AHORA);
        assertThat(recepcion.recibidoPor()).isEqualTo(ACTOR_ID.toString());
        assertThat(recepcion.pullDomainEvents()).isEmpty();
        var linea = recepcion.lineas().get(0);
        assertThat(linea.id()).isEqualTo(LINEA_RECEPCION);
        assertThat(linea.numeroLinea()).isEqualTo(1);
        assertThat(linea.numeroLineaOrden()).isEqualTo(1);
        assertThat(linea.skuId()).isEqualTo(SKU);
        assertThat(linea.numeroLote()).isEqualTo("LOTE-1");
        assertThat(linea.fechaVencimiento()).isEqualTo(VENCIMIENTO);
        assertThat(linea.cantidadRecibida()).isEqualByComparingTo("6");
        assertThat(linea.cantidadAceptada()).isEqualByComparingTo("5");
        assertThat(linea.cantidadRechazada()).isEqualByComparingTo("1");
        assertThat(linea.costoUnitario()).isEqualByComparingTo("5.5");
        assertThat(linea.decisionCalidad()).isEqualTo(Recepcion.DECISION_PARCIAL);
        assertThat(linea.motivoDecision()).isEqualTo("Envase danado");
        assertThat(linea.observacion()).isEqualTo("Caja abierta");
        assertThat(linea.ingresaStock()).isTrue();
    }

    @Test
    void aFullyAcceptedLineHasNoRejectionReasonAndAFullyRejectedOneDoesNotEnterStock() {
        var recepcion = domainValue(registrar(List.of(
                item("L-1", null, VENCIMIENTO, "3", "0", "se ignora", null, null),
                item("L-2", null, VENCIMIENTO, "2", "2", "Vencido en transito", null, null))));

        var aceptada = recepcion.lineas().get(0);
        var rechazada = recepcion.lineas().get(1);
        assertThat(aceptada.decisionCalidad()).isEqualTo(Recepcion.DECISION_ACEPTADO);
        assertThat(aceptada.motivoDecision()).isNull();
        assertThat(aceptada.costoUnitario()).isNull();
        assertThat(rechazada.decisionCalidad()).isEqualTo(Recepcion.DECISION_RECHAZADO);
        assertThat(rechazada.ingresaStock()).isFalse();
        assertThat(recepcion.lineas()).extracting(LineaRecepcion::numeroLinea).containsExactly(1, 2);
    }

    @Test
    void summarizesTheAcceptedQuantityPerOrderLine() {
        var recepcion = domainValue(registrar(List.of(
                item("L-1", null, VENCIMIENTO, "3", "0", null, null, null),
                item("L-2", null, VENCIMIENTO, "4", "1", "Roto", null, null))));

        assertThat(recepcion.aceptadoPorLineaOrden()).containsOnlyKeys(1);
        assertThat(recepcion.aceptadoPorLineaOrden().get(1)).isEqualByComparingTo("6");
    }

    @Test
    void restoringKeepsTheStoredReception() {
        var base = recepcion();

        var restaurada = Recepcion.restore(
                base.id(), base.tenantId(), base.empresaId(), base.establecimientoId(), base.ordenCompraId(),
                base.proveedorId(), base.datos(), base.fechaRecepcion(), base.recibidoPor(), base.lineas());

        assertThat(restaurada.lineas()).isEqualTo(base.lineas());
        assertThat(restaurada.recibidoPor()).isEqualTo(ACTOR_ID.toString());
    }

    @Test
    void onlyAnIssuedOrPartiallyReceivedOrderCanBeReceived() {
        var recibida = orden(EstadoOrdenCompra.RECIBIDA, lineaOrden("10", "0", "0", "10"));

        var result = Recepcion.registrar(
                RECEPCION, ESTABLECIMIENTO, datosRecepcion(), recibida, List.of(itemRecepcion("L-1", "1", "0")), ACTOR, AHORA,
                () -> LINEA_RECEPCION);

        assertThat(domainError(result).code()).isEqualTo(ComprasErrorCodes.RECEPCION_ORDEN_NO_RECEPCIONABLE);
    }

    @Test
    void rejectsInvalidReceptionData() {
        var items = List.of(itemRecepcion("L-1", "1", "0"));
        assertInvalid(registrar(datos("123", "F001", "1", null, null, null, null, null), items));
        assertInvalid(registrar(datos("01", "x".repeat(21), "1", null, null, null, null, null), items));
        assertInvalid(registrar(datos("01", "F001", "x".repeat(41), null, null, null, null, null), items));
        assertInvalid(registrar(datos("01", "F001", "1", "x".repeat(81), null, null, null, null), items));
        assertInvalid(registrar(datos("01", "F001", "1", null, "x".repeat(81), null, null, null), items));
        assertInvalid(registrar(datos("01", "F001", "1", null, null, null, null, "x".repeat(1001)), items));
    }

    @Test
    void validatesTheTemperatureAndHumidityOnlyWhenTheyAreGiven() {
        var items = List.of(itemRecepcion("L-1", "1", "0"));
        assertInvalid(registrar(datos("01", null, null, null, null, "-50.01", null, null), items));
        assertInvalid(registrar(datos("01", null, null, null, null, "100.01", null, null), items));
        assertInvalid(registrar(datos("01", null, null, null, null, "20.123", null, null), items));
        assertInvalid(registrar(datos("01", null, null, null, null, null, "-0.01", null), items));
        assertInvalid(registrar(datos("01", null, null, null, null, null, "100.01", null), items));
        assertInvalid(registrar(datos("01", null, null, null, null, null, "50.123", null), items));
        assertThat(domainValue(registrar(datos("01", null, null, null, null, "-50", "100", null), items)).datos()
                .temperaturaRecepcionC()).isEqualByComparingTo("-50");
        assertThat(domainValue(registrar(datos("01", null, null, null, null, "100", "0", null), items)).datos()
                .humedadRelativaPct()).isEqualByComparingTo("0");
    }

    @Test
    void requiresBetweenOneAndTwoHundredItems() {
        assertInvalid(registrar(null));
        assertInvalid(registrar(List.of()));
        assertInvalid(registrar(Collections.nCopies(201, itemRecepcion("L-1", "0.01", "0"))));
    }

    @Test
    void rejectsAnItemOfALineTheOrderDoesNotHave() {
        var item = new ItemRecepcion(9, "L-1", null, VENCIMIENTO, dec("1"), BigDecimal.ZERO, null, null, null);

        assertThat(domainError(registrar(List.of(item))).code())
                .isEqualTo(ComprasErrorCodes.RECEPCION_LINEA_NO_ENCONTRADA);
    }

    @Test
    void rejectsInvalidLoteData() {
        assertInvalid(registrar(List.of(item(null, null, VENCIMIENTO, "1", "0", null, null, null))));
        assertInvalid(registrar(List.of(item("  ", null, VENCIMIENTO, "1", "0", null, null, null))));
        assertInvalid(registrar(List.of(item("x".repeat(121), null, VENCIMIENTO, "1", "0", null, null, null))));
        assertInvalid(registrar(List.of(item("L-1", null, null, "1", "0", null, null, null))));
        assertInvalid(registrar(List.of(item("L-1", VENCIMIENTO.plusDays(1), VENCIMIENTO, "1", "0", null, null, null))));
        assertThat(domainValue(registrar(List.of(
                item("L-1", VENCIMIENTO, VENCIMIENTO, "1", "0", null, null, null)))).lineas()).hasSize(1);
    }

    @Test
    void rejectsInvalidQuantities() {
        assertInvalid(registrar(List.of(item("L-1", null, VENCIMIENTO, "0", "0", null, null, null))));
        assertInvalid(registrar(List.of(item("L-1", null, VENCIMIENTO, "1.00001", "0", null, null, null))));
        assertInvalid(registrar(List.of(item("L-1", null, VENCIMIENTO, "1000000001", "0", null, null, null))));
        assertInvalid(registrar(List.of(item("L-1", null, VENCIMIENTO, "2", "-1", "x", null, null))));
        assertInvalid(registrar(List.of(item("L-1", null, VENCIMIENTO, "2", "3", "x", null, null))));
        assertInvalid(registrar(List.of(item("L-1", null, VENCIMIENTO, "2", "0.00001", "x", null, null))));
    }

    @Test
    void aRejectionNeedsItsReasonAndTheTextsAreBounded() {
        assertInvalid(registrar(List.of(item("L-1", null, VENCIMIENTO, "2", "1", null, null, null))));
        assertInvalid(registrar(List.of(item("L-1", null, VENCIMIENTO, "2", "1", "  ", null, null))));
        assertInvalid(registrar(List.of(item("L-1", null, VENCIMIENTO, "2", "1", "x".repeat(1001), null, null))));
        assertInvalid(registrar(List.of(item("L-1", null, VENCIMIENTO, "2", "0", null, null, "x".repeat(501)))));
    }

    @Test
    void validatesTheUnitCostWhenItIsGiven() {
        assertInvalid(registrar(List.of(item("L-1", null, VENCIMIENTO, "1", "0", null, "-1", null))));
        assertInvalid(registrar(List.of(item("L-1", null, VENCIMIENTO, "1", "0", null, "1.0000001", null))));
        assertThat(domainValue(registrar(List.of(
                item("L-1", null, VENCIMIENTO, "1", "0", null, "0", null)))).lineas().get(0).costoUnitario())
                .isEqualByComparingTo("0");
    }

    @Test
    void theAcceptedQuantityCannotExceedTheOrderedOneWithItsTolerance() {
        var exceso = registrar(List.of(itemRecepcion("L-1", "12", "0")));

        assertThat(domainError(exceso).code()).isEqualTo(ComprasErrorCodes.RECEPCION_EXCEDE_PENDIENTE);
        assertThat(domainValue(registrar(List.of(itemRecepcion("L-1", "11", "0")))).lineas()).hasSize(1);
    }

    @Test
    void theToleranceAppliesToTheTotalOfTheItemsOfTheSameOrderLine() {
        var dentro = registrar(List.of(itemRecepcion("L-1", "6", "0"), itemRecepcion("L-2", "5", "0")));
        var fuera = registrar(List.of(itemRecepcion("L-1", "6", "0"), itemRecepcion("L-2", "6", "0")));

        assertThat(domainValue(dentro).lineas()).hasSize(2);
        assertThat(domainError(fuera).code()).isEqualTo(ComprasErrorCodes.RECEPCION_EXCEDE_PENDIENTE);
    }

    @Test
    void theToleranceTakesIntoAccountWhatWasAlreadyReceived() {
        var parcial = orden(EstadoOrdenCompra.PARCIALMENTE_RECIBIDA, lineaOrden("10", "0", "0", "8"));

        var result = Recepcion.registrar(
                UUID.randomUUID(), ESTABLECIMIENTO, datosRecepcion(), parcial, List.of(itemRecepcion("L-1", "3", "0")), ACTOR,
                AHORA, () -> LINEA_RECEPCION);

        assertThat(domainError(result).code()).isEqualTo(ComprasErrorCodes.RECEPCION_EXCEDE_PENDIENTE);
    }
}
