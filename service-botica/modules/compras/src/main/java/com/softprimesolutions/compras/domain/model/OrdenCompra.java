package com.softprimesolutions.compras.domain.model;

import com.softprimesolutions.compras.domain.exception.ComprasErrorCodes;
import com.softprimesolutions.compras.domain.valueobject.Actor;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class OrdenCompra extends AggregateRoot {

    private static final int LINEAS_MAXIMAS = 200;
    private static final int MOTIVO_MAXIMO = 300;
    private static final int OBSERVACION_MAXIMA = 1500;
    private static final Pattern MONEDA = Pattern.compile("^[A-Z]{3}$");

    private final UUID id;
    private final UUID tenantId;
    private final UUID empresaId;
    private final UUID proveedorId;
    private final UUID establecimientoDestinoId;
    private final String numero;
    private final LocalDate fechaEmision;
    private final CondicionesOrden condiciones;
    private final BigDecimal subtotal;
    private final BigDecimal descuentoTotal;
    private final BigDecimal impuestoTotal;
    private final BigDecimal total;
    private final EstadoOrdenCompra estado;
    private final String aprobadoPor;
    private final Instant aprobadoAt;
    private final List<LineaOrdenCompra> lineas;
    private final Instant createdAt;
    private final String createdBy;
    private final Instant updatedAt;
    private final String updatedBy;

    private OrdenCompra(Builder datos) {
        this.id = Objects.requireNonNull(datos.id, "id es obligatorio");
        this.tenantId = Objects.requireNonNull(datos.tenantId, "tenantId es obligatorio");
        this.empresaId = Objects.requireNonNull(datos.empresaId, "empresaId es obligatorio");
        this.proveedorId = Objects.requireNonNull(datos.proveedorId, "proveedorId es obligatorio");
        this.establecimientoDestinoId = Objects.requireNonNull(
                datos.establecimientoDestinoId, "establecimientoDestinoId es obligatorio");
        this.numero = Objects.requireNonNull(datos.numero, "numero es obligatorio");
        this.fechaEmision = Objects.requireNonNull(datos.fechaEmision, "fechaEmision es obligatoria");
        this.condiciones = Objects.requireNonNull(datos.condiciones, "condiciones es obligatorio");
        this.subtotal = datos.subtotal;
        this.descuentoTotal = datos.descuentoTotal;
        this.impuestoTotal = datos.impuestoTotal;
        this.total = datos.total;
        this.estado = Objects.requireNonNull(datos.estado, "estado es obligatorio");
        this.aprobadoPor = datos.aprobadoPor;
        this.aprobadoAt = datos.aprobadoAt;
        this.lineas = List.copyOf(datos.lineas);
        this.createdAt = datos.createdAt;
        this.createdBy = datos.createdBy;
        this.updatedAt = datos.updatedAt;
        this.updatedBy = datos.updatedBy;
    }

    public static Result<OrdenCompra, ErrorDetail> crear(
            UUID id, UUID tenantId, UUID empresaId, UUID proveedorId, UUID establecimientoDestinoId, String numero,
            LocalDate hoy, CondicionesOrden condiciones, List<LineaNueva> nuevas, Actor actor, Instant at) {
        Objects.requireNonNull(hoy, "hoy es obligatorio");
        Objects.requireNonNull(actor, "actor es obligatorio");
        Objects.requireNonNull(at, "at es obligatorio");
        var error = errorCondiciones(condiciones, hoy).or(() -> errorCantidadLineas(nuevas));
        if (error.isPresent()) return invalida(error.get());
        return construirLineas(nuevas).map(lineas -> {
            var builder = new Builder();
            builder.id = id;
            builder.tenantId = tenantId;
            builder.empresaId = empresaId;
            builder.proveedorId = proveedorId;
            builder.establecimientoDestinoId = establecimientoDestinoId;
            builder.numero = numero;
            builder.fechaEmision = hoy;
            builder.condiciones = condiciones;
            builder.subtotal = suma(lineas, LineaOrdenCompra::importeBruto);
            builder.descuentoTotal = suma(lineas, LineaOrdenCompra::descuento);
            builder.impuestoTotal = suma(lineas, LineaOrdenCompra::impuesto);
            builder.total = suma(lineas, LineaOrdenCompra::totalLinea);
            builder.estado = EstadoOrdenCompra.BORRADOR;
            builder.lineas = lineas;
            builder.createdAt = at;
            builder.createdBy = actor.codigo();
            return new OrdenCompra(builder);
        });
    }

    public static OrdenCompra restore(Builder datos) {
        return new OrdenCompra(datos);
    }

    public Result<OrdenCompra, ErrorDetail> aprobar(Actor actor, Instant at) {
        Objects.requireNonNull(actor, "actor es obligatorio");
        Objects.requireNonNull(at, "at es obligatorio");
        if (!estado.aprobable()) {
            return estadoInvalido("Solo una orden en borrador o en aprobacion puede aprobarse.");
        }
        var builder = copia(EstadoOrdenCompra.APROBADA, lineas, observacion(), actor, at);
        builder.aprobadoPor = actor.codigo();
        builder.aprobadoAt = at;
        return Result.success(new OrdenCompra(builder));
    }

    public Result<OrdenCompra, ErrorDetail> emitir(Actor actor, Instant at) {
        Objects.requireNonNull(actor, "actor es obligatorio");
        Objects.requireNonNull(at, "at es obligatorio");
        if (!estado.emitible()) return estadoInvalido("Solo una orden aprobada puede emitirse.");
        return Result.success(new OrdenCompra(copia(EstadoOrdenCompra.EMITIDA, lineas, observacion(), actor, at)));
    }

    public Result<OrdenCompra, ErrorDetail> anular(String motivo, Actor actor, Instant at) {
        Objects.requireNonNull(actor, "actor es obligatorio");
        Objects.requireNonNull(at, "at es obligatorio");
        if (!estado.anulable()) {
            return estadoInvalido("La orden no puede anularse en su estado actual o ya tiene recepciones.");
        }
        var razon = Campos.texto(motivo);
        if (razon == null || razon.length() > MOTIVO_MAXIMO) {
            return Result.failure(new ErrorDetail(
                    ComprasErrorCodes.ORDEN_MOTIVO_INVALIDO,
                    "El motivo de anulacion debe tener entre 1 y 300 caracteres.", Map.of()));
        }
        var anulada = "Anulada: " + razon + Optional.ofNullable(observacion()).map(previa -> " | " + previa).orElse("");
        var observacion = anulada.substring(0, Math.min(anulada.length(), OBSERVACION_MAXIMA));
        return Result.success(new OrdenCompra(copia(EstadoOrdenCompra.CANCELADA, lineas, observacion, actor, at)));
    }

    public OrdenCompra conRecepcion(Map<Integer, BigDecimal> aceptadoPorLinea, Actor actor, Instant at) {
        Objects.requireNonNull(actor, "actor es obligatorio");
        Objects.requireNonNull(at, "at es obligatorio");
        var actualizadas = lineas.stream()
                .map(linea -> linea.conRecibido(aceptadoPorLinea.getOrDefault(linea.numeroLinea(), BigDecimal.ZERO)))
                .toList();
        var nuevoEstado = actualizadas.stream().allMatch(LineaOrdenCompra::completa)
                ? EstadoOrdenCompra.RECIBIDA
                : EstadoOrdenCompra.PARCIALMENTE_RECIBIDA;
        return new OrdenCompra(copia(nuevoEstado, actualizadas, observacion(), actor, at));
    }

    public Optional<LineaOrdenCompra> linea(int numeroLinea) {
        return lineas.stream().filter(linea -> linea.numeroLinea() == numeroLinea).findFirst();
    }

    private Builder copia(
            EstadoOrdenCompra nuevoEstado, List<LineaOrdenCompra> nuevasLineas, String nuevaObservacion, Actor actor,
            Instant at) {
        var builder = new Builder();
        builder.id = id;
        builder.tenantId = tenantId;
        builder.empresaId = empresaId;
        builder.proveedorId = proveedorId;
        builder.establecimientoDestinoId = establecimientoDestinoId;
        builder.numero = numero;
        builder.fechaEmision = fechaEmision;
        builder.condiciones = new CondicionesOrden(
                condiciones.fechaEntregaEstimada(), condiciones.moneda(), condiciones.tipoCambio(),
                condiciones.condicionPago(), condiciones.diasCredito(), nuevaObservacion);
        builder.subtotal = subtotal;
        builder.descuentoTotal = descuentoTotal;
        builder.impuestoTotal = impuestoTotal;
        builder.total = total;
        builder.estado = nuevoEstado;
        builder.aprobadoPor = aprobadoPor;
        builder.aprobadoAt = aprobadoAt;
        builder.lineas = nuevasLineas;
        builder.createdAt = createdAt;
        builder.createdBy = createdBy;
        builder.updatedAt = at;
        builder.updatedBy = actor.codigo();
        return builder;
    }

    private static Optional<String> errorCondiciones(CondicionesOrden condiciones, LocalDate hoy) {
        if (!MONEDA.matcher(condiciones.moneda()).matches()) {
            return Optional.of("La moneda debe ser un codigo de 3 letras mayusculas.");
        }
        if (condiciones.tipoCambio().signum() <= 0 || Campos.decimalInvalido(
                condiciones.tipoCambio(), 6, Campos.IMPORTE_MAXIMO)) {
            return Optional.of("El tipo de cambio debe ser mayor que cero y admite hasta 6 decimales.");
        }
        if (condiciones.fechaEntregaEstimada() != null && condiciones.fechaEntregaEstimada().isBefore(hoy)) {
            return Optional.of("La fecha de entrega estimada no puede ser anterior a la fecha de emision.");
        }
        if (condiciones.diasCredito() < 0) {
            return Optional.of("Los dias de credito no pueden ser negativos.");
        }
        if (Campos.excede(condiciones.condicionPago(), 80)) {
            return Optional.of("La condicion de pago admite hasta 80 caracteres.");
        }
        if (Campos.excede(condiciones.observacion(), OBSERVACION_MAXIMA)) {
            return Optional.of("La observacion admite hasta 1500 caracteres.");
        }
        return Optional.empty();
    }

    private static Optional<String> errorCantidadLineas(List<LineaNueva> nuevas) {
        if (nuevas == null || nuevas.isEmpty() || nuevas.size() > LINEAS_MAXIMAS) {
            return Optional.of("La orden debe tener entre 1 y 200 lineas.");
        }
        return Optional.empty();
    }

    private static Result<LineaOrdenCompra, ErrorDetail> linea(int numeroLinea, LineaNueva nueva) {
        var prefijo = "Linea " + numeroLinea + ": ";
        var error = errorLinea(nueva);
        if (error.isPresent()) return invalida(prefijo + error.get());
        var bruto = LineaOrdenCompra.importeBruto(nueva.cantidad(), nueva.precioUnitario());
        var totalLinea = bruto.subtract(nueva.descuento()).add(nueva.impuesto());
        if (bruto.compareTo(Campos.IMPORTE_MAXIMO) > 0 || totalLinea.signum() < 0) {
            return invalida(prefijo + "El importe debe estar entre 0 y 10000000000 despues de descuentos e impuestos.");
        }
        return Result.success(new LineaOrdenCompra(
                numeroLinea, nueva.skuId(), nueva.descripcionSnapshot(), nueva.cantidad(), nueva.unidadMedidaCodigo(),
                nueva.precioUnitario(), nueva.descuento(), nueva.impuesto(), totalLinea, nueva.toleranciaExcesoPct(),
                nueva.toleranciaDefectoPct(), BigDecimal.ZERO));
    }

    private static Optional<String> errorLinea(LineaNueva nueva) {
        if (nueva.cantidad().signum() <= 0 || Campos.decimalInvalido(nueva.cantidad(), 4, Campos.CANTIDAD_MAXIMA)) {
            return Optional.of("La cantidad debe ser mayor que cero, admite hasta 4 decimales y no supera 1000000000.");
        }
        if (nueva.precioUnitario().signum() < 0
                || Campos.decimalInvalido(nueva.precioUnitario(), 6, Campos.IMPORTE_MAXIMO)) {
            return Optional.of("El precio unitario no puede ser negativo y admite hasta 6 decimales.");
        }
        if (nueva.descuento().signum() < 0 || Campos.decimalInvalido(nueva.descuento(), 2, Campos.IMPORTE_MAXIMO)
                || nueva.impuesto().signum() < 0 || Campos.decimalInvalido(nueva.impuesto(), 2, Campos.IMPORTE_MAXIMO)) {
            return Optional.of("El descuento y el impuesto no pueden ser negativos y admiten hasta 2 decimales.");
        }
        if (nueva.toleranciaExcesoPct().signum() < 0
                || Campos.decimalInvalido(nueva.toleranciaExcesoPct(), 4, Campos.PORCENTAJE_MAXIMO)
                || nueva.toleranciaDefectoPct().signum() < 0
                || Campos.decimalInvalido(nueva.toleranciaDefectoPct(), 4, Campos.PORCENTAJE_MAXIMO)) {
            return Optional.of("Las tolerancias deben estar entre 0 y 100 con hasta 4 decimales.");
        }
        return Optional.empty();
    }

    private static Result<List<LineaOrdenCompra>, ErrorDetail> construirLineas(List<LineaNueva> nuevas) {
        Result<List<LineaOrdenCompra>, ErrorDetail> resultado = Result.success(List.of());
        for (var indice = 0; indice < nuevas.size() && resultado.isSuccess(); indice++) {
            var numero = indice + 1;
            resultado = resultado.flatMap(previas -> linea(numero, nuevas.get(numero - 1))
                    .map(nueva -> Stream.concat(previas.stream(), Stream.of(nueva)).toList()));
        }
        return resultado;
    }

    private static BigDecimal suma(List<LineaOrdenCompra> lineas, Function<LineaOrdenCompra, BigDecimal> campo) {
        return lineas.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static <T> Result<T, ErrorDetail> invalida(String mensaje) {
        return Result.failure(new ErrorDetail(ComprasErrorCodes.ORDEN_INVALIDA, mensaje, Map.of()));
    }

    private static <T> Result<T, ErrorDetail> estadoInvalido(String mensaje) {
        return Result.failure(new ErrorDetail(ComprasErrorCodes.ORDEN_ESTADO_INVALIDO, mensaje, Map.of()));
    }

    public static final class Builder {
        public UUID id;
        public UUID tenantId;
        public UUID empresaId;
        public UUID proveedorId;
        public UUID establecimientoDestinoId;
        public String numero;
        public LocalDate fechaEmision;
        public CondicionesOrden condiciones;
        public BigDecimal subtotal;
        public BigDecimal descuentoTotal;
        public BigDecimal impuestoTotal;
        public BigDecimal total;
        public EstadoOrdenCompra estado;
        public String aprobadoPor;
        public Instant aprobadoAt;
        public List<LineaOrdenCompra> lineas;
        public Instant createdAt;
        public String createdBy;
        public Instant updatedAt;
        public String updatedBy;
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID empresaId() { return empresaId; }
    public UUID proveedorId() { return proveedorId; }
    public UUID establecimientoDestinoId() { return establecimientoDestinoId; }
    public String numero() { return numero; }
    public LocalDate fechaEmision() { return fechaEmision; }
    public CondicionesOrden condiciones() { return condiciones; }
    public String observacion() { return condiciones.observacion(); }
    public BigDecimal subtotal() { return subtotal; }
    public BigDecimal descuentoTotal() { return descuentoTotal; }
    public BigDecimal impuestoTotal() { return impuestoTotal; }
    public BigDecimal total() { return total; }
    public EstadoOrdenCompra estado() { return estado; }
    public String aprobadoPor() { return aprobadoPor; }
    public Instant aprobadoAt() { return aprobadoAt; }
    public List<LineaOrdenCompra> lineas() { return lineas; }
    public Instant createdAt() { return createdAt; }
    public String createdBy() { return createdBy; }
    public Instant updatedAt() { return updatedAt; }
    public String updatedBy() { return updatedBy; }
}
