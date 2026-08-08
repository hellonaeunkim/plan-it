package com.frend.planit.domain.chatbot.chatMessage.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.frend.planit.domain.chatbot.chatMessage.entity.AIChatMessage;
import com.frend.planit.domain.chatbot.chatRoom.entity.AIChatRoomEntity;
import com.frend.planit.domain.chatbot.chatRoom.repository.AIChatRoomRepository;
import com.frend.planit.domain.user.entity.User;
import com.frend.planit.domain.user.enums.LoginType;
import com.frend.planit.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class AIChatMessageRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AIChatRoomRepository aiChatRoomRepository;

    @Autowired
    private AIChatMessageRepository aiChatMessageRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findsOnlyRequestedNumberOfRecentTurnsWithinChatRoom() {
        User user = userRepository.save(User.builder()
                .loginId("recent-chat-message-user")
                .nickname("recent-chat-message-user")
                .loginType(LoginType.LOCAL)
                .build());
        AIChatRoomEntity targetChatRoom = aiChatRoomRepository.save(AIChatRoomEntity.of(user));
        AIChatRoomEntity otherChatRoom = aiChatRoomRepository.save(AIChatRoomEntity.of(user));

        for (int turn = 1; turn <= 5; turn++) {
            aiChatMessageRepository.save(targetChatRoom.addChatMessage(
                    "질문 " + turn,
                    "답변 " + turn
            ));
        }
        aiChatMessageRepository.save(otherChatRoom.addChatMessage("다른 채팅방 질문", "다른 채팅방 답변"));
        entityManager.flush();
        entityManager.clear();

        List<AIChatMessage> recentMessages = aiChatMessageRepository.findRecentByChatRoomId(
                targetChatRoom.getId(),
                PageRequest.of(0, 3)
        );

        assertThat(recentMessages)
                .extracting(AIChatMessage::getUserMessage)
                .containsExactly("질문 5", "질문 4", "질문 3");
    }
}
