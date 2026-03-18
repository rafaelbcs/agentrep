package xyz.agentrep.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xyz.agentrep.service.LlmJudgeService;

/**
 * Minimal Spring context for LlmJudgeService integration tests.
 * Loads only: AnthropicChatModel + ChatClient + LlmJudgeService.
 * No Postgres, no Redis, no Security required.
 */
@Configuration
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
public class SpringAiTestConfig {

    @Value("${spring.ai.anthropic.api-key}")
    private String apiKey;

    @Bean
    public AnthropicApi anthropicApi() {
        return new AnthropicApi(apiKey);
    }

    @Bean
    public AnthropicChatModel anthropicChatModel(AnthropicApi anthropicApi) {
        var options = org.springframework.ai.anthropic.AnthropicChatOptions.builder()
            .model("claude-sonnet-4-6")
            .maxTokens(512)
            .build();
        return new AnthropicChatModel(anthropicApi, options);
    }

    @Bean
    public ChatClient.Builder chatClientBuilder(AnthropicChatModel chatModel) {
        return ChatClient.builder(chatModel);
    }

    @Bean
    public io.micrometer.core.instrument.MeterRegistry meterRegistry() {
        return new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
    }

    @Bean
    public xyz.agentrep.service.MetricsService metricsService(
            io.micrometer.core.instrument.MeterRegistry registry) {
        return new xyz.agentrep.service.MetricsService(registry);
    }

    @Bean
    public LlmJudgeService llmJudgeService(ChatClient.Builder builder, ObjectMapper objectMapper,
            xyz.agentrep.service.MetricsService metricsService) {
        return new LlmJudgeService(builder, objectMapper, metricsService);
    }
}
