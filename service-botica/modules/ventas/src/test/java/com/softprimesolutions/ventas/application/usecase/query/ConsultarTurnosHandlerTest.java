package com.softprimesolutions.ventas.application.usecase.query;

import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.turnoResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.query.ObtenerTurnoQuery;
import com.softprimesolutions.ventas.application.dto.query.TurnoActualQuery;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.port.out.VentasReadPort;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConsultarTurnosHandlerTest {

    private final VentasReadPort readPort = mock(VentasReadPort.class);
    private final ConsultarTurnosHandler handler = new ConsultarTurnosHandler(readPort);

    private static ApplicationError error(Result<TurnoResult, ApplicationError> result) {
        return result.fold(value -> { throw new AssertionError(value); }, error -> error);
    }

    @Test
    void returnsTheTurnoById() {
        when(readPort.findTurno(TENANT, TURNO)).thenReturn(Optional.of(turnoResult()));

        var result = handler.obtener(new ObtenerTurnoQuery(TENANT, TURNO));

        assertThat(result.<TurnoResult>fold(value -> value, error -> null)).isEqualTo(turnoResult());
    }

    @Test
    void aMissingTurnoIsNotFound() {
        var error = error(handler.obtener(new ObtenerTurnoQuery(TENANT, TURNO)));

        assertThat(error.code()).isEqualTo("VEN_TURNO_NO_ENCONTRADO");
        assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
    }

    @Test
    void returnsTheOpenTurnoOfATerminal() {
        when(readPort.findTurnoAbierto(TENANT, TERMINAL)).thenReturn(Optional.of(turnoResult()));

        var result = handler.actual(new TurnoActualQuery(TENANT, TERMINAL));

        assertThat(result.<TurnoResult>fold(value -> value, error -> null)).isEqualTo(turnoResult());
    }

    @Test
    void aTerminalWithoutAnOpenTurnoIsNotFound() {
        assertThat(error(handler.actual(new TurnoActualQuery(TENANT, TERMINAL))).code())
                .isEqualTo("VEN_TURNO_NO_ENCONTRADO");
    }

    @Test
    void requiresItsCollaboratorAndTheQueries() {
        assertThatNullPointerException().isThrownBy(() -> new ConsultarTurnosHandler(null));
        assertThatNullPointerException().isThrownBy(() -> handler.obtener(null));
        assertThatNullPointerException().isThrownBy(() -> handler.actual(null));
    }
}
