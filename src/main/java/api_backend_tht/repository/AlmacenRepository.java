package api_backend_tht.repository;

import api_backend_tht.model.entity.Almacen;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AlmacenRepository extends ReactiveCrudRepository<Almacen, Long> {
    Mono<Almacen> findByCodigoAlmacen(int codigoAlmacen);
    Flux<Almacen> findByEstado(String estado);
    Flux<Almacen> findByTipoAlmacen(String tipoAlmacen);


}