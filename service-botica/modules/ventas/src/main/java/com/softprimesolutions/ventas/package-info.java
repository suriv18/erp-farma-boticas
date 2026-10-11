@org.springframework.modulith.ApplicationModule(
        id = "ventas",
        displayName = "Ventas",
        allowedDependencies = {"inventario::api", "organizacion::api", "security::api"})
package com.softprimesolutions.ventas;
