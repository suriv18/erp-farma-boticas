package com.softprimesolutions.security.persistence;

import com.softprimesolutions.testsupport.PostgresTestContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@SpringBootTest
class ProductionSchemaValidationIntegrationTest {

    @Test
    void loadsJpaMappingsUsingCanonicalSchemaNames() {
    }
}
