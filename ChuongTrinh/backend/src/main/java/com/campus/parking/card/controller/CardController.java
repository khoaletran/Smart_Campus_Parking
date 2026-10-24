package com.campus.parking.card.controller;

import com.campus.parking.card.dto.CardDto;
import com.campus.parking.card.dto.CardStatusHistoryDto;
import com.campus.parking.card.dto.ChangeCardStatusRequest;
import com.campus.parking.card.dto.LinkCardRequest;
import com.campus.parking.card.service.CardService;
import com.campus.parking.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {
    private final CardService cardService;

    // 1. Kích hoạt & liên kết thẻ
    @PostMapping("/link")
    public ApiResponse<CardDto> link(@RequestBody @Valid LinkCardRequest req) {
        return ApiResponse.ok("Kích hoạt và liên kết thẻ thành công",
            cardService.linkCardToStudent(req));
    }

    // 2. Khóa thẻ khẩn cấp
    @PatchMapping("/{id}/lock")
    public ApiResponse<CardDto> lockCard(@PathVariable Long id,
                                         @Valid @RequestBody ChangeCardStatusRequest request,
                                         Authentication authentication) {
        String actor = authentication.getName(); // Lấy username người gọi từ JWT Token
        return ApiResponse.ok("Khóa thẻ thành công", cardService.lockCard(id, request, actor));
    }

    // 3. Mở khóa lại thẻ
    @PatchMapping("/{id}/unlock")
    public ApiResponse<CardDto> unlockCard(@PathVariable Long id,
                                           @Valid @RequestBody ChangeCardStatusRequest request,
                                           Authentication authentication) {
        String actor = authentication.getName();
        return ApiResponse.ok("Mở khóa thẻ thành công", cardService.unlockCard(id, request, actor));
    }

    // 4. Xem lịch sử thay đổi trạng thái của thẻ
    @GetMapping("/{id}/history")
    public ApiResponse<List<CardStatusHistoryDto>> getHistory(@PathVariable Long id) {
        return ApiResponse.ok("Lấy lịch sử thẻ thành công", cardService.getCardHistory(id));
    }

    // Lấy toàn bộ danh sách thẻ của một sinh viên
    @GetMapping("/student/{studentId}")
    public ApiResponse<List<CardDto>> getCardsByStudent(@PathVariable Long studentId) {
        return ApiResponse.ok("Lấy danh sách thẻ thành công", cardService.getCardsByStudentId(studentId));
    }
}
