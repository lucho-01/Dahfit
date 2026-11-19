package com.dafit.infrastructure.security;

import com.dafit.domain.port.out.TokenIssuer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lee {@code Authorization: Bearer <jwt>} y deja el usuario en el contexto.
 *
 * <p>Un token invalido no autentica y deja seguir la cadena: la decision de
 * responder 401 es de la configuracion de autorizacion, un solo sitio.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final TokenIssuer tokens;

    public JwtAuthenticationFilter(TokenIssuer tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIX)) {
            tokens.verify(header.substring(PREFIX.length())).ifPresent(token -> {
                var authentication = new UsernamePasswordAuthenticationToken(
                        token.userId(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }

        filterChain.doFilter(request, response);
    }
}
