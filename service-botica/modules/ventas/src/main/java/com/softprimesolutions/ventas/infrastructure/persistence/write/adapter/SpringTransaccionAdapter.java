package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.port.out.TransaccionPort;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

@Component("ventasTransaccionAdapter")
public class SpringTransaccionAdapter implements TransaccionPort {

    private final TransactionOperations transaction;

    public SpringTransaccionAdapter(TransactionOperations transaction) {
        this.transaction = transaction;
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
