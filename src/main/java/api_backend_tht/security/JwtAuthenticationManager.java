package api_backend_tht.security;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gestor de autenticación para validar tokens JWT.
 * Implementa el flujo de autenticación reactive para Spring WebFlux.
 */
@Component("jwtAuthenticationManager")
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationManager.class);

    private final JwtUtil jwtUtil;

    @Autowired
    public JwtAuthenticationManager(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String authToken = authentication.getCredentials().toString();

        return Mono.just(authToken)
                .flatMap(token -> {
                    try {
                        if (!jwtUtil.validateToken(token)) {
                            logger.debug("Token JWT inválido o expirado");
                            return Mono.empty();
                        }

                        Claims claims = jwtUtil.getAllClaimsFromToken(token);
                        String username = claims.getSubject();
                        List<String> roles = claims.get("roles", List.class);

                        List<SimpleGrantedAuthority> authorities = roles.stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList());

                        logger.debug("Token JWT válido para usuario: {}", username);

                        return Mono.just(new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                authorities
                        ));
                    } catch (Exception e) {
                        logger.error("Error al procesar token JWT", e);
                        return Mono.empty();
                    }
                });
    }
}