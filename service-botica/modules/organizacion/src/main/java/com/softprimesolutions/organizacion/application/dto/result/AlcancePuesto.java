package com.softprimesolutions.organizacion.application.dto.result;

import java.util.Set;
import java.util.UUID;

public record AlcancePuesto(
        boolean global, Set<UUID> empresas, Set<UUID> establecimientos, Set<UUID> almacenes, Set<UUID> terminales) {

    public AlcancePuesto {
        empresas = Set.copyOf(empresas);
        establecimientos = Set.copyOf(establecimientos);
        almacenes = Set.copyOf(almacenes);
        terminales = Set.copyOf(terminales);
    }
}
