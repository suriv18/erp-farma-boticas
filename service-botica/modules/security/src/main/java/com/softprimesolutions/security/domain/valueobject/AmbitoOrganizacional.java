package com.softprimesolutions.security.domain.valueobject;

import java.util.UUID;

/** Ámbito expresado exclusivamente con identificadores públicos. */
public record AmbitoOrganizacional(
        TipoAmbito type,
        UUID companyId,
        UUID establishmentId,
        UUID warehouseId,
        UUID terminalId) {
}
