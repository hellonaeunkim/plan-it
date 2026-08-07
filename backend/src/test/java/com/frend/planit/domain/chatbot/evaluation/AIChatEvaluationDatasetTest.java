package com.frend.planit.domain.chatbot.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AIChatEvaluationDatasetTest {

    private static final String DATASET_PATH = "ai-chatbot/evaluation-dataset-v1.json";
    private static JsonNode dataset;

    @BeforeAll
    static void loadDataset() throws IOException {
        try (InputStream input = AIChatEvaluationDatasetTest.class
                .getClassLoader()
                .getResourceAsStream(DATASET_PATH)) {
            assertThat(input).as("평가 데이터 파일이 존재해야 한다").isNotNull();
            dataset = new ObjectMapper().readTree(input);
        }
    }

    @Test
    void containsFixedScheduleAndSixIndependentQuestions() {
        assertThat(dataset.path("datasetVersion").asText()).isEqualTo("ai-chat-eval-v1");
        assertThat(dataset.path("scheduleFixture")).hasSize(3);
        assertThat(dataset.path("independentQuestions")).hasSize(6);

        Set<String> questionIds = new HashSet<>();
        dataset.path("independentQuestions").forEach(question -> {
            assertThat(question.path("id").asText()).isNotBlank();
            assertThat(question.path("category").asText()).isNotBlank();
            assertThat(question.path("language").asText()).isIn("ko", "en");
            assertThat(question.path("question").asText()).isNotBlank();
            assertThat(question.path("requiredFacts")).isNotEmpty();
            assertThat(question.path("forbiddenClaims")).isNotEmpty();
            questionIds.add(question.path("id").asText());
        });

        assertThat(questionIds).hasSize(6);
    }

    @Test
    void definesLongTermMemoryScenarioWithoutPredeterminedSolution() {
        JsonNode scenario = dataset.path("longTermMemoryScenario");

        assertThat(scenario.path("preferenceTurn").path("userMessage").asText())
                .contains("땅콩 알레르기");
        assertThat(scenario.path("fillerTurns").size()).isGreaterThan(3);
        assertThat(scenario.path("finalQuestion").asText()).contains("알레르기");
        assertThat(scenario.path("requiredBehavior").asText()).contains("땅콩");
        assertThat(scenario.path("purpose").asText()).contains("사용자 제약");
        assertThat(scenario.has("hypothesesToVerify")).isFalse();
        assertThat(scenario.has("comparisonExpectation")).isFalse();
    }

    @Test
    void definesFiveQualityCriteria() {
        JsonNode rubric = dataset.path("rubric");

        assertThat(rubric).hasSize(5);
        rubric.forEach(criterion -> {
            assertThat(criterion.path("id").asText()).isNotBlank();
            assertThat(criterion.path("name").asText()).isNotBlank();
            assertThat(criterion.path("score").asText()).isEqualTo("0 또는 1");
        });
    }
}
