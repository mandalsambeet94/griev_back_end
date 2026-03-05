package com.grievance.security;

import com.grievance.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (
                path.equals("/api/auth/login") ||
                        path.equals("/api/auth/register") ||
                        path.startsWith("/swagger-ui") ||
                        path.startsWith("/v3/api-docs") ||
                        path.equals("/hello")
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {

            final String userContact = jwtService.extractUsername(jwt);

            if (userContact != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails =
                        this.userDetailsService.loadUserByUsername(userContact);

                if (jwtService.validateToken(jwt, userDetails)) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities());

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request));

                    SecurityContextHolder.getContext()
                            .setAuthentication(authToken);
                }
            }

            filterChain.doFilter(request, response);

        } catch (io.jsonwebtoken.ExpiredJwtException ex) {

            sendErrorResponse(response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "TOKEN_EXPIRED",
                    "JWT token has expired. Please login again.");

        } catch (Exception ex) {

            sendErrorResponse(response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "INVALID_TOKEN",
                    "Invalid JWT token.");

        }
    }

    private void sendErrorResponse(HttpServletResponse response,
                                   int status,
                                   String error,
                                   String message) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");

        String json = String.format("""
            {
              "timestamp": "%s",
              "status": %d,
              "error": "%s",
              "message": "%s"
            }
            """,
                java.time.LocalDateTime.now(),
                status,
                error,
                message
        );

        response.getWriter().write(json);
    }

}
