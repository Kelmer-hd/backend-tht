package api_backend_tht.mapper;

import api_backend_tht.model.dto.LocalCreateDTO;
import api_backend_tht.model.dto.LocalDTO;
import api_backend_tht.model.entity.Local;
import org.springframework.stereotype.Component;

@Component
public class LocalMapper {

    public LocalDTO toDTO(Local entity){
        LocalDTO dto = new LocalDTO();
        dto.setCodigo(entity.getCodigo());
        dto.setDescripcion(entity.getDescripcion());
        dto.setDireccion(entity.getDireccion());
        dto.setTelefono(entity.getTelefono());
        return dto;
    }

    public Local toEntity(LocalCreateDTO dto){
        Local entity = new Local();
        entity.setCodigo(dto.getCodigo());
        entity.setDescripcion(dto.getDescripcion());
        entity.setDireccion(dto.getDireccion());
        entity.setTelefono(dto.getTelefono());
        return entity;
    }

}