package com.campus.parking.card.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "card_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Khóa ngoại trỏ đến thẻ bị thay đổi trạng thái
    @Column(name = "card_id", nullable = false)
    private Long cardId;

    // Trạng thái cũ (ví dụ: "ACTIVE")
    @Column(name = "old_status", nullable = false, length = 20)
    private String previousStatus;

    // Trạng thái mới (ví dụ: "LOCKED")
    @Column(name = "new_status", nullable = false, length = 20)
    private String newStatus;

    // Lý do khóa/mở thẻ (ví dụ: "Sinh viên báo mất thẻ")
    @Column(length = 255)
    private String reason;

    // Người thực hiện thay đổi (lấy từ Username đăng nhập trong JWT)
    @Column(name = "changed_by", nullable = false, length = 50)
    private String changedBy;

    // Thời điểm phát sinh thay đổi
    @Column(name = "changed_at", updatable = false)
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();
}