package com.frend.planit.domain.chatbot.chatMessage.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class AIChatContextProperties {

    private final int scheduleLookAheadDays;
    private final int maxSchedules;
    private final int maxRecentTurns;

    public AIChatContextProperties(
            @Value("${planit.ai.chat.schedule-look-ahead-days:180}") int scheduleLookAheadDays,
            @Value("${planit.ai.chat.max-schedules:3}") int maxSchedules,
            @Value("${planit.ai.chat.max-recent-turns:3}") int maxRecentTurns) {
        if (scheduleLookAheadDays < 0) {
            throw new IllegalArgumentException("AI 일정 조회 기간은 0일 이상이어야 합니다.");
        }
        if (maxSchedules < 1) {
            throw new IllegalArgumentException("AI 일정 최대 개수는 1개 이상이어야 합니다.");
        }
        if (maxRecentTurns < 1) {
            throw new IllegalArgumentException("AI 최근 대화 턴 수는 1개 이상이어야 합니다.");
        }

        this.scheduleLookAheadDays = scheduleLookAheadDays;
        this.maxSchedules = maxSchedules;
        this.maxRecentTurns = maxRecentTurns;
    }
}
