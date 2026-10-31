package com.softprimesolutions.organizacion.api.dto.response;

import java.util.UUID;

public record NodoResponse(
        UUID id,
        String code,
        String name,
        String status) {
}
