package api_backend_tht.repository;

import api_backend_tht.model.entity.AlmacenTela;
import api_backend_tht.model.entity.EstadoAlmacenTela;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AlmacenTelaRepository extends ReactiveCrudRepository<AlmacenTela, Long> {
    Flux<AlmacenTela> findByAlmacenId(Long almacenId);
    Flux<AlmacenTela> findByTelaId(Long telaId);
    Mono<AlmacenTela> findByAlmacenIdAndTelaId(Long almacenId, Long telaId);

    Flux<AlmacenTela> findByAlmacenIdAndEstado(Long almacenId, EstadoAlmacenTela estado);

    Mono<AlmacenTela> findByAlmacenIdAndTelaIdAndEstado(Long almacenId, Long telaId, EstadoAlmacenTela estado);
}

