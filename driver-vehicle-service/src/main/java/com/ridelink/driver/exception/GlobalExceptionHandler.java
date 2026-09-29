package com.ridelink.driver.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DriverNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(DriverNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(DriverProfileAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleDuplicate(DriverProfileAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(DriverProfileConflictException.class)
    public ResponseEntity<ApiError> handleProfileConflict(DriverProfileConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(RideNotCompletedException.class)
    public ResponseEntity<ApiError> handleRideNotCompleted(RideNotCompletedException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), List.of(ex.getMessage()));
    }

    @ExceptionHandler(LocationNotFoundException.class)
    public ResponseEntity<ApiError> handleLocationNotFound(LocationNotFoundException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), List.of(ex.getMessage()));
    }

    @ExceptionHandler(LocationOutsideServiceAreaException.class)
    public ResponseEntity<ApiError> handleOutsideServiceArea(LocationOutsideServiceAreaException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), List.of(ex.getMessage()));
    }

    @ExceptionHandler(DownstreamServiceException.class)
    public ResponseEntity<ApiError> handleDownstream(DownstreamServiceException ex) {
        return build(HttpStatus.BAD_GATEWAY, ex.getMessage(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage() != null
                ? ex.getMessage() : "You do not have permission to perform this action", null);
    }

    /** Bean-validation failures: one message per field, and the message itself is returned to the client. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> byField = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            boolean required = "NotBlank".equals(fe.getCode()) || "NotNull".equals(fe.getCode());
            // If a field fails several rules (e.g. blank + pattern), show the "required" message only.
            if (!byField.containsKey(fe.getField()) || required) {
                byField.put(fe.getField(), fe.getDefaultMessage());
            }
        }
        List<String> details = byField.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.toList());
        String message = String.join("; ", byField.values());
        return build(HttpStatus.BAD_REQUEST, message.isEmpty() ? "Validation failed" : message, details);
    }

    /** Bad JSON, missing body, wrong types, or an invalid enum value (e.g. availability = "ONLINE"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof InvalidFormatException ife) {
            String field = ife.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("."));
            Class<?> target = ife.getTargetType();
            String message;
            if (target != null && target.isEnum()) {
                String allowed = Arrays.stream(target.getEnumConstants())
                        .map(Object::toString).collect(Collectors.joining(", "));
                message = "Invalid value '" + ife.getValue() + "' for " + field + ". Allowed values: " + allowed;
            } else {
                message = "Invalid value '" + ife.getValue() + "' for " + field
                        + (target != null ? ". Expected type: " + target.getSimpleName() : "");
            }
            return build(HttpStatus.BAD_REQUEST, message, List.of(message));
        }
        return build(HttpStatus.BAD_REQUEST, "Request body is missing or is not valid JSON", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        log.error("Unhandled exception while processing request", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, List<String> details) {
        return ResponseEntity.status(status)
                .body(new ApiError(status.value(), status.getReasonPhrase(), message, details));
    }
}