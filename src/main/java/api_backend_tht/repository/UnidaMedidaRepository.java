package api_backend_tht.repository;

import api_backend_tht.model.entity.UnidadesMedida;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UnidaMedidaRepository extends ReactiveCrudRepository<UnidadesMedida, Long> {
    Mono<UnidadesMedida> findByCodigo(String codigo);
    Flux<UnidadesMedida> findByNombreContainingIgnoreCase(String nombre);
}