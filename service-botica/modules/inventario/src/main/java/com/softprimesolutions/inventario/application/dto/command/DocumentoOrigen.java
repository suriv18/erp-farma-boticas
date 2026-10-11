package com.softprimesolutions.inventario.application.dto.command;

import java.util.Objects;
import java.util.UUID;

public record DocumentoOrigen(String tipo, UUID documentoId, UUID lineaId, UUID proveedorId) {

    public DocumentoOrigen {
        Objects.requireNonNull(tipo, "tipo es obligatorio");
        Objects.requireNonNull(documentoId, "documentoId es obligatorio");
        Objects.requireNonNull(lineaId, "lineaId es obligatorio");
    }
}
