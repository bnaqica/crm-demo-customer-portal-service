package com.bnaqica.crmdemocustomerportalservice.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Decides every authenticated request: the token must carry a verified email, and the caller needs
 * {@code external-standard}, or {@code external-starter} when the endpoint is a {@link StarterFeature}. Anything
 * else, including a path with no endpoint, is denied (403).
 */
@RequiredArgsConstructor
class PortalAccessAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final RequestMappingHandlerMapping handlerMapping;

    @Override
    public AuthorizationResult authorize(
            Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (!(auth instanceof JwtAuthenticationToken jwt)
                || !Boolean.TRUE.equals(jwt.getToken().getClaimAsBoolean("email_verified"))
                || !StringUtils.hasText(jwt.getToken().getClaimAsString("email"))) {
            return new AuthorizationDecision(false);
        }
        Set<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        if (roles.contains(PortalRoles.STANDARD)) {
            return new AuthorizationDecision(true);
        }
        return new AuthorizationDecision(roles.contains(PortalRoles.STARTER) && isStarterFeature(context.getRequest()));
    }

    private boolean isStarterFeature(HttpServletRequest request) {
        HandlerExecutionChain chain;
        try {
            chain = handlerMapping.getHandler(request);
        } catch (Exception ex) {
            return false; // no endpoint matches, for example the wrong HTTP method
        }
        return chain != null
                && chain.getHandler() instanceof HandlerMethod method
                && (AnnotatedElementUtils.hasAnnotation(method.getMethod(), StarterFeature.class)
                        || AnnotatedElementUtils.hasAnnotation(method.getBeanType(), StarterFeature.class));
    }
}
