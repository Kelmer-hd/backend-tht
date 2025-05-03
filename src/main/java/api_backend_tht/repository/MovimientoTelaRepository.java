package api_backend_tht.repository;

import api_backend_tht.model.entity.MovimientoTela;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface MovimientoTelaRepository extends ReactiveCrudRepository<MovimientoTela, Long> {
    /**
     * Busca movimientos por ID de tela
     */
    Flux<MovimientoTela> findByTelaId(Long telaId);

    /**
     * Busca movimientos por referencia de documento (ID de SalidaCorte)
     */
    Flux<MovimientoTela> findByReferenciaDocumento(String referenciaDocumento);

    /**
     * Busca movimientos por tipo
     */
    Flux<MovimientoTela> findByTipoMovimiento(String tipoMovimiento);

    /**
     * Busca movimientos por área de origen
     */
    Flux<MovimientoTela> findByAreaOrigen(String areaOrigen);

    /**
     * Busca movimientos por área de destino
     */
    Flux<MovimientoTela> findByAreaDestino(String areaDestino);

    /**
     * Busca movimientos por fecha
     */
    Flux<MovimientoTela> findByFechaMovimientoBetween(LocalDateTime inicio, LocalDateTime fin);

    /**
     * Busca movimientos por tela y tipo
     */
    Flux<MovimientoTela> findByTelaIdAndTipoMovimiento(Long telaId, String tipoMovimiento);

    /**
     * Busca movimientos por usuario responsable
     */
    Flux<MovimientoTela> findByUsuarioResponsable(String usuarioResponsable);

    /**
     * Busca movimientos por área origen y destino (para traslados)
     */
    Flux<MovimientoTela> findByAreaOrigenAndAreaDestino(String areaOrigen, String areaDestino);

    /**
     * Cuenta movimientos por tela
     */
    Mono<Long> countByTelaId(Long telaId);

    Flux<MovimientoTela> findByEstado(String estado);


}