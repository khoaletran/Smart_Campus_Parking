package com.campus.parking.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterStudentRequest {

    @NotBlank(message = "Mã sinh viên không được để trống")
    private String studentCode; // Dùng MSSV làm tài khoản đăng nhập luôn

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    private String email;

    private String phone;
    private String faculty;
}