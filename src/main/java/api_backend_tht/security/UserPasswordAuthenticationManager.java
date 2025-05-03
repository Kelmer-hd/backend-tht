package api_backend_tht.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component("userPasswordAuthenticationManager")
public class UserPasswordAuthenticationManager implements ReactiveAuthenticationManager {

    private static final Logger logger = LoggerFactory.getLogger(UserPasswordAuthenticationManager.class);

    private final PasswordEncoder passwordEncoder;
    private final ReactiveUserDetailsService userDetailsService;

    @Autowired
    public UserPasswordAuthenticationManager(PasswordEncoder passwordEncoder,
                                             ReactiveUserDetailsService userDetailsService) {
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        logger.debug("Intentando autenticar usuario: {}", username);

        return userDetailsService.findByUsername(username)
                .doOnNext(userDetails -> {
                    logger.debug("Usuario encontrado: {}", userDetails.getUsername());
                    logger.debug("Contraseña almacenada (hash): {}", userDetails.getPassword());

                    // Imprime valores para depuración
                    boolean matches = passwordEncoder.matches(password, userDetails.getPassword());
                    logger.debug("¿Coinciden contraseñas?: {}", matches);
                })
                .filter(userDetails -> {
                    // Realizar comparación de contraseñas de forma segura
                    boolean matches = passwordEncoder.matches(password, userDetails.getPassword());
                    if (!matches) {
                        logger.debug("Contraseña incorrecta para usuario: {}", username);
                    }
                    return matches;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    logger.debug("Usuario no encontrado o credenciales inválidas: {}", username);
                    return Mono.error(new BadCredentialsException("Credenciales inválidas"));
                }))
                .map(userDetails -> {
                    logger.debug("Autenticación exitosa para usuario: {}", username);
                    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    // Aquí está el cambio clave: hacemos un cast explícito a Authentication
                    return (Authentication) token;
                })
                .doOnError(error -> {
                    logger.error("Error durante autenticación: {}", error.getMessage());
                });
    }
}