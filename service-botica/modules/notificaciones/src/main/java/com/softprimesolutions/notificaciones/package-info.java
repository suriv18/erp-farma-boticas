@org.springframework.modulith.ApplicationModule(
        id = "notificaciones",
        displayName = "Notificaciones",
        allowedDependencies = {
            "clientes::api", "compras::api", "finanzas::api", "inventario::api",
            "logistica::api", "pagos::api", "security::api", "ventas::api"
        })
package com.softprimesolutions.notificaciones;
