package api_backend_tht.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelaFiltroDTO {

    private String numGuia;
    private String proveedor;
    private String cliente;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String descripcion;
    private String os;
    private String partida;
    private String estado;
    private String almacen;
    private String tipoTela;
}