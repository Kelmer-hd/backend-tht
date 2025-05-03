package api_backend_tht.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaginacionResultado<T> {
    private List<T> datos;
    private Long total;
    private Integer pagina;
    private Integer tamanoPagina;

    public Integer getTotalPaginas() {
        if (total == 0) return 0;
        return (int) Math.ceil((double) total / tamanoPagina);
    }
}