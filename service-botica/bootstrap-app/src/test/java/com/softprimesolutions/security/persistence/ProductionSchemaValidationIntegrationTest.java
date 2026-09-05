package com.softprimesolutions.security.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create"
})
class ProductionSchemaValidationIntegrationTest {

    @Test
    void loadsJpaMappingsUsingCanonicalSchemaNames() {
    }
}
