package com.campus.parking.identity.entity;

import com.campus.parking.identity.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String password; // Chuỗi băm BCrypt an toàn

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.STUDENT;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true; // Khớp thuộc tính - active: boolean trong Sơ đồ lớp

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // Phương thức nghiệp vụ theo Sơ đồ lớp: Kiểm tra mật khẩu đầu vào
    public boolean verifyPassword(String rawPassword, PasswordEncoder encoder) {
        return encoder.matches(rawPassword, this.password);
    }

    // Phương thức nghiệp vụ theo Sơ đồ lớp: Đổi mật khẩu
    public void changePassword(String oldPass, String newPass, PasswordEncoder encoder) {
        if (!verifyPassword(oldPass, encoder)) {
            throw new IllegalArgumentException("Mật khẩu cũ không chính xác");
        }
        if (newPass == null || newPass.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới tối thiểu 6 ký tự");
        }
        this.password = encoder.encode(newPass);
    }
}