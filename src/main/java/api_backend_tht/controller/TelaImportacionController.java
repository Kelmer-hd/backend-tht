package api_backend_tht.controller;

import api_backend_tht.model.dto.ImportacionResultadoDTO;
import api_backend_tht.repository.AlmacenRepository;
import api_backend_tht.service.TelaImportacionService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TelaImportacionController {

    private final TelaImportacionService telaImportacionService;
    private final AlmacenRepository almacenRepository;
    private final ResourceLoader resourceLoader; // Para cargar recursos

    // Ubicación de la plantilla en el sistema de archivos
    private static final String PLANTILLA_PATH = "classpath:templates/plantilla_telas_base.xlsx";

    /**
     * Endpoint para importar telas desde un archivo Excel a un almacén específico
     * @param almacenId ID del almacén donde se importarán las telas
     * @param filePart Archivo Excel a importar
     * @return Resultado de la importación
     */
    @PostMapping("/almacenes/{almacenId}/telas/importar")
    public Mono<ImportacionResultadoDTO> importarTelasDesdeExcel(
            @PathVariable Long almacenId,
            @RequestPart("file") FilePart filePart) {

        return filePart.content()
                .collectList()
                .map(dataBuffers -> {
                    // Calcular el tamaño total necesario
                    int totalSize = dataBuffers.stream()
                            .mapToInt(buffer -> buffer.readableByteCount())
                            .sum();

                    // Crear un array de bytes con el tamaño total
                    byte[] result = new byte[totalSize];

                    // Copiar cada buffer al array de bytes
                    int position = 0;
                    for (var buffer : dataBuffers) {
                        int length = buffer.readableByteCount();
                        buffer.read(result, position, length);
                        position += length;
                    }

                    return result;
                })
                .flatMap(bytes -> telaImportacionService.importarTelasDesdeExcelParaAlmacen(bytes, almacenId));
    }

    /**
     * Endpoint para descargar plantilla Excel para la importación
     * @param almacenId ID del almacén para el que se descarga la plantilla
     * @return Plantilla Excel con encabezados
     */
    @GetMapping("/almacenes/{almacenId}/telas/plantilla")
    public Mono<ResponseEntity<Resource>> descargarPlantillaExcel(@PathVariable Long almacenId) {
        return almacenRepository.findById(almacenId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No se encontró el almacén con ID: " + almacenId)))
                .flatMap(almacen -> {
                    return Mono.fromCallable(() -> {
                        try {
                            // Cargar la plantilla base desde el sistema de archivos
                            Resource plantillaBase = resourceLoader.getResource(PLANTILLA_PATH);

                            // Abrir la plantilla existente
                            try (InputStream is = plantillaBase.getInputStream();
                                 Workbook workbook = new XSSFWorkbook(is)) {

                                Sheet sheet = workbook.getSheetAt(0);

                                // Actualizar información del almacén en la primera fila
                                Row infoRow = sheet.getRow(0);
                                if (infoRow == null) {
                                    infoRow = sheet.createRow(0);
                                }

                                Cell infoCell = infoRow.getCell(0);
                                if (infoCell == null) {
                                    infoCell = infoRow.createCell(0);
                                }

                                infoCell.setCellValue("Almacén: " + almacen.getNombreAlmacen() + " (ID: " + almacen.getId() + ")");

                                // Opcional: Actualizar nombre de la hoja
                                workbook.setSheetName(0, "Telas para " + almacen.getNombreAlmacen());

                                // Escribir el libro a un ByteArrayOutputStream
                                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                workbook.write(outputStream);

                                // Crear un recurso con el contenido
                                Resource resource = new ByteArrayResource(outputStream.toByteArray());

                                // Configurar la respuesta
                                return ResponseEntity.ok()
                                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=plantilla_telas_" +
                                                        almacen.getNombreAlmacen().replaceAll("\\s+", "_") + ".xlsx")
                                        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                        .body(resource);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Error al procesar la plantilla: " + e.getMessage(), e);
                        }
                    }).subscribeOn(Schedulers.boundedElastic());
                });
    }
}