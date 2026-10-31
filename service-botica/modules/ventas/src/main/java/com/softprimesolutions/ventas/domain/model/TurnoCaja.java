package com.softprimesolutions.ventas.domain.model;

import static com.softprimesolutions.ventas.domain.model.Failures.failure;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public final class TurnoCaja {

    private static final int OBSERVACION_MAXIMA = 1000;

    private final UUID id;
    private final UUID tenantId;
    private final UUID terminalId;
    private final UUID establecimientoId;
    private final Actor cajero;
    private final Instant aperturaAt;
    private final BigDecimal fondoInicial;
    private final EstadoTurno estado;
    private final Instant cierreAt;
    private final BigDecimal totalVentasSistema;
    private final BigDecimal totalSistema;
    private final BigDecimal totalDeclarado;
    private final BigDecimal diferencia;
    private final String observacionCierre;

    private TurnoCaja(
            UUID id, UUID tenantId, UUID terminalId, UUID establecimientoId, Actor cajero, Instant aperturaAt,
            BigDecimal fondoInicial, EstadoTurno estado, Instant cierreAt, BigDecimal totalVentasSistema,
            BigDecimal totalSistema, BigDecimal totalDeclarado, BigDecimal diferencia, String observacionCierre) {
        this.id = Objects.requireNonNull(id, "id es obligatorio");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId es obligatorio");
        this.terminalId = Objects.requireNonNull(terminalId, "terminalId es obligatorio");
        this.establecimientoId = Objects.requireNonNull(establecimientoId, "establecimientoId es obligatorio");
        this.cajero = Objects.requireNonNull(cajero, "cajero es obligatorio");
        this.aperturaAt = Objects.requireNonNull(aperturaAt, "aperturaAt es obligatorio");
        this.fondoInicial = Objects.requireNonNull(fondoInicial, "fondoInicial es obligatorio");
        this.estado = Objects.requireNonNull(estado, "estado es obligatorio");
        this.cierreAt = cierreAt;
        this.totalVentasSistema = Objects.requireNonNull(totalVentasSistema, "totalVentasSistema es obligatorio");
        this.totalSistema = Objects.requireNonNull(totalSistema, "totalSistema es obligatorio");
        this.totalDeclarado = totalDeclarado;
        this.diferencia = diferencia;
        this.observacionCierre = observacionCierre;
    }

    public static Result<TurnoCaja, ErrorDetail> abrir(
            UUID id, UUID tenantId, UUID terminalId, UUID establecimientoId, Actor cajero, BigDecimal fondoInicial,
            Instant ahora) {
        if (!Importes.montoValido(fondoInicial)) {
            return failure(VentasErrorCodes.MONTO_INVALIDO,
                    "El fondo inicial debe estar entre 0 y 1000000000 con hasta 2 decimales.");
        }
        var fondo = Importes.redondear(fondoInicial);
        return Result.success(new TurnoCaja(
                id, tenantId, terminalId, establecimientoId, cajero, ahora, fondo, EstadoTurno.ABIERTO, null,
                Importes.redondear(BigDecimal.ZERO), fondo, null, null, null));
    }

    public static TurnoCaja restore(
            UUID id, UUID tenantId, UUID terminalId, UUID establecimientoId, Actor cajero, Instant aperturaAt,
            BigDecimal fondoInicial, EstadoTurno estado, Instant cierreAt, BigDecimal totalVentasSistema,
            BigDecimal totalSistema, BigDecimal totalDeclarado, BigDecimal diferencia, String observacionCierre) {
        return new TurnoCaja(
                id, tenantId, terminalId, establecimientoId, cajero, aperturaAt, fondoInicial, estado, cierreAt,
                totalVentasSistema, totalSistema, totalDeclarado, diferencia, observacionCierre);
    }

    public Result<TurnoCaja, ErrorDetail> cerrar(
            BigDecimal totalVentas, BigDecimal declarado, String observacion, Instant ahora) {
        if (estado != EstadoTurno.ABIERTO) {
            return failure(VentasErrorCodes.TURNO_ESTADO_INVALIDO, "Solo se puede cerrar un turno abierto.");
        }
        if (!Importes.montoValido(declarado)) {
            return failure(VentasErrorCodes.MONTO_INVALIDO,
                    "El total declarado debe estar entre 0 y 1000000000 con hasta 2 decimales.");
        }
        var nota = Optional.ofNullable(observacion).map(String::trim).filter(Predicate.not(String::isEmpty));
        if (nota.filter(valor -> valor.length() > OBSERVACION_MAXIMA).isPresent()) {
            return failure(VentasErrorCodes.OBSERVACION_INVALIDA,
                    "La observacion de cierre admite hasta 1000 caracteres.");
        }
        var ventas = Importes.redondear(totalVentas);
        var sistema = fondoInicial.add(ventas);
        var declaradoRedondeado = Importes.redondear(declarado);
        return Result.success(new TurnoCaja(
                id, tenantId, terminalId, establecimientoId, cajero, aperturaAt, fondoInicial, EstadoTurno.CERRADO,
                ahora, ventas, sistema, declaradoRedondeado, declaradoRedondeado.subtract(sistema),
                nota.orElse(null)));
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID terminalId() { return terminalId; }
    public UUID establecimientoId() { return establecimientoId; }
    public Actor cajero() { return cajero; }
    public Instant aperturaAt() { return aperturaAt; }
    public BigDecimal fondoInicial() { return fondoInicial; }
    public EstadoTurno estado() { return estado; }
    public Instant cierreAt() { return cierreAt; }
    public BigDecimal totalVentasSistema() { return totalVentasSistema; }
    public BigDecimal totalSistema() { return totalSistema; }
    public BigDecimal totalDeclarado() { return totalDeclarado; }
    public BigDecimal diferencia() { return diferencia; }
    public String observacionCierre() { return observacionCierre; }
}
