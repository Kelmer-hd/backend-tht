package api_backend_tht.mapper;

import api_backend_tht.model.dto.AlmacenDTO;
import api_backend_tht.model.dto.UnidadMedidaCreateDTO;
import api_backend_tht.model.dto.UnidadMedidaDTO;
import api_backend_tht.model.entity.UnidadesMedida;
import org.springframework.stereotype.Component;

@Component
public class UnidaMedidaMapper {

    public UnidadMedidaDTO toDTO(UnidadesMedida entity){
        UnidadMedidaDTO dto = new UnidadMedidaDTO();
        dto.setId(entity.getId());
        dto.setCodigo(entity.getCodigo());
        dto.setNombre(entity.getNombre());
        dto.setDimension(entity.getDimension());
        dto.setIngles(entity.getIngles());
        dto.setAbreviatura(entity.getAbreviatura());
        dto.setTipo(entity.getTipo());
        dto.setAceptaRedondeoPorExceso(entity.isAceptaRedondeoPorExceso());
        dto.setFactorRedondeo(entity.getFactorRedondeo());
        return dto;
    }

    public UnidadesMedida toEntity(UnidadMedidaCreateDTO dto){
        UnidadesMedida entity = new UnidadesMedida();
        entity.setCodigo(dto.getCodigo());
        entity.setNombre(dto.getNombre());
        entity.setDimension(dto.getDimension());
        entity.setIngles(dto.getIngles());
        entity.setAbreviatura(dto.getAbreviatura());
        entity.setTipo(dto.getTipo());
        entity.setAceptaRedondeoPorExceso(dto.isAceptaRedondeoPorExceso());
        entity.setFactorRedondeo(dto.getFactorRedondeo());
        return entity;
    }
}
