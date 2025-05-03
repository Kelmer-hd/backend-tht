package api_backend_tht.controller;

import api_backend_tht.model.dto.PaginacionResultado;
import api_backend_tht.model.dto.TelaBusquedaDTO;
import api_backend_tht.model.entity.AlmacenTela;
import api_backend_tht.service.AlmacenTelaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/almacen-telas")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class AlmacenTelaController {

    private final AlmacenTelaService almacenTelaService;

    @GetMapping("/almacen/{almacenId}")
    public Flux<Map<String, Object>> getTelasDeAlmacen(@PathVariable Long almacenId) {
        return almacenTelaService.getTelasDeAlmacen(almacenId);
    }

    @PostMapping("/asignar")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AlmacenTela> asignarTelaAAlmacen(@RequestBody Map<String, Object> datos) {
        Long almacenId = Long.valueOf(datos.get("almacenId").toString());
        Long telaId = Long.valueOf(datos.get("telaId").toString());
        Double peso = Double.valueOf(datos.get("peso").toString());

        return almacenTelaService.asignarTelaAAlmacen(almacenId, telaId, peso);
    }

    @PatchMapping("/actualizar-peso") // Cambiado el nombre del endpoint
    public Mono<AlmacenTela> actualizarPeso(@RequestBody Map<String, Object> datos) { // Cambiado el nombre del método
        Long almacenId = Long.valueOf(datos.get("almacenId").toString());
        Long telaId = Long.valueOf(datos.get("telaId").toString());
        Double peso = Double.valueOf(datos.get("peso").toString()); // Cambiado de cantidad a peso

        return almacenTelaService.actualizarPeso(almacenId, telaId, peso); // Cambiar este método en el servicio
    }

    @PostMapping("/transferir")
    public Mono<Void> transferirTela(@RequestBody Map<String, Object> datos) {
        Long almacenOrigenId = Long.valueOf(datos.get("almacenOrigenId").toString());
        Long almacenDestinoId = Long.valueOf(datos.get("almacenDestinoId").toString());
        Long telaId = Long.valueOf(datos.get("telaId").toString());
        Double peso = Double.valueOf(datos.get("peso").toString()); // Cambiado de cantidad a peso

        return almacenTelaService.transferirTela(almacenOrigenId, almacenDestinoId, telaId, peso);
    }

    /**
     * Busca telas en un almacén con filtros, ordenamiento y paginación
     * @param almacenId ID del almacén
     * @param busqueda Parámetros de búsqueda
     * @return Resultado paginado de telas
     */
    @PostMapping("/almacen/{almacenId}/buscar")
    public Mono<PaginacionResultado<Map<String, Object>>> buscarTelasEnAlmacen(
            @PathVariable Long almacenId,
            @RequestBody(required = false) TelaBusquedaDTO busqueda) {

        // Si no se proporciona un objeto de búsqueda, crear uno con valores predeterminados
        if (busqueda == null) {
            busqueda = TelaBusquedaDTO.builder().build();
        }

        return almacenTelaService.buscarTelasEnAlmacen(almacenId, busqueda);
    }
}