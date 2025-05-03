package api_backend_tht.repository;

import api_backend_tht.model.entity.Role;
import api_backend_tht.model.entity.UserRole;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface UserRoleRepository extends ReactiveCrudRepository<UserRole, Long> {
    @Query("SELECT r.id, r.name FROM roles r JOIN user_roles ur ON r.id = ur.role_id WHERE ur.user_id = :userId")
    Flux<Role> findRolesByUserId(Long userId);
}