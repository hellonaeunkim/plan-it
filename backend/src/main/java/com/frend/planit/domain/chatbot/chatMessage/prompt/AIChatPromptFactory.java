package com.frend.planit.domain.chatbot.chatMessage.prompt;

import com.frend.planit.domain.calendar.schedule.entity.ScheduleEntity;
import com.frend.planit.domain.chatbot.chatMessage.entity.AIChatMessage;
import com.frend.planit.domain.chatbot.chatbotUtils.AIUserContextHelper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

@Component
public class AIChatPromptFactory {

    public Prompt create(
            List<ScheduleEntity> schedules,
            List<AIChatMessage> chatMessages,
            String currentUserMessage) {
        String travelContext = AIUserContextHelper.buildUserTravelContext(schedules);

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(travelContext));

        chatMessages.forEach(chatMessage -> {
            messages.add(new UserMessage(chatMessage.getUserMessage()));
            messages.add(new AssistantMessage(chatMessage.getBotMessage()));
        });

        messages.add(new UserMessage(currentUserMessage));

        return new Prompt(messages);
    }
}
