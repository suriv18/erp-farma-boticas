package com.softprimesolutions.ventas.application.port.out;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.function.Supplier;

@FunctionalInterface
public interface TransaccionPort {

    <T> Result<T, ApplicationError> ejecutar(Supplier<Result<T, ApplicationError>> trabajo);
}
