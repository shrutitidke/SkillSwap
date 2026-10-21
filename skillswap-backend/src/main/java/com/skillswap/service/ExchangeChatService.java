package com.skillswap.service;

import com.skillswap.dto.ExchangeChatMessageCreateDTO;
import com.skillswap.dto.ExchangeChatMessageResponseDTO;
import com.skillswap.entity.ExchangeChatMessage;
import com.skillswap.entity.SkillExchangeRequest;
import com.skillswap.entity.User;
import com.skillswap.enums.RequestStatus;
import com.skillswap.enums.Role;
import com.skillswap.repository.ExchangeChatMessageRepository;
import com.skillswap.repository.SkillExchangeRequestRepository;
import com.skillswap.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ExchangeChatService {

    private final ExchangeChatMessageRepository messageRepository;
    private final SkillExchangeRequestRepository requestRepository;
    private final UserRepository userRepository;

    public ExchangeChatService(
            ExchangeChatMessageRepository messageRepository,
            SkillExchangeRequestRepository requestRepository,
            UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ExchangeChatMessageResponseDTO> getMessages(
            Long requestId,
            Long afterId,
            Authentication authentication) {
        requireAcceptedParticipant(requestId, authentication);

        List<ExchangeChatMessage> messages = afterId == null
                ? messageRepository
                        .findBySkillExchangeRequestIdOrderBySentAtAscIdAsc(
                                requestId)
                : messageRepository
                        .findBySkillExchangeRequestIdAndIdGreaterThanOrderByIdAsc(
                                requestId,
                                afterId);

        return messages.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ExchangeChatMessageResponseDTO sendMessage(
            Long requestId,
            ExchangeChatMessageCreateDTO request,
            Authentication authentication) {
        User sender = requireAcceptedParticipant(requestId, authentication);
        if (request.content() == null || request.content().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Message cannot be empty");
        }
        if (request.content().length() > 2000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Message cannot exceed 2000 characters");
        }

        SkillExchangeRequest exchange = requestRepository.findById(requestId)
                .orElseThrow(() -> chatNotFound());
        ExchangeChatMessage message = new ExchangeChatMessage();
        message.setSkillExchangeRequest(exchange);
        message.setSender(sender);
        message.setContent(request.content().trim());

        return toResponse(messageRepository.save(message));
    }

    private User requireAcceptedParticipant(
            Long requestId,
            Authentication authentication) {
        User currentUser = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(this::chatNotFound);

        if (currentUser.getRole() != Role.USER) {
            throw chatNotFound();
        }

        SkillExchangeRequest exchange = requestRepository.findById(requestId)
                .orElseThrow(this::chatNotFound);
        boolean isParticipant =
                exchange.getLearner().getId().equals(currentUser.getId())
                        || exchange.getTeacher().getId()
                                .equals(currentUser.getId());
        if (!isParticipant) {
            throw chatNotFound();
        }
        if (exchange.getStatus() != RequestStatus.ACCEPTED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Chat is available only for accepted exchanges");
        }
        return currentUser;
    }

    private ExchangeChatMessageResponseDTO toResponse(
            ExchangeChatMessage message) {
        return new ExchangeChatMessageResponseDTO(
                message.getId(),
                message.getSender().getId(),
                message.getSender().getName(),
                message.getContent(),
                message.getSentAt());
    }

    private ResponseStatusException chatNotFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Exchange chat not found");
    }
}
