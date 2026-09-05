package com.softprimesolutions.shared.persistence.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** Configuración transversal de transacciones; no contiene repositorios de negocio. */
@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
public class PersistenceConfiguration {
}
