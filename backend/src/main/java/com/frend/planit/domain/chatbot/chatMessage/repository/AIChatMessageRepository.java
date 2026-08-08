package com.frend.planit.domain.chatbot.chatMessage.repository;

import com.frend.planit.domain.chatbot.chatMessage.entity.AIChatMessage;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AIChatMessageRepository extends JpaRepository<AIChatMessage, Long> {

    @Query("""
            SELECT message
            FROM AIChatMessage message
            WHERE message.AIChatRoom.id = :chatRoomId
            ORDER BY message.id DESC
            """)
    List<AIChatMessage> findRecentByChatRoomId(
            @Param("chatRoomId") Long chatRoomId,
            Pageable pageable);
}
