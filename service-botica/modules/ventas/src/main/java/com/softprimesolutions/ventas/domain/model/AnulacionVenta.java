package com.softprimesolutions.ventas.domain.model;

import static com.softprimesolutions.ventas.domain.model.Failures.failure;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import java.util.Optional;
import java.util.function.Predicate;

public final class AnulacionVenta {

    private static final int MOTIVO_MAXIMO = 500;

    private AnulacionVenta() {
    }

    public static Result<String, ErrorDetail> validar(
            EstadoVenta estadoVenta, EstadoTurno estadoTurno, String motivo) {
        var normalizado = Optional.ofNullable(motivo).map(String::trim).filter(Predicate.not(String::isEmpty));
        if (normalizado.filter(valor -> valor.length() <= MOTIVO_MAXIMO).isEmpty()) {
            return failure(VentasErrorCodes.MOTIVO_INVALIDO, "El motivo de anulacion debe tener entre 1 y 500 caracteres.");
        }
        if (estadoVenta != EstadoVenta.CONFIRMADA) {
            return failure(VentasErrorCodes.VENTA_ESTADO_INVALIDO, "Solo se puede anular una venta confirmada.");
        }
        if (estadoTurno != EstadoTurno.ABIERTO) {
            return failure(VentasErrorCodes.TURNO_NO_ABIERTO, "Solo se puede anular una venta mientras su turno esta abierto.");
        }
        return Result.success(normalizado.get());
    }
}
