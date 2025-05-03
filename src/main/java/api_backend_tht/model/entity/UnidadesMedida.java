package api_backend_tht.model.entity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("unidades_medida")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnidadesMedida {

    @Id
    private Long id;
    private String codigo;
    private String nombre;
    private UMDimenciones dimension;
    private String ingles;
    private String abreviatura;
    private String tipo;
    private boolean aceptaRedondeoPorExceso;
    private int factorRedondeo;
}