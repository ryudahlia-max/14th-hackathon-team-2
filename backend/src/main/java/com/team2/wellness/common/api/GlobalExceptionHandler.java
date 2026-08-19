package com.team2.wellness.common.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handleApiException(ApiException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.status())
                .body(error(exception.code(), exception.getMessage(), Map.of(), request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(error("VALIDATION_FAILED", "요청 값을 확인해 주세요.", fields, request));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest()
                .body(error("VALIDATION_FAILED", exception.getMessage(), Map.of(), request));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> handleMalformedRequest(Exception exception, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(error("MALFORMED_REQUEST", "요청 형식이나 값을 확인해 주세요.", Map.of(), request));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiError> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest().body(error(
                "MISSING_PARAMETER",
                "필수 요청 값이 없습니다.",
                Map.of(exception.getParameterName(), "필수 값입니다."),
                request
        ));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> handleMaxUploadSize(MaxUploadSizeExceededException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(error("FILE_TOO_LARGE", "이미지는 10MB 이하만 업로드할 수 있습니다.", Map.of(), request));
    }

    @ExceptionHandler(MultipartException.class)
    ResponseEntity<ApiError> handleMultipart(MultipartException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(error("INVALID_MULTIPART_REQUEST", "업로드 요청을 확인해 주세요.", Map.of(), request));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("INTERNAL_ERROR", "서버 오류가 발생했습니다.", Map.of(), request));
    }

    private ApiError error(
            String code,
            String message,
            Map<String, String> fieldErrors,
            HttpServletRequest request
    ) {
        String traceId = MDC.get("traceId");
        if (traceId == null || traceId.isBlank()) {
            traceId = request.getHeader("X-Request-Id");
        }
        return new ApiError(code, message, fieldErrors, traceId, Instant.now());
    }
}
