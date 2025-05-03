package api_backend_tht.service;

import api_backend_tht.model.dto.MovimientoTelaDTO;
import api_backend_tht.model.dto.MovimientoTelaFiltroDTO;
import api_backend_tht.model.entity.MovimientoTela;
import api_backend_tht.repository.MovimientoTelaRepository;
import api_backend_tht.repository.SalidaCorteRepository;
import api_backend_tht.repository.TelaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MovimientoTelaService {
    private final MovimientoTelaRepository movimientoTelaRepository;
    private final TelaRepository telaRepository;
    private final SalidaCorteRepository salidaCorteRepository;

    private static final Logger log = LoggerFactory.getLogger(MovimientoTelaService.class);

    @Autowired
    public MovimientoTelaService(
            MovimientoTelaRepository movimientoTelaRepository,
            TelaRepository telaRepository,
            SalidaCorteRepository salidaCorteRepository) {
        this.movimientoTelaRepository = movimientoTelaRepository;
        this.telaRepository = telaRepository;
        this.salidaCorteRepository = salidaCorteRepository;
    }

    /**
     * Método de utilidad para redondear valores
     */
    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }


    /**
     * Registra un nuevo movimiento de tela
     */
    @Transactional
    public Mono<MovimientoTela> registrarMovimiento(MovimientoTelaDTO dto) {
        log.info("Registrando movimiento de tela ID: {} de {} a {}",
                dto.getTelaId(), dto.getAreaOrigen(), dto.getAreaDestino());

        // Validar que la tela exista
        return telaRepository.findById(dto.getTelaId())
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "La tela con ID " + dto.getTelaId() + " no existe")))
                .flatMap(tela -> {
                    // Verificar stock si es necesario
                    if ("SALIDA".equals(dto.getTipoMovimiento()) || "TRASLADO".equals(dto.getTipoMovimiento())) {
                        // Usar compareTo en lugar de operadores < o >
                        if (tela.getPesoIngresado().compareTo(dto.getCantidad()) < 0 ||
                                tela.getStockReal().compareTo(dto.getCantidad()) < 0) {
                            return Mono.error(new ResponseStatusException(
                                    HttpStatus.BAD_REQUEST, "Stock insuficiente: disponible " +
                                    tela.getPesoIngresado() + ", solicitado " + dto.getCantidad()));
                        }

                        // Actualizar stock para salidas o traslados
                        tela.setPesoIngresado(tela.getPesoIngresado().subtract(dto.getCantidad()));
                        tela.setStockReal(tela.getStockReal().subtract(dto.getCantidad()));
                    } else if ("ENTRADA".equals(dto.getTipoMovimiento())) {
                        // Actualizar stock para entradas
                        tela.setPesoIngresado(tela.getPesoIngresado().add(dto.getCantidad()));
                        tela.setStockReal(tela.getStockReal().add(dto.getCantidad()));
                    }

                    // Crear el movimiento
                    MovimientoTela movimiento = MovimientoTela.builder()
                            .telaId(dto.getTelaId())
                            .areaOrigen(dto.getAreaOrigen())
                            .areaDestino(dto.getAreaDestino())
                            .cantidad(dto.getCantidad()) // Ya no necesitas convertir a double
                            .fechaMovimiento(LocalDateTime.now())
                            .tipoMovimiento(dto.getTipoMovimiento())
                            .referenciaDocumento(dto.getReferenciaDocumento())
                            .usuarioResponsable(dto.getUsuarioResponsable())
                            .estado("COMPLETADO")
                            .observaciones(dto.getObservaciones())
                            .build();

                    // Guardar el movimiento y actualizar la tela
                    return telaRepository.save(tela)
                            .then(movimientoTelaRepository.save(movimiento));
                });
    }

    /**
     * Obtiene el historial de movimientos de una tela con información completa
     */
    public Flux<MovimientoTela> obtenerHistorialTela(Long telaId) {
        return telaRepository.findById(telaId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Tela no encontrada")))
                .flatMapMany(tela -> {
                    return movimientoTelaRepository.findByTelaId(telaId)
                            .sort((m1, m2) -> m2.getFechaMovimiento().compareTo(m1.getFechaMovimiento()))
                            .map(movimiento -> {
                                movimiento.setTela(tela);
                                return movimiento;
                            });
                });
    }

    /**
     * Obtiene movimientos asociados a un documento de referencia (como SalidaCorte)
     */
    public Flux<MovimientoTela> obtenerMovimientosPorDocumento(String documentoId) {
        return movimientoTelaRepository.findByReferenciaDocumento(documentoId)
                .flatMap(movimiento -> {
                    return telaRepository.findById(movimiento.getTelaId())
                            .map(tela -> {
                                movimiento.setTela(tela);
                                return movimiento;
                            });
                });
    }

    /**
     * Anula un movimiento existente
     */
    @Transactional
    public Mono<MovimientoTela> anularMovimiento(Long movimientoId, String motivo, String usuario) {
        log.info("Anulando movimiento ID: {}", movimientoId);

        return movimientoTelaRepository.findById(movimientoId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Movimiento no encontrado")))
                .flatMap(movimiento -> {
                    if (!"COMPLETADO".equals(movimiento.getEstado())) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "Solo se pueden anular movimientos en estado COMPLETADO"));
                    }

                    // Obtener la tela para restaurar el stock
                    return telaRepository.findById(movimiento.getTelaId())
                            .flatMap(tela -> {
                                // Restaurar el stock según el tipo de movimiento
                                if ("SALIDA".equals(movimiento.getTipoMovimiento()) ||
                                        "TRASLADO".equals(movimiento.getTipoMovimiento())) {
                                    // Si fue salida o traslado, aumentamos el stock y peso
                                    tela.setStockReal(tela.getStockReal().add(movimiento.getCantidad()));
                                    tela.setPesoIngresado(tela.getPesoIngresado().add(movimiento.getCantidad()));
                                } else if ("ENTRADA".equals(movimiento.getTipoMovimiento())) {
                                    // Si fue entrada, disminuimos el stock y peso
                                    if (tela.getStockReal().compareTo(movimiento.getCantidad()) < 0 ||
                                            tela.getPesoIngresado().compareTo(movimiento.getCantidad()) < 0) {
                                        return Mono.error(new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST, "No se puede anular la entrada porque no hay suficiente stock"));
                                    }
                                    tela.setStockReal(tela.getStockReal().subtract(movimiento.getCantidad()));
                                    tela.setPesoIngresado(tela.getPesoIngresado().subtract(movimiento.getCantidad()));
                                }

                                // Crear movimiento de anulación
                                MovimientoTela anulacion = MovimientoTela.builder()
                                        .telaId(movimiento.getTelaId())
                                        .areaOrigen(movimiento.getAreaDestino()) // Invertimos origen y destino
                                        .areaDestino(movimiento.getAreaOrigen())
                                        .cantidad(movimiento.getCantidad())
                                        .fechaMovimiento(LocalDateTime.now())
                                        .tipoMovimiento("ANULACION_" + movimiento.getTipoMovimiento())
                                        .referenciaDocumento(movimiento.getReferenciaDocumento())
                                        .usuarioResponsable(usuario)
                                        .estado("COMPLETADO")
                                        .observaciones("Anulación de movimiento ID: " + movimientoId + ". Motivo: " + motivo)
                                        .build();

                                // Actualizar estado del movimiento original
                                movimiento.setEstado("ANULADO");

                                // Guardar todo en una transacción
                                return telaRepository.save(tela)
                                        .then(movimientoTelaRepository.save(movimiento))
                                        .then(movimientoTelaRepository.save(anulacion));
                            });
                })
                .doOnSuccess(anulacion -> log.info("Movimiento anulado exitosamente"))
                .doOnError(error -> log.error("Error al anular movimiento: {}", error.getMessage()));
    }

    /**
     * Busca movimientos por múltiples criterios
     */
    public Flux<MovimientoTela> buscarMovimientos(MovimientoTelaFiltroDTO filtro) {
        Flux<MovimientoTela> movimientos;

        // Aplicar filtros según los criterios proporcionados
        if (filtro.getTelaId() != null) {
            movimientos = movimientoTelaRepository.findByTelaId(filtro.getTelaId());
        } else if (filtro.getTipoMovimiento() != null && !filtro.getTipoMovimiento().isEmpty()) {
            movimientos = movimientoTelaRepository.findByTipoMovimiento(filtro.getTipoMovimiento());
        } else if (filtro.getAreaOrigen() != null && !filtro.getAreaOrigen().isEmpty()) {
            movimientos = movimientoTelaRepository.findByAreaOrigen(filtro.getAreaOrigen());
        } else if (filtro.getAreaDestino() != null && !filtro.getAreaDestino().isEmpty()) {
            movimientos = movimientoTelaRepository.findByAreaDestino(filtro.getAreaDestino());
        } else if (filtro.getFechaInicio() != null && filtro.getFechaFin() != null) {
            movimientos = movimientoTelaRepository.findByFechaMovimientoBetween(
                    filtro.getFechaInicio(), filtro.getFechaFin());
        } else if (filtro.getUsuarioResponsable() != null && !filtro.getUsuarioResponsable().isEmpty()) {
            movimientos = movimientoTelaRepository.findByUsuarioResponsable(filtro.getUsuarioResponsable());
        } else {
            // Si no hay filtros, devolver limitados y ordenados por fecha
            movimientos = movimientoTelaRepository.findAll()
                    .sort((m1, m2) -> m2.getFechaMovimiento().compareTo(m1.getFechaMovimiento()))
                    .take(100); // Limitar a 100 registros
        }

        // Enriquecer con información de telas
        return movimientos.flatMap(movimiento ->
                telaRepository.findById(movimiento.getTelaId())
                        .map(tela -> {
                            movimiento.setTela(tela);
                            return movimiento;
                        })
                        .defaultIfEmpty(movimiento)
        );
    }


    /**
     * Busca movimientos por múltiples criterios con paginación
     */
    public Mono<Map<String, Object>> buscarMovimientosPaginados(MovimientoTelaFiltroDTO filtro, int page, int size) {
        // Crear un flux base según los filtros (igual que el método existente)
        Flux<MovimientoTela> movimientos;

        // Aplicar filtros según los criterios proporcionados
        if (filtro.getTelaId() != null) {
            movimientos = movimientoTelaRepository.findByTelaId(filtro.getTelaId());
        } else if (filtro.getTipoMovimiento() != null && !filtro.getTipoMovimiento().isEmpty()) {
            movimientos = movimientoTelaRepository.findByTipoMovimiento(filtro.getTipoMovimiento());
        } else if (filtro.getAreaOrigen() != null && !filtro.getAreaOrigen().isEmpty()) {
            movimientos = movimientoTelaRepository.findByAreaOrigen(filtro.getAreaOrigen());
        } else if (filtro.getAreaDestino() != null && !filtro.getAreaDestino().isEmpty()) {
            movimientos = movimientoTelaRepository.findByAreaDestino(filtro.getAreaDestino());
        } else if (filtro.getFechaInicio() != null && filtro.getFechaFin() != null) {
            movimientos = movimientoTelaRepository.findByFechaMovimientoBetween(
                    filtro.getFechaInicio(), filtro.getFechaFin());
        } else if (filtro.getUsuarioResponsable() != null && !filtro.getUsuarioResponsable().isEmpty()) {
            movimientos = movimientoTelaRepository.findByUsuarioResponsable(filtro.getUsuarioResponsable());
        } else if (filtro.getEstado() != null && !filtro.getEstado().isEmpty()) {
            movimientos = movimientoTelaRepository.findByEstado(filtro.getEstado());
        } else {
            // Si no hay filtros, devolver todos ordenados por fecha
            movimientos = movimientoTelaRepository.findAll()
                    .sort((m1, m2) -> m2.getFechaMovimiento().compareTo(m1.getFechaMovimiento()));
        }

        // Enriquecer con información de telas
        Flux<MovimientoTela> movimientosEnriquecidos = movimientos.flatMap(movimiento ->
                telaRepository.findById(movimiento.getTelaId())
                        .map(tela -> {
                            movimiento.setTela(tela);
                            return movimiento;
                        })
                        .defaultIfEmpty(movimiento)
        );

        // Contar el total de elementos
        Mono<Long> count = movimientosEnriquecidos.count();

        // Aplicar paginación
        Flux<MovimientoTela> paginado = movimientosEnriquecidos
                .skip(page * size)
                .take(size);

        // Recoger los resultados paginados
        Mono<List<MovimientoTela>> content = paginado.collectList();

        // Combinar todo en un solo resultado
        return Mono.zip(content, count)
                .map(tuple -> {
                    Map<String, Object> resultado = new HashMap<>();
                    resultado.put("content", tuple.getT1());
                    resultado.put("totalElements", tuple.getT2());
                    return resultado;
                });
    }

    /**
     * Obtiene estadísticas de movimientos
     */
    public Mono<Map<String, Object>> obtenerEstadisticas() {
        Mono<Long> totalMovimientos = movimientoTelaRepository.count();

        Mono<List<MovimientoTela>> ultimosMovimientos = movimientoTelaRepository.findAll()
                .sort((m1, m2) -> m2.getFechaMovimiento().compareTo(m1.getFechaMovimiento()))
                .take(10)
                .flatMap(movimiento ->
                        telaRepository.findById(movimiento.getTelaId())
                                .map(tela -> {
                                    movimiento.setTela(tela);
                                    return movimiento;
                                })
                                .defaultIfEmpty(movimiento)
                )
                .collectList();

        // Obtener conteo por tipo de movimiento
        Mono<Map<String, Long>> conteosPorTipo = movimientoTelaRepository.findAll()
                .groupBy(MovimientoTela::getTipoMovimiento)
                .flatMap(group -> Mono.zip(
                        Mono.just(group.key()),
                        group.count()
                ))
                .collectMap(Tuple2::getT1, Tuple2::getT2);

        return Mono.zip(totalMovimientos, ultimosMovimientos, conteosPorTipo)
                .map(tuple -> {
                    Map<String, Object> estadisticas = new HashMap<>();
                    estadisticas.put("totalMovimientos", tuple.getT1());
                    estadisticas.put("ultimosMovimientos", tuple.getT2());
                    estadisticas.put("movimientosPorTipo", tuple.getT3());
                    return estadisticas;
                });
    }
}