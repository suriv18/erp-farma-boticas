package com.softprimesolutions.security.application.port.in;

import com.softprimesolutions.security.application.dto.query.ListarUsuariosQuery;
import com.softprimesolutions.security.application.dto.result.PaginaResult;
import com.softprimesolutions.security.application.dto.result.UsuarioResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ListarUsuariosUseCase {
    Result<PaginaResult<UsuarioResult>, ApplicationError> execute(ListarUsuariosQuery query);
}
