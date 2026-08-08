package com.frend.planit.domain.chatbot.chatMessage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.frend.planit.domain.calendar.schedule.repository.ScheduleRepository;
import com.frend.planit.domain.chatbot.chatMessage.dto.request.AIChatMessageRequest;
import com.frend.planit.domain.chatbot.chatMessage.dto.response.AIChatMessageResponse;
import com.frend.planit.domain.chatbot.chatMessage.entity.AIChatMessage;
import com.frend.planit.domain.chatbot.chatMessage.prompt.AIChatPromptFactory;
import com.frend.planit.domain.chatbot.chatMessage.repository.AIChatMessageRepository;
import com.frend.planit.domain.chatbot.chatRoom.entity.AIChatRoomEntity;
import com.frend.planit.domain.chatbot.chatRoom.repository.AIChatRoomRepository;
import com.frend.planit.domain.user.entity.User;
import com.frend.planit.domain.user.enums.LoginType;
import com.frend.planit.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class AIChatMessageMetricsTest {

    private static final Long USER_ID = 1L;
    private static final Long CHAT_ROOM_ID = 10L;
    private static final String USER_MESSAGE = "서울 여행 일정을 알려줘";
    private static final String BOT_MESSAGE = "등록된 서울 여행 일정을 안내해 드릴게요.";

    @Mock
    private AIChatRoomRepository aiChatRoomRepository;

    @Mock
    private AIChatMessageRepository aiChatMessageRepository;

    @Mock
    private OpenAiChatModel chatClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    private AIChatMessageService aiChatMessageService;

    @BeforeEach
    void setUp() {
        aiChatMessageService = new AIChatMessageService(
                aiChatRoomRepository,
                aiChatMessageRepository,
                chatClient,
                userRepository,
                scheduleRepository,
                new AIChatPromptFactory()
        );

        User user = User.builder()
                .loginId("ai-metrics-user")
                .nickname("ai-metrics-user")
                .loginType(LoginType.LOCAL)
                .build();
        AIChatRoomEntity chatRoom = AIChatRoomEntity.of(user);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(aiChatRoomRepository.findByIdAndUserId(CHAT_ROOM_ID, USER_ID))
                .thenReturn(Optional.of(chatRoom));
        when(scheduleRepository.findAllByUserId(USER_ID)).thenReturn(List.of());
        when(aiChatMessageRepository.save(any(AIChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void logsTokenUsageAndResponseTime(CapturedOutput output) {
        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                .model("openai/gpt-oss-120b")
                .usage(new DefaultUsage(120, 30, 150))
                .build();
        when(chatClient.call(any(Prompt.class))).thenReturn(chatResponse(metadata));

        AIChatMessageResponse response = createMessage();

        assertThat(response.getBotMessage()).isEqualTo(BOT_MESSAGE);
        assertThat(output)
                .contains("event=ai_chat_response_metric")
                .contains("promptVersion=ai-chat-v2")
                .contains("model=openai/gpt-oss-120b")
                .contains("usageAvailable=true")
                .contains("promptTokens=120")
                .contains("completionTokens=30")
                .contains("totalTokens=150")
                .contains("llmDurationMs=")
                .contains("serviceDurationMs=");
    }

    @Test
    void continuesWhenTokenUsageMetadataIsUnavailable(CapturedOutput output) {
        when(chatClient.call(any(Prompt.class))).thenReturn(chatResponse(new ChatResponseMetadata()));

        AIChatMessageResponse response = createMessage();

        assertThat(response.getBotMessage()).isEqualTo(BOT_MESSAGE);
        assertThat(output)
                .contains("event=ai_chat_response_metric")
                .contains("model=unknown")
                .contains("usageAvailable=false")
                .contains("promptTokens=0")
                .contains("completionTokens=0")
                .contains("totalTokens=0");
    }

    @Test
    void continuesWhenMetricsLoggingFails(CapturedOutput output) {
        when(chatClient.call(any(Prompt.class))).thenReturn(chatResponse(null));

        AIChatMessageResponse response = createMessage();

        assertThat(response.getBotMessage()).isEqualTo(BOT_MESSAGE);
        verify(aiChatMessageRepository).save(any(AIChatMessage.class));
        assertThat(output)
                .contains("event=ai_chat_response_metric_failed")
                .contains("promptVersion=ai-chat-v2")
                .contains("errorType=NullPointerException");
    }

    private AIChatMessageResponse createMessage() {
        return aiChatMessageService.createMessages(
                USER_ID,
                CHAT_ROOM_ID,
                new AIChatMessageRequest(USER_MESSAGE)
        );
    }

    private ChatResponse chatResponse(ChatResponseMetadata metadata) {
        return new ChatResponse(
                List.of(new Generation(new AssistantMessage(BOT_MESSAGE))),
                metadata
        );
    }
}
