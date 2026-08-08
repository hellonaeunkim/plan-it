package com.frend.planit.domain.chatbot.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 커밋된 평가 결과 JSON을 읽어 다음 단계의 비교 기준값을 제공한다.
 * <p>
 * 이전 측정값을 테스트 상수로 옮겨 적으면 오타가 나도 컴파일과 테스트가 통과해 잘못된 비교 결과가 조용히 기록된다. 따라서 측정값은 옮겨 적지 않고 결과 파일에서 직접
 * 읽는다. 파일 이름이 틀리면 즉시 실패하므로 오류가 드러난다.
 */
final class AIChatEvaluationArtifacts {

    static final Path TRACKED_ARTIFACT_DIRECTORY = Path.of(
            "..", "docs", "ai-chatbot", "evaluation-results"
    );

    private static final String OVERALL_GROUP = "overall";

    private AIChatEvaluationArtifacts() {
    }

    static OverallBaseline loadOverallBaseline(ObjectMapper objectMapper, String artifactFileName)
            throws IOException {
        JsonNode artifact = readArtifact(objectMapper, artifactFileName);
        JsonNode metadata = artifact.path("metadata");
        JsonNode summary = findOverallSummary(artifact, artifactFileName);

        return new OverallBaseline(
                artifactFileName,
                metadata.path("gitCommit").asText(),
                metadata.path("promptVersion").asText(),
                summary.path("promptTokens").asInt(),
                summary.path("completionTokens").asInt(),
                summary.path("totalTokens").asInt()
        );
    }
    
    static CaseBaseline loadCaseBaseline(
            ObjectMapper objectMapper,
            String artifactFileName,
            String caseId
    ) throws IOException {
        JsonNode artifact = readArtifact(objectMapper, artifactFileName);
        JsonNode metadata = artifact.path("metadata");
        JsonNode result = findCaseResult(artifact, artifactFileName, caseId);

        return new CaseBaseline(
                artifactFileName,
                metadata.path("gitCommit").asText(),
                result.path("promptVersion").asText(),
                caseId,
                result.path("promptTokens").asInt()
        );
    }

    private static JsonNode readArtifact(ObjectMapper objectMapper, String artifactFileName)
            throws IOException {
        Path artifactPath = TRACKED_ARTIFACT_DIRECTORY.resolve(artifactFileName);
        if (!Files.isRegularFile(artifactPath)) {
            throw new IllegalStateException(
                    "비교 기준 평가 결과 파일이 없습니다: " + artifactPath.toAbsolutePath().normalize()
            );
        }
        return objectMapper.readTree(artifactPath.toFile());
    }

    private static JsonNode findOverallSummary(JsonNode artifact, String artifactFileName) {
        for (JsonNode summary : artifact.path("summaries")) {
            if (OVERALL_GROUP.equals(summary.path("group").asText())) {
                return summary;
            }
        }
        throw new IllegalStateException(
                "비교 기준 파일에 overall 집계가 없습니다: " + artifactFileName
        );
    }

    private static JsonNode findCaseResult(
            JsonNode artifact,
            String artifactFileName,
            String caseId
    ) {
        for (JsonNode result : artifact.path("results")) {
            if (caseId.equals(result.path("caseId").asText())) {
                return result;
            }
        }
        throw new IllegalStateException(
                "비교 기준 파일에 질문 결과가 없습니다: " + artifactFileName + " caseId=" + caseId
        );
    }

    record OverallBaseline(
            String artifact,
            String gitCommit,
            String promptVersion,
            int promptTokens,
            int completionTokens,
            int totalTokens
    ) {

        Map<String, Object> toMetadata() {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("artifact", artifact);
            metadata.put("gitCommit", gitCommit);
            metadata.put("promptVersion", promptVersion);
            metadata.put("promptTokens", promptTokens);
            metadata.put("completionTokens", completionTokens);
            metadata.put("totalTokens", totalTokens);
            return metadata;
        }
    }

    record CaseBaseline(
            String artifact,
            String gitCommit,
            String promptVersion,
            String caseId,
            int promptTokens
    ) {

        Map<String, Object> toMetadata() {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("artifact", artifact);
            metadata.put("gitCommit", gitCommit);
            metadata.put("promptVersion", promptVersion);
            metadata.put("caseId", caseId);
            metadata.put("promptTokens", promptTokens);
            return metadata;
        }
    }
}
