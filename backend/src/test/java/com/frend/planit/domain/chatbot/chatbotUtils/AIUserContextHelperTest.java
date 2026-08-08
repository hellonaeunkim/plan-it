package com.frend.planit.domain.chatbot.chatbotUtils;

import static org.assertj.core.api.Assertions.assertThat;

import com.frend.planit.domain.calendar.schedule.dto.request.ScheduleRequest;
import com.frend.planit.domain.calendar.schedule.entity.ScheduleEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AIUserContextHelperTest {

    @Test
    void createsScheduleDataWithoutSystemPolicy() {
        ScheduleEntity schedule = ScheduleEntity.of(
                null,
                ScheduleRequest.builder()
                        .scheduleTitle("서울 여행")
                        .startDate(LocalDate.of(2026, 8, 20))
                        .endDate(LocalDate.of(2026, 8, 21))
                        .build()
        );

        String context = AIUserContextHelper.buildUserTravelContext(
                List.of(schedule),
                LocalDateTime.of(2026, 8, 8, 12, 0)
        );

        assertThat(context)
                .contains("현재 날짜 : 2026-08-08T12:00")
                .contains("다음은 사용자의 여행 일정입니다:")
                .contains("📅 여행 제목: 서울 여행")
                .contains("기간: 2026-08-20 ~ 2026-08-21")
                .doesNotContain("당신은 여행 계획을 돕는 여행 어시스턴트입니다.")
                .doesNotContain("사용자의 질문에 응답해주세요.");
    }
}
