package com.softprimesolutions.organizacion.api.dto.response;

import java.util.List;
import java.util.UUID;

public record EstablecimientoNodoResponse(
        UUID id,
        String code,
        String name,
        String status,
        String timeZone,
        List<NodoResponse> warehouses,
        List<NodoResponse> cashRegisters) {

    public EstablecimientoNodoResponse {
        warehouses = List.copyOf(warehouses);
        cashRegisters = List.copyOf(cashRegisters);
    }
}
