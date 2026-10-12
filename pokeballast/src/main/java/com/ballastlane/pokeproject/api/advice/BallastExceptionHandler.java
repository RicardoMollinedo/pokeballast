package com.ballastlane.pokeproject.api.advice;

import com.ballastlane.pokeproject.dto.BallastResponse;
import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns every exception into a {@link BallastResponse}: the response code, optional data and
 * message of the error, with the matching HTTP status. Stack traces and internal messages never
 * reach the client.
 */
@Slf4j
@RestControllerAdvice
public class BallastExceptionHandler {

    @ExceptionHandler(BallastException.class)
    ResponseEntity<BallastResponse<Object>> handleBallast(BallastException ex) {
        log.warn("[REQUEST] Business error - code={}, message={}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
                .body(BallastResponse.error(ex.getCode(), ex.getData(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<BallastResponse<Map<String, String>>> handleInvalidBody(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
        log.warn("[REQUEST] Invalid request body - fields={}", fields.keySet());
        return respond(BallastErrorCode.INVALID_REQUEST, fields);
    }

    /** Invalid JSON, wrong value types, unknown fields or missing required parameters. */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingRequestHeaderException.class})
    ResponseEntity<BallastResponse<Object>> handleMalformed(Exception ex) {
        log.warn("[REQUEST] Malformed request - type={}", ex.getClass().getSimpleName());
        return respond(BallastErrorCode.MALFORMED_REQUEST, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<BallastResponse<Object>> handleNoResource(NoResourceFoundException ex) {
        log.warn("[REQUEST] Unknown route - path={}", ex.getResourcePath());
        return respond(BallastErrorCode.RESOURCE_NOT_FOUND, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<BallastResponse<Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        log.warn("[REQUEST] Method not allowed - method={}", ex.getMethod());
        return respond(BallastErrorCode.METHOD_NOT_ALLOWED, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<BallastResponse<Object>> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        log.warn("[REQUEST] Unsupported content type - contentType={}", ex.getContentType());
        return respond(BallastErrorCode.UNSUPPORTED_MEDIA_TYPE, null);
    }

    /** Raised by {@code @PreAuthorize}: anonymous callers get 401, authenticated ones without the role 403. */
    @ExceptionHandler(AuthorizationDeniedException.class)
    ResponseEntity<BallastResponse<Object>> handleDenied(AuthorizationDeniedException ex) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean anonymous = authentication == null || authentication instanceof AnonymousAuthenticationToken;
        BallastErrorCode code = anonymous ? BallastErrorCode.UNAUTHORIZED : BallastErrorCode.ACCESS_DENIED;
        log.warn("[REQUEST] Access denied - code={}", code.code());
        return respond(code, null);
    }

    /** Last resort: log everything, reveal nothing. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<BallastResponse<Object>> handleUnexpected(Exception ex) {
        log.error("[REQUEST] Unexpected error", ex);
        return respond(BallastErrorCode.UNEXPECTED, null);
    }

    private static <T> ResponseEntity<BallastResponse<T>> respond(BallastErrorCode code, T data) {
        return ResponseEntity.status(code.httpStatus())
                .body(BallastResponse.error(code.code(), data, code.defaultMessage()));
    }
}
