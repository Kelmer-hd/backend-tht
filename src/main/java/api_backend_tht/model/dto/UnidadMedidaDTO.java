package api_backend_tht.model.dto;
import api_backend_tht.model.entity.UMDimenciones;
import lombok.Data;

@Data
public class UnidadMedidaDTO {

    private Long id;
    private String codigo;
    private String nombre;
    private UMDimenciones dimension;
    private String ingles;
    private String abreviatura;
    private String tipo;
    private boolean aceptaRedondeoPorExceso;
    private int factorRedondeo;
}
