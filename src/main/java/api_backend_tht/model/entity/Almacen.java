package api_backend_tht.model.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("almacenes") // Usar @Table de Spring Data R2DBC
public class Almacen {

    @Id
    private Long id;
    private int codigoAlmacen;
    private String nombreAlmacen;
    private String abreviatura;
    private String descripcion;
    private String estado;
    private String tipoAlmacen;
    private String local;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}