package com.frend.planit.domain.chatbot.chatMessage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.frend.planit.domain.accommodation.service.AccommodationService;
import com.frend.planit.domain.chatbot.chatMessage.dto.request.AIChatMessageRequest;
import com.frend.planit.domain.chatbot.chatMessage.dto.response.AIChatMessageResponse;
import com.frend.planit.domain.chatbot.chatMessage.entity.AIChatMessage;
import com.frend.planit.domain.chatbot.chatMessage.repository.AIChatMessageRepository;
import com.frend.planit.domain.chatbot.chatRoom.entity.AIChatRoomEntity;
import com.frend.planit.domain.chatbot.chatRoom.repository.AIChatRoomRepository;
import com.frend.planit.domain.user.entity.User;
import com.frend.planit.domain.user.enums.LoginType;
import com.frend.planit.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.ai.openai.api-key=dummy",
        "spring.mail.host=localhost",
        "spring.mail.port=2525",
        "spring.mail.username=test",
        "spring.mail.password=test"
})
@ActiveProfiles("test")
@Transactional
class AIChatMessagePersistenceTest {

    private static final String USER_MESSAGE = "서울 여행 일정을 알려줘";
    private static final String BOT_MESSAGE = "등록된 서울 여행 일정을 안내해 드릴게요.";

    @Autowired
    private AIChatMessageService aiChatMessageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AIChatRoomRepository aiChatRoomRepository;

    @Autowired
    private AIChatMessageRepository aiChatMessageRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private OpenAiChatModel chatClient;

    @MockitoBean
    private AccommodationService accommodationService;

    @Test
    void createMessagesPersistsMessageInDatabase() {
        User user = userRepository.save(User.builder()
                .loginId("chat-message-persistence-user")
                .nickname("chat-message-persistence-user")
                .loginType(LoginType.LOCAL)
                .build());
        AIChatRoomEntity chatRoom = aiChatRoomRepository.save(AIChatRoomEntity.of(user));

        ChatResponse chatResponse = new ChatResponse(
                List.of(new Generation(new AssistantMessage(BOT_MESSAGE))));
        when(chatClient.call(any(Prompt.class))).thenReturn(chatResponse);

        AIChatMessageResponse response = aiChatMessageService.createMessages(
                user.getId(),
                chatRoom.getId(),
                new AIChatMessageRequest(USER_MESSAGE));

        entityManager.flush();
        entityManager.clear();

        assertThat(response.getUserMessage()).isEqualTo(USER_MESSAGE);
        assertThat(response.getBotMessage()).isEqualTo(BOT_MESSAGE);

        List<AIChatMessage> savedMessages = aiChatMessageRepository.findAll();
        assertThat(savedMessages).singleElement().satisfies(savedMessage -> {
            assertThat(savedMessage.getUserMessage()).isEqualTo(USER_MESSAGE);
            assertThat(savedMessage.getBotMessage()).isEqualTo(BOT_MESSAGE);
            assertThat(savedMessage.getAIChatRoom().getId()).isEqualTo(chatRoom.getId());
        });
    }
}
