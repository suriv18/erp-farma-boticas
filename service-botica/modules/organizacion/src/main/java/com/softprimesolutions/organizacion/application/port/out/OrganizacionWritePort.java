package com.softprimesolutions.organizacion.application.port.out;

import com.softprimesolutions.organizacion.domain.model.Almacen;
import com.softprimesolutions.organizacion.domain.model.EmpresaOperadora;
import com.softprimesolutions.organizacion.domain.model.Establecimiento;
import com.softprimesolutions.organizacion.domain.model.TerminalPos;
import java.util.UUID;

public interface OrganizacionWritePort {

    SaveEmpresaOutcome save(EmpresaOperadora empresa);

    SaveEstablecimientoOutcome save(Establecimiento establecimiento);

    SaveAlmacenOutcome save(Almacen almacen);

    SaveTerminalOutcome save(TerminalPos terminal);

    boolean tenantExists(UUID tenantId);

    boolean empresaBelongsToTenant(UUID empresaId, UUID tenantId);

    boolean establecimientoBelongsToTenant(UUID establecimientoId, UUID tenantId);

    enum SaveEmpresaOutcome { CREATED, UPDATED, TENANT_NOT_FOUND, DUPLICATE_RUC }

    enum SaveEstablecimientoOutcome {
        CREATED, UPDATED, EMPRESA_NOT_FOUND, EMPRESA_NO_OPERATIVA, DUPLICATE_CODIGO, DUPLICATE_DIGEMID
    }

    enum SaveAlmacenOutcome {
        CREATED, UPDATED, ESTABLECIMIENTO_NOT_FOUND, ESTABLECIMIENTO_NO_OPERATIVO, DUPLICATE_CODIGO
    }

    enum SaveTerminalOutcome {
        CREATED, UPDATED, ESTABLECIMIENTO_NOT_FOUND, ESTABLECIMIENTO_NO_OPERATIVO, DUPLICATE_CODIGO
    }
}
