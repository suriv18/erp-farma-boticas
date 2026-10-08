package com.softprimesolutions.compras.application.usecase.command;

import static com.softprimesolutions.compras.ComprasFixtures.ACTOR_ID;
import static com.softprimesolutions.compras.ComprasFixtures.AHORA;
import static com.softprimesolutions.compras.ComprasFixtures.EMPRESA;
import static com.softprimesolutions.compras.ComprasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.compras.ComprasFixtures.HOY;
import static com.softprimesolutions.compras.ComprasFixtures.ORDEN;
import static com.softprimesolutions.compras.ComprasFixtures.PROVEEDOR;
import static com.softprimesolutions.compras.ComprasFixtures.SKU;
import static com.softprimesolutions.compras.ComprasFixtures.TENANT;
import static com.softprimesolutions.compras.ComprasFixtures.dec;
import static com.softprimesolutions.compras.ComprasFixtures.error;
import static com.softprimesolutions.compras.ComprasFixtures.proveedor;
import static com.softprimesolutions.compras.ComprasFixtures.value;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.compras.application.dto.command.CrearOrdenCompraCommand;
import com.softprimesolutions.compras.application.dto.command.LineaOrdenCompraInput;
import com.softprimesolutions.compras.application.port.out.GuardadoOutcome;
import com.softprimesolutions.compras.application.port.out.NumeracionPort;
import com.softprimesolutions.compras.application.port.out.OrdenCompraWritePort;
import com.softprimesolutions.compras.application.port.out.ProveedorWritePort;
import com.softprimesolutions.compras.application.port.out.ReferenciasComprasPort;
import com.softprimesolutions.compras.application.port.out.ReferenciasComprasPort.EstablecimientoRef;
import com.softprimesolutions.compras.application.port.out.ReferenciasComprasPort.SkuRef;
import com.softprimesolutions.compras.domain.model.EstadoOrdenCompra;
import com.softprimesolutions.compras.domain.model.EstadoProveedor;
import com.softprimesolutions.compras.domain.model.OrdenCompra;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CrearOrdenCompraHandlerTest {

    private final OrdenCompraWritePort ordenes = mock(OrdenCompraWritePort.class);
    private final ProveedorWritePort proveedores = mock(ProveedorWritePort.class);
    private final ReferenciasComprasPort referencias = mock(ReferenciasComprasPort.class);
    private final NumeracionPort numeracion = mock(NumeracionPort.class);

    private CrearOrdenCompraHandler handlerAt(Instant ahora) {
        return new CrearOrdenCompraHandler(ordenes, proveedores, referencias, numeracion, () -> ORDEN, () -> ahora);
    }

    private final CrearOrdenCompraHandler handler = handlerAt(AHORA);

    @BeforeEach
    void everythingIsValidByDefault() {
        when(proveedores.findById(TENANT, PROVEEDOR)).thenReturn(Optional.of(proveedor(EstadoProveedor.ACTIVO)));
        when(referencias.establecimiento(TENANT, ESTABLECIMIENTO))
                .thenReturn(Optional.of(new EstablecimientoRef(ESTABLECIMIENTO, EMPRESA, true)));
        when(referencias.skus(eq(TENANT), anyCollection()))
                .thenReturn(Map.of(SKU, new SkuRef(SKU, "Paracetamol 500 mg", true)));
        when(referencias.existeUnidadMedida("UND")).thenReturn(true);
        when(numeracion.siguienteNumeroOrden()).thenReturn("OC-2026-000001");
        when(ordenes.insertar(any())).thenReturn(GuardadoOutcome.GUARDADO);
    }

    private static LineaOrdenCompraInput linea(String cantidad) {
        return new LineaOrdenCompraInput(SKU, dec(cantidad), "UND", dec("5"), null, null, null, null);
    }

    private static CrearOrdenCompraCommand command(LineaOrdenCompraInput... lineas) {
        return new CrearOrdenCompraCommand(
                TENANT, ACTOR_ID, PROVEEDOR, ESTABLECIMIENTO, null, null, null, null, null, null, List.of(lineas));
    }

    private OrdenCompra saved() {
        var captor = ArgumentCaptor.forClass(OrdenCompra.class);
        verify(ordenes).insertar(captor.capture());
        return captor.getValue();
    }

    @Test
    void createsADraftTakingTheConditionsFromTheProveedorWhenTheyAreOmitted() {
        var result = value(handler.execute(command(linea("10"))));

        assertThat(result.id()).isEqualTo(ORDEN);
        assertThat(result.numero()).isEqualTo("OC-2026-000001");
        assertThat(result.estado()).isEqualTo("BORRADOR");
        assertThat(result.moneda()).isEqualTo("PEN");
        assertThat(result.tipoCambio()).isEqualByComparingTo("1");
        assertThat(result.condicionPago()).isEqualTo("CONTADO");
        assertThat(result.diasCredito()).isZero();
        assertThat(result.total()).isEqualByComparingTo("50.00");
        assertThat(result.lineas().get(0).descripcion()).isEqualTo("Paracetamol 500 mg");
        var orden = saved();
        assertThat(orden.empresaId()).isEqualTo(EMPRESA);
        assertThat(orden.proveedorId()).isEqualTo(PROVEEDOR);
        assertThat(orden.establecimientoDestinoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(orden.createdBy()).isEqualTo(ACTOR_ID.toString());
        assertThat(orden.estado()).isEqualTo(EstadoOrdenCompra.BORRADOR);
    }

    @Test
    void usesTheConditionsGivenInTheCommand() {
        var command = new CrearOrdenCompraCommand(
                TENANT, ACTOR_ID, PROVEEDOR, ESTABLECIMIENTO, HOY.plusDays(7), "USD", dec("3.75"), "CREDITO 15", 15,
                "Urgente",
                List.of(new LineaOrdenCompraInput(
                        SKU, dec("10"), "UND", dec("5"), dec("1"), dec("2"), dec("10"), dec("5"))));

        var result = value(handler.execute(command));

        assertThat(result.moneda()).isEqualTo("USD");
        assertThat(result.tipoCambio()).isEqualByComparingTo("3.75");
        assertThat(result.condicionPago()).isEqualTo("CREDITO 15");
        assertThat(result.diasCredito()).isEqualTo(15);
        assertThat(result.fechaEntregaEstimada()).isEqualTo(HOY.plusDays(7));
        assertThat(result.observacion()).isEqualTo("Urgente");
        assertThat(result.total()).isEqualByComparingTo("51.00");
        assertThat(result.lineas().get(0).toleranciaExcesoPct()).isEqualByComparingTo("10");
        assertThat(result.lineas().get(0).toleranciaDefectoPct()).isEqualByComparingTo("5");
    }

    @Test
    void theIssueDateIsTheLimaDateEvenWhenUtcAlreadyIsTheNextDay() {
        var anocheceEnLima = Instant.parse("2026-06-16T03:00:00Z");

        var result = value(handlerAt(anocheceEnLima).execute(command(linea("1"))));

        assertThat(result.fechaEmision()).isEqualTo(LocalDate.of(2026, 6, 15));
    }

    @Test
    void rejectsAnUnknownOrInactiveProveedor() {
        when(proveedores.findById(TENANT, PROVEEDOR)).thenReturn(Optional.empty());
        assertThat(error(handler.execute(command(linea("1")))).code()).isEqualTo("COM_PROVEEDOR_NO_ENCONTRADO");

        when(proveedores.findById(TENANT, PROVEEDOR)).thenReturn(Optional.of(proveedor(EstadoProveedor.BLOQUEADO)));
        var noOperable = error(handler.execute(command(linea("1"))));
        assertThat(noOperable.code()).isEqualTo("COM_PROVEEDOR_NO_OPERABLE");
        assertThat(noOperable.category()).isEqualTo(ErrorCategory.CONFLICT);
        verify(ordenes, never()).insertar(any());
    }

    @Test
    void rejectsAnUnknownOrInactiveDestinationEstablishment() {
        when(referencias.establecimiento(TENANT, ESTABLECIMIENTO)).thenReturn(Optional.empty());
        assertThat(error(handler.execute(command(linea("1")))).code()).isEqualTo("COM_ESTABLECIMIENTO_NO_ENCONTRADO");

        when(referencias.establecimiento(TENANT, ESTABLECIMIENTO))
                .thenReturn(Optional.of(new EstablecimientoRef(ESTABLECIMIENTO, EMPRESA, false)));
        assertThat(error(handler.execute(command(linea("1")))).code()).isEqualTo("COM_ESTABLECIMIENTO_NO_OPERABLE");
    }

    @Test
    void rejectsLinesWithAnUnknownOrInactiveSkuOrAnUnknownUnit() {
        when(referencias.skus(eq(TENANT), anyCollection())).thenReturn(Map.of());
        assertThat(error(handler.execute(command(linea("1")))).code()).isEqualTo("COM_SKU_NO_ENCONTRADO");

        when(referencias.skus(eq(TENANT), anyCollection()))
                .thenReturn(Map.of(SKU, new SkuRef(SKU, "Paracetamol", false)));
        assertThat(error(handler.execute(command(linea("1")))).code()).isEqualTo("COM_SKU_NO_OPERABLE");

        when(referencias.skus(eq(TENANT), anyCollection()))
                .thenReturn(Map.of(SKU, new SkuRef(SKU, "Paracetamol", true)));
        when(referencias.existeUnidadMedida("UND")).thenReturn(false);
        assertThat(error(handler.execute(command(linea("1")))).code()).isEqualTo("COM_UNIDAD_MEDIDA_NO_ENCONTRADA");
        verify(ordenes, never()).insertar(any());
    }

    @Test
    void reportsTheDomainValidationErrors() {
        var error = error(handler.execute(command(linea("0"))));

        assertThat(error.code()).isEqualTo("COM_ORDEN_INVALIDA");
        assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
        verify(ordenes, never()).insertar(any());
    }

    @Test
    void anOrderWithoutLinesIsRejectedByTheDomainWithoutLookingForSkus() {
        var error = error(handler.execute(command()));

        assertThat(error.code()).isEqualTo("COM_ORDEN_INVALIDA");
    }

    @Test
    void reportsAConcurrentModificationWhenTheNumberCollides() {
        when(ordenes.insertar(any())).thenReturn(GuardadoOutcome.DUPLICADO);

        var error = error(handler.execute(command(linea("1"))));

        assertThat(error.code()).isEqualTo("COM_MODIFICACION_CONCURRENTE");
    }
}
