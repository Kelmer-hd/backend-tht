package api_backend_tht.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuración de seguridad para la aplicación.
 * Define políticas de seguridad, autenticación y autorización.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final ReactiveAuthenticationManager jwtAuthManager;
    private final ServerSecurityContextRepository securityContextRepository;

    @Autowired
    public SecurityConfig(
            @Qualifier("jwtAuthenticationManager") ReactiveAuthenticationManager jwtAuthManager,
            ServerSecurityContextRepository securityContextRepository) {
        this.jwtAuthManager = jwtAuthManager;
        this.securityContextRepository = securityContextRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(Arrays.asList(
                "http://localhost:4200",    // Angular dev server
                "http://127.0.0.1:4200"     // Angular usando IP literal
        ));
        corsConfig.setMaxAge(3600L);
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        corsConfig.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        corsConfig.setExposedHeaders(Arrays.asList("Authorization"));
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        logger.info("Configuración CORS inicializada");

        return new CorsWebFilter(source);
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((swe, e) -> {
                            logger.error("Error de autenticación: {}", e.getMessage());
                            return Mono.fromRunnable(() ->
                                    swe.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED));
                        })
                        .accessDeniedHandler((swe, e) -> {
                            logger.error("Acceso denegado: {}", e.getMessage());
                            return Mono.fromRunnable(() ->
                                    swe.getResponse().setStatusCode(HttpStatus.FORBIDDEN));
                        })
                )
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .authenticationManager(jwtAuthManager)
                .securityContextRepository(securityContextRepository)
                .authorizeExchange(authorizeExchange -> authorizeExchange
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers("/api/auth/**").permitAll()
                        .pathMatchers("/api/public/**").permitAll()
                        .pathMatchers("/api/admin/**").hasRole("ADMIN")
                        .pathMatchers("/api/mod/**").hasAnyRole("MODERATOR", "ADMIN")
                        .pathMatchers("/api/user/**").hasAnyRole("USER", "MODERATOR", "ADMIN")
                        .anyExchange().authenticated()
                )
                .build();
    }
}