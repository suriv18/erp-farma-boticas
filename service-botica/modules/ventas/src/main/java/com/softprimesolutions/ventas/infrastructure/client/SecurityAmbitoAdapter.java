package com.softprimesolutions.ventas.infrastructure.client;

import com.softprimesolutions.security.api.AutorizacionAmbito;
import com.softprimesolutions.security.api.RecursoAmbito;
import com.softprimesolutions.ventas.application.port.out.AmbitoPort;
import org.springframework.stereotype.Component;

@Component
public class SecurityAmbitoAdapter implements AmbitoPort {

    private final AutorizacionAmbito autorizacion;

    public SecurityAmbitoAdapter(AutorizacionAmbito autorizacion) {
        this.autorizacion = autorizacion;
    }

    @Override
    public boolean permite(Ambito ambito) {
        return autorizacion.permite(
                ambito.tenantId(), ambito.actorId(), ambito.permiso(),
                new RecursoAmbito(ambito.establecimientoId(), ambito.almacenId(), ambito.terminalId()));
    }
}
