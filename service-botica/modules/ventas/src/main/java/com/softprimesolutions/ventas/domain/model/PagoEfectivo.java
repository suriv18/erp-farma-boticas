package com.softprimesolutions.ventas.domain.model;

import static com.softprimesolutions.ventas.domain.model.Failures.failure;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import java.math.BigDecimal;

public record PagoEfectivo(BigDecimal monto, BigDecimal montoRecibido, BigDecimal vuelto) {

    public static Result<PagoEfectivo, ErrorDetail> cobrar(BigDecimal total, BigDecimal recibido) {
        if (!Importes.montoValido(recibido)) {
            return failure(VentasErrorCodes.MONTO_INVALIDO,
                    "El monto recibido debe estar entre 0 y 1000000000 con hasta 2 decimales.");
        }
        var recibidoRedondeado = Importes.redondear(recibido);
        if (recibidoRedondeado.compareTo(total) < 0) {
            return failure(VentasErrorCodes.MONTO_RECIBIDO_INSUFICIENTE,
                    "El monto recibido no cubre el total de la venta.");
        }
        return Result.success(new PagoEfectivo(total, recibidoRedondeado, recibidoRedondeado.subtract(total)));
    }
}
