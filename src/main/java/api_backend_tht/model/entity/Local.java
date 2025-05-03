package api_backend_tht.model.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table("locales")
public class Local {

    @Id
    private Long id;
    private String codigo;
    private String descripcion;
    private String direccion;
    private String telefono;

}
