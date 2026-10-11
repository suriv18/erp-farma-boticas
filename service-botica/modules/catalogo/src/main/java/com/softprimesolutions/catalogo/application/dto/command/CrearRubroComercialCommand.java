package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.util.UUID;

public record CrearRubroComercialCommand(
        UUID tenantId, String codigo, String nombre, String descripcion, boolean esFarmaceutico, int orden)
        implements Command<RubroComercialResult> {
}
