package api_backend_tht.controller;

import api_backend_tht.model.dto.AuthRequest;
import api_backend_tht.model.dto.AuthResponse;
import api_backend_tht.model.dto.SignupRequest;
import api_backend_tht.model.entity.User;
import api_backend_tht.model.entity.UserRole;
import api_backend_tht.repository.RoleRepository;
import api_backend_tht.repository.UserRepository;
import api_backend_tht.repository.UserRoleRepository;
import api_backend_tht.security.JwtUtil;
import api_backend_tht.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controlador para operaciones de autenticación.
 * Proporciona endpoints para inicio de sesión y registro de usuarios.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final ReactiveAuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthController(
            @Qualifier("userPasswordAuthenticationManager") ReactiveAuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/signin")
    public Mono<ResponseEntity<AuthResponse>> authenticateUser(@RequestBody AuthRequest loginRequest) {
        logger.info("Solicitud de inicio de sesión para usuario: {}", loginRequest.getUsername());

        // Crear autenticación con nombre de usuario y contraseña
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(),
                loginRequest.getPassword()
        );

        // Autenticar usando el userPasswordAuthenticationManager
        return authenticationManager.authenticate(authentication)
                .map(auth -> {
                    UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();

                    List<String> roles = userDetails.getAuthorities().stream()
                            .map(item -> item.getAuthority().replace("ROLE_", ""))
                            .collect(Collectors.toList());

                    String jwt = jwtUtil.generateToken(userDetails, roles);

                    logger.info("Inicio de sesión exitoso para usuario: {}", userDetails.getUsername());

                    return ResponseEntity.ok(AuthResponse.builder()
                            .token(jwt)
                            .id(userDetails.getId())
                            .username(userDetails.getUsername())
                            .email(userDetails.getEmail())
                            .roles(roles)
                            .build());
                })
                .doOnError(error -> {
                    logger.error("Error en autenticación: {}", error.getMessage());
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/signup")
    public Mono<ResponseEntity<String>> registerUser(@RequestBody SignupRequest signUpRequest) {
        return userRepository.existsByUsername(signUpRequest.getUsername())
                .flatMap(existsUsername -> {
                    if (Boolean.TRUE.equals(existsUsername)) {
                        return Mono.just(ResponseEntity
                                .badRequest()
                                .body("Error: Username is already taken!"));
                    }

                    return userRepository.existsByEmail(signUpRequest.getEmail())
                            .flatMap(existsEmail -> {
                                if (Boolean.TRUE.equals(existsEmail)) {
                                    return Mono.just(ResponseEntity
                                            .badRequest()
                                            .body("Error: Email is already in use!"));
                                }

                                // Create new user's account
                                User user = User.builder()
                                        .username(signUpRequest.getUsername())
                                        .email(signUpRequest.getEmail())
                                        .password(passwordEncoder.encode(signUpRequest.getPassword()))
                                        .enabled(true)
                                        .build();

                                return userRepository.save(user)
                                        .flatMap(savedUser -> {
                                            Set<String> strRoles = signUpRequest.getRoles();
                                            Set<String> roleNames = new HashSet<>();

                                            if (strRoles == null || strRoles.isEmpty()) {
                                                roleNames.add("USER");
                                            } else {
                                                roleNames.addAll(strRoles);
                                            }

                                            return Mono.just(roleNames)
                                                    .flatMapMany(roles -> {
                                                        return Flux.fromIterable(roles)
                                                                .flatMap(roleName -> roleRepository.findByName(roleName)
                                                                        .switchIfEmpty(Mono.error(new RuntimeException("Error: Role not found.")))
                                                                        .flatMap(role -> {
                                                                            UserRole userRole = UserRole.builder()
                                                                                    .userId(savedUser.getId())
                                                                                    .roleId(role.getId())
                                                                                    .build();
                                                                            return userRoleRepository.save(userRole);
                                                                        })
                                                                );
                                                    })
                                                    .then(Mono.just(ResponseEntity.ok("User registered successfully!")));
                                        });
                            });
                });
    }
}