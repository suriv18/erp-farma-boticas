package com.softprimesolutions.organizacion.application.dto.result;

import java.util.UUID;

public record NodoResult(UUID id, String code, String name, String status) {
}
