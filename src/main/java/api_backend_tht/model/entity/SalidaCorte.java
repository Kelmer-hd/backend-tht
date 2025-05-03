package api_backend_tht.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("salidas_corte")
public class SalidaCorte {
    @Id
    private Long id;

    private Long telaId;
    private String servicioCorte;
    private LocalDate fechaSalida;
    private String notaSalida;
    private String op;
    private BigDecimal salidaCorte;
    private String areaDestino;
    private String estado;
    private String usuarioResponsable;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaActualizacion;

    @Transient
    private Tela tela;
}