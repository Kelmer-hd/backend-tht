package api_backend_tht.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para la creación/modificación de movimientos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoTelaDTO {
    @NotNull(message = "El ID de la tela es obligatorio")
    private Long telaId;

    @NotNull(message = "El área de origen es obligatoria")
    private String areaOrigen;

    @NotNull(message = "El área de destino es obligatoria")
    private String areaDestino;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor que cero")
    private BigDecimal cantidad;  // Cambiado de Double a BigDecimal

    @NotNull(message = "El tipo de movimiento es obligatorio")
    private String tipoMovimiento;

    private String referenciaDocumento;

    @NotNull(message = "El usuario responsable es obligatorio")
    private String usuarioResponsable;

    private String observaciones;
}
