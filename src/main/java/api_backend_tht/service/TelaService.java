package api_backend_tht.service;

import api_backend_tht.exception.ResourceNotFoudException;
import api_backend_tht.mapper.TelaMapper;
import api_backend_tht.model.dto.TelaBusquedaDTO;
import api_backend_tht.model.dto.TelaCreateDTO;
import api_backend_tht.model.dto.TelaDTO;
import api_backend_tht.model.dto.TelaFiltroDTO;
import api_backend_tht.model.entity.Tela;
import api_backend_tht.repository.TelaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelaService {

    private final TelaRepository telaRepository;
    private final TelaMapper telaMapper;

    // Constantes para mensajes de error
    private static final String TELA_NOT_FOUND = "No se encontró la tela con ID: %d";

    // -----------------------------
    // Operaciones CRUD básicas
    // -----------------------------

    /**
     * Obtiene todas las telas
     * @return Flux de TelaDTO
     */
    public Flux<TelaDTO> findAll() {
        return telaRepository.findAll()
                .map(telaMapper::toDto);
    }

    /**
     * Busca una tela por su ID
     * @param id ID de la tela
     * @return Mono con la tela encontrada
     */
    public Mono<TelaDTO> findById(Long id) {
        return telaRepository.findById(id)
                .map(telaMapper::toDto)
                .switchIfEmpty(Mono.error(new ResourceNotFoudException(
                        String.format(TELA_NOT_FOUND, id))));
    }

    /**
     * Guarda una nueva tela
     * @param dto DTO con los datos de la tela a crear
     * @return Mono con la tela creada
     */
    public Mono<TelaDTO> save(TelaCreateDTO dto) {
        Tela tela = telaMapper.toEntity(dto);
        return telaRepository.save(tela)
                .map(telaMapper::toDto);
    }

    /**
     * Actualiza una tela existente
     * @param id ID de la tela a actualizar
     * @param dto DTO con los nuevos datos
     * @return Mono con la tela actualizada
     */
    public Mono<TelaDTO> update(Long id, TelaCreateDTO dto) {
        return telaRepository.findById(id)
                .map(tela -> telaMapper.updateEntityFromDto(tela, dto))
                .flatMap(telaRepository::save)
                .map(telaMapper::toDto)
                .switchIfEmpty(Mono.error(new ResourceNotFoudException(
                        String.format(TELA_NOT_FOUND, id))));
    }

    /**
     * Elimina una tela por su ID
     * @param id ID de la tela a eliminar
     * @return Mono vacío que indica la finalización de la operación
     */
    public Mono<Void> deleteById(Long id) {
        return telaRepository.findById(id)
                .flatMap(tela -> telaRepository.deleteById(id))
                .switchIfEmpty(Mono.error(new ResourceNotFoudException(
                        String.format(TELA_NOT_FOUND, id))));
    }

    // -----------------------------
    // Operaciones de búsqueda
    // -----------------------------

    /**
     * Busca telas por término y campo específico o en todos los campos
     * @param busqueda Parámetros de búsqueda
     * @return Flux de telas que coinciden con la búsqueda
     */
    public Flux<TelaDTO> buscarPorTermino(TelaBusquedaDTO busqueda) {
        String termino = busqueda.getTermino() != null ? busqueda.getTermino().toLowerCase() : "";
        String campo = busqueda.getCampo() != null ? busqueda.getCampo() : "todos";

        return telaRepository.findAll()
                .filter(tela -> filtrarPorTerminoYCampo(tela, termino, campo))
                .map(telaMapper::toDto);
    }

    /**
     * Método auxiliar para filtrar telas por término y campo
     * @param tela Tela a evaluar
     * @param termino Término de búsqueda
     * @param campo Campo específico o "todos"
     * @return true si la tela cumple con el criterio de búsqueda
     */
    private boolean filtrarPorTerminoYCampo(Tela tela, String termino, String campo) {
        if (termino.isEmpty()) {
            return true;
        }

        switch (campo) {
            case "numGuia":
                return contieneCadena(tela.getNumGuia(), termino);
            case "partida":
                return contieneCadena(tela.getPartida(), termino);
            case "os":
                return contieneCadena(tela.getOs(), termino);
            case "proveedor":
                return contieneCadena(tela.getProveedor(), termino);
            case "cliente":
                return contieneCadena(tela.getCliente(), termino);
            case "marca":
                return contieneCadena(tela.getMarca(), termino);
            case "op":
                return contieneCadena(tela.getOp(), termino);
            case "tipoTela":
                return contieneCadena(tela.getTipoTela(), termino);
            case "descripcion":
                return contieneCadena(tela.getDescripcion(), termino);
            case "ench":
                return contieneCadena(tela.getEnch(), termino);
            case "estado":
                return contieneCadena(tela.getEstado(), termino);
            case "almacen":
                return contieneCadena(tela.getAlmacen(), termino);
            case "todos":
            default:
                return contieneCadena(tela.getNumGuia(), termino) ||
                        contieneCadena(tela.getPartida(), termino) ||
                        contieneCadena(tela.getOs(), termino) ||
                        contieneCadena(tela.getProveedor(), termino) ||
                        contieneCadena(tela.getCliente(), termino) ||
                        contieneCadena(tela.getMarca(), termino) ||
                        contieneCadena(tela.getOp(), termino) ||
                        contieneCadena(tela.getTipoTela(), termino) ||
                        contieneCadena(tela.getDescripcion(), termino) ||
                        contieneCadena(tela.getEnch(), termino) ||
                        contieneCadena(tela.getEstado(), termino) ||
                        contieneCadena(tela.getAlmacen(), termino);
        }
    }

    /**
     * Método utilitario para evaluar si una cadena contiene un término
     * @param cadena La cadena a evaluar
     * @param termino El término a buscar
     * @return true si la cadena contiene el término (insensible a mayúsculas/minúsculas)
     */
    private boolean contieneCadena(String cadena, String termino) {
        return cadena != null && cadena.toLowerCase().contains(termino);
    }

    /**
     * Busca telas según criterios de filtro avanzados
     * @param filtro Criterios de filtro
     * @return Flux de telas que cumplen con los criterios
     */
    public Flux<TelaDTO> buscarPorFiltros(TelaFiltroDTO filtro) {
        return telaRepository.findAll()
                .filter(tela -> aplicarFiltros(tela, filtro))
                .map(telaMapper::toDto);
    }

    /**
     * Aplica los filtros a una tela específica
     * @param tela La tela a evaluar
     * @param filtro Los filtros a aplicar
     * @return true si la tela cumple con los filtros, false en caso contrario
     */
    private boolean aplicarFiltros(Tela tela, TelaFiltroDTO filtro) {
        if (filtro == null) {
            return true;
        }

        boolean cumple = true;

        if (filtro.getNumGuia() != null && !filtro.getNumGuia().isEmpty()) {
            cumple = tela.getNumGuia() != null &&
                    tela.getNumGuia().contains(filtro.getNumGuia());
        }

        if (cumple && filtro.getProveedor() != null && !filtro.getProveedor().isEmpty()) {
            cumple = tela.getProveedor() != null &&
                    tela.getProveedor().contains(filtro.getProveedor());
        }

        if (cumple && filtro.getCliente() != null && !filtro.getCliente().isEmpty()) {
            cumple = tela.getCliente() != null &&
                    tela.getCliente().contains(filtro.getCliente());
        }

        if (cumple && filtro.getDescripcion() != null && !filtro.getDescripcion().isEmpty()) {
            cumple = tela.getDescripcion() != null &&
                    tela.getDescripcion().contains(filtro.getDescripcion());
        }

        if (cumple && filtro.getOs() != null && !filtro.getOs().isEmpty()) {
            cumple = tela.getOs() != null &&
                    tela.getOs().contains(filtro.getOs());
        }

        if (cumple && filtro.getPartida() != null && !filtro.getPartida().isEmpty()) {
            cumple = tela.getPartida() != null &&
                    tela.getPartida().contains(filtro.getPartida());
        }

        // Nuevos campos
        if (cumple && filtro.getEstado() != null && !filtro.getEstado().isEmpty()) {
            cumple = tela.getEstado() != null &&
                    tela.getEstado().contains(filtro.getEstado());
        }

        if (cumple && filtro.getAlmacen() != null && !filtro.getAlmacen().isEmpty()) {
            cumple = tela.getAlmacen() != null &&
                    tela.getAlmacen().contains(filtro.getAlmacen());
        }


        if (cumple && filtro.getTipoTela() != null && !filtro.getTipoTela().isEmpty()) {
            cumple = tela.getTipoTela() != null &&
                    tela.getTipoTela().contains(filtro.getTipoTela());
        }

        // Filtros de fecha
        if (cumple && filtro.getFechaInicio() != null && tela.getFechaIngreso() != null) {
            cumple = !tela.getFechaIngreso().isBefore(filtro.getFechaInicio());
        }

        if (cumple && filtro.getFechaFin() != null && tela.getFechaIngreso() != null) {
            cumple = !tela.getFechaIngreso().isAfter(filtro.getFechaFin());
        }

        return cumple;
    }

    /**
     * Busca telas por rango de fechas de ingreso
     * @param fechaInicio Fecha de inicio del rango
     * @param fechaFin Fecha de fin del rango
     * @return Flux de telas dentro del rango de fechas especificado
     */
    public Flux<TelaDTO> findByFechaIngresoBetween(LocalDate fechaInicio, LocalDate fechaFin) {
        return telaRepository.findByFechaIngresoBetween(fechaInicio, fechaFin)
                .map(telaMapper::toDto);
    }

    // -----------------------------
    // Métodos de estadísticas
    // -----------------------------

    /**
     * Obtiene estadísticas de telas agrupadas por un criterio específico
     * @param campo Campo por el que se agruparán las estadísticas
     * @return Flux con las estadísticas agrupadas
     */
    public Flux<Map<String, Object>> getEstadisticasPorCampo(String campo) {
        Function<Tela, String> keyExtractor;
        String keyName;

        switch (campo.toLowerCase()) {
            case "proveedor":
                keyExtractor = Tela::getProveedor;
                keyName = "proveedor";
                break;
            case "cliente":
                keyExtractor = Tela::getCliente;
                keyName = "cliente";
                break;
            case "marca":
                keyExtractor = Tela::getMarca;
                keyName = "marca";
                break;
            case "tipotela":
                keyExtractor = Tela::getTipoTela;
                keyName = "tipoTela";
                break;
            case "estado":
                keyExtractor = Tela::getEstado;
                keyName = "estado";
                break;
            case "almacen":
                keyExtractor = Tela::getAlmacen;
                keyName = "almacen";
                break;
            default:
                return Flux.error(new IllegalArgumentException("Criterio no válido: " + campo));
        }

        return telaRepository.findAll()
                .collectList()
                .flatMapMany(telas -> Flux.fromIterable(
                        generarEstadisticasAgrupadas(telas, keyExtractor, keyName))
                );
    }

    /**
     * Método auxiliar para generar estadísticas agrupadas
     * @param telas Lista de telas
     * @param keyExtractor Función para extraer la clave de agrupación
     * @param keyName Nombre de la clave en las estadísticas
     * @return Lista de mapas con estadísticas por clave
     */
    private List<Map<String, Object>> generarEstadisticasAgrupadas(
            List<Tela> telas,
            Function<Tela, String> keyExtractor,
            String keyName) {

        // Modificar para manejar valores nulos correctamente
        return telas.stream()
                .collect(Collectors.groupingBy(
                        tela -> Optional.ofNullable(keyExtractor.apply(tela)).orElse("No especificado"),
                        Collectors.toList()))
                .entrySet().stream()
                .map(entry -> {
                    Map<String, Object> stats = new HashMap<>();
                    stats.put(keyName, entry.getKey());
                    stats.put("cantidadTelas", entry.getValue().size());

                    // Calculando totalPeso usando BigDecimal
                    BigDecimal totalPeso = entry.getValue().stream()
                            .map(Tela::getPesoIngresado)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    stats.put("totalPeso", totalPeso);

                    // Calculando totalRollos
                    stats.put("totalRollos", entry.getValue().stream()
                            .mapToInt(Tela::getCantRolloIngresado)
                            .sum());

                    // Calculando stockTotal usando BigDecimal
                    BigDecimal stockTotal = entry.getValue().stream()
                            .map(Tela::getStockReal)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    stats.put("stockTotal", stockTotal);

                    return stats;
                })
                .collect(Collectors.toList());
    }

}