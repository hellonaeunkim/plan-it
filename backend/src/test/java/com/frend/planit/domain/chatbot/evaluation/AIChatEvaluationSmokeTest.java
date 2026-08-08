package com.frend.planit.domain.chatbot.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StringUtils;

/**
 * 실제 Groq API를 호출하는 스모크 테스트.
 *
 * 기본 {@code ./gradlew test}에서는 {@code external-ai} 태그가 제외되어 실행되지 않는다.
 * 실제 호출을 확인하려면 {@code ./gradlew groqEvaluationTest}로 실행한다.
 */
@Tag("external-ai")
@SpringBootTest(properties = {
        "spring.mail.host=localhost",
        "spring.mail.port=2525",
        "spring.mail.username=test",
        "spring.mail.password=test"
})
@ActiveProfiles({"test", "external-ai"})
class AIChatEvaluationSmokeTest {

    private static final String EXPECTED_BASE_URL = "https://api.groq.com/openai";
    private static final String EXPECTED_MODEL = "openai/gpt-oss-120b";
    private static final String PLACEHOLDER_API_KEY = "NEED_TO_INPUT_ON_SECRET";
    private static final String DUMMY_API_KEY = "dummy";

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:}")
    private String model;

    @Autowired
    private OpenAiChatModel chatClient;

    @Test
    void directGroqCallReturnsRealUsageMetadata() {
        assertRealGroqConfigured();

        ChatResponse response = chatClient.call(
                new Prompt(new UserMessage("연결 확인용 테스트 메시지입니다. 한 문장으로만 답해주세요."))
        );

        assertThat(response.getResult().getOutput().getText())
                .as("Groq 응답 텍스트가 비어있지 않아야 한다")
                .isNotBlank();

        assertThat(response.getMetadata().getModel())
                .as("응답 모델명이 기대한 모델과 일치해야 한다")
                .isEqualTo(EXPECTED_MODEL);

        Usage usage = response.getMetadata().getUsage();
        assertThat(usage.getPromptTokens())
                .as("promptTokens는 0보다 커야 한다")
                .isGreaterThan(0);
        assertThat(usage.getTotalTokens())
                .as("totalTokens는 0보다 커야 한다")
                .isGreaterThan(0);
    }

    private void assertRealGroqConfigured() {
        boolean configured = StringUtils.hasText(apiKey)
                && !PLACEHOLDER_API_KEY.equals(apiKey)
                && !DUMMY_API_KEY.equals(apiKey);

        assertThat(configured)
                .as("실제 Groq API 키(spring.ai.openai.api-key)가 설정되어야 한다")
                .isTrue();

        assertThat(baseUrl)
                .as("외부 AI 평가의 base URL은 Groq API여야 한다")
                .isEqualTo(EXPECTED_BASE_URL);

        assertThat(model)
                .as("외부 AI 평가 모델이 고정되어야 한다")
                .isEqualTo(EXPECTED_MODEL);

        assertThat(Mockito.mockingDetails(chatClient).isMock())
                .as("OpenAiChatModel은 Mock이 아닌 실제 빈이어야 한다")
                .isFalse();
    }
}
