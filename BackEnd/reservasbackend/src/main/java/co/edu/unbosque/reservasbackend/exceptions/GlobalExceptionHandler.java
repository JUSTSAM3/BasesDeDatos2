package co.edu.unbosque.reservasbackend.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.net.URI;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Violación de integridad de datos. Posible conflicto o registro duplicado.");
        problemDetail.setTitle("Data Integrity Violation");
        problemDetail.setType(URI.create("https://api.reservas.com/errors/data-integrity"));
        return problemDetail;
    }
}
