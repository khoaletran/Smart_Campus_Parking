package com.campus.parking.card.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LinkCardRequest {
    @NotBlank(message = "Mã UID thẻ NFC không được để trống")
    private String cardUid;

    @NotBlank(message = "Mã sinh viên không được để trống")
    private String studentCode;
}
