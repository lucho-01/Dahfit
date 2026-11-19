package com.dafit.infrastructure.config;

import com.dafit.domain.exception.DomainException;
import com.dafit.domain.port.out.TokenIssuer;
import com.dafit.infrastructure.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Seguridad HTTP.
 *
 * <p>La cadena existe siempre, pero solo exige token cuando
 * {@code dafit.security.enabled=true}. Asi el frontend puede integrar el login
 * sin que el contrato actual deje de funcionar de un dia para otro, y el
 * encendido es un cambio de configuracion, no de codigo.
 *
 * <p>Cuando esta encendida: sesiones sin estado, CSRF desactivado (API con
 * token, sin cookies), 401 para peticiones sin autenticar. Publicos: login,
 * salud y documentacion.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final int MIN_SECRET_BYTES = 32;

    private static final String[] PUBLIC_PATHS = {
            "/api/auth/**",
            "/actuator/health",
            "/actuator/health/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   TokenIssuer tokens,
                                                   DafitProperties properties) throws Exception {

        DafitProperties.Security security = properties.security();
        if (security.enabled()) {
            failFastIfSecretIsWeak(security.jwtSecret());
        }

        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> {
                    // Alta de perfil y de credenciales: sin esto un usuario
                    // nuevo no puede ni crear su cuenta ni obtener su token.
                    auth.requestMatchers(HttpMethod.POST,
                            "/api/users", "/api/users/*/credentials").permitAll();
                    auth.requestMatchers(PUBLIC_PATHS).permitAll();
                    if (security.enabled()) {
                        auth.anyRequest().authenticated();
                    } else {
                        auth.anyRequest().permitAll();
                    }
                });

        if (security.enabled()) {
            http.addFilterBefore(
                    new JwtAuthenticationFilter(tokens),
                    UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private static void failFastIfSecretIsWeak(String secret) {
        if (secret == null
                || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new DomainException.Validation(
                    "dafit.security.enabled=true exige DAFIT_JWT_SECRET de al menos "
                            + MIN_SECRET_BYTES + " bytes");
        }
    }
}
