package com.bnaqica.crmdemocustomerportalservice.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an endpoint (or every endpoint of a controller) as a starter feature, open to {@code external-starter} and
 * {@code external-standard}. Endpoints without it are standard features, open to {@code external-standard} only.
 * {@link PortalAccessAuthorizationManager} enforces this.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface StarterFeature {}
