package api_backend_tht.controller;

import api_backend_tht.model.dto.AlmacenCreateDTO;
import api_backend_tht.model.dto.AlmacenDTO;
import api_backend_tht.model.dto.CambioEstadoDTO;
import api_backend_tht.service.AlmacenService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/api/almacenes")
@AllArgsConstructor
public class AlmacenController {

    private final AlmacenService almacenService;

    @GetMapping
    public Flux<AlmacenDTO> findAll() {
        return almacenService.findAll();
    }

    @GetMapping("/{id}")
    public Mono<AlmacenDTO> findById(@PathVariable Long id) {
        return almacenService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AlmacenDTO> create(@Valid @RequestBody AlmacenCreateDTO dto) {
        return almacenService.create(dto);
    }

    @PutMapping("/{id}")
    public Mono<AlmacenDTO> update(@PathVariable Long id, @Valid @RequestBody AlmacenCreateDTO dto) {
        return almacenService.update(id, dto);
    }

    @PatchMapping("/{id}/cambiar-estado")
    public Mono<AlmacenDTO> cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoDTO dto) {
        return almacenService.cambiarEstado(id, dto.getEstado());
    }
}