package xyz.agentrep.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import xyz.agentrep.model.OutcomeVerdict;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmJudgeResilienceTest {

    @Mock ChatClient.Builder builder;
    @Mock ChatClient chatClient;
    @Mock ChatClient.ChatClientRequestSpec requestSpec;
    @Mock ChatClient.CallResponseSpec callSpec;

    LlmJudgeService service;
    MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @BeforeEach
    void setUp() {
        when(builder.build()).thenReturn(chatClient);
        service = new LlmJudgeService(builder, new ObjectMapper(), new MetricsService(meterRegistry));
    }

    @Test
    void llmException_returnsFallbackFailure() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("Claude timeout"));

        LlmJudgeService.JudgeResult result = service.evaluate(
            "Write a test", "code-review", null, null, null);

        assertThat(result.verdict()).isEqualTo(OutcomeVerdict.FAILURE);
        assertThat(result.confidence()).isEqualByComparingTo("0");
        assertThat(result.reasoning()).contains("LLM evaluation failed");
    }

    @Test
    void malformedJson_returnsFallbackFailure() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("not valid json at all %%%");

        LlmJudgeService.JudgeResult result = service.evaluate(
            "Write a test", "code-review", null, null, null);

        assertThat(result.verdict()).isEqualTo(OutcomeVerdict.FAILURE);
        assertThat(result.confidence()).isEqualByComparingTo("0");
    }

    @Test
    void validSuccessResponse_parsed() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(
            """
            {"verdict":"SUCCESS","confidence":0.92,"reasoning":"Task was completed correctly."}
            """
        );

        LlmJudgeService.JudgeResult result = service.evaluate(
            "Write a test", "code-review", null, null, null);

        assertThat(result.verdict()).isEqualTo(OutcomeVerdict.SUCCESS);
        assertThat(result.confidence().doubleValue()).isEqualTo(0.92);
    }

    @Test
    void validFailureResponse_parsed() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(
            """
            {"verdict":"FAILURE","confidence":0.85,"reasoning":"Deliverable was empty."}
            """
        );

        LlmJudgeService.JudgeResult result = service.evaluate(
            "Write a test", "code-review", null, null, null);

        assertThat(result.verdict()).isEqualTo(OutcomeVerdict.FAILURE);
        assertThat(result.confidence().doubleValue()).isEqualTo(0.85);
    }
}
