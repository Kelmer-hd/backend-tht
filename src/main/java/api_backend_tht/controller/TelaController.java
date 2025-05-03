package api_backend_tht.controller;

import api_backend_tht.exception.ResourceNotFoudException;
import api_backend_tht.model.dto.*;
import api_backend_tht.service.ReporteService;
import api_backend_tht.service.TelaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/telas")
@RequiredArgsConstructor
@Tag(name = "Telas", description = "API para gestión de telas")
@Slf4j
public class TelaController {

    private final TelaService telaService;
    private final ReporteService reporteService;

    /**
     * Obtiene todas las telas
     * @return Flux con todas las telas
     */
    @GetMapping
    @Operation(summary = "Obtiene todas las telas")
    public Flux<TelaDTO> getAllTelas() {
        return telaService.findAll();
    }

    /**
     * Obtiene una tela por su ID
     * @param id ID de la tela
     * @return Mono con la tela si existe
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una tela por su ID")
    public Mono<ResponseEntity<TelaDTO>> getTelaById(@PathVariable Long id) {
        return telaService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Crea una nueva tela
     * @param dto Datos de la tela a crear
     * @return Mono con la tela creada
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una nueva tela")
    public Mono<TelaDTO> createTela(@Valid @RequestBody TelaCreateDTO dto) {
        return telaService.save(dto);
    }

    /**
     * Actualiza una tela existente
     * @param id ID de la tela a actualizar
     * @param dto Nuevos datos de la tela
     * @return Mono con la tela actualizada
     */
    @PutMapping("/{id}")
    @Operation(summary = "Actualiza una tela existente")
    public Mono<ResponseEntity<TelaDTO>> updateTela(
            @PathVariable Long id,
            @Valid @RequestBody TelaCreateDTO dto) {
        return telaService.update(id, dto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Elimina una tela por su ID
     * @param id ID de la tela a eliminar
     * @return Mono vacío que indica la finalización de la operación
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina una tela por su ID")
    public Mono<Void> deleteTela(@PathVariable Long id) {
        return telaService.deleteById(id);
    }

    /**
     * Búsqueda unificada de telas por diferentes criterios
     * @param termino Término a buscar
     * @param campo Campo específico para la búsqueda (opcional)
     * @return Flux con las telas que coinciden con la búsqueda
     */
    @GetMapping("/buscar")
    @Operation(summary = "Búsqueda de telas por término en un campo específico o en todos")
    public Flux<TelaDTO> buscarTelas(
            @RequestParam String termino,
            @RequestParam(required = false, defaultValue = "todos") String campo) {

        TelaBusquedaDTO busqueda = new TelaBusquedaDTO();
        busqueda.setTermino(termino);
        busqueda.setCampo(campo);
        return telaService.buscarPorTermino(busqueda);
    }

    /**
     * Búsqueda avanzada de telas con múltiples criterios
     * @param filtro Objeto con los filtros a aplicar
     * @return Flux con las telas que cumplen con los criterios de búsqueda
     */
    @PostMapping("/buscar/avanzado")
    @Operation(summary = "Búsqueda avanzada con múltiples criterios")
    public Flux<TelaDTO> buscarTelasAvanzado(@RequestBody TelaFiltroDTO filtro) {
        return telaService.buscarPorFiltros(filtro);
    }

    /**
     * Busca telas por rango de fechas
     * @param fechaInicio Fecha de inicio (formato ISO)
     * @param fechaFin Fecha de fin (formato ISO)
     * @return Flux con las telas dentro del rango de fechas
     */
    @GetMapping("/buscar/fechas")
    @Operation(summary = "Busca telas por rango de fechas")
    public Flux<TelaDTO> getTelasByFechaIngreso(
            @RequestParam("inicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam("fin") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return telaService.findByFechaIngresoBetween(fechaInicio, fechaFin);
    }

    /**
     * Obtiene estadísticas de telas según el criterio de agrupación
     * @param criterio Criterio de agrupación (proveedor, cliente, marca, tipoTela, estado, almacen)
     * @return Flux con las estadísticas agrupadas
     */
    @GetMapping("/estadisticas")
    @Operation(summary = "Obtiene estadísticas de telas según criterio de agrupación")
    public Flux<Map<String, Object>> getEstadisticas(
            @RequestParam(defaultValue = "proveedor") String criterio) {
        try {
            return telaService.getEstadisticasPorCampo(criterio);
        } catch (IllegalArgumentException e) {
            return Flux.error(new ResourceNotFoudException("Criterio de agrupación no válido: " + criterio));
        }
    }

    /**
     * Genera un reporte en el formato especificado
     * @param formato Formato del reporte (excel o pdf)
     * @return Mono con el recurso del archivo generado
     */
    @GetMapping("/reportes")
    @Operation(summary = "Genera un reporte de telas en el formato especificado")
    public Mono<ResponseEntity<Resource>> generarReporte(
            @RequestParam(defaultValue = "excel") String formato) {

        return reporteService.generarReporte(null, formato, "Telas")
                .map(resource -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"reporte-telas." +
                                    (formato.equalsIgnoreCase("excel") ? "xlsx" : "pdf") + "\"");

                    MediaType mediaType = formato.equalsIgnoreCase("excel")
                            ? MediaType.APPLICATION_OCTET_STREAM
                            : MediaType.APPLICATION_PDF;

                    return ResponseEntity.ok()
                            .headers(headers)
                            .contentType(mediaType)
                            .body(resource);
                })
                .onErrorResume(e -> {
                    log.error("Error al generar reporte: {}", e.getMessage());
                    return Mono.error(new ResourceNotFoudException("Error al generar el reporte: " + e.getMessage()));
                });
    }

    /**
     * Genera un reporte filtrado en el formato especificado
     * @param filtros Objeto con los filtros a aplicar
     * @param formato Formato del reporte (excel o pdf, por defecto excel)
     * @return Mono con el recurso del archivo generado
     */
    @PostMapping("/reportes/filtrado")
    @Operation(summary = "Genera un reporte filtrado en el formato especificado")
    public Mono<ResponseEntity<Resource>> generarReporteFiltrado(
            @RequestBody TelaFiltroDTO filtros,
            @RequestParam(defaultValue = "excel") String formato) {

        return reporteService.generarReporte(filtros, formato, "Telas Filtradas")
                .map(resource -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"reporte-telas-filtrado." +
                                    (formato.equalsIgnoreCase("excel") ? "xlsx" : "pdf") + "\"");

                    MediaType mediaType = formato.equalsIgnoreCase("excel")
                            ? MediaType.APPLICATION_OCTET_STREAM
                            : MediaType.APPLICATION_PDF;

                    return ResponseEntity.ok()
                            .headers(headers)
                            .contentType(mediaType)
                            .body(resource);
                })
                .onErrorResume(e -> {
                    log.error("Error al generar reporte filtrado: {}", e.getMessage());
                    return Mono.error(new ResourceNotFoudException("Error al generar el reporte filtrado: " + e.getMessage()));
                });
    }
}