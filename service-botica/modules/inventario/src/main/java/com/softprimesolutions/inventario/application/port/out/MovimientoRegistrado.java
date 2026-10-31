package com.softprimesolutions.inventario.application.port.out;

import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;

public record MovimientoRegistrado(MovimientoResult resultado, String huella) {
}
