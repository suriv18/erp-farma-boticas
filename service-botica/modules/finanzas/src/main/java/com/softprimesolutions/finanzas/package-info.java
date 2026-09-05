@org.springframework.modulith.ApplicationModule(
        id = "finanzas",
        displayName = "Finanzas",
        allowedDependencies = {
            "clientes::api", "compras::api", "organizacion::api", "ventas::api"
        })
package com.softprimesolutions.finanzas;
