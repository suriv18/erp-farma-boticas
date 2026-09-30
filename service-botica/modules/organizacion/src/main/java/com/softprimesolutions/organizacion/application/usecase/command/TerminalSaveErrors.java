package com.softprimesolutions.organizacion.application.usecase.command;

import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort.SaveTerminalOutcome;
import com.softprimesolutions.organizacion.domain.model.TerminalPos;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import java.util.Optional;

final class TerminalSaveErrors {

    private TerminalSaveErrors() {
    }

    static Optional<ApplicationError> conflict(SaveTerminalOutcome outcome, TerminalPos terminal) {
        return switch (outcome) {
            case DUPLICATE_SERIE_BOLETA -> Optional.of(
                    serieDuplicada("ORG_TERMINAL_SERIE_BOLETA_DUPLICADA", terminal.serieBoletaDefecto()));
            case DUPLICATE_SERIE_FACTURA -> Optional.of(
                    serieDuplicada("ORG_TERMINAL_SERIE_FACTURA_DUPLICADA", terminal.serieFacturaDefecto()));
            case INTEGRITY_VIOLATION -> Optional.of(notSaved());
            default -> Optional.empty();
        };
    }

    static ApplicationError notSaved() {
        return new StandardApplicationError(
                "ORG_TERMINAL_NO_GUARDADO",
                "No se pudo guardar el terminal: los datos violan una restricción de la base de datos.",
                ErrorCategory.CONFLICT);
    }

    private static ApplicationError serieDuplicada(String code, String serie) {
        return new StandardApplicationError(
                code, "La serie " + serie + " ya está asignada a otra caja de esta empresa.",
                ErrorCategory.CONFLICT);
    }
}
