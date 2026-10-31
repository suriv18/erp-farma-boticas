package com.softprimesolutions.compras.application.usecase.query;

import com.softprimesolutions.compras.application.error.ComprasErrors;
import com.softprimesolutions.shared.application.error.ApplicationError;
import java.util.Optional;

final class Paginacion {

    private static final int TAMANO_MAXIMO = 100;

    private Paginacion() {
    }

    static Optional<ApplicationError> invalida(int page, int size) {
        if (page < 0 || size < 1 || size > TAMANO_MAXIMO) {
            return Optional.of(ComprasErrors.paginacionInvalida(page, size));
        }
        return Optional.empty();
    }
}
