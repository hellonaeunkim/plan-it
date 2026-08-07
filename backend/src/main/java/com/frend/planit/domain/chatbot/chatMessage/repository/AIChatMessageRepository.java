package com.frend.planit.domain.chatbot.chatMessage.repository;

import com.frend.planit.domain.chatbot.chatMessage.entity.AIChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIChatMessageRepository extends JpaRepository<AIChatMessage, Long> {
}
