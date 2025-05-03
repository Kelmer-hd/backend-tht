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

@Component
public class AuthenticationManager implements ReactiveAuthenticationManager {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String authToken = authentication.getCredentials().toString();

        try {
            if (!jwtUtil.validateToken(authToken)) {
                return Mono.empty();
            }

            Claims claims = jwtUtil.getAllClaimsFromToken(authToken);
            List<String> rolesMap = claims.get("roles", List.class);

            List<SimpleGrantedAuthority> authorities = rolesMap.stream()
                    .map(role -> new SimpleGrantedAuthority(role))
                    .collect(Collectors.toList());

            return Mono.just(new UsernamePasswordAuthenticationToken(
                    claims.getSubject(),
                    null,
                    authorities
            ));
        } catch (Exception e) {
            return Mono.empty();
        }
    }
}