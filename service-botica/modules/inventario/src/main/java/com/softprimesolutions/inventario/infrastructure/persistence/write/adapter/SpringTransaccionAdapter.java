package com.softprimesolutions.inventario.infrastructure.persistence.write.adapter;

import com.softprimesolutions.inventario.application.port.out.TransaccionPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionOperations;

public final class SpringTransaccionAdapter implements TransaccionPort {

    private final TransactionOperations transaction;

    public SpringTransaccionAdapter(TransactionOperations transaction) {
        this.transaction = Objects.requireNonNull(transaction, "transaction es obligatorio");
    }

    @Override
    public <T> Result<T, ApplicationError> ejecutar(Supplier<Result<T, ApplicationError>> trabajo) {
        return transaction.execute(status -> {
            var resultado = trabajo.get();
            if (resultado.isFailure()) status.setRollbackOnly();
            return resultado;
        });
    }
}
