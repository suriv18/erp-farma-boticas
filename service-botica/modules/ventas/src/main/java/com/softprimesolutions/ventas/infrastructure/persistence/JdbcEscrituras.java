package com.softprimesolutions.ventas.infrastructure.persistence;

import com.softprimesolutions.ventas.application.port.out.GuardadoOutcome;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;

public final class JdbcEscrituras {

    private JdbcEscrituras() {
    }

    public static GuardadoOutcome guardarUnico(String restriccion, Runnable escritura) {
        try {
            escritura.run();
            return GuardadoOutcome.GUARDADO;
        } catch (DuplicateKeyException exception) {
            if (!String.valueOf(exception.getMessage()).contains(restriccion)) throw exception;
            return GuardadoOutcome.DUPLICADO;
        }
    }

    public static void exigirUnaFila(String sql, int filas) {
        if (filas != 1) throw new JdbcUpdateAffectedIncorrectNumberOfRowsException(sql, 1, filas);
    }
}
