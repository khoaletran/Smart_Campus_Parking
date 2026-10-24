package com.campus.parking.card.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardStatusHistoryDto {
    private Long id;
    private Long cardId;
    private String previousStatus;
    private String newStatus;
    private String reason;
    private String changedBy;
    private LocalDateTime changedAt;
}