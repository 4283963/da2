package com.reefer.diagnosis.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DiagnosisException.class)
    public ResponseEntity<ErrorResponse> handleDiagnosisException(DiagnosisException ex) {
        log.error("业务异常: {}", ex.getMessage(), ex);
        ErrorResponse error = new ErrorResponse(ex.getCode(), ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.valueOf(ex.getCode())).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("系统异常: {}", ex.getMessage(), ex);
        ErrorResponse error = new ErrorResponse(500, "系统内部错误: " + ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("参数错误: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(400, ex.getMessage(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    public static class ErrorResponse {
        private final int code;
        private final String message;
        private final LocalDateTime timestamp;

        public ErrorResponse(int code, String message, LocalDateTime timestamp) {
            this.code = code;
            this.message = message;
            this.timestamp = timestamp;
        }

        public int getCode() { return code; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
