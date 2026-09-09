package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.SkuResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.UUID;

public record ConsultarSkuQuery(UUID tenantId, UUID skuId) implements Query<SkuResult> {
}
