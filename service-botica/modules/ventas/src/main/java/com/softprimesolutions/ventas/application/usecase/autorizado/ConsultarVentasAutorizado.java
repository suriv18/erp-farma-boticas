package com.softprimesolutions.ventas.application.usecase.autorizado;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.query.ListarVentasQuery;
import com.softprimesolutions.ventas.application.dto.query.ObtenerVentaQuery;
import com.softprimesolutions.ventas.application.dto.result.PaginaResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResumenResult;
import com.softprimesolutions.ventas.application.port.in.ConsultarVentasUseCase;
import com.softprimesolutions.ventas.application.port.out.ActorActualPort;
import com.softprimesolutions.ventas.application.port.out.AmbitoPort.Ambito;
import java.util.Objects;

public final class ConsultarVentasAutorizado implements ConsultarVentasUseCase {

    static final String PERMISO = "ventas.ventas.consultar";

    private final ConsultarVentasUseCase delegado;
    private final ActorActualPort actor;
    private final ControlAmbito control;

    public ConsultarVentasAutorizado(
            ConsultarVentasUseCase delegado, ActorActualPort actor, ControlAmbito control) {
        this.delegado = Objects.requireNonNull(delegado, "delegado es obligatorio");
        this.actor = Objects.requireNonNull(actor, "actor es obligatorio");
        this.control = Objects.requireNonNull(control, "control es obligatorio");
    }

    @Override
    public Result<VentaResult, ApplicationError> obtener(ObtenerVentaQuery query) {
        return control.despues(
                delegado.obtener(query),
                venta -> new Ambito(
                        query.tenantId(), actor.actorId(), PERMISO, venta.establecimientoId(), null,
                        venta.terminalId()));
    }

    @Override
    public Result<PaginaResult<VentaResumenResult>, ApplicationError> listar(ListarVentasQuery query) {
        return control.antes(
                new Ambito(query.tenantId(), actor.actorId(), PERMISO, query.establecimientoId(), null, null),
                () -> delegado.listar(query));
    }
}
