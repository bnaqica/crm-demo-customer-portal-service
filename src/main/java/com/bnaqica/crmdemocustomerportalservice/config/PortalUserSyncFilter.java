package com.bnaqica.crmdemocustomerportalservice.config;

import com.bnaqica.crmdemocustomerportalservice.service.PortalUserSyncService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs after authorization in the security filter chain, so only callers who passed every portal access rule are
 * recorded. Syncs the portal user on each call: creates them on the first, updates a changed email on later ones.
 * Not a bean, so the servlet container doesn't also register it outside the security chain.
 */
@RequiredArgsConstructor
class PortalUserSyncFilter extends OncePerRequestFilter {

    private final PortalUserSyncService syncService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwt) {
            syncService.sync(PortalPrincipals.from(jwt.getToken()));
        }
        chain.doFilter(request, response);
    }
}
