package com.softprimesolutions.shared.application.cqrs;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface QueryHandler<Q extends Query<R>, R> {

    Result<R, ApplicationError> handle(Q query);
}
