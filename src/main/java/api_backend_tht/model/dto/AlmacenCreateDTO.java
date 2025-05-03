package api_backend_tht.model.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AlmacenCreateDTO {

    @NotNull(message = "El código de almacén es obligatorio")
    private Integer codigoAlmacen;

    @NotBlank(message = "El nombre del almacén es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nombreAlmacen;

    @NotBlank(message = "La abreviatura es obligatoria")
    @Size(max = 10, message = "La abreviatura no debe exceder los 10 caracteres")
    private String abreviatura;

    private String descripcion;

    @NotBlank(message = "El estado es obligatorio")
    @Pattern(regexp = "ACTIVO|INACTIVO", message = "El estado debe ser ACTIVO o INACTIVO")
    private String estado;

    @NotBlank(message = "El tipo de almacén es obligatorio")
    @Pattern(regexp = "PRINCIPAL|SECUNDARIO|TEMPORAL", message = "El tipo de almacén debe ser PRINCIPAL, SECUNDARIO o TEMPORAL")
    private String tipoAlmacen;

    @NotBlank(message = "El local es obligatorio")
    private String local;
}