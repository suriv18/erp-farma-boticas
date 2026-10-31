package com.softprimesolutions.security.api;

import java.util.Set;
import java.util.UUID;

public record AlcanceResultado(
        boolean global,
        Set<UUID> empresas,
        Set<UUID> establecimientos,
        Set<UUID> almacenes,
        Set<UUID> terminales) {

    public AlcanceResultado {
        empresas = Set.copyOf(empresas);
        establecimientos = Set.copyOf(establecimientos);
        almacenes = Set.copyOf(almacenes);
        terminales = Set.copyOf(terminales);
    }
}
