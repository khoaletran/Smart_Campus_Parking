package com.campus.parking.card.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mã UID vật lý đọc từ chip NFC/RFID trên thẻ sinh viên (duy nhất)
    @Column(name = "card_uid", nullable = false, unique = true, length = 64)
    private String cardUid;

    // Khóa ngoại logic trỏ sang id của bảng students (Module Identity)
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    // Trạng thái thẻ: "ACTIVE" (đang dùng), "LOCKED" (bị khóa do mất)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    // Ngày phát hành/kích hoạt thẻ vào hệ thống
    @Column(name = "issued_at", updatable = false)
    @Builder.Default
    private LocalDateTime issueDate = LocalDateTime.now();

    // --- CÁC PHƯƠNG THỨC NGHIỆP VỤ THEO ĐÚNG SƠ ĐỒ LỚP UML ---

    // 1. Gán thẻ cho một sinh viên cụ thể
    public void linkStudent(Long studentId) {
        if (studentId == null) {
            throw new IllegalArgumentException("Mã sinh viên không được để trống");
        }
        this.studentId = studentId;
        this.status = "ACTIVE";
    }

    // 2. Khóa thẻ khi sinh viên báo mất
    public void lockCard() {
        this.status = "LOCKED";
    }

    // 3. Mở khóa lại thẻ
    public void unlockCard() {
        this.status = "ACTIVE";
    }

    // 4. Kiểm tra thẻ có đang hợp lệ để gửi xe không
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(this.status);
    }
}