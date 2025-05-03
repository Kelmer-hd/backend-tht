package api_backend_tht.controller;

import api_backend_tht.model.dto.MovimientoTelaDTO;
import api_backend_tht.model.dto.MovimientoTelaFiltroDTO;
import api_backend_tht.model.entity.MovimientoTela;
import api_backend_tht.service.MovimientoTelaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/movimientos-tela")
@Validated
@CrossOrigin(origins = "http://localhost:4200")
public class MovimientoTelaController {
    private final MovimientoTelaService movimientoTelaService;

    @Autowired
    public MovimientoTelaController(MovimientoTelaService movimientoTelaService) {
        this.movimientoTelaService = movimientoTelaService;
    }

    /**
     * Registra un nuevo movimiento de tela
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<MovimientoTela> registrarMovimiento(@Valid @RequestBody MovimientoTelaDTO dto) {
        return movimientoTelaService.registrarMovimiento(dto);
    }

    /**
     * Anula un movimiento existente
     */
    @PutMapping("/{id}/anular")
    public Mono<MovimientoTela> anularMovimiento(
            @PathVariable Long id,
            @RequestParam String motivo,
            @RequestParam String usuario) {
        return movimientoTelaService.anularMovimiento(id, motivo, usuario);
    }

    /**
     * Obtiene el historial de movimientos de una tela
     */
    @GetMapping("/tela/{telaId}")
    public Flux<MovimientoTela> obtenerHistorialTela(@PathVariable Long telaId) {
        return movimientoTelaService.obtenerHistorialTela(telaId);
    }

    /**
     * Obtiene los movimientos asociados a un documento
     */
    @GetMapping("/documento/{documentoId}")
    public Flux<MovimientoTela> obtenerMovimientosPorDocumento(@PathVariable String documentoId) {
        return movimientoTelaService.obtenerMovimientosPorDocumento(documentoId);
    }

    @GetMapping
    public Mono<Map<String, Object>> buscarMovimientos(
            MovimientoTelaFiltroDTO filtro,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return movimientoTelaService.buscarMovimientosPaginados(filtro, page, size);
    }

    /**
     * Obtiene estadísticas de movimientos
     */
    @GetMapping("/estadisticas")
    public Mono<Map<String, Object>> obtenerEstadisticas() {
        return movimientoTelaService.obtenerEstadisticas();
    }

    /**
     * Obtiene un movimiento por su ID
     */
    @GetMapping("/{id}")
    public Mono<MovimientoTela> obtenerMovimiento(@PathVariable Long id) {
        return movimientoTelaService.buscarMovimientos(
                        MovimientoTelaFiltroDTO.builder().telaId(id).build())
                .next()
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Movimiento no encontrado")));
    }

}