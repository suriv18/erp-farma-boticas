package com.softprimesolutions;

import com.softprimesolutions.testsupport.PostgresTestContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@SpringBootTest
class ServiceBoticaApplicationTests {

    @Test
    void contextLoads() {
    }
}
