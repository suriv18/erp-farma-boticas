package com.softprimesolutions.organizacion.application.port.out;

import com.softprimesolutions.organizacion.application.dto.result.AlmacenResult;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.organizacion.application.dto.result.EstructuraCorporativaResult;
import com.softprimesolutions.organizacion.application.dto.result.PaginaResult;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPosResult;
import java.util.Optional;
import java.util.UUID;

public interface OrganizacionReadPort {

    PaginaResult<EmpresaOperadoraResult> findEmpresas(UUID tenantId, String search, int page, int size);

    Optional<EmpresaOperadoraResult> findEmpresaById(UUID tenantId, UUID empresaId);

    PaginaResult<EstablecimientoResult> findEstablecimientos(
            UUID tenantId, UUID empresaId, String search, int page, int size);

    Optional<EstablecimientoResult> findEstablecimientoById(UUID tenantId, UUID establecimientoId);

    PaginaResult<AlmacenResult> findAlmacenes(
            UUID tenantId, UUID establecimientoId, String search, int page, int size);

    Optional<AlmacenResult> findAlmacenById(UUID tenantId, UUID almacenId);

    PaginaResult<TerminalPosResult> findTerminales(
            UUID tenantId, UUID establecimientoId, String search, int page, int size);

    Optional<TerminalPosResult> findTerminalById(UUID tenantId, UUID terminalId);

    EstructuraCorporativaResult findEstructuraCorporativa(UUID tenantId);
}
