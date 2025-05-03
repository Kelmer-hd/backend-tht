package api_backend_tht.model.dto;

import lombok.Data;

@Data
public class AlmacenDTO {

    private Long id;
    private int codigoAlmacen;
    private String nombreAlmacen;
    private String abreviatura;
    private String descripcion;
    private String estado;
    private String tipoAlmacen;
    private String local;

}
