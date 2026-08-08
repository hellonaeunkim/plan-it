package com.frend.planit.domain.chatbot.chatMessage.config;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class AIChatContextPropertiesTest {

    @Test
    void rejectsNegativeScheduleLookAheadDays() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AIChatContextProperties(-1, 3));
    }

    @Test
    void rejectsNonPositiveMaxSchedules() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AIChatContextProperties(180, 0));
    }
}
