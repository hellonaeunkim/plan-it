package com.frend.planit.domain.chatbot.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.frend.planit.domain.chatbot.evaluation.AIChatMetricLogParser.AIChatMetric;
import org.junit.jupiter.api.Test;

class AIChatMetricLogParserTest {

    private static final String VALID_METRIC = """
            2026-08-08 10:00:00 INFO AIChatMessageService >> \
            event=ai_chat_response_metric promptVersion=ai-chat-v1 \
            model=openai/gpt-oss-120b usageAvailable=true \
            promptTokens=120 completionTokens=30 totalTokens=150 \
            llmDurationMs=321 serviceDurationMs=456
            """;

    @Test
    void parsesSingleSuccessfulMetric() {
        AIChatMetric metric = AIChatMetricLogParser.parseSingleSuccessfulMetric(VALID_METRIC);

        assertThat(metric.promptVersion()).isEqualTo("ai-chat-v1");
        assertThat(metric.model()).isEqualTo("openai/gpt-oss-120b");
        assertThat(metric.promptTokens()).isEqualTo(120);
        assertThat(metric.completionTokens()).isEqualTo(30);
        assertThat(metric.totalTokens()).isEqualTo(150);
        assertThat(metric.llmDurationMs()).isEqualTo(321);
        assertThat(metric.serviceDurationMs()).isEqualTo(456);
    }

    @Test
    void rejectsMissingMetric() {
        assertThatThrownBy(() -> AIChatMetricLogParser.parseSingleSuccessfulMetric("unrelated log"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("actual=0");
    }

    @Test
    void rejectsDuplicateMetrics() {
        String duplicated = VALID_METRIC + System.lineSeparator() + VALID_METRIC;

        assertThatThrownBy(() -> AIChatMetricLogParser.parseSingleSuccessfulMetric(duplicated))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("actual=2");
    }

    @Test
    void rejectsMetricLoggingFailure() {
        String failed = "event=ai_chat_response_metric_failed promptVersion=ai-chat-v1";

        assertThatThrownBy(() -> AIChatMetricLogParser.parseSingleSuccessfulMetric(failed))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("기록이 실패");
    }

    @Test
    void rejectsUnavailableUsageMetadata() {
        String unavailable = VALID_METRIC.replace("usageAvailable=true", "usageAvailable=false");

        assertThatThrownBy(() -> AIChatMetricLogParser.parseSingleSuccessfulMetric(unavailable))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("토큰 사용량 메타데이터가 없습니다");
    }
}
