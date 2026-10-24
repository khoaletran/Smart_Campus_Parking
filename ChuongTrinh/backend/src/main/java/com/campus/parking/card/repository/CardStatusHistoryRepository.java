package com.campus.parking.card.repository;

import com.campus.parking.card.entity.CardStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardStatusHistoryRepository extends JpaRepository<CardStatusHistory, Long> {
    List<CardStatusHistory> findByCardIdOrderByChangedAtDesc(Long cardId);
}
