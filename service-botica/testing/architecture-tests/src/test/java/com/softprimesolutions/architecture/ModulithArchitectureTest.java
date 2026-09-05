package com.softprimesolutions.architecture;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.ServiceBoticaApplication;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithArchitectureTest {

    private static final List<String> EXPECTED_MODULES = List.of(
            "app-channel", "catalogo", "clientes", "cmr", "compras", "crm", "farmacia",
            "finanzas", "integraciones", "inventario", "logistica", "notificaciones",
            "organizacion", "pagos", "rrhh", "security", "ventas");

    @Test
    void verifiesModuleBoundariesAndCycles() {
        modules().verify();
    }

    @Test
    void detectsEveryExpectedBusinessModule() {
        var modules = modules();
        var detectedCount = StreamSupport.stream(modules.spliterator(), false).count();

        assertEquals(EXPECTED_MODULES.size(), detectedCount);
        assertAll(EXPECTED_MODULES.stream()
                .map(name -> () -> assertTrue(
                        modules.getModuleByName(name).isPresent(),
                        () -> "No se detectó el módulo " + name)));
    }

    private static ApplicationModules modules() {
        return ApplicationModules.of(ServiceBoticaApplication.class);
    }
}
