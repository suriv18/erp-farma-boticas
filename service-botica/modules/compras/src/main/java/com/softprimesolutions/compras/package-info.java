@org.springframework.modulith.ApplicationModule(
        id = "compras",
        displayName = "Compras",
        allowedDependencies = {"catalogo::api", "inventario::api", "organizacion::api"})
package com.softprimesolutions.compras;
