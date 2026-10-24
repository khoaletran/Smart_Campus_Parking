package com.campus.parking.card.repository;

import com.campus.parking.card.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    // Tìm thẻ theo mã chip UID (khi xe quẹt thẻ ở cổng)
    Optional<Card> findByCardUid(String cardUid);

    // Kiểm tra mã chip UID này đã có ai đăng ký chưa
    boolean existsByCardUid(String cardUid);

    // Tìm thẻ đang hoạt động của sinh viên
    List<Card> findByStudentId(Long studentId);
}
