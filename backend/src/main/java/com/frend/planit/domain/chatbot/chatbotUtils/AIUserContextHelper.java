package com.frend.planit.domain.chatbot.chatbotUtils;

import com.frend.planit.domain.calendar.schedule.day.entity.ScheduleDayEntity;
import com.frend.planit.domain.calendar.schedule.entity.ScheduleEntity;
import com.frend.planit.domain.calendar.schedule.travel.entity.TravelEntity;
import java.time.LocalDateTime;
import java.util.List;

public class AIUserContextHelper {

    public static String buildUserTravelContext(List<ScheduleEntity> schedules) {

        StringBuilder sb = new StringBuilder();

        sb.append("""
                당신은 여행 계획을 돕는 여행 어시스턴트입니다.
                
                사용자가 입력한 언어를 자동으로 감지하여, "그 언어로만" 응답해야 합니다.
                예를 들어, 사용자가 한국어로 질문하면 반드시 한국어로만 답하고,
                영어로 질문하면 영어로만, 일본어로 질문하면 일본어로만 답해야 합니다.
                절대 다른 언어를 혼용하거나 언어를 전환하지 마십시오.
                
                모든 응답은 자연스럽고 친절하며, 간결하게 작성해야 합니다.
                
                당신의 주요 임무는 사용자의 여행 일정을 기반으로 친절하고 유용한 정보를 제공해야 합니다.
                이 채팅은 “plan-it”이라는 여행 계획 서비스의 일부이며,
                사용자가 더 나은 여행을 경험할 수 있도록 돕는 것이 목표입니다.
                """.stripIndent().trim()
        );

        if (schedules == null || schedules.isEmpty()) {
            sb.append("""
                    사용자가 여행 일정을 등록하지 않았습니다.
                    """.stripIndent().trim()
            );

            return sb.toString();
        }

        sb.append("현재 날짜 : ").append(LocalDateTime.now()).append("\n");

        sb.append("다음은 사용자의 여행 일정입니다:\n\n");

        for (ScheduleEntity schedule : schedules) {
            sb.append("📅 여행 제목: ").append(schedule.getScheduleTitle()).append("\n");
            sb.append("   기간: ").append(schedule.getStartDate())
                    .append(" ~ ").append(schedule.getEndDate()).append("\n");

            for (ScheduleDayEntity day : schedule.getScheduleDayList()) {
                if (day.getTravelList().isEmpty()) {
                    continue;
                }

                sb.append("   ▸ 날짜: ").append(day.getDate()).append("\n");

                for (TravelEntity travel : day.getTravelList()) {
                    sb.append("     - 장소: ").append(travel.getLocation()).append("\n");
                    sb.append("       카테고리: ").append(travel.getCategory()).append("\n");
                    sb.append("       방문 시간: ")
                            .append(String.format("%02d시 %02d분", travel.getVisitHour(),
                                    travel.getVisitMinute()))
                            .append("\n");
                }

                sb.append("\n");
            }

            sb.append("\n");
        }

        sb.append("위 여행 일정을 참고하여 사용자의 질문에 응답해주세요.");

        return sb.toString();
    }
}
