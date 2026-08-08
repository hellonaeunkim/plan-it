package com.frend.planit.domain.chatbot.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frend.planit.domain.accommodation.service.AccommodationService;
import com.frend.planit.domain.calendar.entity.CalendarEntity;
import com.frend.planit.domain.calendar.repository.CalendarRepository;
import com.frend.planit.domain.calendar.schedule.day.entity.ScheduleDayEntity;
import com.frend.planit.domain.calendar.schedule.dto.request.ScheduleRequest;
import com.frend.planit.domain.calendar.schedule.entity.ScheduleEntity;
import com.frend.planit.domain.calendar.schedule.repository.ScheduleRepository;
import com.frend.planit.domain.calendar.schedule.travel.dto.request.TravelRequest;
import com.frend.planit.domain.calendar.schedule.travel.entity.TravelEntity;
import com.frend.planit.domain.calendar.schedule.travel.repository.TravelRepository;
import com.frend.planit.domain.chatbot.chatMessage.dto.request.AIChatMessageRequest;
import com.frend.planit.domain.chatbot.chatMessage.dto.response.AIChatMessageResponse;
import com.frend.planit.domain.chatbot.chatMessage.service.AIChatMessageService;
import com.frend.planit.domain.chatbot.chatRoom.entity.AIChatRoomEntity;
import com.frend.planit.domain.chatbot.chatRoom.repository.AIChatRoomRepository;
import com.frend.planit.domain.chatbot.evaluation.AIChatMetricLogParser.AIChatMetric;
import com.frend.planit.domain.user.entity.User;
import com.frend.planit.domain.user.enums.LoginType;
import com.frend.planit.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.StringUtils;

/**
 * 실제 Groq와 현재 AIChatMessageService 구현을 사용해 개선 전 기준값을 기록한다.
 *
 * 이 테스트는 외부 API를 12회 호출하므로 기본 test 작업에서는 실행하지 않는다.
 * 사용자가 터미널 출력을 캡처할 때만 groqBaselineEvaluation 작업으로 명시 실행한다.
 */
@Tag("external-ai")
@Tag("ai-baseline")
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(properties = {
        "spring.mail.host=localhost",
        "spring.mail.port=2525",
        "spring.mail.username=test",
        "spring.mail.password=test"
})
@ActiveProfiles({"test", "external-ai"})
class AIChatBaselineEvaluationTest {

