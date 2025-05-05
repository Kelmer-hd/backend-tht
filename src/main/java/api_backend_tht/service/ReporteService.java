package api_backend_tht.service;

import api_backend_tht.exception.ResourceNotFoudException;
import api_backend_tht.model.dto.TelaFiltroDTO;
import api_backend_tht.model.entity.Tela;
import api_backend_tht.repository.TelaRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReporteService {

    private final TelaRepository telaRepository;

    /**
     * Método unificado para generar reportes
     * @param filtros Filtros a aplicar (null para todos)
     * @param formato Formato del reporte (excel o pdf)
     * @param nombreReporte Nombre del reporte
     * @return Mono con el recurso del archivo generado
     */
    public Mono<Resource> generarReporte(TelaFiltroDTO filtros, String formato, String nombreReporte) {
        Flux<Tela> telasFlux = filtros == null
                ? telaRepository.findAll()
                : telaRepository.findAll().filter(tela -> aplicarFiltros(tela, filtros));

        return telasFlux.collectList()
                .flatMap(telas -> {
                    if ("excel".equalsIgnoreCase(formato)) {
                        return generarExcel(telas, nombreReporte);
                    } else if ("pdf".equalsIgnoreCase(formato)) {
                        return generarPDF(telas, nombreReporte);
                    } else {
                        return Mono.error(new IllegalArgumentException("Formato no soportado: " + formato));
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error al generar reporte: {}", e.getMessage(), e);
                    return Mono.error(new ResourceNotFoudException("Error al generar el reporte: " + e.getMessage()));
                });
    }

    /**
     * Genera reporte en formato Excel de todas las telas
     * @return Mono con el recurso del archivo Excel
     */
    public Mono<Resource> generarReporteTelaExcel() {
        return generarReporte(null, "excel", "Telas");
    }

    /**
     * Genera reporte en formato Excel de telas filtradas según criterios
     * @param filtros Objeto con los filtros a aplicar
     * @return Mono con el recurso del archivo Excel
     */
    public Mono<Resource> generarReporteTelaExcelFiltrado(TelaFiltroDTO filtros) {
        return generarReporte(filtros, "excel", "Telas Filtradas");
    }

    /**
     * Genera reporte en formato PDF de todas las telas
     * @return Mono con el recurso del archivo PDF
     */
    public Mono<Resource> generarReporteTelaPDF() {
        return generarReporte(null, "pdf", "Telas");
    }

    /**
     * Genera reporte en formato PDF de telas filtradas según criterios
     * @param filtros Objeto con los filtros a aplicar
     * @return Mono con el recurso del archivo PDF
     */
    public Mono<Resource> generarReporteTelaPDFFiltrado(TelaFiltroDTO filtros) {
        return generarReporte(filtros, "pdf", "Telas Filtradas");
    }

    /**
     * Método auxiliar para generar Excel a partir de una lista de telas
     * @param telas Lista de telas para incluir en el reporte
     * @param nombreHoja Nombre de la hoja de Excel
     * @return Mono con el recurso del archivo Excel
     */
    private Mono<Resource> generarExcel(List<Tela> telas, String nombreHoja) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(nombreHoja);

            // Crear encabezados
            Row headerRow = sheet.createRow(0);
            String[] columns = {"ID", "Num. Guía", "Partida", "OS", "Proveedor",
                    "Fecha Ingreso", "Cliente", "Marca", "OP", "Tipo Tela",
                    "Descripción", "Ench", "Rollos", "Peso", "Stock Real", "Almacén", "Estado"};

            // Configurar estilos
            CellStyle headerStyle = crearEstiloEncabezado(workbook);
            CellStyle decimalStyle = crearEstiloDecimal(workbook);
            CellStyle dateStyle = crearEstiloFecha(workbook);

            // Aplicar encabezados
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Llenar datos
            int rowNum = 1;
            for (Tela tela : telas) {
                Row row = sheet.createRow(rowNum++);
                int colIdx = 0;

                // Datos básicos
                row.createCell(colIdx++).setCellValue(tela.getId());
                agregarCeldaTexto(row, colIdx++, tela.getNumGuia());
                agregarCeldaTexto(row, colIdx++, tela.getPartida());
                agregarCeldaTexto(row, colIdx++, tela.getOs());
                agregarCeldaTexto(row, colIdx++, tela.getProveedor());

                // Fecha de ingreso
                Cell cellFecha = row.createCell(colIdx++);
                if (tela.getFechaIngreso() != null) {
                    cellFecha.setCellValue(Date.from(tela.getFechaIngreso().atStartOfDay(ZoneId.systemDefault()).toInstant()));
                    cellFecha.setCellStyle(dateStyle);
                }

                // Continuar con datos básicos
                agregarCeldaTexto(row, colIdx++, tela.getCliente());
                agregarCeldaTexto(row, colIdx++, tela.getMarca());
                agregarCeldaTexto(row, colIdx++, tela.getOp());
                agregarCeldaTexto(row, colIdx++, tela.getTipoTela());
                agregarCeldaTexto(row, colIdx++, tela.getDescripcion());
                agregarCeldaTexto(row, colIdx++, tela.getEnch());

                // Datos numéricos
                row.createCell(colIdx++).setCellValue(tela.getCantRolloIngresado());

                // Celdas con valores BigDecimal
                agregarCeldaDecimal(row, colIdx++, tela.getPesoIngresado(), decimalStyle);
                agregarCeldaDecimal(row, colIdx++, tela.getStockReal(), decimalStyle);

                // Nuevos campos
                agregarCeldaTexto(row, colIdx++, tela.getAlmacen());
                agregarCeldaTexto(row, colIdx++, tela.getEstado());

                // Fechas de creación y actualización

                /*
                Cell cellCreacion = row.createCell(colIdx++);
                if (tela.getFechaCreacion() != null) {
                    cellCreacion.setCellValue(Date.from(tela.getFechaCreacion().atZone(ZoneId.systemDefault()).toInstant()));
                    cellCreacion.setCellStyle(dateStyle);
                }

                Cell cellActualizacion = row.createCell(colIdx++);
                if (tela.getFechaActualizacion() != null) {
                    cellActualizacion.setCellValue(Date.from(tela.getFechaActualizacion().atZone(ZoneId.systemDefault()).toInstant()));
                    cellActualizacion.setCellStyle(dateStyle);
                }

                 */

            }

            // Ajustar ancho de columnas automáticamente
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return Mono.just(new ByteArrayResource(outputStream.toByteArray()));
        } catch (IOException e) {
            log.error("Error al generar Excel: {}", e.getMessage(), e);
            return Mono.error(new ResourceNotFoudException("Error al generar el archivo Excel: " + e.getMessage()));
        }
    }

    /**
     * Método auxiliar para generar PDF con título personalizado
     * @param telas Lista de telas para incluir en el reporte
     * @param titulo Título del reporte
     * @return Mono con el recurso del archivo PDF
     */
    private Mono<Resource> generarPDF(List<Tela> telas, String titulo) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Configuración de fuentes (usando la versión correcta de Font)
            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 16, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font normalFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.NORMAL);

            // Agregar título con fecha y hora
            document.add(new Paragraph("Reporte de " + titulo, titleFont));
            document.add(new Paragraph("Generado: " + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), normalFont));
            document.add(new Paragraph(" ")); // Espacio

            // Crear tabla - ajustar para incluir nuevos campos
            PdfPTable table = new PdfPTable(19); // Número de columnas actualizado
            table.setWidthPercentage(100);

            // Configurar anchos relativos de columnas
            float[] columnWidths = {
                    0.5f, 0.8f, 0.8f, 0.6f, 1.0f, 0.8f, 1.0f, 0.8f, 0.7f, 0.8f,
                    1.2f, 0.7f, 0.6f, 0.7f, 0.7f, 0.8f, 0.8f, 0.8f, 0.8f
            };
            table.setWidths(columnWidths);

            // Agregar encabezados
            String[] headers = {"ID", "Num. Guía", "Partida", "OS", "Proveedor",
                    "Fecha Ingreso", "Cliente", "Marca", "OP", "Tipo Tela",
                    "Descripción", "Ench", "Rollos", "Peso", "Stock", "Almacén",
                    "Estado"};

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                cell.setBackgroundColor(new BaseColor(220, 220, 220));
                table.addCell(cell);
            }

            // Agregar datos
            for (Tela tela : telas) {
                addCell(table, String.valueOf(tela.getId()), normalFont);
                addCell(table, tela.getNumGuia(), normalFont);
                addCell(table, tela.getPartida(), normalFont);
                addCell(table, tela.getOs(), normalFont);
                addCell(table, tela.getProveedor(), normalFont);
                addCell(table, formatearFecha(tela.getFechaIngreso()), normalFont);
                addCell(table, tela.getCliente(), normalFont);
                addCell(table, tela.getMarca(), normalFont);
                addCell(table, tela.getOp(), normalFont);
                addCell(table, tela.getTipoTela(), normalFont);
                addCell(table, tela.getDescripcion(), normalFont);
                addCell(table, tela.getEnch(), normalFont);
                addCell(table, String.valueOf(tela.getCantRolloIngresado()), normalFont);
                addCell(table, formatearDecimal(tela.getPesoIngresado()), normalFont);
                addCell(table, formatearDecimal(tela.getStockReal()), normalFont);

                // Nuevos campos

                addCell(table, tela.getAlmacen(), normalFont);
                addCell(table, tela.getEstado(), normalFont);
                // addCell(table, formatearFechaHora(tela.getFechaCreacion()), normalFont);
                // addCell(table, formatearFechaHora(tela.getFechaActualizacion()), normalFont);
            }

            document.add(table);

            // Agregar pie de página con total de registros
            document.add(new Paragraph(" ")); // Espacio
            document.add(new Paragraph("Total de registros: " + telas.size(), normalFont));

            document.close();

            return Mono.just(new ByteArrayResource(outputStream.toByteArray()));
        } catch (DocumentException e) {
            log.error("Error al generar PDF: {}", e.getMessage(), e);
            return Mono.error(new ResourceNotFoudException("Error al generar el documento PDF: " + e.getMessage()));
        }
    }

    /**
     * Método auxiliar para agregar celdas a la tabla PDF
     * @param table Tabla PDF
     * @param value Valor a agregar
     * @param font Fuente a usar
     */
    private void addCell(PdfPTable table, String value, com.itextpdf.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value != null ? value : "", font));
        cell.setPadding(4);
        table.addCell(cell);
    }

    /**
     * Formatea una fecha para mostrar en PDF
     */
    private String formatearFecha(LocalDate fecha) {
        return fecha != null ? fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
    }

    /**
     * Formatea una fecha-hora para mostrar en PDF
     */
    private String formatearFechaHora(LocalDateTime fechaHora) {
        return fechaHora != null ? fechaHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
    }

    /**
     * Formatea un valor decimal para mostrar en PDF
     */
    private String formatearDecimal(BigDecimal valor) {
        if (valor == null) {
            return "0.0000";
        }
        DecimalFormat df = new DecimalFormat("#,##0.0000");
        return df.format(valor);
    }
    /**
     * Crea estilo para encabezados de Excel
     */
    private CellStyle crearEstiloEncabezado(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Crea estilo para valores decimales
     */
    private CellStyle crearEstiloDecimal(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.0000"));
        return style;
    }

    /**
     * Crea estilo para fechas
     */
    private CellStyle crearEstiloFecha(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("dd/mm/yyyy"));
        return style;
    }

    /**
     * Agrega una celda de texto a la fila
     */
    private void agregarCeldaTexto(Row row, int index, String valor) {
        row.createCell(index).setCellValue(valor != null ? valor : "");
    }

    /**
     * Agrega una celda con valor decimal a la fila
     */
    private void agregarCeldaDecimal(Row row, int index, BigDecimal valor, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(valor != null ? valor.doubleValue() : 0.0);
        cell.setCellStyle(style);
    }

    /**
     * Aplica los filtros a una tela específica
     * @param tela La tela a evaluar
     * @param filtros Los filtros a aplicar
     * @return true si la tela cumple con los filtros, false en caso contrario
     */
    private boolean aplicarFiltros(Tela tela, TelaFiltroDTO filtros) {
        if (filtros == null) {
            return true;
        }

        boolean cumpleFiltros = true;

        if (filtros.getNumGuia() != null && !filtros.getNumGuia().isEmpty()) {
            cumpleFiltros = cumpleFiltros && tela.getNumGuia() != null &&
                    tela.getNumGuia().toLowerCase().contains(filtros.getNumGuia().toLowerCase());
        }

        if (cumpleFiltros && filtros.getProveedor() != null && !filtros.getProveedor().isEmpty()) {
            cumpleFiltros = cumpleFiltros && tela.getProveedor() != null &&
                    tela.getProveedor().toLowerCase().contains(filtros.getProveedor().toLowerCase());
        }

        if (cumpleFiltros && filtros.getCliente() != null && !filtros.getCliente().isEmpty()) {
            cumpleFiltros = cumpleFiltros && tela.getCliente() != null &&
                    tela.getCliente().toLowerCase().contains(filtros.getCliente().toLowerCase());
        }

        if (cumpleFiltros && filtros.getDescripcion() != null && !filtros.getDescripcion().isEmpty()) {
            cumpleFiltros = cumpleFiltros && tela.getDescripcion() != null &&
                    tela.getDescripcion().toLowerCase().contains(filtros.getDescripcion().toLowerCase());
        }

        if (cumpleFiltros && filtros.getOs() != null && !filtros.getOs().isEmpty()) {
            cumpleFiltros = cumpleFiltros && tela.getOs() != null &&
                    tela.getOs().toLowerCase().contains(filtros.getOs().toLowerCase());
        }

        if (cumpleFiltros && filtros.getPartida() != null && !filtros.getPartida().isEmpty()) {
            cumpleFiltros = cumpleFiltros && tela.getPartida() != null &&
                    tela.getPartida().toLowerCase().contains(filtros.getPartida().toLowerCase());
        }

        // Nuevos campos
        if (cumpleFiltros && filtros.getEstado() != null && !filtros.getEstado().isEmpty()) {
            cumpleFiltros = cumpleFiltros && tela.getEstado() != null &&
                    tela.getEstado().toLowerCase().contains(filtros.getEstado().toLowerCase());
        }

        if (cumpleFiltros && filtros.getAlmacen() != null && !filtros.getAlmacen().isEmpty()) {
            cumpleFiltros = cumpleFiltros && tela.getAlmacen() != null &&
                    tela.getAlmacen().toLowerCase().contains(filtros.getAlmacen().toLowerCase());
        }

        if (cumpleFiltros && filtros.getTipoTela() != null && !filtros.getTipoTela().isEmpty()) {
            cumpleFiltros = cumpleFiltros && tela.getTipoTela() != null &&
                    tela.getTipoTela().toLowerCase().contains(filtros.getTipoTela().toLowerCase());
        }

        // Filtros de fecha
        if (cumpleFiltros && filtros.getFechaInicio() != null && tela.getFechaIngreso() != null) {
            cumpleFiltros = cumpleFiltros &&
                    (tela.getFechaIngreso().isEqual(filtros.getFechaInicio()) ||
                            tela.getFechaIngreso().isAfter(filtros.getFechaInicio()));
        }

        if (cumpleFiltros && filtros.getFechaFin() != null && tela.getFechaIngreso() != null) {
            cumpleFiltros = cumpleFiltros &&
                    (tela.getFechaIngreso().isEqual(filtros.getFechaFin()) ||
                            tela.getFechaIngreso().isBefore(filtros.getFechaFin()));
        }

        return cumpleFiltros;
    }
}