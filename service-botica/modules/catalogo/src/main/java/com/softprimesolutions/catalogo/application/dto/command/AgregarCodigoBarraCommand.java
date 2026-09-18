package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.SkuResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.time.LocalDate;
import java.util.UUID;

public record AgregarCodigoBarraCommand(
        UUID tenantId, UUID skuId, String codigoBarra, String tipoCodigo, LocalDate vigenteDesde,
        LocalDate vigenteHasta) implements Command<SkuResult> {
}
