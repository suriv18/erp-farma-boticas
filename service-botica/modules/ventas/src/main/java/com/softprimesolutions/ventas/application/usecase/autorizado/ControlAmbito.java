package com.softprimesolutions.ventas.application.usecase.autorizado;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.application.port.out.AmbitoPort;
import com.softprimesolutions.ventas.application.port.out.AmbitoPort.Ambito;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ControlAmbito {

    private final AmbitoPort ambito;

    public ControlAmbito(AmbitoPort ambito) {
        this.ambito = Objects.requireNonNull(ambito, "ambito es obligatorio");
    }

    <T> Result<T, ApplicationError> antes(Ambito solicitado, Supplier<Result<T, ApplicationError>> operacion) {
        return ambito.permite(solicitado) ? operacion.get() : Result.failure(VentasErrors.fueraDeAmbito());
    }

    <T> Result<T, ApplicationError> despues(
            Result<T, ApplicationError> resultado, Function<T, Ambito> solicitado) {
        return resultado.flatMap(valor -> antes(solicitado.apply(valor), () -> Result.success(valor)));
    }
}
