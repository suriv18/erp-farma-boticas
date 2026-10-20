package com.softprimesolutions.organizacion.application.usecase.query;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerMiPuestoQuery;
import com.softprimesolutions.organizacion.application.dto.result.AlcancePuesto;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoPuestoResult;
import com.softprimesolutions.organizacion.application.dto.result.MiPuestoResult;
import com.softprimesolutions.organizacion.application.port.in.ObtenerMiPuestoUseCase;
import com.softprimesolutions.organizacion.application.port.out.AlcancePuestoPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ObtenerMiPuestoHandler implements ObtenerMiPuestoUseCase {

    private final OrganizacionReadPort readPort;
    private final AlcancePuestoPort alcancePort;

    public ObtenerMiPuestoHandler(OrganizacionReadPort readPort, AlcancePuestoPort alcancePort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
        this.alcancePort = Objects.requireNonNull(alcancePort, "alcancePort es obligatorio");
    }

    @Override
    public Result<MiPuestoResult, ApplicationError> execute(ObtenerMiPuestoQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        var alcance = alcancePort.alcanceDe(query.tenantId(), query.usuarioId());
        var visibles = readPort.findEstablecimientosPuesto(query.tenantId()).stream()
                .filter(establecimiento -> esVisible(alcance, establecimiento))
                .map(establecimiento -> recortar(alcance, establecimiento))
                .toList();
        return Result.success(new MiPuestoResult(visibles));
    }

    private static boolean cubreEstablecimiento(AlcancePuesto alcance, EstablecimientoPuestoResult establecimiento) {
        return alcance.global()
                || alcance.empresas().contains(establecimiento.empresaId())
                || alcance.establecimientos().contains(establecimiento.id());
    }

    private static boolean esVisible(AlcancePuesto alcance, EstablecimientoPuestoResult establecimiento) {
        return cubreEstablecimiento(alcance, establecimiento)
                || establecimiento.almacenes().stream().anyMatch(almacen -> alcance.almacenes().contains(almacen.id()))
                || establecimiento.terminales().stream().anyMatch(terminal -> alcance.terminales().contains(terminal.id()));
    }

    private static EstablecimientoPuestoResult recortar(AlcancePuesto alcance, EstablecimientoPuestoResult establecimiento) {
        if (cubreEstablecimiento(alcance, establecimiento)) {
            return establecimiento;
        }
        return new EstablecimientoPuestoResult(
                establecimiento.id(), establecimiento.empresaId(), establecimiento.codigo(), establecimiento.nombre(),
                establecimiento.almacenes().stream()
                        .filter(almacen -> alcance.almacenes().contains(almacen.id()))
                        .toList(),
                establecimiento.terminales().stream()
                        .filter(terminal -> alcance.terminales().contains(terminal.id()))
                        .toList());
    }
}
