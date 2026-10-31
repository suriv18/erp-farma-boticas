package com.softprimesolutions.compras;

import com.softprimesolutions.compras.application.dto.command.ProveedorInput;
import com.softprimesolutions.compras.domain.model.CondicionesOrden;
import com.softprimesolutions.compras.domain.model.DatosProveedor;
import com.softprimesolutions.compras.domain.model.DatosRecepcion;
import com.softprimesolutions.compras.domain.model.EstadoOrdenCompra;
import com.softprimesolutions.compras.domain.model.EstadoProveedor;
import com.softprimesolutions.compras.domain.model.ItemRecepcion;
import com.softprimesolutions.compras.domain.model.LineaNueva;
import com.softprimesolutions.compras.domain.model.LineaOrdenCompra;
import com.softprimesolutions.compras.domain.model.OrdenCompra;
import com.softprimesolutions.compras.domain.model.Proveedor;
import com.softprimesolutions.compras.domain.model.Recepcion;
import com.softprimesolutions.compras.domain.valueobject.Actor;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ComprasFixtures {

    public static final UUID TENANT = UUID.fromString("0f6d4c2e-3b1a-4c8e-9a51-7d2b6e4f1a90");
    public static final UUID PROVEEDOR = UUID.fromString("11111111-1111-4111-8111-111111111111");
    public static final UUID EMPRESA = UUID.fromString("22222222-2222-4222-8222-222222222222");
    public static final UUID ESTABLECIMIENTO = UUID.fromString("33333333-3333-4333-8333-333333333333");
    public static final UUID ALMACEN = UUID.fromString("44444444-4444-4444-8444-444444444444");
    public static final UUID SKU = UUID.fromString("55555555-5555-4555-8555-555555555555");
    public static final UUID ORDEN = UUID.fromString("66666666-6666-4666-8666-666666666666");
    public static final UUID RECEPCION = UUID.fromString("77777777-7777-4777-8777-777777777777");
    public static final UUID LINEA_RECEPCION = UUID.fromString("88888888-8888-4888-8888-888888888888");
    public static final UUID LOTE = UUID.fromString("99999999-9999-4999-8999-999999999999");
    public static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    public static final Actor ACTOR = new Actor(ACTOR_ID);
    public static final Instant AHORA = Instant.parse("2026-06-15T12:00:00Z");
    public static final LocalDate HOY = LocalDate.of(2026, 6, 15);
    public static final LocalDate VENCIMIENTO = LocalDate.of(2027, 6, 30);
    public static final ApplicationError CONFLICTO =
            new StandardApplicationError("TEST_CONFLICT", "Conflicto de prueba.", ErrorCategory.CONFLICT);

    private ComprasFixtures() {
    }

    public static BigDecimal dec(String valor) {
        return new BigDecimal(valor);
    }

    public static <T> T domainValue(Result<T, ErrorDetail> result) {
        return result.fold(value -> value, error -> { throw new AssertionError(error); });
    }

    public static <T> ErrorDetail domainError(Result<T, ErrorDetail> result) {
        return result.fold(value -> { throw new AssertionError(value); }, error -> error);
    }

    public static <T> T value(Result<T, ApplicationError> result) {
        return result.fold(value -> value, error -> { throw new AssertionError(error); });
    }

    public static <T> ApplicationError error(Result<T, ApplicationError> result) {
        return result.fold(value -> { throw new AssertionError(value); }, error -> error);
    }

    public static <T> Result<T, ApplicationError> ok(T value) {
        return Result.success(value);
    }

    public static <T> Result<T, ApplicationError> conflict() {
        return Result.failure(CONFLICTO);
    }

    public static DatosProveedor datosProveedor() {
        return domainValue(DatosProveedor.crear(
                null, "20100070970", "Laboratorios SAC", null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null));
    }

    public static ProveedorInput proveedorInput() {
        return new ProveedorInput(
                null, "20100070970", "Laboratorios SAC", null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null);
    }

    public static Proveedor proveedor(EstadoProveedor estado) {
        return Proveedor.restore(PROVEEDOR, TENANT, datosProveedor(), estado, AHORA, ACTOR_ID.toString(), null, null);
    }

    public static CondicionesOrden condiciones() {
        return new CondicionesOrden(null, "PEN", BigDecimal.ONE, "CONTADO", 0, "Reposicion");
    }

    public static LineaNueva lineaNueva(
            String cantidad, String precio, String descuento, String impuesto, String exceso, String defecto) {
        return new LineaNueva(
                SKU, "Paracetamol 500 mg", dec(cantidad), "UND", dec(precio), dec(descuento), dec(impuesto),
                dec(exceso), dec(defecto));
    }

    public static LineaNueva lineaNueva() {
        return lineaNueva("10", "5.5", "0", "9.9", "10", "0");
    }

    public static OrdenCompra ordenNueva() {
        return domainValue(OrdenCompra.crear(
                ORDEN, TENANT, EMPRESA, PROVEEDOR, ESTABLECIMIENTO, "OC-2026-000001", HOY, condiciones(),
                List.of(lineaNueva()), ACTOR, AHORA));
    }

    public static LineaOrdenCompra lineaOrden(String cantidad, String exceso, String defecto, String recibido) {
        return new LineaOrdenCompra(
                1, SKU, "Paracetamol 500 mg", dec(cantidad), "UND", dec("5.5"), dec("0"), dec("0"), dec("55.00"),
                dec(exceso), dec(defecto), dec(recibido));
    }

    public static OrdenCompra orden(EstadoOrdenCompra estado, LineaOrdenCompra... lineas) {
        var builder = new OrdenCompra.Builder();
        builder.id = ORDEN;
        builder.tenantId = TENANT;
        builder.empresaId = EMPRESA;
        builder.proveedorId = PROVEEDOR;
        builder.establecimientoDestinoId = ESTABLECIMIENTO;
        builder.numero = "OC-2026-000001";
        builder.fechaEmision = HOY;
        builder.condiciones = condiciones();
        builder.subtotal = dec("55.00");
        builder.descuentoTotal = dec("0");
        builder.impuestoTotal = dec("0");
        builder.total = dec("55.00");
        builder.estado = estado;
        builder.lineas = List.of(lineas);
        builder.createdAt = AHORA;
        builder.createdBy = ACTOR_ID.toString();
        return OrdenCompra.restore(builder);
    }

    public static OrdenCompra ordenEmitida() {
        return orden(EstadoOrdenCompra.EMITIDA, lineaOrden("10", "10", "0", "0"));
    }

    public static DatosRecepcion datosRecepcion() {
        return new DatosRecepcion(
                "REC-2026-000001", ALMACEN, "01", "F001", "123", "T001-45", null, null, null, "Recepcion");
    }

    public static ItemRecepcion itemRecepcion(String lote, String recibida, String rechazada) {
        return new ItemRecepcion(
                1, lote, null, VENCIMIENTO, dec(recibida), dec(rechazada), "Envase danado", dec("5.5"), null);
    }

    public static Recepcion recepcion() {
        return domainValue(Recepcion.registrar(
                RECEPCION, ESTABLECIMIENTO, datosRecepcion(), ordenEmitida(),
                List.of(itemRecepcion("LOTE-1", "6", "1")), ACTOR, AHORA, () -> LINEA_RECEPCION));
    }
}
