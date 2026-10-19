package com.softprimesolutions.ventas.application.usecase.autorizado;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.port.out.AmbitoPort;
import java.util.ArrayList;
import java.util.List;

final class AmbitoDouble implements AmbitoPort {

    private final boolean permitido;
    private final List<Ambito> consultas = new ArrayList<>();

    AmbitoDouble(boolean permitido) {
        this.permitido = permitido;
    }

    @Override
    public boolean permite(Ambito ambito) {
        consultas.add(ambito);
        return permitido;
    }

    List<Ambito> consultas() {
        return consultas;
    }

    static ApplicationError error(Result<?, ApplicationError> result) {
        return result.fold(value -> { throw new AssertionError(value); }, error -> error);
    }
}
