package api_backend_tht.controller;

import api_backend_tht.model.dto.UnidadMedidaCreateDTO;
import api_backend_tht.model.dto.UnidadMedidaDTO;
import api_backend_tht.service.UnidadMedidaService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/unidadesmedias")
@AllArgsConstructor
public class UnidadMedidaController {

    private final UnidadMedidaService unidadMedidaService;

    @GetMapping
    public Flux<UnidadMedidaDTO> getAll() {
        return unidadMedidaService.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<UnidadMedidaDTO>> getById(@PathVariable Long id) {
        return unidadMedidaService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UnidadMedidaDTO> create(@Valid @RequestBody UnidadMedidaCreateDTO dto) {
        return unidadMedidaService.create(dto);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<UnidadMedidaDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody UnidadMedidaCreateDTO dto) {
        return unidadMedidaService.update(id, dto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable Long id) {
        return unidadMedidaService.delete(id);
    }
}