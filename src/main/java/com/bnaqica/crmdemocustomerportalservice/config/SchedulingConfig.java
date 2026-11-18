package com.bnaqica.crmdemocustomerportalservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on {@code @Scheduled} jobs such as the outbox relay. */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
