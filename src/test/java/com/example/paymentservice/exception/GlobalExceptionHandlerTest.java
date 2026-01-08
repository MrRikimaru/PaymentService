package com.example.paymentservice.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleMethodArgumentNotValidException_ShouldReturnBadRequest() {
        // Arrange
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError =
                new FieldError("paymentRequestDTO", "amount", "must be greater than 0");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        // Act
        ResponseEntity<Map<String, Object>> response = handler.handleValidationExceptions(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("errors");
        Map<String, String> errors = (Map<String, String>) response.getBody().get("errors");
        assertThat(errors).containsEntry("amount", "must be greater than 0");
    }

    @Test
    void handleConstraintViolationException_ShouldReturnBadRequest() {
        // Arrange
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("Invalid value");

        Set<ConstraintViolation<?>> violations = new HashSet<>();
        violations.add(violation);

        ConstraintViolationException ex =
                new ConstraintViolationException("Validation failed", violations);

        // Act
        ResponseEntity<Map<String, Object>> response =
                handler.handleConstraintViolationException(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("message");
    }

    @Test
    void handleDataIntegrityViolationException_ShouldReturnConflict() {
        // Arrange
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("Duplicate payment");

        // Act
        ResponseEntity<Map<String, Object>> response =
                handler.handleDataIntegrityViolationException(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody())
                .containsEntry("message", "Duplicate payment detected for this order and user");
    }

    @Test
    void handleDataAccessException_ShouldReturnServiceUnavailable() {
        // Arrange
        DataAccessException ex = new DataAccessException("Database error") {};

        // Act
        ResponseEntity<Map<String, Object>> response = handler.handleDataAccessException(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("message", "Database error occurred");
    }
}
