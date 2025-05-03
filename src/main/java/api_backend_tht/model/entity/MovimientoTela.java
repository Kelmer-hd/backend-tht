package api_backend_tht.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("movimientos_tela")
public class MovimientoTela {

    @Id
    private Long id;
    private Long telaId;
    private String areaOrigen;
    private String areaDestino;
    private BigDecimal cantidad;
    private LocalDateTime fechaMovimiento;
    private String tipoMovimiento;
    private String referenciaDocumento;
    private String usuarioResponsable;
    private String estado;
    private String observaciones;

    @Transient
    private Tela tela;
}