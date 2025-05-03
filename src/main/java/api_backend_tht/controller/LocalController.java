package api_backend_tht.controller;

import api_backend_tht.model.dto.LocalCreateDTO;
import api_backend_tht.model.dto.LocalDTO;
import api_backend_tht.service.LocalService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/locales")
@AllArgsConstructor
public class LocalController {


    private final LocalService localService;

    @GetMapping
    public Flux<LocalDTO> getAll() {
        return localService.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<LocalDTO>> getById(@PathVariable Long id) {
        return localService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<LocalDTO> create(@Valid @RequestBody LocalCreateDTO dto) {
        return localService.create(dto);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<LocalDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody LocalCreateDTO dto) {
        return localService.update(id, dto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@PathVariable Long id) {
        return localService.delete(id);
    }
}
