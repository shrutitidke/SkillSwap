package com.skillswap.repository;

import com.skillswap.entity.ExchangeChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExchangeChatMessageRepository
        extends JpaRepository<ExchangeChatMessage, Long> {

    List<ExchangeChatMessage>
    findBySkillExchangeRequestIdOrderBySentAtAscIdAsc(Long requestId);

    List<ExchangeChatMessage>
    findBySkillExchangeRequestIdAndIdGreaterThanOrderByIdAsc(
            Long requestId,
            Long afterId);

    void deleteBySkillExchangeRequestIdIn(List<Long> requestIds);
}
