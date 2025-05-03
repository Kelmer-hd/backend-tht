package api_backend_tht.model.dto;

import api_backend_tht.model.entity.UMDimenciones;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UnidadMedidaCreateDTO {

    @NotNull(message = "El código es obligatio")
    private String codigo;

    @NotNull(message = "El nombre es obligatio")
    private String nombre;

    @NotNull(message = "La dimención es obligatia")
    private UMDimenciones dimension;

    @NotNull(message = "El campo ingles es obligatio")
    private String ingles;

    @NotNull(message = "La abreviatura es obligatia")
    private String abreviatura;

    @NotNull(message = "El Tipo es obligatio")
    private String tipo;

    @NotNull(message = "El nombre es obligatio")
    private boolean aceptaRedondeoPorExceso;

    @NotNull(message = "El factor de redondeo es obligatio")
    private int factorRedondeo;
}
