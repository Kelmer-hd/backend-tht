package api_backend_tht.service;

import api_backend_tht.mapper.UnidaMedidaMapper;
import api_backend_tht.model.dto.UnidadMedidaCreateDTO;
import api_backend_tht.model.dto.UnidadMedidaDTO;
import api_backend_tht.model.entity.UnidadesMedida;
import api_backend_tht.repository.UnidaMedidaRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class UnidadMedidaService {

    private final UnidaMedidaRepository unidaMedidaRepository;
    private final UnidaMedidaMapper unidaMedidaMapper;

    public Flux<UnidadMedidaDTO> findAll() {
        return unidaMedidaRepository.findAll()
                .map(unidaMedidaMapper::toDTO);
    }

    public Mono<UnidadMedidaDTO> findById(Long id) {
        return unidaMedidaRepository.findById(id)
                .map(unidaMedidaMapper::toDTO)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró la unidad de medida")));
    }

    public Mono<UnidadMedidaDTO> create(UnidadMedidaCreateDTO dto) {
        UnidadesMedida unidadesMedida = unidaMedidaMapper.toEntity(dto);
        return unidaMedidaRepository.save(unidadesMedida)
                .map(unidaMedidaMapper::toDTO);
    }

    public Mono<UnidadMedidaDTO> update(Long id, UnidadMedidaCreateDTO dto) {
        return unidaMedidaRepository.findById(id)
                .flatMap(unidadesMedida -> {
                    // actualizar campos
                    unidadesMedida.setCodigo(dto.getCodigo());
                    unidadesMedida.setNombre(dto.getNombre());
                    unidadesMedida.setDimension(dto.getDimension());
                    unidadesMedida.setIngles(dto.getIngles());
                    unidadesMedida.setAbreviatura(dto.getAbreviatura());
                    unidadesMedida.setTipo(dto.getTipo());
                    unidadesMedida.setAceptaRedondeoPorExceso(dto.isAceptaRedondeoPorExceso());
                    unidadesMedida.setFactorRedondeo(dto.getFactorRedondeo());

                    return unidaMedidaRepository.save(unidadesMedida);
                })
                .map(unidaMedidaMapper::toDTO)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró la unidad de medida")));
    }

    public Mono<Void> delete(Long id) {
        return unidaMedidaRepository.findById(id)
                .flatMap(unidaMedidaRepository::delete)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró la unidad de medida")));
    }
}