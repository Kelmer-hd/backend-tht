package api_backend_tht;

import api_backend_tht.model.entity.Role;
import api_backend_tht.model.entity.User;
import api_backend_tht.model.entity.UserRole;
import api_backend_tht.repository.RoleRepository;
import api_backend_tht.repository.UserRepository;
import api_backend_tht.repository.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    public DatabaseInitializer(
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public void run(String... args) {
        logger.info("Inicializando datos de la base de datos...");

        // Crear roles si no existen
        createRoleIfNotFound("USER").subscribe();
        createRoleIfNotFound("MODERATOR").subscribe();
        Mono<Role> adminRole = createRoleIfNotFound("ADMIN");

        // Crear usuario administrador si no existe
        adminRole.flatMap(role -> {
            return userRepository.findByUsername("admin")
                    .defaultIfEmpty(new User())
                    .flatMap(user -> {
                        if (user.getId() == null) {
                            // El usuario no existe, crearlo
                            User newUser = new User();
                            newUser.setUsername("admin");
                            newUser.setEmail("admin@example.com");

                            // Encode the password - here we use "admin123" directly
                            String rawPassword = "admin123";
                            String encodedPassword = passwordEncoder.encode(rawPassword);
                            logger.debug("Contraseña original: {}, Contraseña codificada: {}", rawPassword, encodedPassword);

                            newUser.setPassword(encodedPassword);
                            newUser.setEnabled(true);

                            return userRepository.save(newUser)
                                    .flatMap(savedUser -> {
                                        UserRole userRole = new UserRole();
                                        userRole.setUserId(savedUser.getId());
                                        userRole.setRoleId(role.getId());
                                        return userRoleRepository.save(userRole)
                                                .then(Mono.just(savedUser));
                                    });
                        }
                        return Mono.just(user);
                    });
        }).subscribe(
                user -> logger.info("Usuario admin creado o ya existente: {}", user.getUsername()),
                error -> logger.error("Error al crear usuario admin: {}", error.getMessage())
        );
    }

    private Mono<Role> createRoleIfNotFound(String name) {
        return roleRepository.findByName(name)
                .defaultIfEmpty(new Role())
                .flatMap(role -> {
                    if (role.getId() == null) {
                        // El rol no existe, crearlo
                        Role newRole = new Role();
                        newRole.setName(name);
                        return roleRepository.save(newRole);
                    }
                    return Mono.just(role);
                });
    }
}