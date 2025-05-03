package api_backend_tht.service;

import api_backend_tht.exception.ResourceNotFoudException;
import api_backend_tht.model.dto.ImportacionResultadoDTO;
import api_backend_tht.model.entity.AlmacenTela;
import api_backend_tht.model.entity.EstadoAlmacenTela;
import api_backend_tht.model.entity.Tela;
import api_backend_tht.repository.AlmacenRepository;
import api_backend_tht.repository.AlmacenTelaRepository;
import api_backend_tht.repository.TelaRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelaImportacionService {

    private final TelaRepository telaRepository;
    private final AlmacenRepository almacenRepository;
    private final AlmacenTelaRepository almacenTelaRepository;

    private static final String ESTADO_ACTIVO = "ACTIVO";
    private static final String MSG_ERROR_ALMACEN_NO_ENCONTRADO = "No se encontró el almacén con ID: ";
    /**
     * Importa telas desde Excel asociándolas a un almacén específico
     * @param fileContent Contenido del archivo Excel
     * @param almacenId ID del almacén donde se importarán las telas
     * @return Resultado de la importación
     */
    public Mono<ImportacionResultadoDTO> importarTelasDesdeExcelParaAlmacen(byte[] fileContent, Long almacenId) {
        return almacenRepository.findById(almacenId)
                .switchIfEmpty(Mono.error(new ResourceNotFoudException(MSG_ERROR_ALMACEN_NO_ENCONTRADO + almacenId)))
                .flatMap(almacen -> procesarArchivoExcelReactivo(fileContent)
                        .flatMap(resultado -> guardarTelasYCrearRelaciones(resultado, almacenId, almacen.getNombreAlmacen()))
                );
    }

    /**
     * Procesa el archivo Excel de manera reactiva
     * @param fileContent Contenido del archivo Excel
     * @return Mono con el resultado del procesamiento
     */
    private Mono<ResultadoProcesamiento> procesarArchivoExcelReactivo(byte[] fileContent) {
        return Mono.fromCallable(() -> extraerTelasDesdeExcel(fileContent))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    log.error("Error al procesar el archivo Excel", e);
                    List<String> errores = new ArrayList<>();
                    errores.add("Error al procesar el archivo: " + e.getMessage());

                    return Mono.just(ResultadoProcesamiento.builder()
                            .telasParaGuardar(new ArrayList<>())
                            .resultado(ImportacionResultadoDTO.builder()
                                    .totalRegistros(0)
                                    .registrosImportados(0)
                                    .registrosFallidos(0)
                                    .errores(errores)
                                    .telasImportadas(new ArrayList<>())
                                    .build())
                            .build());
                });
    }

    /**
     * Guarda las telas extraídas y crea las relaciones con el almacén
     * @param resultado Resultado del procesamiento
     * @param almacenId ID del almacén
     * @param nombreAlmacen Nombre del almacén
     * @return Mono con el resultado final de la importación
     */
    private Mono<ImportacionResultadoDTO> guardarTelasYCrearRelaciones(
            ResultadoProcesamiento resultado, Long almacenId, String nombreAlmacen) {

        List<Tela> telasParaGuardar = resultado.getTelasParaGuardar();
        ImportacionResultadoDTO resultadoParcial = resultado.getResultado();

        if (telasParaGuardar.isEmpty()) {
            return Mono.just(resultadoParcial);
        }

        // Asignar almacén a las telas si aún no está establecido
        telasParaGuardar.forEach(tela -> {
            if (tela.getAlmacen() == null || tela.getAlmacen().trim().isEmpty()) {
                tela.setAlmacen(nombreAlmacen);
            }
        });

        return telaRepository.saveAll(telasParaGuardar)
                .collectList()
                .flatMap(telasGuardadas -> {
                    ImportacionResultadoDTO resultadoFinal = ImportacionResultadoDTO.builder()
                            .totalRegistros(resultadoParcial.getTotalRegistros())
                            .registrosImportados(telasGuardadas.size())
                            .registrosFallidos(resultadoParcial.getRegistrosFallidos())
                            .errores(resultadoParcial.getErrores())
                            .telasImportadas(telasGuardadas)
                            .build();

                    if (telasGuardadas.isEmpty()) {
                        return Mono.just(resultadoFinal);
                    }

                    return crearRelacionesAlmacenTelas(telasGuardadas, almacenId)
                            .thenReturn(resultadoFinal);
                });
    }
    /**
     * Extrae las telas desde el archivo Excel
     * @param fileContent Contenido del archivo Excel
     * @return Resultado del procesamiento con las telas extraídas
     */
    private ResultadoProcesamiento extraerTelasDesdeExcel(byte[] fileContent) {
        List<String> errores = new ArrayList<>();
        int totalRegistros = 0;
        int registrosFallidos = 0;
        List<Tela> telasParaGuardar = new ArrayList<>();

        try (InputStream is = new ByteArrayInputStream(fileContent);
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0); // Primera hoja

            // Determinar el total de filas (excluyendo encabezados)
            totalRegistros = sheet.getPhysicalNumberOfRows() - 1;
            if (totalRegistros <= 0) {
                errores.add("El archivo no contiene datos para importar");
                return crearResultadoVacio(errores);
            }

            // Iterar por cada fila (omitiendo la fila de encabezados)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    Tela tela = mapRowToTela(row);
                    telasParaGuardar.add(tela);
                } catch (Exception e) {
                    registrosFallidos++;
                    errores.add("Error en fila " + (i + 1) + ": " + e.getMessage());
                    log.error("Error procesando fila {}: {}", i + 1, e.getMessage());
                }
            }
        } catch (Exception e) {
            errores.add("Error al procesar el archivo: " + e.getMessage());
            log.error("Error al procesar el archivo Excel", e);
        }

        ImportacionResultadoDTO resultadoParcial = ImportacionResultadoDTO.builder()
                .totalRegistros(totalRegistros)
                .registrosImportados(0) // Se actualizará después de guardar
                .registrosFallidos(registrosFallidos)
                .errores(errores)
                .telasImportadas(new ArrayList<>()) // Se actualizará después de guardar
                .build();

        return ResultadoProcesamiento.builder()
                .telasParaGuardar(telasParaGuardar)
                .resultado(resultadoParcial)
                .build();
    }

    /**
     * Crea un resultado vacío con los errores proporcionados
     * @param errores Lista de errores
     * @return Resultado del procesamiento vacío
     */
    private ResultadoProcesamiento crearResultadoVacio(List<String> errores) {
        return ResultadoProcesamiento.builder()
                .telasParaGuardar(new ArrayList<>())
                .resultado(ImportacionResultadoDTO.builder()
                        .totalRegistros(0)
                        .registrosImportados(0)
                        .registrosFallidos(0)
                        .errores(errores)
                        .telasImportadas(new ArrayList<>())
                        .build())
                .build();
    }

    /**
     * Crea relaciones entre las telas importadas y el almacén
     * @param telas Lista de telas importadas
     * @param almacenId ID del almacén
     * @return Mono vacío que indica la finalización de la operación
     */
    private Mono<Void> crearRelacionesAlmacenTelas(List<Tela> telas, Long almacenId) {
        if (telas == null || telas.isEmpty()) {
            return Mono.empty();
        }

        List<AlmacenTela> relaciones = telas.stream()
                .map(tela -> AlmacenTela.builder()
                        .almacenId(almacenId)
                        .telaId(tela.getId())
                        .fechaAsignacion(LocalDateTime.now())
                        .estado(EstadoAlmacenTela.ACTIVO)  // Usar el enum
                        .build())
                .collect(Collectors.toList());

        return almacenTelaRepository.saveAll(relaciones).then();
    }

    /**
     * Mapea una fila del Excel a una entidad Tela
     * @param row Fila del Excel
     * @return Entidad Tela con los datos de la fila
     */
    private Tela mapRowToTela(Row row) {
        // Esta función mapea una fila de Excel a una entidad Tela según el orden de columnas de la plantilla
        String numGuia = getCellValueAsString(row.getCell(0));       // Num. Guía*
        String partida = getCellValueAsString(row.getCell(1));       // Partida
        String os = getCellValueAsString(row.getCell(2));            // OS
        String proveedor = getCellValueAsString(row.getCell(3));     // Proveedor*
        LocalDate fechaIngreso = getCellValueAsLocalDate(row.getCell(4)); // Fecha Ingreso*
        String cliente = getCellValueAsString(row.getCell(5));       // Cliente
        String marca = getCellValueAsString(row.getCell(6));         // Marca
        String op = getCellValueAsString(row.getCell(7));            // OP
        String tipoTela = getCellValueAsString(row.getCell(8));      // Tipo Tela
        String descripcion = getCellValueAsString(row.getCell(9));   // Descripción
        String ench = getCellValueAsString(row.getCell(10));         // Ench
        int cantRolloIngresado = getCellValueAsInt(row.getCell(11)); // Cant. Rollo Ingresado*
        BigDecimal pesoIngresado = getCellValueAsBigDecimal(row.getCell(12)); // Peso Ingresado*
        BigDecimal stockReal = getCellValueAsBigDecimal(row.getCell(13));    // Stock Real
        String almacen = getCellValueAsString(row.getCell(14));      // Almacén

        // Validaciones básicas
        validarCamposObligatorios(numGuia, proveedor, fechaIngreso, cantRolloIngresado, pesoIngresado);

        // Crear la entidad con valores automáticos para estado, fechaCreacion y fechaActualizacion
        return Tela.builder()
                .numGuia(numGuia)
                .partida(partida)
                .os(os)
                .proveedor(proveedor)
                .fechaIngreso(fechaIngreso)
                .cliente(cliente)
                .marca(marca)
                .op(op)
                .tipoTela(tipoTela)
                .descripcion(descripcion)
                .ench(ench)
                .cantRolloIngresado(cantRolloIngresado)
                .pesoIngresado(pesoIngresado)
                .stockReal(stockReal != null ? stockReal : pesoIngresado) // Si no se proporciona stock, usar el peso ingresado
                .estado("ACTIVO") // Establecer estado predeterminado
                .almacen(almacen)
                // Los campos de auditoría se generan automáticamente
                .build();
    }

    /**
     * Valida los campos obligatorios de una tela
     * @param numGuia Número de guía
     * @param proveedor Proveedor
     * @param fechaIngreso Fecha de ingreso
     * @param cantRolloIngresado Cantidad de rollos ingresados
     * @param pesoIngresado Peso ingresado
     */
    /**
     * Valida los campos obligatorios de una tela
     */
    private void validarCamposObligatorios(String numGuia, String proveedor,
                                           LocalDate fechaIngreso, int cantRolloIngresado, BigDecimal pesoIngresado) {

        List<String> errores = new ArrayList<>();

        if (numGuia == null || numGuia.trim().isEmpty()) {
            errores.add("El número de guía es obligatorio");
        }

        if (proveedor == null || proveedor.trim().isEmpty()) {
            errores.add("El proveedor es obligatorio");
        }

        if (fechaIngreso == null) {
            errores.add("La fecha de ingreso es obligatoria");
        }

        if (cantRolloIngresado <= 0) {
            errores.add("La cantidad de rollos debe ser mayor a cero");
        }

        if (pesoIngresado == null || pesoIngresado.compareTo(BigDecimal.ZERO) <= 0) {
            errores.add("El peso ingresado debe ser mayor a cero");
        }

        if (!errores.isEmpty()) {
            throw new IllegalArgumentException("Error en la validación de datos: " +
                    String.join(", ", errores));
        }
    }

    // Métodos auxiliares para extraer valores de celdas

    /**
     * Obtiene el valor de una celda como String
     * @param cell Celda del Excel
     * @return Valor de la celda como String
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default: return null;
        }
    }

    /**
     * Obtiene el valor de una celda como entero
     * @param cell Celda del Excel
     * @return Valor de la celda como entero
     */
    private int getCellValueAsInt(Cell cell) {
        if (cell == null) return 0;

        switch (cell.getCellType()) {
            case NUMERIC: return (int) cell.getNumericCellValue();
            case STRING:
                try {
                    return Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return 0;
                }
            default: return 0;
        }
    }

    /**
     * Obtiene el valor de una celda como double
     * @param cell Celda del Excel
     * @return Valor de la celda como double
     */
    private double getCellValueAsDouble(Cell cell) {
        if (cell == null) return 0.0;

        switch (cell.getCellType()) {
            case NUMERIC: return cell.getNumericCellValue();
            case STRING:
                try {
                    return Double.parseDouble(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            default: return 0.0;
        }
    }

    /**
     * Obtiene el valor de una celda como LocalDate
     * @param cell Celda del Excel
     * @return Valor de la celda como LocalDate
     */
    private LocalDate getCellValueAsLocalDate(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate();
                }
                return null;
            case STRING:
                try {
                    String dateStr = cell.getStringCellValue().trim();
                    if (dateStr.isEmpty()) return null;

                    // Intentar con diferentes formatos de fecha
                    try {
                        // Formato dd/MM/yyyy
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        return LocalDate.parse(dateStr, formatter);
                    } catch (Exception e1) {
                        try {
                            // Formato yyyy-MM-dd
                            return LocalDate.parse(dateStr);
                        } catch (Exception e2) {
                            try {
                                // Formato MM/dd/yyyy
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
                                return LocalDate.parse(dateStr, formatter);
                            } catch (Exception e3) {
                                return null;
                            }
                        }
                    }
                } catch (Exception e) {
                    return null;
                }
            default: return null;
        }
    }

    /**
     * Obtiene el valor de una celda como BigDecimal
     * @param cell Celda del Excel
     * @return Valor como BigDecimal, o BigDecimal.ZERO si la celda es null
     */
    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) {
            return BigDecimal.ZERO;
        }

        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    // Usar setScale para controlar la precisión y el modo de redondeo
                    return BigDecimal.valueOf(cell.getNumericCellValue())
                            .setScale(4, RoundingMode.HALF_UP);
                case STRING:
                    try {
                        return new BigDecimal(cell.getStringCellValue())
                                .setScale(4, RoundingMode.HALF_UP);
                    } catch (NumberFormatException e) {
                        log.warn("No se pudo convertir el valor de string '{}' a BigDecimal",
                                cell.getStringCellValue());
                        return BigDecimal.ZERO;
                    }
                case FORMULA:
                    try {
                        return BigDecimal.valueOf(cell.getNumericCellValue())
                                .setScale(4, RoundingMode.HALF_UP);
                    } catch (Exception e) {
                        log.warn("Error al evaluar fórmula para BigDecimal: {}", e.getMessage());
                        return BigDecimal.ZERO;
                    }
                default:
                    return BigDecimal.ZERO;
            }
        } catch (Exception e) {
            log.error("Error al convertir celda a BigDecimal: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Clase interna para el resultado del procesamiento del Excel
     */
    @Data
    @Builder
    private static class ResultadoProcesamiento {
        private List<Tela> telasParaGuardar;
        private ImportacionResultadoDTO resultado;
    }
}