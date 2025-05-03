package api_backend_tht.service;

import api_backend_tht.exception.InsufficientStockException;
import api_backend_tht.exception.InvalidOperationException;
import api_backend_tht.exception.ResourceNotFoudException;
import api_backend_tht.model.dto.SalidaCorteDTO;
import api_backend_tht.model.entity.MovimientoTela;
import api_backend_tht.model.entity.SalidaCorte;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalidaCorteService {
    private final SalidaCorteRepository salidaCorteRepository;
    private final MovimientoTelaRepository movimientoTelaRepository;
    private final TelaRepository telaRepository;

    // Constantes para estados y áreas
    private static final String ESTADO_COMPLETADO = "COMPLETADO";
    private static final String ESTADO_ANULADO = "ANULADO";
    private static final String AREA_ALMACEN = "ALMACEN";
    private static final String TIPO_MOVIMIENTO_SALIDA = "SALIDA";
    private static final String TIPO_MOVIMIENTO_ANULACION = "ANULACION";
    private static final String TIPO_MOVIMIENTO_DEVOLUCION = "DEVOLUCION_SOBRANTE";

    private static final Logger log = LoggerFactory.getLogger(SalidaCorteService.class);

    @Autowired
    public SalidaCorteService(
            SalidaCorteRepository salidaCorteRepository,
            MovimientoTelaRepository movimientoTelaRepository,
            TelaRepository telaRepository) {
        this.salidaCorteRepository = salidaCorteRepository;
        this.movimientoTelaRepository = movimientoTelaRepository;
        this.telaRepository = telaRepository;
    }

    /**
     * Método de utilidad para redondear valores
     */
    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    /**
     * Registra una salida de tela
     */
    @Transactional
    public Mono<SalidaCorte> registrarSalidaCorte(SalidaCorteDTO dto) {
        log.info("Iniciando registro de salida de tela para ID: {} con destino: {}",
                dto.getTelaId(), dto.getAreaDestino());

        // Validar que la tela exista y tenga stock suficiente
        return telaRepository.findById(dto.getTelaId())
                .switchIfEmpty(Mono.error(new ResourceNotFoudException(
                        "La tela con ID " + dto.getTelaId() + " no existe")))
                .flatMap(tela -> {
                    // Verificar que hay stock suficiente
                    if (tela.getPesoIngresado().compareTo(dto.getSalidaCorte()) < 0 ||
                            tela.getStockReal().compareTo(dto.getSalidaCorte()) < 0) {
                        return Mono.error(new InsufficientStockException(
                                "Stock insuficiente: disponible " +
                                        tela.getPesoIngresado() + ", solicitado " + dto.getSalidaCorte()));
                    }

                    // Crear el registro de salida
                    SalidaCorte salidaCorte = SalidaCorte.builder()
                            .telaId(dto.getTelaId())
                            .servicioCorte(dto.getServicioCorte())
                            .fechaSalida(dto.getFechaSalida())
                            .notaSalida(dto.getNotaSalida())
                            .op(dto.getOp())
                            .salidaCorte(dto.getSalidaCorte())
                            .areaDestino(dto.getAreaDestino())
                            .estado(ESTADO_COMPLETADO)  // Directamente completado
                            .usuarioResponsable(dto.getUsuarioResponsable())
                            .fechaRegistro(LocalDateTime.now())
                            .fechaActualizacion(LocalDateTime.now())
                            .build();

                    // Actualizar el stock y peso de la tela
                    tela.disminuirStock(dto.getSalidaCorte());

                    // Guardar todo en una transacción
                    return salidaCorteRepository.save(salidaCorte)
                            .flatMap(salidaGuardada -> {
                                // Registrar el movimiento
                                MovimientoTela movimiento = MovimientoTela.builder()
                                        .telaId(dto.getTelaId())
                                        .areaOrigen(AREA_ALMACEN)  // Por defecto viene del almacén
                                        .areaDestino(dto.getAreaDestino())
                                        .cantidad(dto.getSalidaCorte())
                                        .fechaMovimiento(LocalDateTime.now())
                                        .tipoMovimiento(TIPO_MOVIMIENTO_SALIDA)
                                        .referenciaDocumento(salidaGuardada.getId().toString())
                                        .usuarioResponsable(dto.getUsuarioResponsable())
                                        .estado(ESTADO_COMPLETADO)
                                        .observaciones("OP: " + dto.getOp() + ", Nota: " + dto.getNotaSalida())
                                        .build();

                                return telaRepository.save(tela)
                                        .then(movimientoTelaRepository.save(movimiento))
                                        .thenReturn(salidaGuardada);
                            });
                })
                .doOnSuccess(salida -> log.info("Salida de tela registrada exitosamente con ID: {}", salida.getId()))
                .doOnError(error -> log.error("Error al registrar salida: {}", error.getMessage()));
    }

    /**
     * Anula una salida de tela existente
     */
    @Transactional
    public Mono<SalidaCorte> anularSalidaCorte(Long salidaCorteId, String motivo, String usuario) {
        log.info("Anulando salida de tela ID: {}", salidaCorteId);

        return salidaCorteRepository.findById(salidaCorteId)
                .switchIfEmpty(Mono.error(new ResourceNotFoudException(
                        "Salida de tela no encontrada")))
                .flatMap(salida -> {
                    if (!ESTADO_COMPLETADO.equals(salida.getEstado())) {
                        return Mono.error(new InvalidOperationException(
                                "Solo se pueden anular salidas en estado COMPLETADO"));
                    }

                    // Obtener la tela para restaurar el stock
                    return telaRepository.findById(salida.getTelaId())
                            .flatMap(tela -> {
                                // Restaurar el stock y peso
                                tela.aumentarStock(salida.getSalidaCorte());

                                // Registrar movimiento de anulación
                                MovimientoTela anulacion = MovimientoTela.builder()
                                        .telaId(salida.getTelaId())
                                        .areaOrigen(salida.getAreaDestino())
                                        .areaDestino(AREA_ALMACEN)  // Devolvemos al almacén
                                        .cantidad(salida.getSalidaCorte())
                                        .fechaMovimiento(LocalDateTime.now())
                                        .tipoMovimiento(TIPO_MOVIMIENTO_ANULACION)
                                        .referenciaDocumento(salidaCorteId.toString())
                                        .usuarioResponsable(usuario)
                                        .estado(ESTADO_COMPLETADO)
                                        .observaciones("Anulación: " + motivo)
                                        .build();

                                // Actualizar estado de la salida
                                salida.setEstado(ESTADO_ANULADO);
                                salida.setFechaActualizacion(LocalDateTime.now());

                                // Guardar todo en una transacción
                                return telaRepository.save(tela)
                                        .then(movimientoTelaRepository.save(anulacion))
                                        .then(salidaCorteRepository.save(salida));
                            });
                })
                .doOnSuccess(salida -> log.info("Salida de tela anulada exitosamente"))
                .doOnError(error -> log.error("Error al anular salida: {}", error.getMessage()));
    }

    /**
     * Registra un consumo parcial de la tela (menos de lo que salió inicialmente)
     */
    @Transactional
    public Mono<SalidaCorte> registrarConsumoReal(Long salidaCorteId, BigDecimal consumoReal, String observacion, String usuario) {
        // Verificar que el consumo real sea mayor que cero
        if (consumoReal.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new InvalidOperationException(
                    "El consumo real debe ser mayor que cero"));
        }

        return salidaCorteRepository.findById(salidaCorteId)
                .switchIfEmpty(Mono.error(new ResourceNotFoudException(
                        "Salida de tela no encontrada")))
                .flatMap(salida -> {
                    if (!ESTADO_COMPLETADO.equals(salida.getEstado())) {
                        return Mono.error(new InvalidOperationException(
                                "Solo se puede registrar consumo de salidas COMPLETADAS"));
                    }

                    if (consumoReal.compareTo(salida.getSalidaCorte()) > 0) {
                        return Mono.error(new InvalidOperationException(
                                "El consumo real no puede ser mayor que la cantidad enviada"));
                    }

                    // Calcular el sobrante
                    BigDecimal sobrante = salida.getSalidaCorte().subtract(consumoReal);

                    // Si hay sobrante, devolver a stock
                    if (sobrante.compareTo(BigDecimal.ZERO) > 0) {
                        return telaRepository.findById(salida.getTelaId())
                                .flatMap(tela -> {
                                    // Restaurar el sobrante al stock y peso
                                    tela.aumentarStock(sobrante);

                                    // Registrar movimiento de devolución de sobrante
                                    MovimientoTela devolucion = MovimientoTela.builder()
                                            .telaId(salida.getTelaId())
                                            .areaOrigen(salida.getAreaDestino())
                                            .areaDestino(AREA_ALMACEN)  // Devolvemos al almacén
                                            .cantidad(sobrante)
                                            .fechaMovimiento(LocalDateTime.now())
                                            .tipoMovimiento(TIPO_MOVIMIENTO_DEVOLUCION)
                                            .referenciaDocumento(salidaCorteId.toString())
                                            .usuarioResponsable(usuario)
                                            .estado(ESTADO_COMPLETADO)
                                            .observaciones("Devolución de sobrante. " + observacion)
                                            .build();

                                    // Actualizar la salida para reflejar el consumo real
                                    salida.setSalidaCorte(consumoReal); // Actualizamos al consumo real
                                    salida.setFechaActualizacion(LocalDateTime.now());

                                    // Guardar todo en una transacción
                                    return telaRepository.save(tela)
                                            .then(movimientoTelaRepository.save(devolucion))
                                            .then(salidaCorteRepository.save(salida));
                                });
                    } else {
                        // No hay sobrante, el consumo fue exacto
                        return Mono.just(salida);
                    }
                });
    }

    /**
     * Obtiene las salidas asociadas a una tela específica
     */
    public Flux<SalidaCorte> obtenerSalidasPorTela(Long telaId) {
        return telaRepository.findById(telaId)
                .switchIfEmpty(Mono.error(new ResourceNotFoudException("Tela no encontrada con ID: " + telaId)))
                .flatMapMany(tela ->
                        salidaCorteRepository.findByTelaId(telaId)
                                .map(salida -> {
                                    salida.setTela(tela);
                                    return salida;
                                })
                );
    }

    /**
     * Busca salidas de tela por diferentes criterios con paginación
     */
    public Mono<Map<String, Object>> buscarSalidasPaginadas(
            String op, String areaDestino, LocalDate fechaInicio, LocalDate fechaFin,
            int page, int size) {

        // Construir el flux base según los filtros
        Flux<SalidaCorte> salidas;

        if (op != null && !op.isEmpty()) {
            salidas = salidaCorteRepository.findByOp(op);
        } else if (areaDestino != null && !areaDestino.isEmpty()) {
            salidas = salidaCorteRepository.findByAreaDestino(areaDestino);
        } else if (fechaInicio != null && fechaFin != null) {
            salidas = salidaCorteRepository.findByFechaSalidaBetween(fechaInicio, fechaFin);
        } else {
            salidas = salidaCorteRepository.findAll()
                    .sort((s1, s2) -> s2.getFechaRegistro().compareTo(s1.getFechaRegistro()));
        }

        // Enriquecer con información de telas
        Flux<SalidaCorte> salidasEnriquecidas = salidas.flatMap(salida ->
                telaRepository.findById(salida.getTelaId())
                        .map(tela -> {
                            salida.setTela(tela);
                            return salida;
                        })
                        .defaultIfEmpty(salida)
        );

        // Contar el total de elementos
        Mono<Long> count = salidasEnriquecidas.count();

        // Aplicar paginación
        Flux<SalidaCorte> paginado = salidasEnriquecidas
                .skip(page * size)
                .take(size);

        // Recoger los resultados paginados
        Mono<List<SalidaCorte>> content = paginado.collectList();

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
     * Obtiene el detalle completo de una salida
     */
    public Mono<SalidaCorte> obtenerDetalleSalida(Long salidaId) {
        return salidaCorteRepository.findById(salidaId)
                .switchIfEmpty(Mono.error(new ResourceNotFoudException(
                        "Salida de tela no encontrada")))
                .flatMap(salida -> {
                    return telaRepository.findById(salida.getTelaId())
                            .map(tela -> {
                                salida.setTela(tela);
                                return salida;
                            });
                });
    }
}