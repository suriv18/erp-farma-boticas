package com.softprimesolutions.compras.domain.model;

import com.softprimesolutions.compras.domain.exception.ComprasErrorCodes;
import com.softprimesolutions.compras.domain.valueobject.Actor;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class Recepcion extends AggregateRoot {

    public static final String ESTADO_CONFIRMADA = "CONFIRMADA";
    public static final String DECISION_ACEPTADO = "ACEPTADO";
    public static final String DECISION_PARCIAL = "ACEPTADO_PARCIAL";
    public static final String DECISION_RECHAZADO = "RECHAZADO";

    private static final int LINEAS_MAXIMAS = 200;
    private static final BigDecimal TEMPERATURA_MINIMA = new BigDecimal("-50");
    private static final BigDecimal TEMPERATURA_MAXIMA = new BigDecimal("100");

    private final UUID id;
    private final UUID tenantId;
    private final UUID empresaId;
    private final UUID establecimientoId;
    private final UUID ordenCompraId;
    private final UUID proveedorId;
    private final DatosRecepcion datos;
    private final Instant fechaRecepcion;
    private final String recibidoPor;
    private final List<LineaRecepcion> lineas;

    private Recepcion(
            UUID id, UUID tenantId, UUID empresaId, UUID establecimientoId, UUID ordenCompraId, UUID proveedorId,
            DatosRecepcion datos, Instant fechaRecepcion, String recibidoPor, List<LineaRecepcion> lineas) {
        this.id = Objects.requireNonNull(id, "id es obligatorio");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId es obligatorio");
        this.empresaId = Objects.requireNonNull(empresaId, "empresaId es obligatorio");
        this.establecimientoId = Objects.requireNonNull(establecimientoId, "establecimientoId es obligatorio");
        this.ordenCompraId = Objects.requireNonNull(ordenCompraId, "ordenCompraId es obligatorio");
        this.proveedorId = Objects.requireNonNull(proveedorId, "proveedorId es obligatorio");
        this.datos = Objects.requireNonNull(datos, "datos es obligatorio");
        this.fechaRecepcion = Objects.requireNonNull(fechaRecepcion, "fechaRecepcion es obligatoria");
        this.recibidoPor = Objects.requireNonNull(recibidoPor, "recibidoPor es obligatorio");
        this.lineas = List.copyOf(lineas);
    }

    public static Result<Recepcion, ErrorDetail> registrar(
            UUID id, UUID establecimientoId, DatosRecepcion datos, OrdenCompra orden, List<ItemRecepcion> items,
            Actor actor, Instant at, Supplier<UUID> identificadores) {
        Objects.requireNonNull(orden, "orden es obligatoria");
        Objects.requireNonNull(actor, "actor es obligatorio");
        Objects.requireNonNull(at, "at es obligatorio");
        if (!orden.estado().recepcionable()) {
            return error(ComprasErrorCodes.RECEPCION_ORDEN_NO_RECEPCIONABLE,
                    "La orden debe estar emitida o parcialmente recibida para recibir mercaderia.");
        }
        var invalido = errorDatos(datos).or(() -> errorCantidadItems(items));
        if (invalido.isPresent()) return error(ComprasErrorCodes.RECEPCION_INVALIDA, invalido.get());
        var lineas = new ArrayList<LineaRecepcion>();
        var aceptado = new HashMap<Integer, BigDecimal>();
        for (var indice = 0; indice < items.size(); indice++) {
            var item = items.get(indice);
            var ordenLinea = orden.linea(item.numeroLineaOrden());
            if (ordenLinea.isEmpty()) {
                return error(ComprasErrorCodes.RECEPCION_LINEA_NO_ENCONTRADA,
                        "La orden no tiene la linea " + item.numeroLineaOrden() + ".");
            }
            var prefijo = "Item " + (indice + 1) + ": ";
            var errorItem = errorItem(item);
            if (errorItem.isPresent()) return error(ComprasErrorCodes.RECEPCION_INVALIDA, prefijo + errorItem.get());
            var aceptada = item.cantidadRecibida().subtract(item.cantidadRechazada());
            var acumulado = aceptado.getOrDefault(item.numeroLineaOrden(), BigDecimal.ZERO).add(aceptada);
            if (acumulado.compareTo(ordenLinea.get().maximoRecibible()) > 0) {
                return error(ComprasErrorCodes.RECEPCION_EXCEDE_PENDIENTE,
                        prefijo + "La cantidad aceptada supera lo pendiente de la linea "
                                + item.numeroLineaOrden() + " de la orden.");
            }
            aceptado.put(item.numeroLineaOrden(), acumulado);
            lineas.add(linea(identificadores.get(), indice + 1, ordenLinea.get(), item, aceptada));
        }
        return Result.success(new Recepcion(
                id, orden.tenantId(), orden.empresaId(), establecimientoId, orden.id(), orden.proveedorId(), datos,
                at, actor.codigo(), lineas));
    }

    public static Recepcion restore(
            UUID id, UUID tenantId, UUID empresaId, UUID establecimientoId, UUID ordenCompraId, UUID proveedorId,
            DatosRecepcion datos, Instant fechaRecepcion, String recibidoPor, List<LineaRecepcion> lineas) {
        return new Recepcion(
                id, tenantId, empresaId, establecimientoId, ordenCompraId, proveedorId, datos, fechaRecepcion,
                recibidoPor, lineas);
    }

    public Map<Integer, BigDecimal> aceptadoPorLineaOrden() {
        return lineas.stream().collect(Collectors.groupingBy(
                LineaRecepcion::numeroLineaOrden,
                Collectors.reducing(BigDecimal.ZERO, LineaRecepcion::cantidadAceptada, BigDecimal::add)));
    }

    private static LineaRecepcion linea(
            UUID id, int numeroLinea, LineaOrdenCompra ordenLinea, ItemRecepcion item, BigDecimal aceptada) {
        var decision = aceptada.compareTo(item.cantidadRecibida()) == 0 ? DECISION_ACEPTADO
                : aceptada.signum() == 0 ? DECISION_RECHAZADO : DECISION_PARCIAL;
        var motivo = item.cantidadRechazada().signum() > 0 ? Campos.texto(item.motivoRechazo()) : null;
        return new LineaRecepcion(
                id, numeroLinea, item.numeroLineaOrden(), ordenLinea.skuId(), item.numeroLote().trim(),
                item.fechaFabricacion(), item.fechaVencimiento(), item.cantidadRecibida(), aceptada,
                item.cantidadRechazada(), item.costoUnitario(), decision, motivo,
                Campos.texto(item.observacion()));
    }

    private static Optional<String> errorDatos(DatosRecepcion datos) {
        if (Campos.excede(datos.documentoProveedorTipo(), 2) || Campos.excede(datos.documentoProveedorSerie(), 20)
                || Campos.excede(datos.documentoProveedorNumero(), 40)) {
            return Optional.of("El documento del proveedor excede la longitud permitida.");
        }
        if (Campos.excede(datos.guiaRemisionRemitente(), 80) || Campos.excede(datos.guiaRemisionTransportista(), 80)) {
            return Optional.of("Las guias de remision admiten hasta 80 caracteres.");
        }
        if (Campos.excede(datos.observacion(), 1000)) {
            return Optional.of("La observacion admite hasta 1000 caracteres.");
        }
        var temperatura = datos.temperaturaRecepcionC();
        if (temperatura != null && (temperatura.compareTo(TEMPERATURA_MINIMA) < 0
                || temperatura.compareTo(TEMPERATURA_MAXIMA) > 0 || temperatura.stripTrailingZeros().scale() > 2)) {
            return Optional.of("La temperatura debe estar entre -50 y 100 con hasta 2 decimales.");
        }
        var humedad = datos.humedadRelativaPct();
        if (humedad != null && (humedad.signum() < 0 || humedad.compareTo(Campos.PORCENTAJE_MAXIMO) > 0
                || humedad.stripTrailingZeros().scale() > 2)) {
            return Optional.of("La humedad relativa debe estar entre 0 y 100 con hasta 2 decimales.");
        }
        return Optional.empty();
    }

    private static Optional<String> errorCantidadItems(List<ItemRecepcion> items) {
        if (items == null || items.isEmpty() || items.size() > LINEAS_MAXIMAS) {
            return Optional.of("La recepcion debe tener entre 1 y 200 lineas.");
        }
        return Optional.empty();
    }

    private static Optional<String> errorItem(ItemRecepcion item) {
        var numeroLote = Campos.texto(item.numeroLote());
        if (numeroLote == null || numeroLote.length() > 120) {
            return Optional.of("El numero de lote debe tener entre 1 y 120 caracteres.");
        }
        if (item.fechaVencimiento() == null) {
            return Optional.of("La fecha de vencimiento es obligatoria.");
        }
        if (item.fechaFabricacion() != null && item.fechaFabricacion().isAfter(item.fechaVencimiento())) {
            return Optional.of("La fecha de fabricacion no puede ser posterior al vencimiento.");
        }
        if (item.cantidadRecibida().signum() <= 0
                || Campos.decimalInvalido(item.cantidadRecibida(), 4, Campos.CANTIDAD_MAXIMA)) {
            return Optional.of("La cantidad recibida debe ser mayor que cero y admite hasta 4 decimales.");
        }
        if (item.cantidadRechazada().signum() < 0 || item.cantidadRechazada().compareTo(item.cantidadRecibida()) > 0
                || Campos.decimalInvalido(item.cantidadRechazada(), 4, Campos.CANTIDAD_MAXIMA)) {
            return Optional.of("La cantidad rechazada debe estar entre 0 y la cantidad recibida.");
        }
        if (item.cantidadRechazada().signum() > 0 && Campos.texto(item.motivoRechazo()) == null) {
            return Optional.of("El motivo del rechazo es obligatorio cuando hay cantidad rechazada.");
        }
        if (Campos.excede(item.motivoRechazo(), 1000) || Campos.excede(item.observacion(), 500)) {
            return Optional.of("El motivo admite hasta 1000 caracteres y la observacion hasta 500.");
        }
        if (item.costoUnitario() != null && (item.costoUnitario().signum() < 0
                || Campos.decimalInvalido(item.costoUnitario(), 6, Campos.IMPORTE_MAXIMO))) {
            return Optional.of("El costo unitario no puede ser negativo y admite hasta 6 decimales.");
        }
        return Optional.empty();
    }

    private static <T> Result<T, ErrorDetail> error(String codigo, String mensaje) {
        return Result.failure(new ErrorDetail(codigo, mensaje, Map.of()));
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID empresaId() { return empresaId; }
    public UUID establecimientoId() { return establecimientoId; }
    public UUID ordenCompraId() { return ordenCompraId; }
    public UUID proveedorId() { return proveedorId; }
    public DatosRecepcion datos() { return datos; }
    public Instant fechaRecepcion() { return fechaRecepcion; }
    public String recibidoPor() { return recibidoPor; }
    public List<LineaRecepcion> lineas() { return lineas; }
}
