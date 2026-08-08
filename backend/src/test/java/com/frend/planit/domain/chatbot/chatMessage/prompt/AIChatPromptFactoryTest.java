package com.frend.planit.domain.chatbot.chatMessage.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import com.frend.planit.domain.chatbot.chatMessage.entity.AIChatMessage;
import com.frend.planit.domain.chatbot.chatRoom.entity.AIChatRoomEntity;
import com.frend.planit.domain.user.entity.User;
import com.frend.planit.domain.user.enums.LoginType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

class AIChatPromptFactoryTest {

    private final AIChatPromptFactory promptFactory = new AIChatPromptFactory();

    @Test
    void createsPromptWithSystemContextConversationHistoryAndCurrentMessageInOrder() {
        AIChatRoomEntity chatRoom = AIChatRoomEntity.of(User.builder()
                .loginId("prompt-factory-user")
                .nickname("prompt-factory-user")
                .loginType(LoginType.LOCAL)
                .build());
        AIChatMessage firstMessage = chatRoom.addChatMessage("첫 번째 질문", "첫 번째 답변");
        AIChatMessage secondMessage = chatRoom.addChatMessage("두 번째 질문", "두 번째 답변");

        Prompt prompt = promptFactory.create(
                List.of(),
                List.of(firstMessage, secondMessage),
                "현재 질문"
        );

        List<Message> messages = prompt.getInstructions();
        assertThat(messages).hasSize(6);
        assertMessage(messages.get(0), SystemMessage.class, emptyScheduleSystemMessage());
        assertMessage(messages.get(1), UserMessage.class, "첫 번째 질문");
        assertMessage(messages.get(2), AssistantMessage.class, "첫 번째 답변");
        assertMessage(messages.get(3), UserMessage.class, "두 번째 질문");
        assertMessage(messages.get(4), AssistantMessage.class, "두 번째 답변");
        assertMessage(messages.get(5), UserMessage.class, "현재 질문");
    }

    private void assertMessage(
            Message message,
            Class<? extends Message> expectedType,
            String expectedText) {
        assertThat(message).isInstanceOf(expectedType);
        assertThat(message.getText()).isEqualTo(expectedText);
    }

    private String emptyScheduleSystemMessage() {
        return """
                당신은 여행 계획을 돕는 여행 어시스턴트입니다.

                사용자가 입력한 언어를 자동으로 감지하여, "그 언어로만" 응답해야 합니다.
                예를 들어, 사용자가 한국어로 질문하면 반드시 한국어로만 답하고,
                영어로 질문하면 영어로만, 일본어로 질문하면 일본어로만 답해야 합니다.
                절대 다른 언어를 혼용하거나 언어를 전환하지 마십시오.

                모든 응답은 자연스럽고 친절하며, 간결하게 작성해야 합니다.

                당신의 주요 임무는 사용자의 여행 일정을 기반으로 친절하고 유용한 정보를 제공해야 합니다.
                이 채팅은 “plan-it”이라는 여행 계획 서비스의 일부이며,
                사용자가 더 나은 여행을 경험할 수 있도록 돕는 것이 목표입니다.사용자가 여행 일정을 등록하지 않았습니다.
                """.stripIndent().trim();
    }
}
