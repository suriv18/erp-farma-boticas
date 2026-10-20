package com.softprimesolutions.organizacion.infrastructure.client;

import com.softprimesolutions.organizacion.application.dto.result.AlcancePuesto;
import com.softprimesolutions.organizacion.application.port.out.AlcancePuestoPort;
import com.softprimesolutions.security.api.AlcanceUsuario;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SecurityAlcanceAdapter implements AlcancePuestoPort {

    private final AlcanceUsuario alcanceUsuario;

    public SecurityAlcanceAdapter(AlcanceUsuario alcanceUsuario) {
        this.alcanceUsuario = alcanceUsuario;
    }

    @Override
    public AlcancePuesto alcanceDe(UUID tenantId, UUID usuarioId) {
        var alcance = alcanceUsuario.alcanceDe(tenantId, usuarioId);
        return new AlcancePuesto(
                alcance.global(), alcance.empresas(), alcance.establecimientos(), alcance.almacenes(),
                alcance.terminales());
    }
}
