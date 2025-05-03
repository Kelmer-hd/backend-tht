package api_backend_tht.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repositorio del contexto de seguridad para solicitudes HTTP.
 * Extrae y valida el token JWT de las solicitudes entrantes.
 */
@Component
public class SecurityContextRepository implements ServerSecurityContextRepository {

    private static final Logger logger = LoggerFactory.getLogger(SecurityContextRepository.class);

    private final ReactiveAuthenticationManager authenticationManager;

    @Autowired
    public SecurityContextRepository(@Qualifier("jwtAuthenticationManager") ReactiveAuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        // No es necesario para autenticación basada en JWT
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String authToken = authHeader.substring(7);

            Authentication auth = new UsernamePasswordAuthenticationToken(authToken, authToken);

            return this.authenticationManager.authenticate(auth)
                    .doOnNext(authentication ->
                            logger.debug("Contexto de seguridad cargado para usuario: {}",
                                    authentication.getName()))
                    .map(SecurityContextImpl::new);
        }

        return Mono.empty();
    }
}