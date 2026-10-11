package com.softprimesolutions.inventario.application.mapper;

import com.softprimesolutions.inventario.application.dto.result.LoteResult;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.inventario.application.port.out.RegistroMovimiento;
import com.softprimesolutions.inventario.domain.model.Lote;
import java.time.LocalDate;

public final class InventarioApplicationMapper {

    private InventarioApplicationMapper() {
    }

    public static LoteResult toResult(Lote lote, LocalDate hoy) {
        return new LoteResult(
                lote.id().value(), lote.skuId(), lote.numeroLote(), lote.fechaVencimiento(),
                lote.estado().name(), lote.motivoEstado(), lote.bloqueadoAt(), lote.vendible(hoy));
    }

    public static MovimientoResult toResult(RegistroMovimiento registro) {
        return new MovimientoResult(
                registro.movimientoId(), registro.posicion().id().value(), registro.lote().id().value(),
                registro.tipo().name(), registro.tipo().naturaleza(), registro.cantidad(),
                registro.stockAnterior(), registro.stockPosterior(), registro.fechaNegocio());
    }
}
