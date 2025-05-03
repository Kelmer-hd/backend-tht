package api_backend_tht.repository;

import api_backend_tht.model.entity.Tela;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public interface TelaRepository extends ReactiveCrudRepository<Tela, Long> {

    Flux<Tela> findByNumGuia(String numGuia);

    Flux<Tela> findByProveedor(String proveedor);

    Flux<Tela> findByCliente(String cliente);

    Flux<Tela> findByFechaIngresoBetween(LocalDate fechaInicio, LocalDate fechaFin);

    Flux<Tela> findByMarca(String marca);

    Flux<Tela> findByOp(String op);

    Flux<Tela> findByTipoTela(String tipoTela);



}