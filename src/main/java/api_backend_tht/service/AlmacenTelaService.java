package api_backend_tht.service;

import api_backend_tht.model.dto.PaginacionResultado;
import api_backend_tht.model.dto.TelaBusquedaDTO;
import api_backend_tht.model.entity.AlmacenTela;
import api_backend_tht.model.entity.EstadoAlmacenTela;
import api_backend_tht.model.entity.Tela;
import api_backend_tht.repository.AlmacenRepository;
import api_backend_tht.repository.AlmacenTelaRepository;
import api_backend_tht.repository.TelaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlmacenTelaService {

    private final AlmacenTelaRepository almacenTelaRepository;
    private final AlmacenRepository almacenRepository;
    private final TelaRepository telaRepository;

    /**
     * Asigna una tela a un almacén
     */
    public Mono<AlmacenTela> asignarTelaAAlmacen(Long almacenId, Long telaId, Double peso) {
        if (peso == null || peso <= 0) {
            return Mono.error(new IllegalArgumentException("El peso debe ser mayor que cero"));
        }

        return Mono.zip(
                almacenRepository.findById(almacenId)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Almacén no encontrado"))),
                telaRepository.findById(telaId)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Tela no encontrada")))
        ).flatMap(tuple -> {
            AlmacenTela almacenTela = AlmacenTela.builder()
                    .almacenId(almacenId)
                    .telaId(telaId)
                    .peso(peso)
                    .fechaAsignacion(LocalDateTime.now())
                    .estado(EstadoAlmacenTela.ACTIVO)  // Añadir el estado
                    .build();

            return almacenTelaRepository.save(almacenTela);
        });
    }

    /**
     * Obtiene todas las telas de un almacén con información detallada
     */
    public Flux<Map<String, Object>> getTelasDeAlmacen(Long almacenId) {
        return almacenTelaRepository.findByAlmacenId(almacenId)
                .flatMap(almacenTela -> {
                    return telaRepository.findById(almacenTela.getTelaId())
                            .map(tela -> {
                                Map<String, Object> result = new HashMap<>();
                                result.put("almacenTela", almacenTela);
                                result.put("tela", tela);
                                return result;
                            });
                });
    }

    /**
     * Actualiza el peso de una tela en un almacén
     */
    public Mono<AlmacenTela> actualizarPeso(Long almacenId, Long telaId, Double nuevoPeso) {
        if (nuevoPeso == null || nuevoPeso < 0) {
            return Mono.error(new IllegalArgumentException("El peso debe ser mayor o igual a cero"));
        }

        return almacenTelaRepository.findByAlmacenIdAndTelaId(almacenId, telaId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró la relación entre almacén y tela")))
                .flatMap(almacenTela -> {
                    almacenTela.setPeso(nuevoPeso);
                    return almacenTelaRepository.save(almacenTela);
                });
    }

    /**
     * Transfiere tela de un almacén a otro
     */
    public Mono<Void> transferirTela(Long almacenOrigenId, Long almacenDestinoId, Long telaId, Double pesoATransferir) {
        if (pesoATransferir <= 0) {
            return Mono.error(new IllegalArgumentException("El peso a transferir debe ser mayor que cero"));
        }

        return almacenTelaRepository.findByAlmacenIdAndTelaIdAndEstado(almacenOrigenId, telaId, EstadoAlmacenTela.ACTIVO)  // Modificar para filtrar por estado activo
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró la tela en el almacén de origen")))
                .flatMap(origen -> {
                    // Verificar que haya suficiente peso para transferir
                    if (origen.getPeso() < pesoATransferir) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "No hay suficiente peso disponible para transferir"));
                    }

                    // Disminuir peso en origen
                    origen.setPeso(origen.getPeso() - pesoATransferir);

                    // Si el peso llega a cero, cambiar el estado a CONSUMIDO
                    if (origen.getPeso() <= 0) {
                        origen.setEstado(EstadoAlmacenTela.CONSUMIDO);
                    }

                    return almacenTelaRepository.findByAlmacenIdAndTelaIdAndEstado(almacenDestinoId, telaId, EstadoAlmacenTela.ACTIVO)  // Modificar para filtrar por estado activo
                            .defaultIfEmpty(AlmacenTela.builder()
                                    .almacenId(almacenDestinoId)
                                    .telaId(telaId)
                                    .peso(0.0)
                                    .fechaAsignacion(LocalDateTime.now())
                                    .estado(EstadoAlmacenTela.ACTIVO)  // Añadir el estado
                                    .build())
                            .flatMap(destino -> {
                                // Aumentar peso en destino
                                destino.setPeso((destino.getPeso() != null ? destino.getPeso() : 0) + pesoATransferir);

                                // Guardar ambos cambios como una transacción
                                return almacenTelaRepository.save(origen)
                                        .then(almacenTelaRepository.save(destino))
                                        .then();
                            });
                });
    }

    /**
     * Busca telas en un almacén con filtros, ordenamiento y paginación
     * @param almacenId ID del almacén
     * @param busqueda Parámetros de búsqueda
     * @return Resultado paginado con las telas y metadata
     */
    public Mono<PaginacionResultado<Map<String, Object>>> buscarTelasEnAlmacen(
            Long almacenId,
            TelaBusquedaDTO busqueda) {

        // Validar parámetros
        if (busqueda.getPagina() < 0) {
            return Mono.error(new IllegalArgumentException("El número de página debe ser mayor o igual a 0"));
        }

        if (busqueda.getTamanoPagina() <= 0) {
            return Mono.error(new IllegalArgumentException("El tamaño de página debe ser mayor a 0"));
        }

        // Obtener todas las telas del almacén
        Flux<Map<String, Object>> telasFlux = obtenerTelasDelAlmacen(almacenId);

        // Aplicar filtro si hay término de búsqueda
        telasFlux = aplicarFiltro(telasFlux, busqueda.getTermino(), busqueda.getCampo());

        // Contador del total de registros
        Mono<Long> totalRegistros = telasFlux.count();

        // Aplicar ordenamiento y paginación
        Flux<Map<String, Object>> telasPaginadas = aplicarOrdenamientoYPaginacion(
                telasFlux,
                busqueda.getOrdenCampo(),
                busqueda.getOrdenDir(),
                busqueda.getPagina(),
                busqueda.getTamanoPagina());

        // Combinar resultados y total
        return Mono.zip(telasPaginadas.collectList(), totalRegistros)
                .map(tuple -> PaginacionResultado.<Map<String, Object>>builder()
                        .datos(tuple.getT1())
                        .total(tuple.getT2())
                        .pagina(busqueda.getPagina())
                        .tamanoPagina(busqueda.getTamanoPagina())
                        .build());
    }

    /**
     * Obtiene las telas asociadas a un almacén específico
     */
    private Flux<Map<String, Object>> obtenerTelasDelAlmacen(Long almacenId) {
        return almacenTelaRepository.findByAlmacenIdAndEstado(almacenId, EstadoAlmacenTela.ACTIVO)  // Modificar para filtrar por estado activo
                .flatMap(almacenTela -> telaRepository.findById(almacenTela.getTelaId())
                        .map(tela -> {
                            Map<String, Object> result = new HashMap<>();
                            result.put("almacenTela", almacenTela);
                            result.put("tela", tela);
                            return result;
                        }));
    }

    /**
     * Aplica filtros a la lista de telas según el término y campo
     */
    private Flux<Map<String, Object>> aplicarFiltro(
            Flux<Map<String, Object>> telasFlux,
            String termino,
            String campo) {

        if (termino == null || termino.trim().isEmpty()) {
            return telasFlux;
        }

        String terminoLower = termino.toLowerCase();
        return telasFlux.filter(item -> {
            Tela tela = (Tela) item.get("tela");

            // Filtrar por un campo específico
            if (!"todos".equals(campo)) {
                return filtrarPorCampoEspecifico(tela, campo, terminoLower);
            }

            // Filtrar por todos los campos relevantes
            return filtrarPorTodosCampos(tela, terminoLower);
        });
    }

    /**
     * Filtra una tela por un campo específico
     */
    private boolean filtrarPorCampoEspecifico(Tela tela, String campo, String termino) {
        switch (campo) {
            case "numGuia":
                return contieneCadena(tela.getNumGuia(), termino);
            case "partida":
                return contieneCadena(tela.getPartida(), termino);
            case "proveedor":
                return contieneCadena(tela.getProveedor(), termino);
            case "cliente":
                return contieneCadena(tela.getCliente(), termino);
            default:
                return false;
        }
    }

    /**
     * Filtra una tela por todos los campos relevantes
     */
    private boolean filtrarPorTodosCampos(Tela tela, String termino) {
        return contieneCadena(tela.getNumGuia(), termino) ||
                contieneCadena(tela.getPartida(), termino) ||
                contieneCadena(tela.getProveedor(), termino) ||
                contieneCadena(tela.getCliente(), termino);
    }

    /**
     * Verifica si una cadena contiene otra, con manejo de nulos
     */
    private boolean contieneCadena(String cadena, String subcadena) {
        return cadena != null && cadena.toLowerCase().contains(subcadena);
    }

    /**
     * Aplica ordenamiento y paginación al flujo de telas
     */
    private Flux<Map<String, Object>> aplicarOrdenamientoYPaginacion(
            Flux<Map<String, Object>> telasFlux,
            String ordenCampo,
            String ordenDir,
            int pagina,
            int tamanoPagina) {

        Comparator<Map<String, Object>> comparator = crearComparador(ordenCampo, ordenDir);

        return telasFlux
                .sort(comparator)
                .skip(pagina * tamanoPagina)
                .take(tamanoPagina);
    }

    /**
     * Crea un comparador para ordenar las telas
     */
    private Comparator<Map<String, Object>> crearComparador(String ordenCampo, String ordenDir) {
        return (item1, item2) -> {
            Tela tela1 = (Tela) item1.get("tela");
            Tela tela2 = (Tela) item2.get("tela");

            int resultado = 0;

            switch (ordenCampo) {
                case "numGuia":
                    resultado = compareNullableStrings(tela1.getNumGuia(), tela2.getNumGuia());
                    break;
                case "partida":
                    resultado = compareNullableStrings(tela1.getPartida(), tela2.getPartida());
                    break;
                case "proveedor":
                    resultado = compareNullableStrings(tela1.getProveedor(), tela2.getProveedor());
                    break;
                case "cliente":
                    resultado = compareNullableStrings(tela1.getCliente(), tela2.getCliente());
                    break;
                case "fechaIngreso":
                    resultado = compareNullableDates(tela1.getFechaIngreso(), tela2.getFechaIngreso());
                    break;
                default:
                    resultado = compareNullableDates(tela1.getFechaIngreso(), tela2.getFechaIngreso());
            }

            return "desc".equals(ordenDir) ? -resultado : resultado;
        };
    }

    /**
     * Compara dos cadenas con manejo de nulos
     */
    private int compareNullableStrings(String s1, String s2) {
        if (s1 == null && s2 == null) return 0;
        if (s1 == null) return -1;
        if (s2 == null) return 1;
        return s1.compareToIgnoreCase(s2);
    }

    /**
     * Compara dos fechas con manejo de nulos
     */
    private int compareNullableDates(LocalDate d1, LocalDate d2) {
        if (d1 == null && d2 == null) return 0;
        if (d1 == null) return -1;
        if (d2 == null) return 1;
        return d1.compareTo(d2);
    }
}