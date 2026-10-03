package com.softprimesolutions.ventas.application.usecase.command;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.LineaVentaInput;
import com.softprimesolutions.ventas.application.dto.command.RegistrarVentaCommand;
import com.softprimesolutions.ventas.application.dto.query.ObtenerVentaQuery;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.application.port.in.ConsultarVentasUseCase;
import com.softprimesolutions.ventas.application.port.in.RegistrarVentaUseCase;
import com.softprimesolutions.ventas.application.port.out.GuardadoOutcome;
import com.softprimesolutions.ventas.application.port.out.NumeracionPort;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort.AlmacenRef;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort.SkuVentaRef;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort.TerminalRef;
import com.softprimesolutions.ventas.application.port.out.SalidaInventarioPort;
import com.softprimesolutions.ventas.application.port.out.SalidaInventarioPort.SalidaSolicitada;
import com.softprimesolutions.ventas.application.port.out.TransaccionPort;
import com.softprimesolutions.ventas.application.port.out.TurnoWritePort;
import com.softprimesolutions.ventas.application.port.out.VentaWritePort;
import com.softprimesolutions.ventas.application.port.out.VentaWritePort.VentaExistente;
import com.softprimesolutions.ventas.domain.model.LineaVenta;
import com.softprimesolutions.ventas.domain.model.LoteConsumo;
import com.softprimesolutions.ventas.domain.model.TurnoCaja;
import com.softprimesolutions.ventas.domain.model.Venta;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class RegistrarVentaHandler implements RegistrarVentaUseCase {

    private static final int MAX_INTENTOS = 3;
    private static final int CLAVE_IDEMPOTENCIA_MAX = 160;

    private final VentaWritePort ventas;
    private final TurnoWritePort turnos;
    private final ReferenciasVentasPort referencias;
    private final SalidaInventarioPort inventario;
    private final NumeracionPort numeracion;
    private final ConsultarVentasUseCase consultas;
    private final TransaccionPort transaccion;
    private final IdentifierGenerator identifiers;
    private final ClockPort clock;

    public RegistrarVentaHandler(
            VentaWritePort ventas, TurnoWritePort turnos, ReferenciasVentasPort referencias,
            SalidaInventarioPort inventario, NumeracionPort numeracion, ConsultarVentasUseCase consultas,
            TransaccionPort transaccion, IdentifierGenerator identifiers, ClockPort clock) {
        this.ventas = Objects.requireNonNull(ventas, "ventas es obligatorio");
        this.turnos = Objects.requireNonNull(turnos, "turnos es obligatorio");
        this.referencias = Objects.requireNonNull(referencias, "referencias es obligatorio");
        this.inventario = Objects.requireNonNull(inventario, "inventario es obligatorio");
        this.numeracion = Objects.requireNonNull(numeracion, "numeracion es obligatorio");
        this.consultas = Objects.requireNonNull(consultas, "consultas es obligatorio");
        this.transaccion = Objects.requireNonNull(transaccion, "transaccion es obligatorio");
        this.identifiers = Objects.requireNonNull(identifiers, "identifiers es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<VentaResult, ApplicationError> execute(RegistrarVentaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        if (claveInvalida(command.idempotencyKey())) {
            return Result.failure(VentasErrors.claveIdempotenciaInvalida());
        }
        var solicitud = new Solicitud(command, huella(command));
        var resultado = intentar(solicitud);
        for (var intento = 1; intento < MAX_INTENTOS && esConcurrencia(resultado); intento++) {
            resultado = intentar(solicitud);
        }
        return resultado;
    }

    private Result<VentaResult, ApplicationError> intentar(Solicitud solicitud) {
        var command = solicitud.command();
        var previa = ventas.findPorIdempotencia(command.tenantId(), command.idempotencyKey());
        if (previa.isPresent()) return repetir(command, previa.get(), solicitud.huella());
        return transaccion.ejecutar(() -> registrar(solicitud))
                .flatMap(ventaId -> consultas.obtener(new ObtenerVentaQuery(command.tenantId(), ventaId)));
    }

    private Result<VentaResult, ApplicationError> repetir(
            RegistrarVentaCommand command, VentaExistente previa, String huella) {
        if (!previa.huella().equals(huella)) return Result.failure(VentasErrors.conflictoIdempotencia());
        return consultas.obtener(new ObtenerVentaQuery(command.tenantId(), previa.id()));
    }

    private Result<UUID, ApplicationError> registrar(Solicitud solicitud) {
        var command = solicitud.command();
        return validarContexto(command)
                .flatMap(contexto -> construir(command, contexto))
                .flatMap(venta -> persistir(solicitud, venta));
    }

    private Result<Contexto, ApplicationError> validarContexto(RegistrarVentaCommand command) {
        var terminal = referencias.terminal(command.tenantId(), command.terminalId());
        if (terminal.isEmpty()) return Result.failure(VentasErrors.terminalNoEncontrada());
        if (!terminal.get().operable()) return Result.failure(VentasErrors.terminalNoOperable());
        var turno = turnos.bloquearTurnoAbierto(command.tenantId(), command.terminalId());
        if (turno.isEmpty()) return Result.failure(VentasErrors.noHayTurnoAbierto());
        var almacen = referencias.almacen(command.tenantId(), command.almacenId());
        if (almacen.isEmpty()) return Result.failure(VentasErrors.almacenNoEncontrado());
        var errorAlmacen = errorDeAlmacen(almacen.get(), terminal.get());
        if (errorAlmacen.isPresent()) return Result.failure(errorAlmacen.get());
        var skus = referencias.skus(
                command.tenantId(),
                command.lineas().stream().map(LineaVentaInput::skuId).collect(Collectors.toSet()));
        var errorSku = command.lineas().stream()
                .map(linea -> errorDeSku(linea.skuId(), skus.get(linea.skuId())))
                .flatMap(Optional::stream)
                .findFirst();
        if (errorSku.isPresent()) return Result.failure(errorSku.get());
        return Result.success(new Contexto(terminal.get(), turno.get(), skus));
    }

    private static Optional<ApplicationError> errorDeAlmacen(AlmacenRef almacen, TerminalRef terminal) {
        if (!almacen.operable()) return Optional.of(VentasErrors.almacenNoOperable());
        if (!almacen.establecimientoId().equals(terminal.establecimientoId())) {
            return Optional.of(VentasErrors.almacenDeOtroEstablecimiento());
        }
        return Optional.empty();
    }

    private static Optional<ApplicationError> errorDeSku(UUID skuId, SkuVentaRef sku) {
        if (sku == null) return Optional.of(VentasErrors.skuNoEncontrado(skuId));
        if (!sku.operable()) return Optional.of(VentasErrors.skuNoOperable(skuId));
        return Optional.empty();
    }

    private Result<Venta, ApplicationError> construir(RegistrarVentaCommand command, Contexto contexto) {
        return lineas(command, contexto.skus()).flatMap(lineas -> VentasErrors.fromDomain(Venta.registrar(
                identifiers.next(), command.tenantId(), contexto.terminal().id(), contexto.turno().id(),
                contexto.terminal().establecimientoId(), new Actor(command.actorId()),
                numeracion.siguienteNumeroOperacion(
                        command.tenantId(), contexto.terminal().id(), contexto.terminal().codigo()),
                clock.now(), lineas, command.montoRecibido())));
    }

    private Result<List<LineaVenta>, ApplicationError> lineas(
            RegistrarVentaCommand command, Map<UUID, SkuVentaRef> skus) {
        Result<List<LineaVenta>, ApplicationError> acumuladas = Result.success(List.of());
        for (var indice = 0; indice < command.lineas().size(); indice++) {
            var entrada = command.lineas().get(indice);
            var numero = indice + 1;
            acumuladas = acumuladas.flatMap(previas -> linea(numero, entrada, skus.get(entrada.skuId()))
                    .map(nueva -> Stream.concat(previas.stream(), Stream.of(nueva)).toList()));
        }
        return acumuladas;
    }

    private Result<LineaVenta, ApplicationError> linea(int numero, LineaVentaInput entrada, SkuVentaRef sku) {
        return VentasErrors.fromDomain(LineaVenta.nueva(
                identifiers.next(), numero, entrada.skuId(), sku.descripcion(), sku.unidadVentaCodigo(),
                sku.permiteFraccion(), entrada.cantidad(), entrada.precioUnitario()));
    }

    private Result<UUID, ApplicationError> persistir(Solicitud solicitud, Venta venta) {
        var command = solicitud.command();
        if (ventas.insertar(venta, command.idempotencyKey(), solicitud.huella()) == GuardadoOutcome.DUPLICADO) {
            return Result.failure(VentasErrors.modificacionConcurrente());
        }
        Result<UUID, ApplicationError> acumulado = Result.success(venta.id());
        for (var linea : enOrdenDeBloqueo(venta)) {
            acumulado = acumulado.flatMap(ventaId -> descontar(command, venta, linea).map(lotes -> ventaId));
        }
        return acumulado;
    }

    private static List<LineaVenta> enOrdenDeBloqueo(Venta venta) {
        return venta.lineas().stream()
                .sorted(Comparator.comparing(LineaVenta::skuId).thenComparingInt(LineaVenta::numeroLinea))
                .toList();
    }

    private Result<List<LoteConsumo>, ApplicationError> descontar(
            RegistrarVentaCommand command, Venta venta, LineaVenta linea) {
        return inventario.descontar(new SalidaSolicitada(
                        command.tenantId(), command.almacenId(), linea.skuId(), linea.cantidad(), venta.id(),
                        linea.id(), command.actorId(), command.idempotencyKey() + ":" + linea.numeroLinea()))
                .map(lotes -> {
                    ventas.registrarLotes(command.tenantId(), linea.id(), lotes);
                    return lotes;
                });
    }

    private static boolean esConcurrencia(Result<VentaResult, ApplicationError> resultado) {
        return resultado.fold(venta -> false, error -> VentasErrors.CONCURRENCIA.equals(error.code()));
    }

    private static boolean claveInvalida(String clave) {
        return clave == null || clave.isBlank() || clave.length() > CLAVE_IDEMPOTENCIA_MAX;
    }

    private static String huella(RegistrarVentaCommand command) {
        var lineas = command.lineas().stream()
                .map(linea -> String.join(":", text(linea.skuId()), plano(linea.cantidad()), plano(linea.precioUnitario())))
                .collect(Collectors.joining(","));
        var canonico = String.join("|",
                text(command.terminalId()), text(command.almacenId()), lineas, plano(command.montoRecibido()),
                text(command.actorId()));
        return UUID.nameUUIDFromBytes(canonico.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String plano(BigDecimal valor) {
        return Optional.ofNullable(valor).map(numero -> numero.stripTrailingZeros().toPlainString()).orElse("");
    }

    private static String text(Object valor) {
        return Objects.toString(valor, "");
    }

    private record Contexto(TerminalRef terminal, TurnoCaja turno, Map<UUID, SkuVentaRef> skus) {
    }

    private record Solicitud(RegistrarVentaCommand command, String huella) {
    }
}
