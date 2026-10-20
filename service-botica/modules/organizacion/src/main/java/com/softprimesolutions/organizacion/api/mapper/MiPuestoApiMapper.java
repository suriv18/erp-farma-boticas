package com.softprimesolutions.organizacion.api.mapper;

import com.softprimesolutions.organizacion.api.dto.response.MiPuestoResponse;
import com.softprimesolutions.organizacion.application.dto.result.MiPuestoResult;

public final class MiPuestoApiMapper {

    private MiPuestoApiMapper() {
    }

    public static MiPuestoResponse toResponse(MiPuestoResult result) {
        return new MiPuestoResponse(result.establecimientos().stream()
                .map(establecimiento -> new MiPuestoResponse.EstablecimientoPuesto(
                        establecimiento.id(), establecimiento.codigo(), establecimiento.nombre(),
                        establecimiento.almacenes().stream()
                                .map(almacen -> new MiPuestoResponse.AlmacenPuesto(
                                        almacen.id(), almacen.codigo(), almacen.nombre(), almacen.tipo(),
                                        almacen.permiteVenta()))
                                .toList(),
                        establecimiento.terminales().stream()
                                .map(terminal -> new MiPuestoResponse.TerminalPuesto(
                                        terminal.id(), terminal.codigo(), terminal.nombre(), terminal.estado(),
                                        terminal.operable()))
                                .toList()))
                .toList());
    }
}
