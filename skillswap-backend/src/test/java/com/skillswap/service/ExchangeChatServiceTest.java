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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeChatServiceTest {

    @Mock
    private ExchangeChatMessageRepository messageRepository;

    @Mock
    private SkillExchangeRequestRepository requestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    private ExchangeChatService chatService;
    private User learner;
    private User teacher;
    private User outsider;
    private SkillExchangeRequest acceptedExchange;

    @BeforeEach
    void setUp() {
        chatService = new ExchangeChatService(
                messageRepository,
                requestRepository,
                userRepository);
        learner = user(1L, "Learner", "learner@example.test", Role.USER);
        teacher = user(2L, "Teacher", "teacher@example.test", Role.USER);
        outsider = user(3L, "Outsider", "outsider@example.test", Role.USER);
        acceptedExchange = new SkillExchangeRequest();
        acceptedExchange.setId(25L);
        acceptedExchange.setLearner(learner);
        acceptedExchange.setTeacher(teacher);
        acceptedExchange.setStatus(RequestStatus.ACCEPTED);

        lenient().when(requestRepository.findById(25L))
                .thenReturn(Optional.of(acceptedExchange));
    }

    @Test
    void participantCanLoadPreviousMessages() {
        authenticateAs(learner);
        ExchangeChatMessage previous = message(
                50L, learner, "Let's meet on Friday.");
        when(messageRepository
                .findBySkillExchangeRequestIdOrderBySentAtAscIdAsc(25L))
                .thenReturn(List.of(previous));

        List<ExchangeChatMessageResponseDTO> messages =
                chatService.getMessages(25L, null, authentication);

        assertEquals(1, messages.size());
        assertEquals(learner.getId(), messages.get(0).senderId());
        assertEquals("Let's meet on Friday.", messages.get(0).content());
        verify(messageRepository)
                .findBySkillExchangeRequestIdOrderBySentAtAscIdAsc(25L);
    }

    @Test
    void participantCanLoadOnlyMessagesAfterCursor() {
        authenticateAs(teacher);
        ExchangeChatMessage newer = message(52L, learner, "See you then.");
        when(messageRepository
                .findBySkillExchangeRequestIdAndIdGreaterThanOrderByIdAsc(
                        25L, 51L))
                .thenReturn(List.of(newer));

        List<ExchangeChatMessageResponseDTO> messages =
                chatService.getMessages(25L, 51L, authentication);

        assertEquals(List.of(52L), messages.stream()
                .map(ExchangeChatMessageResponseDTO::id)
                .toList());
    }

    @Test
    void participantCanSendMessageAsTheirOwnIdentity() {
        authenticateAs(learner);
        ExchangeChatMessage saved = message(
                51L, learner, "  What should I prepare?  ");
        when(messageRepository.save(any(ExchangeChatMessage.class)))
                .thenAnswer(invocation -> {
                    ExchangeChatMessage message = invocation.getArgument(0);
                    message.setId(saved.getId());
                    message.setSentAt(saved.getSentAt());
                    return message;
                });

        ExchangeChatMessageResponseDTO response = chatService.sendMessage(
                25L,
                new ExchangeChatMessageCreateDTO("  What should I prepare?  "),
                authentication);

        assertEquals(learner.getId(), response.senderId());
        assertEquals("Learner", response.senderName());
        assertEquals("What should I prepare?", response.content());
        verify(messageRepository).save(any(ExchangeChatMessage.class));
    }

    @Test
    void nonParticipantCannotReadOrSendMessages() {
        authenticateAs(outsider);

        ResponseStatusException readException = assertThrows(
                ResponseStatusException.class,
                () -> chatService.getMessages(25L, null, authentication));
        ResponseStatusException sendException = assertThrows(
                ResponseStatusException.class,
                () -> chatService.sendMessage(
                        25L,
                        new ExchangeChatMessageCreateDTO("Hello"),
                        authentication));

        assertEquals(HttpStatus.NOT_FOUND, readException.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, sendException.getStatusCode());
        verify(messageRepository, never())
                .findBySkillExchangeRequestIdOrderBySentAtAscIdAsc(any());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void adminCannotReadOrSendMessages() {
        User admin = user(4L, "Admin", "admin@example.test", Role.ADMIN);
        authenticateAs(admin);

        ResponseStatusException readException = assertThrows(
                ResponseStatusException.class,
                () -> chatService.getMessages(25L, null, authentication));
        ResponseStatusException sendException = assertThrows(
                ResponseStatusException.class,
                () -> chatService.sendMessage(
                        25L,
                        new ExchangeChatMessageCreateDTO("Hello"),
                        authentication));

        assertEquals(HttpStatus.NOT_FOUND, readException.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, sendException.getStatusCode());
        verify(requestRepository, never()).findById(25L);
        verify(messageRepository, never())
                .findBySkillExchangeRequestIdOrderBySentAtAscIdAsc(any());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void pendingExchangeCannotUseChat() {
        authenticateAs(learner);
        acceptedExchange.setStatus(RequestStatus.PENDING);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> chatService.getMessages(25L, null, authentication));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(messageRepository, never())
                .findBySkillExchangeRequestIdOrderBySentAtAscIdAsc(any());
    }

    @Test
    void pendingExchangeCannotSendChatMessages() {
        authenticateAs(learner);
        acceptedExchange.setStatus(RequestStatus.PENDING);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> chatService.sendMessage(
                        25L,
                        new ExchangeChatMessageCreateDTO("Hello"),
                        authentication));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void emptyMessageCannotBeSaved() {
        authenticateAs(learner);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> chatService.sendMessage(
                        25L,
                        new ExchangeChatMessageCreateDTO("  \n "),
                        authentication));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void oversizedMessageCannotBeSaved() {
        authenticateAs(learner);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> chatService.sendMessage(
                        25L,
                        new ExchangeChatMessageCreateDTO("x".repeat(2001)),
                        authentication));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(messageRepository, never()).save(any());
    }

    private void authenticateAs(User user) {
        when(authentication.getName()).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));
    }

    private User user(Long id, String name, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    private ExchangeChatMessage message(
            Long id,
            User sender,
            String content) {
        ExchangeChatMessage message = new ExchangeChatMessage();
        message.setId(id);
        message.setSender(sender);
        message.setSkillExchangeRequest(acceptedExchange);
        message.setContent(content);
        message.setSentAt(LocalDateTime.of(2026, 10, 21, 14, 0));
        return message;
    }
}
