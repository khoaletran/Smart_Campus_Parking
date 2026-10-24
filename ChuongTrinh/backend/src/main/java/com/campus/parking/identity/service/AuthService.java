package com.campus.parking.identity.service;

import com.campus.parking.identity.dto.*;
import com.campus.parking.identity.entity.Student;
import com.campus.parking.identity.entity.User;
import com.campus.parking.identity.enums.Role;
import com.campus.parking.identity.repository.StudentRepository;
import com.campus.parking.identity.repository.UserRepository;
import com.campus.parking.identity.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService implements IdentityModuleApi {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse registerStudent(RegisterStudentRequest req) {
        // Kiểm tra xem MSSV này đã đăng ký User hoặc Student chưa
        if (userRepository.existsByUsername(req.getStudentCode()) || studentRepository.existsByStudentCode(req.getStudentCode())) {
            throw new IllegalArgumentException("Mã sinh viên đã được đăng ký tài khoản: " + req.getStudentCode());
        }
        if (studentRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng: " + req.getEmail());
        }

        // 1. Tạo User với username chính là studentCode
        User user = User.builder()
                .username(req.getStudentCode()) // Đồng nhất username = MSSV
                .password(passwordEncoder.encode(req.getPassword()))
                .role(Role.STUDENT)
                .active(true)
                .build();
        userRepository.save(user);

        // 2. Tạo Student
        Student student = Student.builder()
                .user(user)
                .studentCode(req.getStudentCode())
                .fullName(req.getFullName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .faculty(req.getFaculty())
                .build();
        studentRepository.save(student);

        // 3. Cấp Token đăng nhập
        String token = jwtService.generateToken(user.getUsername(), user.getRole().name(), user.getId());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .studentCode(student.getStudentCode())
                .fullName(student.getFullName())
                .build();
    }

    public AuthResponse login(LoginRequest req) {
        String token = authenticate(req.getUsername(), req.getPassword());
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        AuthResponse.AuthResponseBuilder builder = AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name());

        if (user.getRole() == Role.STUDENT) {
            studentRepository.findByUserId(user.getId()).ifPresent(s -> {
                builder.studentCode(s.getStudentCode());
                builder.fullName(s.getFullName());
            });
        }

        return builder.build();
    }

    // --- Hiện thực các phương thức của Interface IdentityModuleApi ---

    @Override
    public String authenticate(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Sai tên đăng nhập hoặc mật khẩu"));

        if (!user.verifyPassword(password, passwordEncoder)) {
            throw new IllegalArgumentException("Sai tên đăng nhập hoặc mật khẩu");
        }
        if (!user.isActive()) {
            throw new IllegalStateException("Tài khoản đã bị vô hiệu hóa");
        }

        return jwtService.generateToken(user.getUsername(), user.getRole().name(), user.getId());
    }

    @Override
    public Long verifyToken(String token) {
        String username = jwtService.extractUsername(token);
        if (jwtService.isTokenValid(token, username)) {
            return jwtService.extractUserId(token);
        }
        throw new IllegalArgumentException("Token không hợp lệ hoặc đã hết hạn");
    }

    @Override
    public StudentDto getStudentById(Long studentId) {
        Student s = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên với ID: " + studentId));
        return toDto(s);
    }

    @Override
    public StudentDto getStudentByUserId(Long userId) {
        Student s = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ sinh viên"));
        return toDto(s);
    }

    private StudentDto toDto(Student s) {
        return StudentDto.builder()
                .id(s.getId())
                .studentCode(s.getStudentCode())
                .fullName(s.getFullName())
                .email(s.getEmail())
                .phone(s.getPhone())
                .faculty(s.getFaculty())
                .build();
    }

    @Override
    public StudentDto getStudentByCode(String studentCode) {
        Student s = studentRepository.findByStudentCode(studentCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên có MSSV: " + studentCode));
        return toDto(s);
    }
}