    private static final String STAGE = "baseline";
    private static final String DATASET_PATH = "ai-chatbot/evaluation-dataset-v1.json";
    private static final Path ARTIFACT_PATH = Path.of(
            "build", "ai-evaluation", "baseline-results.json"
    );
    private static final String EXPECTED_BASE_URL = "https://api.groq.com/openai";
    private static final String EXPECTED_MODEL = "openai/gpt-oss-120b";
    private static final String PLACEHOLDER_API_KEY = "NEED_TO_INPUT_ON_SECRET";
    private static final String DUMMY_API_KEY = "dummy";
    private static final long REQUEST_INTERVAL_MS = 10_000L;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:}")
    private String model;

    @Autowired
    private OpenAiChatModel chatClient;

    @Autowired
    private AIChatMessageService aiChatMessageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CalendarRepository calendarRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private TravelRepository travelRepository;

    @Autowired
    private AIChatRoomRepository aiChatRoomRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccommodationService accommodationService;

    @Test
    void recordsBaselineTokenUsageAndResponses(CapturedOutput output) throws Exception {
        assertRealGroqConfigured();
        GitSnapshot gitSnapshot = requireCleanGitSnapshot();
        JsonNode dataset = loadDataset();
        User user = createFixture(dataset.path("scheduleFixture"));
        List<EvaluationResult> results = new ArrayList<>();

        try {
            runIndependentQuestions(dataset, user, output, results);
            runLongTermScenario(dataset, user, output, results);

            assertThat(results).hasSize(12);
            assertThat(results).allMatch(EvaluationResult::successful);
            printSummaries(results);
        } finally {
            writeArtifact(dataset, results, gitSnapshot);
        }
    }

    private void runIndependentQuestions(
            JsonNode dataset,
            User user,
            CapturedOutput output,
            List<EvaluationResult> results
    ) throws InterruptedException {
        for (JsonNode question : dataset.path("independentQuestions")) {
            AIChatRoomEntity chatRoom = aiChatRoomRepository.save(AIChatRoomEntity.of(user));
            executeCase(
                    "independent",
                    question.path("id").asText(),
                    question.path("question").asText(),
                    user,
                    chatRoom,
                    output,
                    results
            );
        }
    }

    private void runLongTermScenario(
            JsonNode dataset,
            User user,
            CapturedOutput output,
            List<EvaluationResult> results
    ) throws InterruptedException {
        JsonNode scenario = dataset.path("longTermMemoryScenario");
        AIChatRoomEntity chatRoom = aiChatRoomRepository.save(AIChatRoomEntity.of(user));

        executeCase(
                "long-term",
                "long-term-1-preference",
                scenario.path("preferenceTurn").path("userMessage").asText(),
                user,
                chatRoom,
                output,
                results
        );

        int sequence = 2;
        for (JsonNode fillerTurn : scenario.path("fillerTurns")) {
            executeCase(
                    "long-term",
                    "long-term-" + sequence + "-filler",
                    fillerTurn.asText(),
                    user,
                    chatRoom,
                    output,
                    results
            );
            sequence++;
        }

        executeCase(
                "long-term",
                "long-term-" + sequence + "-recall",
                scenario.path("finalQuestion").asText(),
                user,
                chatRoom,
                output,
                results
        );
    }

    private void executeCase(
            String group,
            String caseId,
            String userMessage,
            User user,
            AIChatRoomEntity chatRoom,
            CapturedOutput output,
            List<EvaluationResult> results
    ) throws InterruptedException {
        paceRequests(results);
        int logStart = output.getAll().length();

        try {
            AIChatMessageResponse response = aiChatMessageService.createMessages(
                    user.getId(),
                    chatRoom.getId(),
                    new AIChatMessageRequest(userMessage)
            );
            String newLogs = output.getAll().substring(logStart);
            AIChatMetric metric = AIChatMetricLogParser.parseSingleSuccessfulMetric(newLogs);

            assertThat(metric.model()).isEqualTo(EXPECTED_MODEL);
            assertThat(metric.promptTokens()).isPositive();
            assertThat(metric.totalTokens()).isPositive();

            EvaluationResult result = EvaluationResult.success(
                    group,
                    caseId,
                    userMessage,
                    response.getBotMessage(),
                    metric
            );
            results.add(result);
            printResult(result);
        } catch (RuntimeException | AssertionError error) {
            results.add(EvaluationResult.failure(
                    group,
                    caseId,
                    userMessage,
                    error.getClass().getSimpleName()
            ));
            System.out.printf(
                    "AI_EVAL_FAILURE stage=%s group=%s case=%s errorType=%s%n",
                    STAGE,
                    group,
                    caseId,
                    error.getClass().getSimpleName()
            );
            throw error;
        }
    }

    private void paceRequests(List<EvaluationResult> results) throws InterruptedException {
        if (!results.isEmpty()) {
            Thread.sleep(REQUEST_INTERVAL_MS);
        }
    }

    private User createFixture(JsonNode scheduleFixture) {
        User user = userRepository.save(User.builder()
                .loginId("ai-baseline-evaluation-user")
                .nickname("ai-baseline-evaluation-user")
                .loginType(LoginType.LOCAL)
                .build());

        LocalDate earliestDate = LocalDate.MAX;
        LocalDate latestDate = LocalDate.MIN;
        for (JsonNode scheduleNode : scheduleFixture) {
            LocalDate startDate = LocalDate.parse(scheduleNode.path("startDate").asText());
            LocalDate endDate = LocalDate.parse(scheduleNode.path("endDate").asText());
            earliestDate = earliestDate.isBefore(startDate) ? earliestDate : startDate;
            latestDate = latestDate.isAfter(endDate) ? latestDate : endDate;
        }

        CalendarEntity calendar = calendarRepository.save(CalendarEntity.builder()
                .user(user)
                .calendarTitle("AI 기준선 평가")
                .startDate(earliestDate.atStartOfDay())
                .endDate(latestDate.atTime(23, 59))
                .build());

        int travelIndex = 1;
        for (JsonNode scheduleNode : scheduleFixture) {
            ScheduleEntity schedule = createSchedule(calendar, scheduleNode);
            for (JsonNode dayNode : scheduleNode.path("days")) {
                LocalDate date = LocalDate.parse(dayNode.path("date").asText());
                ScheduleDayEntity day = schedule.getScheduleDayList().stream()
                        .filter(candidate -> candidate.getDate().equals(date))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("일정 날짜 fixture가 없습니다: " + date));

                for (JsonNode travelNode : dayNode.path("travels")) {
                    TravelRequest request = TravelRequest.builder()
                            .scheduleDayId(day.getId())
                            .kakaomapId("ai-eval-place-" + travelIndex++)
                            .location(travelNode.path("location").asText())
                            .category(travelNode.path("category").asText())
                            .lat(37.0)
                            .lng(127.0)
                            .hour(String.format(
                                    Locale.ROOT,
                                    "%02d",
                                    travelNode.path("visitHour").asInt()
                            ))
                            .minute(String.format(
                                    Locale.ROOT,
                                    "%02d",
                                    travelNode.path("visitMinute").asInt()
                            ))
                            .build();
                    travelRepository.save(TravelEntity.of(request, day));
                }
            }
        }

        entityManager.clear();
        return user;
    }

    private ScheduleEntity createSchedule(CalendarEntity calendar, JsonNode scheduleNode) {
        ScheduleRequest request = ScheduleRequest.builder()
                .scheduleTitle(scheduleNode.path("scheduleTitle").asText())
                .startDate(LocalDate.parse(scheduleNode.path("startDate").asText()))
                .endDate(LocalDate.parse(scheduleNode.path("endDate").asText()))
                .blockColor("#3b82f6")
                .build();
        return scheduleRepository.save(ScheduleEntity.of(calendar, request));
    }

    private JsonNode loadDataset() throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(DATASET_PATH)) {
            assertThat(input).as("평가 데이터 파일이 존재해야 한다").isNotNull();
            return objectMapper.readTree(input);
        }
    }

    private void assertRealGroqConfigured() {
        boolean configured = StringUtils.hasText(apiKey)
                && !PLACEHOLDER_API_KEY.equals(apiKey)
                && !DUMMY_API_KEY.equals(apiKey);

        assertThat(configured)
                .as("실제 Groq API 키(spring.ai.openai.api-key)가 설정되어야 한다")
                .isTrue();
        assertThat(baseUrl).isEqualTo(EXPECTED_BASE_URL);
        assertThat(model).isEqualTo(EXPECTED_MODEL);
        assertThat(Mockito.mockingDetails(chatClient).isMock()).isFalse();
    }

    private void printResult(EvaluationResult result) {
        System.out.printf(
                Locale.ROOT,
                "AI_EVAL_RESULT stage=%s group=%s case=%s promptTokens=%d "
                        + "completionTokens=%d totalTokens=%d llmDurationMs=%d serviceDurationMs=%d%n",
                STAGE,
                result.group(),
                result.caseId(),
                result.promptTokens(),
                result.completionTokens(),
                result.totalTokens(),
                result.llmDurationMs(),
                result.serviceDurationMs()
        );
    }

    private void printSummaries(List<EvaluationResult> results) {
        EvaluationSummary independent = summarize("independent", results);
        EvaluationSummary longTerm = summarize("long-term", results);
        EvaluationSummary overall = summarize("overall", results);

        System.out.printf(
                "AI_EVAL_SUMMARY stage=%s group=independent requests=%d "
                        + "promptTokens=%d completionTokens=%d totalTokens=%d%n",
                STAGE,
                independent.requests(),
                independent.promptTokens(),
                independent.completionTokens(),
                independent.totalTokens()
        );
        System.out.printf(
                Locale.ROOT,
                "AI_EVAL_SUMMARY stage=%s group=long-term requests=%d "
                        + "promptTokens=%d completionTokens=%d totalTokens=%d "
                        + "firstPromptTokens=%d lastPromptTokens=%d growthPercent=%.2f%n",
                STAGE,
                longTerm.requests(),
                longTerm.promptTokens(),
                longTerm.completionTokens(),
                longTerm.totalTokens(),
                longTerm.firstPromptTokens(),
                longTerm.lastPromptTokens(),
                longTerm.growthPercent()
        );
        System.out.printf(
                "AI_EVAL_SUMMARY stage=%s group=overall requests=%d "
                        + "promptTokens=%d completionTokens=%d totalTokens=%d%n",
                STAGE,
                overall.requests(),
                overall.promptTokens(),
                overall.completionTokens(),
                overall.totalTokens()
        );
    }

    private EvaluationSummary summarize(String group, List<EvaluationResult> results) {
        List<EvaluationResult> selected = "overall".equals(group)
                ? results.stream().filter(EvaluationResult::successful).toList()
                : results.stream()
                        .filter(EvaluationResult::successful)
                        .filter(result -> result.group().equals(group))
                        .toList();

        int firstPromptTokens = selected.isEmpty() ? 0 : selected.getFirst().promptTokens();
        int lastPromptTokens = selected.isEmpty() ? 0 : selected.getLast().promptTokens();
        double growthPercent = firstPromptTokens == 0
                ? 0.0
                : (lastPromptTokens - firstPromptTokens) * 100.0 / firstPromptTokens;

        return new EvaluationSummary(
                group,
                selected.size(),
                selected.stream().mapToInt(EvaluationResult::promptTokens).sum(),
                selected.stream().mapToInt(EvaluationResult::completionTokens).sum(),
                selected.stream().mapToInt(EvaluationResult::totalTokens).sum(),
                firstPromptTokens,
                lastPromptTokens,
                growthPercent
        );
    }

    private void writeArtifact(
            JsonNode dataset,
            List<EvaluationResult> results,
            GitSnapshot gitSnapshot
    ) throws Exception {
        Files.createDirectories(ARTIFACT_PATH.getParent());

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("stage", STAGE);
        metadata.put("datasetVersion", dataset.path("datasetVersion").asText());
        metadata.put("datasetEvaluationDate", dataset.path("evaluationDate").asText());
        metadata.put("executedAt", OffsetDateTime.now().toString());
        metadata.put("gitCommit", gitSnapshot.commit());
        metadata.put("workingTreeClean", gitSnapshot.workingTreeClean());
        metadata.put("model", model);
        metadata.put("baseUrl", baseUrl);
        metadata.put("modelOptions", Map.of(
                "model", model,
                "temperature", "provider-default",
                "maxTokens", "provider-default"
        ));
        metadata.put("promptVersion", results.stream()
                .filter(EvaluationResult::successful)
                .map(EvaluationResult::promptVersion)
                .findFirst()
                .orElse("unknown"));
        metadata.put("requestIntervalMs", REQUEST_INTERVAL_MS);

        Map<String, Object> artifact = new LinkedHashMap<>();
        artifact.put("metadata", metadata);
        artifact.put("results", results);
        artifact.put("summaries", List.of(
                summarize("independent", results),
                summarize("long-term", results),
                summarize("overall", results)
        ));

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(ARTIFACT_PATH.toFile(), artifact);
        System.out.println("AI_EVAL_ARTIFACT path=" + ARTIFACT_PATH);
    }

    private GitSnapshot requireCleanGitSnapshot() throws IOException, InterruptedException {
        String commit = runGitCommand("rev-parse", "--short", "HEAD");
        String workingTreeStatus = runGitCommand(
                "status",
                "--porcelain",
                "--untracked-files=all"
        );

        if (!workingTreeStatus.isBlank()) {
            String firstChange = workingTreeStatus.lines().findFirst().orElse("unknown");
            System.out.println(
                    "AI_EVAL_ABORTED reason=dirty-working-tree firstChange=" + firstChange
            );
            throw new IllegalStateException(
                    "기준선 측정 전에 변경사항을 커밋하거나 임시 보관해야 합니다. "
                            + "작업 트리가 깨끗하지 않습니다: "
                            + firstChange
            );
        }

        return new GitSnapshot(commit, true);
    }

    private String runGitCommand(String... arguments) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(arguments));

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes()).trim();
        int exitCode = process.waitFor();
        if (exitCode != 0 || output.isBlank()) {
            if (exitCode == 0 && "status".equals(arguments[0])) {
                return output;
            }
            throw new IllegalStateException("측정 Git 상태를 확인할 수 없습니다: " + output);
        }
        return output;
    }

    record GitSnapshot(String commit, boolean workingTreeClean) {
    }

    record EvaluationResult(
            String group,
            String caseId,
            String userMessage,
            String botMessage,
            String promptVersion,
            String model,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            Long llmDurationMs,
            Long serviceDurationMs,
            String errorType
    ) {
        static EvaluationResult success(
                String group,
                String caseId,
                String userMessage,
                String botMessage,
                AIChatMetric metric
        ) {
            return new EvaluationResult(
                    group,
                    caseId,
                    userMessage,
                    botMessage,
                    metric.promptVersion(),
                    metric.model(),
                    metric.promptTokens(),
                    metric.completionTokens(),
                    metric.totalTokens(),
                    metric.llmDurationMs(),
                    metric.serviceDurationMs(),
                    null
            );
        }

        static EvaluationResult failure(
                String group,
                String caseId,
                String userMessage,
                String errorType
        ) {
            return new EvaluationResult(
                    group,
                    caseId,
                    userMessage,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    errorType
            );
        }

        boolean successful() {
            return errorType == null;
        }
    }

    record EvaluationSummary(
            String group,
            int requests,
            int promptTokens,
            int completionTokens,
            int totalTokens,
            int firstPromptTokens,
            int lastPromptTokens,
            double growthPercent
    ) {
    }
}
