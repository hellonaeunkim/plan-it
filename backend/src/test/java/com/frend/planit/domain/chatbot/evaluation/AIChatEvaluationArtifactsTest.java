package com.frend.planit.domain.chatbot.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    @Test
    void failsWhenRequiredOverallTokenIsMissing() {
        ObjectNode artifact = validOverallArtifact();
        ((ObjectNode) artifact.path("summaries").get(0)).remove("promptTokens");

        assertThatThrownBy(() -> AIChatEvaluationArtifacts.parseOverallBaseline(
                artifact,
                "missing-token.json"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("promptTokens 값은 양의 정수여야 합니다");
    }

    @Test
    void failsWhenPromptVersionIsBlank() {
        ObjectNode artifact = validOverallArtifact();
        ((ObjectNode) artifact.path("metadata")).put("promptVersion", "");

        assertThatThrownBy(() -> AIChatEvaluationArtifacts.parseOverallBaseline(
                artifact,
                "blank-version.json"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("promptVersion 문자열이 없습니다");
    }

    @Test
    void failsWhenTotalTokensDoesNotMatchTokenSum() {
        ObjectNode artifact = validOverallArtifact();
        ((ObjectNode) artifact.path("summaries").get(0)).put("totalTokens", 130);

        assertThatThrownBy(() -> AIChatEvaluationArtifacts.parseOverallBaseline(
                artifact,
                "invalid-total.json"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("totalTokens가 promptTokens와 completionTokens의 합과 다릅니다");
    }

    @Test
    void failsWhenMetadataAndCasePromptVersionsDiffer() {
        ObjectNode artifact = validCaseArtifact();
        ((ObjectNode) artifact.path("results").get(0)).put("promptVersion", "ai-chat-v2");

        assertThatThrownBy(() -> AIChatEvaluationArtifacts.parseCaseBaseline(
                artifact,
                "mismatched-version.json",
                "question-1"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("metadata와 질문 결과의 promptVersion이 다릅니다");
    }

    private ObjectNode validOverallArtifact() {
        ObjectNode artifact = objectMapper.createObjectNode();
        artifact.putObject("metadata")
                .put("gitCommit", "abc1234")
                .put("promptVersion", "ai-chat-test");
        artifact.putArray("summaries")
                .addObject()
                .put("group", "overall")
                .put("promptTokens", 100)
                .put("completionTokens", 20)
                .put("totalTokens", 120);
        return artifact;
    }

    private ObjectNode validCaseArtifact() {
        ObjectNode artifact = objectMapper.createObjectNode();
        artifact.putObject("metadata")
                .put("gitCommit", "abc1234")
                .put("promptVersion", "ai-chat-v1");
        artifact.putArray("results")
                .addObject()
                .put("caseId", "question-1")
                .put("promptVersion", "ai-chat-v1")
                .put("promptTokens", 100);
        return artifact;
    }
}
