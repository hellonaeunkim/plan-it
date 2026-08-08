package com.frend.planit.domain.calendar.schedule.repository;

import com.frend.planit.domain.calendar.schedule.entity.ScheduleEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleRepository extends JpaRepository<ScheduleEntity, Long> {

    // 전체 스케줄 조회
    @Query("SELECT s FROM ScheduleEntity s WHERE s.calendar.id = :calendarId")
    List<ScheduleEntity> findAllByCalendarId(@Param("calendarId") Long calendarId);

    // 전체 스케줄 조회
    @Query("SELECT s FROM ScheduleEntity s WHERE s.id = :scheduleId AND s.calendar.id = :calendarId")
    Optional<ScheduleEntity> findByIdAndCalendarId(
            @Param("calendarId") Long calendarId,
            @Param("scheduleId") Long scheduleId);

    // AI 컨텍스트에 제공할 진행 중이거나 가까운 일정
    @Query("""
            SELECT s
            FROM ScheduleEntity s
            WHERE s.calendar.user.id = :userId
              AND s.endDate >= :fromDate
              AND s.startDate <= :toDate
            ORDER BY s.startDate ASC, s.id ASC
            """)
    List<ScheduleEntity> findForAIContext(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);
}
