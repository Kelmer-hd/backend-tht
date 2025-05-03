package api_backend_tht.controller;

import api_backend_tht.model.dto.SalidaCorteDTO;
import api_backend_tht.model.entity.SalidaCorte;
import api_backend_tht.repository.SalidaCorteRepository;
import api_backend_tht.repository.TelaRepository;
import api_backend_tht.service.SalidaCorteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/salidas-tela")
@Validated
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class SalidaCorteController {
    private final SalidaCorteService salidaCorteService;
    private final SalidaCorteRepository salidaCorteRepository;
    private final TelaRepository telaRepository;


    /**
     * Registra una nueva salida de tela
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<SalidaCorte> registrarSalida(@Valid @RequestBody SalidaCorteDTO dto) {
        return salidaCorteService.registrarSalidaCorte(dto);
    }

    /**
     * Anula una salida de tela existente
     */
    @PutMapping("/{id}/anular")
    public Mono<SalidaCorte> anularSalida(
            @PathVariable Long id,
            @RequestParam String motivo,
            @RequestParam String usuario) {
        return salidaCorteService.anularSalidaCorte(id, motivo, usuario);
    }

    /**
     * Registra el consumo real (puede ser menor al enviado)
     */
    @PutMapping("/{id}/consumo-real")
    public Mono<SalidaCorte> registrarConsumoReal(
            @PathVariable Long id,
            @RequestParam @Positive BigDecimal consumoReal,
            @RequestParam(required = false) String observacion,
            @RequestParam String usuario) {
        return salidaCorteService.registrarConsumoReal(id, consumoReal,
                observacion != null ? observacion : "", usuario);
    }

    /**
     * Obtiene una salida por su ID
     */
    @GetMapping("/{id}")
    public Mono<SalidaCorte> obtenerSalida(@PathVariable Long id) {
        return salidaCorteService.obtenerDetalleSalida(id);
    }

    /**
     * Busca salidas por diferentes criterios
     */
    @GetMapping
    public Mono<Map<String, Object>> buscarSalidas(
            @RequestParam(required = false) String op,
            @RequestParam(required = false) String areaDestino,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return salidaCorteService.buscarSalidasPaginadas(op, areaDestino, fechaInicio, fechaFin, page, size);
    }


    @GetMapping("/tela/{telaId}")
    public Flux<SalidaCorte> obtenerSalidasPorTela(@PathVariable Long telaId) {
        return salidaCorteService.obtenerSalidasPorTela(telaId);
    }

}