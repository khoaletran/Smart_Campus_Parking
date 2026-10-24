package com.campus.parking.card.service;

import com.campus.parking.card.dto.CardDto;
import com.campus.parking.card.dto.CardStatusHistoryDto;
import com.campus.parking.card.dto.ChangeCardStatusRequest;
import com.campus.parking.card.dto.LinkCardRequest;
import com.campus.parking.card.entity.Card;
import com.campus.parking.card.entity.CardStatusHistory;
import com.campus.parking.card.repository.CardRepository;
import com.campus.parking.card.repository.CardStatusHistoryRepository;
import com.campus.parking.identity.dto.StudentDto;
import com.campus.parking.identity.service.IdentityModuleApi;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {
    private final CardRepository cardRepository;
    private final CardStatusHistoryRepository cardStatusHistoryRepository;
    private final IdentityModuleApi identityModuleApi;

    @Transactional
    public CardDto linkCardToStudent(LinkCardRequest req) {
        // 1. Kiểm tra tính duy nhất
        if (cardRepository.existsByCardUid(req.getCardUid())) {
            throw new IllegalArgumentException("Mã thẻ NFC/RFID này đã được kích hoạt trước đó: " + req.getCardUid());
        }

        // 2. Giao tiếp liên module
        StudentDto studentDto = identityModuleApi.getStudentByCode(req.getStudentCode());

        // 3. Quy tắc nghiệp vụ
//        cardRepository.findByStudentId(studentDto.getId()).ifPresent(oldCard -> {
//            if(oldCard.isActive()){
//                throw new IllegalStateException("Sinh viên " + studentDto.getStudentCode() + " đã có thẻ đang hoạt động. Cần khóa thẻ cũ trước!");
//            }
//        });

        // 4. Khởi tạo đối tượng Thẻ và lưu vào CSDL
        Card card = Card.builder()
                .cardUid(req.getCardUid())
                .studentId(studentDto.getId())
                .status("ACTIVE")
                .build();
        cardRepository.save(card);

        // 5. Đóng gói và trả về CardDto
        return toDto(card);
    }

    private Card getCardById(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thẻ với ID: " + cardId));
    }

    private void recordHistory(Long cardId, String oldStatus, String newStatus, String reason, String actor) {
        CardStatusHistory history = CardStatusHistory.builder()
                .cardId(cardId)
                .previousStatus(oldStatus)
                .newStatus(newStatus)
                .reason(reason)
                .changedBy(actor)
                .build();
        cardStatusHistoryRepository.save(history);
    }


    @Transactional
    public CardDto lockCard(Long cardId, ChangeCardStatusRequest req, String actor){
        Card card = getCardById(cardId);
        String oldStatus = card.getStatus();

        if ("LOCKED".equalsIgnoreCase(oldStatus)) {
            throw new IllegalStateException("Thẻ này hiện tại đã bị khóa rồi!");
        }

        card.lockCard();
        cardRepository.save(card);

        recordHistory(cardId, oldStatus, "LOCKED", req.getReason(), actor);

        return toDto(card);
    }

    @Transactional
    public CardDto unlockCard(Long cardId, ChangeCardStatusRequest req, String actor){
        Card card = getCardById(cardId);
        String oldStatus = card.getStatus();

        if("ACTIVE".equalsIgnoreCase(oldStatus)) {
            throw new IllegalStateException("Thẻ này hiện tại đang hoạt động, không cần mở khóa!");
        }

        card.unlockCard();
        cardRepository.save(card);

        recordHistory(cardId, oldStatus, "ACTIVE", req.getReason(), actor);
        return toDto(card);
    }

    public List<CardStatusHistoryDto> getCardHistory(Long cardId) {
        getCardById(cardId);

        return cardStatusHistoryRepository.findByCardIdOrderByChangedAtDesc(cardId)
                .stream()
                .map(this::toHistoryDto)
                .toList();
    }

    public CardDto toDto(Card card) {
        return CardDto.builder()
                .id(card.getId())
                .cardUid(card.getCardUid())
                .studentId(card.getStudentId())
                .status(card.getStatus())
                .issueDate(card.getIssueDate())
                .build();
    }

    public List<CardDto> getCardsByStudentId(Long studentId) {
        return cardRepository.findByStudentId(studentId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private CardStatusHistoryDto toHistoryDto(CardStatusHistory h) {
        return CardStatusHistoryDto.builder()
                .id(h.getId())
                .cardId(h.getCardId())
                .previousStatus(h.getPreviousStatus())
                .newStatus(h.getNewStatus())
                .reason(h.getReason())
                .changedBy(h.getChangedBy())
                .changedAt(h.getChangedAt())
                .build();
    }
}
