package com.ballastlane.pokeproject.exceptions;

import java.util.Objects;

/**
 * The application's single exception: code and message are mandatory, data is optional.
 * Pure Java on purpose, so bl can throw it without depending on the web layer.
 */
public class BallastException extends RuntimeException {

    private static final int DEFAULT_HTTP_STATUS = 400;

    private final String code;
    private final transient Object data;
    private final int httpStatus;

    public BallastException(String code, String message) {
        this(code, message, null, DEFAULT_HTTP_STATUS);
    }

    public BallastException(String code, String message, Object data, int httpStatus) {
        super(Objects.requireNonNull(message, "message is mandatory"));
        this.code = Objects.requireNonNull(code, "code is mandatory");
        this.data = data;
        this.httpStatus = httpStatus;
    }

    public BallastException(BallastErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage(), null);
    }

    public BallastException(BallastErrorCode errorCode, String message, Object data) {
        this(errorCode.code(), message, data, errorCode.httpStatus());
    }

    public String getCode() {
        return code;
    }

    @SuppressWarnings("unchecked")
    public <T> T getData() {
        return (T) data;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
