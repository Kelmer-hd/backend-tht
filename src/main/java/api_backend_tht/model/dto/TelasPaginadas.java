package api_backend_tht.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Clase para representar resultados paginados de telas
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TelasPaginadas {

    /**
     * Lista de telas para la página actual
     */
    private List<TelaDTO> datos;

    /**
     * Número total de elementos en todas las páginas
     */
    private int total;

    /**
     * Número de página actual (empezando en 0)
     */
    private int pagina;

    /**
     * Tamaño de la página (elementos por página)
     */
    private int tamanoPagina;

    /**
     * Número total de páginas
     */
    private int totalPaginas;
}