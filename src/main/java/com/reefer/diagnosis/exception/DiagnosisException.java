package com.reefer.diagnosis.exception;

public class DiagnosisException extends RuntimeException {

    private final int code;

    public DiagnosisException(String message) {
        super(message);
        this.code = 500;
    }

    public DiagnosisException(int code, String message) {
        super(message);
        this.code = code;
    }

    public DiagnosisException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }

    public int getCode() {
        return code;
    }
}
