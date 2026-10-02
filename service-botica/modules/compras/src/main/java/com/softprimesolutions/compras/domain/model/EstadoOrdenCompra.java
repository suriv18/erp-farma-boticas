package com.softprimesolutions.compras.domain.model;

import java.util.Arrays;
import java.util.Optional;

public enum EstadoOrdenCompra {
    BORRADOR(true, false, true, false),
    EN_APROBACION(true, false, true, false),
    APROBADA(false, true, true, false),
    EMITIDA(false, false, true, true),
    PARCIALMENTE_RECIBIDA(false, false, false, true),
    RECIBIDA(false, false, false, false),
    CANCELADA(false, false, false, false),
    CERRADA(false, false, false, false);

    private final boolean aprobable;
    private final boolean emitible;
    private final boolean anulable;
    private final boolean recepcionable;

    EstadoOrdenCompra(boolean aprobable, boolean emitible, boolean anulable, boolean recepcionable) {
        this.aprobable = aprobable;
        this.emitible = emitible;
        this.anulable = anulable;
        this.recepcionable = recepcionable;
    }

    public static Optional<EstadoOrdenCompra> desde(String valor) {
        return Arrays.stream(values()).filter(estado -> estado.name().equals(valor)).findFirst();
    }

    public boolean aprobable() { return aprobable; }
    public boolean emitible() { return emitible; }
    public boolean anulable() { return anulable; }
    public boolean recepcionable() { return recepcionable; }
}
