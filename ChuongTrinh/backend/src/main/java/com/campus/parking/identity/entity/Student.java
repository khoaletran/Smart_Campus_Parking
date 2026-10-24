package com.campus.parking.identity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // Đường liên kết 1..1 sang tài khoản User

    @Column(name = "student_code", nullable = false, unique = true, length = 20)
    private String studentCode; // MSSV: SV2026001

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "phone_number", length = 15)
    private String phone; // Số điện thoại

    @Column(length = 100)
    private String faculty; // Khoa

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public void updateProfile(String fullName, String phone) {
        if (fullName != null && !fullName.isBlank()) {
            this.fullName = fullName;
        }
        if (phone != null && !phone.isBlank()) {
            this.phone = phone;
        }
    }
}