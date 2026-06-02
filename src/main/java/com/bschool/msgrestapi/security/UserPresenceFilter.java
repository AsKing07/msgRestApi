package com.bschool.msgrestapi.security;

import com.bschool.msgrestapi.service.UserPresenceService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class UserPresenceFilter extends OncePerRequestFilter {

    private final UserPresenceService userPresenceService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Number userId = jwtAuth.getToken().getClaim("userId");
            if (userId != null) {
                userPresenceService.markActive(userId.longValue());
            }
        }

        filterChain.doFilter(request, response);
    }
}
