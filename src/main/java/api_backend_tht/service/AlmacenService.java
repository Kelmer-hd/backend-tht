package api_backend_tht.service;

import api_backend_tht.exception.ResourceNotFoudException;
import api_backend_tht.mapper.AlmacenMapper;
import api_backend_tht.model.dto.AlmacenCreateDTO;
import api_backend_tht.model.dto.AlmacenDTO;
import api_backend_tht.model.entity.Almacen;
import api_backend_tht.repository.AlmacenRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;


@Service
@AllArgsConstructor
public class AlmacenService {

    private final AlmacenRepository almacenRepository;
    private final AlmacenMapper almacenMapper;

    public Flux<AlmacenDTO> findAll() {
        return almacenRepository.findAll()
                .map(almacenMapper::toDTO);
    }

    public Mono<AlmacenDTO> findById(Long id) {
        return almacenRepository.findById(id)
                .map(almacenMapper::toDTO)
                .switchIfEmpty(Mono.error(new ResourceNotFoudException("No se encontró el almacén con ID: " + id)));
    }

    public Mono<AlmacenDTO> create(AlmacenCreateDTO dto) {
        Almacen almacen = almacenMapper.toEntity(dto);
        almacen.setFechaCreacion(LocalDateTime.now());
        almacen.setFechaActualizacion(LocalDateTime.now());
        almacen.setEstado("ACTIVO");
        return almacenRepository.save(almacen)
                .map(almacenMapper::toDTO);
    }

    public Mono<AlmacenDTO> update(Long id, AlmacenCreateDTO dto) {
        return almacenRepository.findById(id)
                .flatMap(almacen -> {
                    // actualizar campos
                    almacen.setCodigoAlmacen(dto.getCodigoAlmacen());
                    almacen.setNombreAlmacen(dto.getNombreAlmacen());
                    almacen.setAbreviatura(dto.getAbreviatura());
                    almacen.setDescripcion(dto.getDescripcion());
                    almacen.setEstado(dto.getEstado());
                    almacen.setTipoAlmacen(dto.getTipoAlmacen());
                    almacen.setLocal(dto.getLocal());
                    almacen.setFechaActualizacion(LocalDateTime.now());

                    return almacenRepository.save(almacen);
                })
                .map(almacenMapper::toDTO)
                .switchIfEmpty(Mono.error(new ResourceNotFoudException("No se encontró el almacén con ID: " + id)));
    }

    public Mono<AlmacenDTO> cambiarEstado(Long id, String estado) {
        // La validación ya se hace en el DTO con anotaciones
        return almacenRepository.findById(id)
                .flatMap(almacen -> {
                    almacen.setEstado(estado);
                    almacen.setFechaActualizacion(LocalDateTime.now());
                    return almacenRepository.save(almacen);
                })
                .map(almacenMapper::toDTO)
                .switchIfEmpty(Mono.error(new ResourceNotFoudException("No se encontró el almacén con ID: " + id)));
    }
}