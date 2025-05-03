package api_backend_tht.mapper;

import api_backend_tht.model.dto.AlmacenCreateDTO;
import api_backend_tht.model.dto.AlmacenDTO;
import api_backend_tht.model.entity.Almacen;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AlmacenMapper {

    public AlmacenDTO toDTO(Almacen entity) {
        AlmacenDTO dto = new AlmacenDTO();
        dto.setId(entity.getId());
        dto.setCodigoAlmacen(entity.getCodigoAlmacen());
        dto.setNombreAlmacen(entity.getNombreAlmacen());
        dto.setAbreviatura(entity.getAbreviatura());
        dto.setDescripcion(entity.getDescripcion());
        dto.setEstado(entity.getEstado());
        dto.setTipoAlmacen(entity.getTipoAlmacen());
        dto.setLocal(entity.getLocal());
        return dto;
    }

    public Almacen toEntity(AlmacenCreateDTO dto) {
        Almacen entity = new Almacen();
        entity.setCodigoAlmacen(dto.getCodigoAlmacen());
        entity.setNombreAlmacen(dto.getNombreAlmacen());
        entity.setAbreviatura(dto.getAbreviatura());
        entity.setDescripcion(dto.getDescripcion());
        entity.setEstado(dto.getEstado());
        entity.setTipoAlmacen(dto.getTipoAlmacen());
        entity.setLocal(dto.getLocal());
        entity.setFechaCreacion(LocalDateTime.now());
        entity.setFechaActualizacion(LocalDateTime.now());
        return entity;
    }
}
