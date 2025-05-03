package api_backend_tht.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("telas")
public class Tela {

    @Id
    private Long id;
    private String numGuia;
    private String partida;
    private String os;
    private String proveedor;
    private LocalDate fechaIngreso;
    private String cliente;
    private String marca;
    private String op;
    private String tipoTela;
    private String descripcion;
    private String ench;
    private int cantRolloIngresado;
    private BigDecimal pesoIngresado;
    private BigDecimal stockReal;
    private String estado;
    private String almacen;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public void aumentarStock(BigDecimal cantidad) {
        this.stockReal = this.stockReal.add(cantidad);
        this.pesoIngresado = this.pesoIngresado.add(cantidad);
    }

    public void disminuirStock(BigDecimal cantidad) {
        this.stockReal = this.stockReal.subtract(cantidad);
        this.pesoIngresado = this.pesoIngresado.subtract(cantidad);
    }
}