package api_backend_tht.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("almacen_telas")
public class AlmacenTela {
    @Id
    private Long id;
    private Long almacenId;
    private Long telaId;
    private Double peso;
    private LocalDateTime fechaAsignacion;
    private EstadoAlmacenTela estado;

    @Transient
    private Almacen almacen;

    @Transient
    private Tela tela;
}