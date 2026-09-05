@org.springframework.modulith.ApplicationModule(
        id = "ventas",
        displayName = "Ventas",
        allowedDependencies = {
            "clientes::api", "inventario::api", "organizacion::api", "pagos::api"
        })
package com.softprimesolutions.ventas;
