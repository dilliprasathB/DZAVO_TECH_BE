package com.dzavo.exception;

import com.dzavo.dto.ApiResponse;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle Jakarta validation failures (HTTP 400)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        logger.warn("[Validation] Submission rejected due to invalid fields: {}", errors);

        ApiResponse response = ApiResponse.validationFailure(
                "Please check the submitted information.",
                errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle malformed JSON or unreadable request body (HTTP 400)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        logger.warn("[Validation] Malformed JSON request body: {}", ex.getMessage());
        ApiResponse response = ApiResponse.error("Invalid request payload format.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle Email & SMTP delivery failures (HTTP 500)
     * Protects internal SMTP details and credentials from exposure.
     */
    @ExceptionHandler({MailException.class, MessagingException.class})
    public ResponseEntity<ApiResponse> handleMailException(Exception ex) {
        logger.error("[Email Delivery Error] Failed to transmit message via SMTP: {}", ex.getMessage(), ex);

        ApiResponse response = ApiResponse.error(
                "We could not submit your enquiry. Please try again later."
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handle generic unexpected server exceptions (HTTP 500)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGenericException(Exception ex) {
        logger.error("[Unexpected Server Error] {}", ex.getMessage(), ex);

        ApiResponse response = ApiResponse.error(
                "We could not submit your enquiry. Please try again later."
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
