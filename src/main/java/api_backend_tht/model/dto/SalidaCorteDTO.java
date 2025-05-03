package api_backend_tht.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class SalidaCorteDTO {
    @NotNull(message = "El ID de tela es obligatorio")
    private Long telaId;

    @NotBlank(message = "El servicio de corte es obligatorio")
    @Size(max = 100, message = "El servicio de corte no puede exceder los 100 caracteres")
    private String servicioCorte;

    @NotNull(message = "La fecha de salida es obligatoria")
    private LocalDate fechaSalida;

    @NotBlank(message = "La nota de salida es obligatoria")
    @Size(max = 50, message = "La nota de salida no puede exceder los 50 caracteres")
    private String notaSalida;

    @NotBlank(message = "La OP es obligatoria")
    @Size(max = 50, message = "La OP no puede exceder los 50 caracteres")
    private String op;

    @NotNull(message = "La cantidad de salida es obligatoria")
    @Positive(message = "La cantidad debe ser mayor que cero")
    private BigDecimal salidaCorte;

    @NotBlank(message = "El área de destino es obligatoria")
    private String areaDestino;

    private String usuarioResponsable;
}