package com.bnaqica.crmdemocustomerportalservice.config;

import static org.springframework.security.config.Customizer.withDefaults;

import com.bnaqica.crmdemocustomerportalservice.service.PortalUserSyncService;
import jakarta.servlet.DispatcherType;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Every endpoint needs a valid JWT from the {@code portal} realm ({@code JWT_ISSUER_URI}), except the health check.
 * A token from any other realm, such as {@code crm-internal} or {@code crm-admin}, fails issuer validation and gets
 * 401. Realm roles become authorities through {@link PortalRoles}; {@link PortalAccessAuthorizationManager} then
 * allows only verified users with the role the endpoint needs (deny by default), and {@link PortalUserSyncFilter}
 * records each allowed caller. CSRF protection is off
 * because requests carry a bearer token and there are no sessions or cookies.
 */
@Configuration
@EnableMethodSecurity
// Also binds CorsProperties in test slices, which don't run @ConfigurationPropertiesScan
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

    @Bean
    @Profile("!local")
    SecurityFilterChain securedFilterChain(
            HttpSecurity http,
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
            PortalUserSyncService syncService)
            throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(withDefaults()) // uses the corsConfigurationSource bean below
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests.requestMatchers("/actuator/health/**")
                        .permitAll()
                        // An error inside the chain is re-dispatched to /error; the original request was already
                        // authorized, and denying the dispatch would turn a starter's 500 into 403
                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        .anyRequest()
                        .access(new PortalAccessAuthorizationManager(handlerMapping)))
                .addFilterAfter(new PortalUserSyncFilter(syncService), AuthorizationFilter.class)
                .oauth2ResourceServer(
                        server -> server.jwt(jwt -> jwt.jwtAuthenticationConverter(PortalRoles.converter())))
                .build();
    }

    /** Lets the browser origins in {@code PORTAL_CORS_ALLOWED_ORIGINS} call the API with a bearer token. */
    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(properties.allowedOrigins());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-Id"));
        cors.setMaxAge(Duration.ofHours(1));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    /** No authentication in the {@code local} profile, so the API and Swagger UI work without an identity provider. */
    @Bean
    @Profile("local")
    SecurityFilterChain localFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(withDefaults())
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .build();
    }
}
