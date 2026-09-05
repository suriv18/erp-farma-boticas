package com.softprimesolutions;

import org.springframework.boot.SpringApplication;
import org.springframework.modulith.Modulith;

/**
 * Único punto de ensamblaje y ejecución del backend ERP Botica.
 */
@Modulith(systemName = "ERP Botica")
public class ServiceBoticaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceBoticaApplication.class, args);
    }
}
