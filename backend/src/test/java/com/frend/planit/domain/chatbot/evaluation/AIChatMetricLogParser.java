package com.frend.planit.domain.chatbot.evaluation;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AIChatMetricLogParser {

    private static final String SUCCESS_EVENT = "event=ai_chat_response_metric ";
    private static final String FAILURE_EVENT = "event=ai_chat_response_metric_failed";

    private AIChatMetricLogParser() {
    }

    static AIChatMetric parseSingleSuccessfulMetric(String logSegment) {
        if (logSegment.contains(FAILURE_EVENT)) {
            throw new IllegalStateException("AI 응답 메트릭 기록이 실패했습니다.");
        }

        List<String> metricLines = logSegment.lines()
                .filter(line -> line.contains(SUCCESS_EVENT))
                .toList();

        if (metricLines.size() != 1) {
            throw new IllegalStateException(
                    "AI 응답 메트릭은 호출당 정확히 1개여야 합니다. actual=" + metricLines.size()
            );
        }

        String metricLine = metricLines.getFirst();
        boolean usageAvailable = booleanValue(metricLine, "usageAvailable");
        if (!usageAvailable) {
            throw new IllegalStateException("실제 토큰 사용량 메타데이터가 없습니다.");
        }

        return new AIChatMetric(
                textValue(metricLine, "promptVersion"),
                textValue(metricLine, "model"),
                intValue(metricLine, "promptTokens"),
                intValue(metricLine, "completionTokens"),
                intValue(metricLine, "totalTokens"),
                longValue(metricLine, "llmDurationMs"),
                longValue(metricLine, "serviceDurationMs")
        );
    }

    private static String textValue(String line, String key) {
        return match(line, key + "=([^\\s]+)", key).group(1);
    }

    private static boolean booleanValue(String line, String key) {
        return Boolean.parseBoolean(match(line, key + "=(true|false)", key).group(1));
    }

    private static int intValue(String line, String key) {
        return Integer.parseInt(match(line, key + "=(\\d+)", key).group(1));
    }

    private static long longValue(String line, String key) {
        return Long.parseLong(match(line, key + "=(\\d+)", key).group(1));
    }

    private static Matcher match(String line, String expression, String key) {
        Matcher matcher = Pattern.compile(expression).matcher(line);
        if (!matcher.find()) {
            throw new IllegalStateException("AI 응답 메트릭에 필수 값이 없습니다. key=" + key);
        }
        return matcher;
    }

    record AIChatMetric(
            String promptVersion,
            String model,
            int promptTokens,
            int completionTokens,
            int totalTokens,
            long llmDurationMs,
            long serviceDurationMs
    ) {
    }
}
