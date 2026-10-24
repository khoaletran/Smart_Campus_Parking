package com.campus.parking.common.exception;

import com.campus.parking.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // 1. Bắt các lỗi nghiệp vụ do lập trình viên chủ động ném ra (throw new IllegalArgumentException)
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    // 2. Bắt các lỗi trạng thái (ví dụ tài khoản bị khóa: throw new IllegalStateException)
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleIllegalStateException(IllegalStateException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    // 3. Bắt các lỗi validate dữ liệu đầu vào (@NotBlank, @Email, @Size từ DTO)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ApiResponse.<Map<String, String>>builder()
                .success(false)
                .message("Dữ liệu gửi lên không hợp lệ")
                .data(errors)
                .build();
    }

    // 4. Bắt mọi lỗi hệ thống bất ngờ khác (chống sập app và chống lộ mã nguồn)
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGeneralException(Exception ex) {
        return ApiResponse.error("Đã xảy ra lỗi hệ thống: " + ex.getMessage());
    }
}
