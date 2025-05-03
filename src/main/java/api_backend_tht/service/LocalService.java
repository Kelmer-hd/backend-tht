package api_backend_tht.service;

import api_backend_tht.mapper.LocalMapper;
import api_backend_tht.model.dto.LocalCreateDTO;
import api_backend_tht.model.dto.LocalDTO;
import api_backend_tht.model.entity.Local;
import api_backend_tht.repository.LocalRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class LocalService {

    private final LocalRepository localRepository;
    private final LocalMapper localMapper;

    public Flux<LocalDTO> findAll(){
        return  localRepository.findAll()
                .map(localMapper::toDTO);
    }


    public Mono<LocalDTO> findById(Long id){
        return localRepository.findById(id)
                .map(localMapper::toDTO)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontro el local"
                )));
    }

    public Mono<LocalDTO> create(LocalCreateDTO dto) {
        Local local = localMapper.toEntity(dto);
        return localRepository.save(local)
                .map(localMapper::toDTO);
    }

    public Mono<LocalDTO> update(Long id, LocalCreateDTO dto) {
        return localRepository.findById(id)
                .flatMap(local -> {
                    // actualizar campos
                    local.setCodigo(dto.getCodigo());
                    local.setDescripcion(dto.getDescripcion());
                    local.setDireccion(dto.getDireccion());
                    local.setTelefono(dto.getTelefono());

                    return localRepository.save(local);
                })
                .map(localMapper::toDTO)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró el local")));
    }

    public Mono<Void> delete(Long id) {
        return localRepository.findById(id)
                .flatMap(localRepository::delete)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró el local")));
    }
}
