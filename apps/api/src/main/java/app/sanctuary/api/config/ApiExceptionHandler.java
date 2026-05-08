package app.sanctuary.api.config;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import app.sanctuary.api.auth.service.AuthFlowException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AuthFlowException.class)
    public ResponseEntity<Map<String, String>> handleAuthFlowException(AuthFlowException exception) {
        return ResponseEntity.status(exception.status())
            .body(Map.of("message", exception.getMessage()));
    }
}
