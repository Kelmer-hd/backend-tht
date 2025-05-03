package api_backend_tht.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO para filtrar movimientos en búsquedas
 */
@Data
@Builder
public class MovimientoTelaFiltroDTO {
    private Long telaId;
    private String tipoMovimiento;
    private String areaOrigen;
    private String areaDestino;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String usuarioResponsable;
    private String estado;
}