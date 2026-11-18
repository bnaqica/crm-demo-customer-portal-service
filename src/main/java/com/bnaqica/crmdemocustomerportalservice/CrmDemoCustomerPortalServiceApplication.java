package com.bnaqica.crmdemocustomerportalservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CrmDemoCustomerPortalServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrmDemoCustomerPortalServiceApplication.class, args);
    }
}
