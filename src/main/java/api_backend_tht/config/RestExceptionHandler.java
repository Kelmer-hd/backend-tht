package api_backend_tht.config;

import api_backend_tht.exception.ResourceNotFoudException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {


    @ExceptionHandler(ResourceNotFoudException.class)
    public ResponseEntity<ProblemDetail> handleEntityNotFoundException(ResourceNotFoudException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());

        problemDetail.setTitle("Recurso no encontrado");
        problemDetail.setType(URI.create("https://api.tudominio.com/errors/not-found"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problemDetail);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ProblemDetail> handleValidationErrors(WebExchangeBindException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Error de validación");

        problemDetail.setTitle("Error en los datos de entrada");
        problemDetail.setType(URI.create("https://api.tudominio.com/errors/validation"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        problemDetail.setProperty("errors", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(problemDetail);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatusException(ResponseStatusException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                ex.getStatusCode(), ex.getReason());

        problemDetail.setTitle("Error en la solicitud");
        problemDetail.setType(URI.create("https://api.tudominio.com/errors/request"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Se produjo un error interno");

        problemDetail.setTitle("Error interno del servidor");
        problemDetail.setType(URI.create("https://api.tudominio.com/errors/server"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("exception", ex.getClass().getSimpleName());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(problemDetail);
    }

}
