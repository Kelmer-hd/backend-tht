package api_backend_tht.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelaBusquedaDTO {
    private String termino;
    private String campo = "todos";
    private String ordenCampo = "fechaIngreso";
    private String ordenDir = "desc";
    private int pagina = 0;
    private int tamanoPagina = 10;
}