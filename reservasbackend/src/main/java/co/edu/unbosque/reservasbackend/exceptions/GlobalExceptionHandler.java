package co.edu.unbosque.reservasbackend.exceptions;

import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail handleDataAccessException(DataAccessException ex) {
        // Extraer PSQLException de la cadena de causas
        Throwable cause = ex.getCause();
        while (cause != null && !(cause instanceof PSQLException)) {
            cause = cause.getCause();
        }

        if (cause instanceof PSQLException psqlEx) {
            String sqlState = psqlEx.getSQLState();
            ServerErrorMessage serverError = psqlEx.getServerErrorMessage();
            String dbMessage = (serverError != null && serverError.getMessage() != null) 
                                ? serverError.getMessage() 
                                : "Error interno de base de datos";

            // 1. Solape de horario (23P01 de EXCLUDE o trigger)
            if ("23P01".equals(sqlState)) {
                return createProblemDetail(HttpStatus.CONFLICT, "El espacio ya está reservado en ese horario.", "Conflicto de Horario");
            } 
            // 2. Errores de negocio (RAISE EXCEPTION sin ERRCODE explícito -> P0001)
            else if ("P0001".equals(sqlState)) {
                String lowerMsg = dbMessage.toLowerCase();
                if (lowerMsg.contains("cruce de horario") || lowerMsg.contains("e_espacio_ocupado") || lowerMsg.contains("no tiene disponibilidad")) {
                    return createProblemDetail(HttpStatus.CONFLICT, "El espacio ya está reservado en ese horario.", "Conflicto de Horario");
                }
                return createProblemDetail(HttpStatus.UNPROCESSABLE_ENTITY, dbMessage, "Regla de Negocio");
            } 
            // 3. Claves foráneas (23503)
            else if ("23503".equals(sqlState)) {
                return createProblemDetail(HttpStatus.UNPROCESSABLE_ENTITY, "El recurso referenciado (cliente, espacio o empleado) no existe.", "Error de Referencia");
            } 
            // 4. Otras violaciones (empiezan por 23)
            else if (sqlState != null && sqlState.startsWith("23")) {
                return createProblemDetail(HttpStatus.CONFLICT, "Violación de integridad de datos. Posible conflicto o registro duplicado.", "Data Integrity Violation");
            }
        }

        return createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Error en la operación de base de datos.", "Internal Error");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        List<String> errores = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.add(error.getField() + ": " + error.getDefaultMessage());
        }
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Campos inválidos: " + String.join(", ", errores));
        problemDetail.setTitle("Error de Validación");
        problemDetail.setType(URI.create("https://api.reservas.com/errors/validation"));
        return problemDetail;
    }
    
    private ProblemDetail createProblemDetail(HttpStatus status, String detail, String title) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("https://api.reservas.com/errors/db"));
        return problemDetail;
    }
}
