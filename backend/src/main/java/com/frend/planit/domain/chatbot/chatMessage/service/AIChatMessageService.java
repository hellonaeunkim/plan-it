package com.frend.planit.domain.chatbot.chatMessage.service;

import com.frend.planit.domain.calendar.schedule.entity.ScheduleEntity;
import com.frend.planit.domain.calendar.schedule.repository.ScheduleRepository;
import com.frend.planit.domain.chatbot.chatMessage.dto.request.AIChatMessageRequest;
import com.frend.planit.domain.chatbot.chatMessage.dto.response.AIChatMessageResponse;
import com.frend.planit.domain.chatbot.chatMessage.entity.AIChatMessage;
import com.frend.planit.domain.chatbot.chatMessage.prompt.AIChatPromptFactory;
import com.frend.planit.domain.chatbot.chatMessage.repository.AIChatMessageRepository;
import com.frend.planit.domain.chatbot.chatRoom.entity.AIChatRoomEntity;
import com.frend.planit.domain.chatbot.chatRoom.repository.AIChatRoomRepository;
import com.frend.planit.domain.user.entity.User;
import com.frend.planit.domain.user.repository.UserRepository;
import com.frend.planit.global.exception.ServiceException;
import com.frend.planit.global.response.ErrorType;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIChatMessageService {

    private final AIChatRoomRepository aiChatRoomRepository;
    private final AIChatMessageRepository aiChatMessageRepository;
    private final OpenAiChatModel chatClient;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final AIChatPromptFactory promptFactory;


    @Transactional
    public AIChatMessageResponse createMessages(
            Long userId,
            Long chatRoomId,
            AIChatMessageRequest request) {
        long requestStartedAt = System.nanoTime();

        // 로그인 인증 사용자 여부 확인
        checkUser(userId);

        // 채팅방 조회
        AIChatRoomEntity chatRoom = aiChatRoomRepository.findByIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new ServiceException(ErrorType.AI_CHAT_ROOM_NOT_FOUND));

        // 사용자 Schedule 조회
        List<ScheduleEntity> userSchedules = scheduleRepository.findAllByUserId(userId);

        Prompt prompt = promptFactory.create(
                userSchedules,
                chatRoom.getAIChatMessages(),
                request.getUserMessage()
        );

        long llmStartedAt = System.nanoTime();
        ChatResponse chatResponse = chatClient.call(prompt);
        long llmDurationMs = elapsedMillis(llmStartedAt);

        String botMessage = chatResponse
                .getResult()
                .getOutput()
                .getText();

        // 메세지 저장
        AIChatMessage message = chatRoom.addChatMessage(request.getUserMessage(), botMessage);
        AIChatMessage savedMessage = aiChatMessageRepository.save(message);

        try {
            logMetrics(chatResponse, llmDurationMs, elapsedMillis(requestStartedAt));
        } catch (RuntimeException e) {
            log.warn(
                    "event=ai_chat_response_metric_failed promptVersion={} errorType={}",
                    promptFactory.getPromptVersion(),
                    e.getClass().getSimpleName()
            );
        }

        return AIChatMessageResponse.from(savedMessage);
    }

    private void logMetrics(ChatResponse chatResponse, long llmDurationMs, long serviceDurationMs) {
        ChatResponseMetadata metadata = chatResponse.getMetadata();
        Usage usage = metadata.getUsage();

        int promptTokens = tokenCount(usage.getPromptTokens());
        int completionTokens = tokenCount(usage.getCompletionTokens());
        int totalTokens = tokenCount(usage.getTotalTokens());
        boolean usageAvailable = totalTokens > 0;
        String model = StringUtils.hasText(metadata.getModel()) ? metadata.getModel() : "unknown";

        log.info(
                "event=ai_chat_response_metric promptVersion={} model={} usageAvailable={} "
                        + "promptTokens={} completionTokens={} totalTokens={} "
                        + "llmDurationMs={} serviceDurationMs={}",
                promptFactory.getPromptVersion(),
                model,
                usageAvailable,
                promptTokens,
                completionTokens,
                totalTokens,
                llmDurationMs,
                serviceDurationMs
        );
    }

    private int tokenCount(Integer tokens) {
        return tokens == null ? 0 : tokens;
    }

    private long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    // 사용자 조회
    public User checkUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorType.USER_NOT_FOUND));
    }
}
