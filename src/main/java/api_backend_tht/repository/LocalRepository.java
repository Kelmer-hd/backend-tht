package api_backend_tht.repository;

import api_backend_tht.model.entity.Local;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface LocalRepository extends ReactiveCrudRepository<Local, Long> {
}
