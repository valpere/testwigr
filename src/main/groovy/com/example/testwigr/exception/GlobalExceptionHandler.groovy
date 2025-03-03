package com.example.testwigr.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<?> resourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
            message: ex.getMessage(),
            details: request.getDescription(false)
        )
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    ResponseEntity<?> userAlreadyExistsException(UserAlreadyExistsException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
            message: ex.getMessage(),
            details: request.getDescription(false)
        )
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT)
    }

    @ExceptionHandler(SecurityException.class)
    ResponseEntity<?> securityException(SecurityException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
            message: ex.getMessage(),
            details: request.getDescription(false)
        )
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<?> globalExceptionHandler(Exception ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
            message: ex.getMessage(),
            details: request.getDescription(false)
        )
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = new ErrorResponse(
            message: 'Validation failed',
            details: errors
        )
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    static class ErrorResponse {

        String message
        String details

    }

}
