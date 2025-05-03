package api_backend_tht.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LocalCreateDTO {

    @NotNull(message = "El código es obligatio")
    private String codigo;

    @NotNull(message = "La descripción es obligatio")
    private String descripcion;

    @NotNull(message = "La dirección es obligatio")
    private String direccion;

    @NotNull(message = "El teléfono es obligatio")
    private String telefono;
}
