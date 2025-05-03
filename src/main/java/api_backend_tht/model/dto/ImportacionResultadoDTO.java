package api_backend_tht.model.dto;

import api_backend_tht.model.entity.Tela;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ImportacionResultadoDTO {
    private int totalRegistros;
    private int registrosImportados;
    private int registrosFallidos;
    private List<String> errores;
    private List<Tela> telasImportadas; // Para rastrear las telas importadas
}