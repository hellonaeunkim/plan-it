package com.frend.planit.domain.chatbot.chatMessage.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIChatTimeConfig {

    @Bean
    public Clock aiChatClock() {
        return Clock.systemDefaultZone();
    }
}
