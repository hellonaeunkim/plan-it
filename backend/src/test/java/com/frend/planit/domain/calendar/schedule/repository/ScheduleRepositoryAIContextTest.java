package com.frend.planit.domain.calendar.schedule.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.frend.planit.domain.calendar.entity.CalendarEntity;
import com.frend.planit.domain.calendar.repository.CalendarRepository;
import com.frend.planit.domain.calendar.schedule.dto.request.ScheduleRequest;
import com.frend.planit.domain.calendar.schedule.entity.ScheduleEntity;
import com.frend.planit.domain.user.entity.User;
import com.frend.planit.domain.user.enums.LoginType;
import com.frend.planit.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class ScheduleRepositoryAIContextTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 9);
    private static final LocalDate RANGE_END = TODAY.plusDays(180);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CalendarRepository calendarRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    private User user;
    private CalendarEntity calendar;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .loginId("ai-schedule-context-user")
                .nickname("ai-schedule-context-user")
                .loginType(LoginType.LOCAL)
                .build());
        calendar = calendarRepository.save(CalendarEntity.builder()
                .user(user)
                .calendarTitle("AI 일정 조회 테스트")
                .startDate(LocalDateTime.of(2026, 1, 1, 0, 0))
                .endDate(LocalDateTime.of(2027, 12, 31, 23, 59))
                .build());
    }

    @Test
    void findsOngoingAndUpcomingSchedulesWithinConfiguredRange() {
        saveSchedule("지난 일정", TODAY.minusDays(10), TODAY.minusDays(1));
        saveSchedule("진행 중 일정", TODAY.minusDays(2), TODAY.plusDays(1));
        saveSchedule("가까운 일정", TODAY.plusDays(10), TODAY.plusDays(11));
        saveSchedule("조회 종료일 일정", RANGE_END, RANGE_END);
        saveSchedule("범위 밖 일정", RANGE_END.plusDays(1), RANGE_END.plusDays(2));

        List<ScheduleEntity> result = scheduleRepository.findForAIContext(
                user.getId(),
                TODAY,
                RANGE_END,
                PageRequest.of(0, 10)
        );

        assertThat(result)
                .extracting(ScheduleEntity::getScheduleTitle)
                .containsExactly("진행 중 일정", "가까운 일정", "조회 종료일 일정");
    }

    @Test
    void limitsSchedulesAfterSortingByStartDate() {
        saveSchedule("네 번째 일정", TODAY.plusDays(4), TODAY.plusDays(4));
        saveSchedule("두 번째 일정", TODAY.plusDays(2), TODAY.plusDays(2));
        saveSchedule("첫 번째 일정", TODAY.plusDays(1), TODAY.plusDays(1));
        saveSchedule("세 번째 일정", TODAY.plusDays(3), TODAY.plusDays(3));

        List<ScheduleEntity> result = scheduleRepository.findForAIContext(
                user.getId(),
                TODAY,
                RANGE_END,
                PageRequest.of(0, 3)
        );

        assertThat(result)
                .extracting(ScheduleEntity::getScheduleTitle)
                .containsExactly("첫 번째 일정", "두 번째 일정", "세 번째 일정");
    }

    private void saveSchedule(String title, LocalDate startDate, LocalDate endDate) {
        ScheduleRequest request = ScheduleRequest.builder()
                .scheduleTitle(title)
                .startDate(startDate)
                .endDate(endDate)
                .blockColor("#3b82f6")
                .build();
        scheduleRepository.save(ScheduleEntity.of(calendar, request));
    }
}
