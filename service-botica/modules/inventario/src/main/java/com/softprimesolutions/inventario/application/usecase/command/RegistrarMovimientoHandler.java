package com.softprimesolutions.inventario.application.usecase.command;

import com.softprimesolutions.inventario.application.dto.command.RegistrarMovimientoCommand;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.inventario.application.error.InventarioErrors;
import com.softprimesolutions.inventario.application.mapper.InventarioApplicationMapper;
import com.softprimesolutions.inventario.application.port.in.RegistrarMovimientoUseCase;
import com.softprimesolutions.inventario.application.port.out.InventarioWritePort;
import com.softprimesolutions.inventario.application.port.out.InventarioWritePort.RegistroOutcome;
import com.softprimesolutions.inventario.application.port.out.MovimientoRegistrado;
import com.softprimesolutions.inventario.application.port.out.ReferenciasInventarioPort;
import com.softprimesolutions.inventario.application.port.out.ReferenciasInventarioPort.EstadoReferencia;
import com.softprimesolutions.inventario.application.port.out.RegistroMovimiento;
import com.softprimesolutions.inventario.domain.model.AplicacionMovimiento;
import com.softprimesolutions.inventario.domain.model.Lote;
import com.softprimesolutions.inventario.domain.model.PosicionInventario;
import com.softprimesolutions.inventario.domain.model.TipoMovimiento;
import com.softprimesolutions.inventario.domain.valueobject.Actor;
import com.softprimesolutions.inventario.domain.valueobject.LoteId;
import com.softprimesolutions.inventario.domain.valueobject.PosicionId;
import com.softprimesolutions.inventario.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.result.Result;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class RegistrarMovimientoHandler implements RegistrarMovimientoUseCase {

    private static final int MAX_INTENTOS = 3;
    private static final int MOTIVO_MAX = 1000;
    private static final int CLAVE_IDEMPOTENCIA_MAX = 200;

    private final InventarioWritePort writePort;
    private final ReferenciasInventarioPort referencias;
    private final IdentifierGenerator identifiers;
    private final ClockPort clock;

    public RegistrarMovimientoHandler(
            InventarioWritePort writePort, ReferenciasInventarioPort referencias,
            IdentifierGenerator identifiers, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.referencias = Objects.requireNonNull(referencias, "referencias es obligatorio");
        this.identifiers = Objects.requireNonNull(identifiers, "identifiers es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<MovimientoResult, ApplicationError> execute(RegistrarMovimientoCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var tipo = TipoMovimiento.desde(command.tipo()).filter(valor -> valor.manual() == (command.origen() == null));
        if (tipo.isEmpty()) return Result.failure(InventarioErrors.tipoMovimientoInvalido());
        var motivo = motivoNormalizado(command.motivo());
        if (motivo == null) return Result.failure(InventarioErrors.motivoMovimientoInvalido());
        if (claveIdempotenciaInvalida(command.idempotencyKey())) {
            return Result.failure(InventarioErrors.claveIdempotenciaInvalida());
        }
        if (referenciaLoteInvalida(command, tipo.get())) {
            return Result.failure(InventarioErrors.referenciaLoteInvalida());
        }
        var errorReferencia = errorDe(
                        referencias.estadoAlmacen(command.tenantId(), command.almacenId()),
                        InventarioErrors.almacenNoEncontrado(), InventarioErrors.almacenNoOperable())
                .or(() -> errorDe(
                        referencias.estadoSku(command.tenantId(), command.skuId()),
                        InventarioErrors.skuNoEncontrado(), InventarioErrors.skuNoOperable()));
        if (errorReferencia.isPresent()) return Result.failure(errorReferencia.get());
        return conReintentos(new Solicitud(command, tipo.get(), motivo, businessUuid(command), huella(command, motivo)));
    }

    private Result<MovimientoResult, ApplicationError> conReintentos(Solicitud solicitud) {
        var resultado = intentar(solicitud);
        for (var intento = 1; intento < MAX_INTENTOS && esConcurrencia(resultado); intento++) {
            resultado = intentar(solicitud);
        }
        return resultado;
    }

    private Result<MovimientoResult, ApplicationError> intentar(Solicitud solicitud) {
        var previo = Optional.ofNullable(solicitud.businessUuid()).flatMap(
                clave -> writePort.findMovimientoPorBusinessUuid(solicitud.command().tenantId(), clave));
        if (previo.isPresent()) return repetir(previo.get(), solicitud.huella());
        var ahora = clock.now();
        var hoy = LocalDate.ofInstant(ahora, ZoneOffset.UTC);
        return resolverLote(solicitud.command(), hoy, ahora)
                .flatMap(lote -> aplicar(solicitud, lote, ahora));
    }

    private static Result<MovimientoResult, ApplicationError> repetir(MovimientoRegistrado previo, String huella) {
        if (!previo.huella().equals(huella)) return Result.failure(InventarioErrors.conflictoIdempotencia());
        return Result.success(previo.resultado());
    }

    private Result<LoteResuelto, ApplicationError> resolverLote(
            RegistrarMovimientoCommand command, LocalDate hoy, Instant ahora) {
        if (command.loteId() != null) {
            return writePort.findLote(command.tenantId(), command.loteId())
                    .map(lote -> mismoSku(lote, command))
                    .orElseGet(() -> Result.failure(InventarioErrors.loteNoEncontrado()));
        }
        return writePort.findLotePorClave(
                        command.tenantId(), command.skuId(), command.numeroLote().trim(), command.fechaVencimiento())
                .<Result<LoteResuelto, ApplicationError>>map(lote -> Result.success(new LoteResuelto(lote, false)))
                .orElseGet(() -> crearLote(command, hoy, ahora));
    }

    private static Result<LoteResuelto, ApplicationError> mismoSku(Lote lote, RegistrarMovimientoCommand command) {
        if (!lote.skuId().equals(command.skuId())) return Result.failure(InventarioErrors.loteSkuDistinto());
        return Result.success(new LoteResuelto(lote, false));
    }

    private Result<LoteResuelto, ApplicationError> crearLote(
            RegistrarMovimientoCommand command, LocalDate hoy, Instant ahora) {
        return Lote.create(
                        new LoteId(identifiers.next()), new TenantId(command.tenantId()), command.skuId(),
                        command.numeroLote(), command.fechaVencimiento(), hoy, ahora)
                .fold(
                        lote -> Result.<LoteResuelto, ApplicationError>success(new LoteResuelto(lote, true)),
                        error -> Result.failure(InventarioErrors.fromDomain(error)));
    }

    private Result<MovimientoResult, ApplicationError> aplicar(
            Solicitud solicitud, LoteResuelto resuelto, Instant ahora) {
        var command = solicitud.command();
        if (solicitud.tipo().ingreso() && !resuelto.lote().estado().admiteIngreso()) {
            return Result.failure(InventarioErrors.loteNoAdmiteIngreso());
        }
        var existente = writePort.findPosicion(
                command.tenantId(), command.almacenId(), resuelto.lote().id().value());
        var posicion = existente.orElseGet(() -> PosicionInventario.nueva(
                new PosicionId(identifiers.next()), resuelto.lote().id().value(), command.almacenId(),
                command.skuId()));
        return posicion.aplicar(solicitud.tipo(), command.cantidad()).fold(
                aplicacion -> registrar(solicitud, resuelto, existente.isEmpty(), aplicacion, ahora),
                error -> Result.failure(InventarioErrors.fromDomain(error)));
    }

    private Result<MovimientoResult, ApplicationError> registrar(
            Solicitud solicitud, LoteResuelto resuelto, boolean posicionNueva, AplicacionMovimiento aplicacion,
            Instant ahora) {
        var command = solicitud.command();
        var registro = new RegistroMovimiento(
                identifiers.next(), new TenantId(command.tenantId()), command.almacenId(), command.skuId(),
                resuelto.lote(), resuelto.nuevo(), aplicacion.posicion(), posicionNueva, solicitud.tipo(),
                command.cantidad(), aplicacion.stockAnterior(), aplicacion.stockPosterior(), solicitud.motivo(),
                new Actor(command.actorId()), ahora, solicitud.businessUuid(), solicitud.huella(), command.origen());
        if (writePort.registrar(registro) == RegistroOutcome.MODIFICACION_CONCURRENTE) {
            return Result.failure(InventarioErrors.modificacionConcurrente());
        }
        return Result.success(InventarioApplicationMapper.toResult(registro));
    }

    private static boolean esConcurrencia(Result<MovimientoResult, ApplicationError> resultado) {
        return resultado.fold(
                movimiento -> false, error -> InventarioErrors.CONCURRENCIA.equals(error.code()));
    }

    private static Optional<ApplicationError> errorDe(
            EstadoReferencia estado, ApplicationError inexistente, ApplicationError noOperable) {
        return switch (estado) {
            case INEXISTENTE -> Optional.of(inexistente);
            case NO_OPERABLE -> Optional.of(noOperable);
            case OPERABLE -> Optional.empty();
        };
    }

    private static boolean referenciaLoteInvalida(RegistrarMovimientoCommand command, TipoMovimiento tipo) {
        return command.loteId() == null
                && (!tipo.ingreso() || vacio(command.numeroLote()) || command.fechaVencimiento() == null);
    }

    private static boolean claveIdempotenciaInvalida(String clave) {
        return clave != null && (clave.isBlank() || clave.length() > CLAVE_IDEMPOTENCIA_MAX);
    }

    private static UUID businessUuid(RegistrarMovimientoCommand command) {
        return Optional.ofNullable(command.idempotencyKey())
                .map(clave -> UUID.nameUUIDFromBytes(("idempotency:" + clave.trim()).getBytes(StandardCharsets.UTF_8)))
                .orElse(null);
    }

    private static String huella(RegistrarMovimientoCommand command, String motivo) {
        var cantidad = Optional.ofNullable(command.cantidad())
                .map(valor -> valor.stripTrailingZeros().toPlainString()).orElse("");
        var numeroLote = Optional.ofNullable(command.numeroLote()).map(String::trim).orElse("");
        var canonico = String.join("|",
                text(command.almacenId()), text(command.skuId()), text(command.loteId()), numeroLote,
                text(command.fechaVencimiento()), text(command.tipo()), cantidad, motivo, text(command.actorId()),
                text(command.origen()));
        return UUID.nameUUIDFromBytes(canonico.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String text(Object valor) {
        return Objects.toString(valor, "");
    }

    private static String motivoNormalizado(String motivo) {
        var normalizado = motivo == null ? null : motivo.trim();
        return normalizado == null || normalizado.isEmpty() || normalizado.length() > MOTIVO_MAX
                ? null
                : normalizado;
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private record Solicitud(
            RegistrarMovimientoCommand command, TipoMovimiento tipo, String motivo, UUID businessUuid,
            String huella) {
    }

    private record LoteResuelto(Lote lote, boolean nuevo) {
    }
}
