package api_backend_tht.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;


@Data
public class TelaCreateDTO {

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
}
