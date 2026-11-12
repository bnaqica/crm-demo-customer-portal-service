package com.bnaqica.crmdemocustomerportalservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CrmDemoCustomerPortalServiceApplicationTests {

    /** Fails if Flyway can't migrate the database or the application can't start against it. */
    @Test
    void contextLoads() {}
}
