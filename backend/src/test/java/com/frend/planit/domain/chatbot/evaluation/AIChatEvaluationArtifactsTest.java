package com.frend.planit.domain.chatbot.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frend.planit.domain.chatbot.evaluation.AIChatEvaluationArtifacts.CaseBaseline;
import com.frend.planit.domain.chatbot.evaluation.AIChatEvaluationArtifacts.OverallBaseline;
import org.junit.jupiter.api.Test;

/**
 * 커밋된 평가 결과 파일에서 비교 기준값을 정확히 읽는지 검증한다.
 *
 * 실제 Groq를 호출하지 않으므로 기본 {@code ./gradlew test}에서 함께 실행된다.
 */
class AIChatEvaluationArtifactsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readsOverallTotalsFromResponsePolicyV4Artifact() throws Exception {
        OverallBaseline baseline = AIChatEvaluationArtifacts.loadOverallBaseline(
                objectMapper,
                "response-policy-refined-dc84c46.json"
        );

        assertThat(baseline.gitCommit()).isEqualTo("dc84c46");
        assertThat(baseline.promptVersion()).isEqualTo("ai-chat-v4");
        assertThat(baseline.promptTokens()).isEqualTo(10_998);
        assertThat(baseline.completionTokens()).isEqualTo(2_001);
        assertThat(baseline.totalTokens()).isEqualTo(12_999);
    }

    @Test
    void readsSingleCasePromptTokensFromBaselineArtifact() throws Exception {
        CaseBaseline baseline = AIChatEvaluationArtifacts.loadCaseBaseline(
                objectMapper,
                "baseline-3c0c931.json",
                "specific-date-schedule"
        );

        assertThat(baseline.gitCommit()).isEqualTo("3c0c931");
        assertThat(baseline.promptVersion()).isEqualTo("ai-chat-v1");
        assertThat(baseline.caseId()).isEqualTo("specific-date-schedule");
        assertThat(baseline.promptTokens()).isEqualTo(715);
    }

    @Test
    void failsWhenArtifactFileIsMissing() {
        assertThatThrownBy(() -> AIChatEvaluationArtifacts.loadOverallBaseline(
                objectMapper,
                "does-not-exist.json"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("비교 기준 평가 결과 파일이 없습니다");
    }

    @Test
    void failsWhenCaseIsNotInArtifact() {
        assertThatThrownBy(() -> AIChatEvaluationArtifacts.loadCaseBaseline(
                objectMapper,
                "baseline-3c0c931.json",
                "no-such-case"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("비교 기준 파일에 질문 결과가 없습니다");
    }
}
