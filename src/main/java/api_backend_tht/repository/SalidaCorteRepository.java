package api_backend_tht.repository;

import api_backend_tht.model.entity.SalidaCorte;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Repository
public interface SalidaCorteRepository extends ReactiveCrudRepository<SalidaCorte, Long> {
    Flux<SalidaCorte> findByTelaId(Long telaId);
    Flux<SalidaCorte> findByFechaSalidaBetween(LocalDate inicio, LocalDate fin);
    Flux<SalidaCorte> findByOp(String op);
    Flux<SalidaCorte> findByAreaDestino(String areaDestino);
    Mono<SalidaCorte> findByNotaSalida(String notaSalida);
}