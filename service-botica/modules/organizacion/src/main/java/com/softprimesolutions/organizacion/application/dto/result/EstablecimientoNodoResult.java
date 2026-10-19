package com.softprimesolutions.organizacion.application.dto.result;

import java.util.List;
import java.util.UUID;

public record EstablecimientoNodoResult(
        UUID id,
        String code,
        String name,
        String status,
        String timeZone,
        List<NodoResult> warehouses,
        List<NodoResult> cashRegisters) {

    public EstablecimientoNodoResult {
        warehouses = List.copyOf(warehouses);
        cashRegisters = List.copyOf(cashRegisters);
    }
}
