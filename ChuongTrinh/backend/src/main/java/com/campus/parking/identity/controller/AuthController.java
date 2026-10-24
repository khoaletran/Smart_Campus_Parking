package com.campus.parking.identity.controller;

import com.campus.parking.common.dto.ApiResponse;
import com.campus.parking.identity.dto.*;
import com.campus.parking.identity.entity.User;
import com.campus.parking.identity.repository.UserRepository;
import com.campus.parking.identity.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterStudentRequest request) {
        return ApiResponse.ok("Đăng ký tài khoản thành công", authService.registerStudent(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Đăng nhập thành công", authService.login(request));
    }

    @GetMapping("/profile")
    public ApiResponse<StudentDto> getProfile(Authentication authentication) {
        // Lấy username từ Token JWT đã xác thực an toàn, không nhận studentId từ client để chống lỗ hổng IDOR
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        return ApiResponse.ok(authService.getStudentByUserId(user.getId()));
    }
}