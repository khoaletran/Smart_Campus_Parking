package com.campus.parking.card.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangeCardStatusRequest {
    @NotBlank(message = "Lý do thay đổi trạng thái thẻ không được để trống")
    private String reason;
}