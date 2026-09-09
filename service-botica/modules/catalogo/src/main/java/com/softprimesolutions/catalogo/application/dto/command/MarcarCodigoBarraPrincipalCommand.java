package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.SkuResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.util.UUID;

public record MarcarCodigoBarraPrincipalCommand(UUID tenantId, UUID skuId, String codigoBarra)
        implements Command<SkuResult> {
}
