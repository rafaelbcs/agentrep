package br.com.agentrep.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import br.com.agentrep.model.OutcomeVerdict;

import java.math.BigDecimal;

@Service
@Slf4j
public class LlmJudgeService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;

    public LlmJudgeService(ChatClient.Builder builder, ObjectMapper objectMapper,
                           MetricsService metricsService) {
        this.chatClient     = builder.build();
        this.objectMapper   = objectMapper;
        this.metricsService = metricsService;
    }

    public record JudgeResult(
        OutcomeVerdict verdict,
        BigDecimal confidence,
        String reasoning
    ) {}

    public JudgeResult evaluate(
        String taskDescription,
        String taskCategory,
        String deliverableUrl,
        String deliverableHash,
        String deliverableContent
    ) {
        String prompt = buildPrompt(taskDescription, taskCategory, deliverableUrl, deliverableHash, deliverableContent);

        Timer.Sample timer = metricsService.startLlmTimer();
        try {
            String content = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

            log.debug("LLM Judge raw response: {}", content);
            JudgeResult result = parseResponse(content);
            metricsService.recordLlmJudge(true);
            return result;
        } catch (Exception e) {
            log.error("LLM Judge call failed for task '{}', defaulting to FAILURE", taskDescription, e);
            metricsService.recordLlmJudge(false);
            return new JudgeResult(OutcomeVerdict.FAILURE, BigDecimal.ZERO, "LLM evaluation failed: " + e.getMessage());
        } finally {
            metricsService.stopLlmTimer(timer);
        }
    }

    private String buildPrompt(
        String taskDescription,
        String taskCategory,
        String deliverableUrl,
        String deliverableHash,
        String deliverableContent
    ) {
        String content = deliverableContent != null
            ? deliverableContent.substring(0, Math.min(deliverableContent.length(), 2000))
            : "not provided";

        return """
            You are an impartial judge evaluating whether an AI agent delivered on a task.

            TASK REQUESTED: %s
            TASK CATEGORY: %s
            DELIVERABLE URL: %s
            DELIVERABLE HASH (sha256): %s
            DELIVERABLE CONTENT PREVIEW: %s

            Evaluate strictly based on the evidence provided:
            1. Was a deliverable provided?
            2. Does it match what was requested?
            3. Is it complete and usable?

            Rules:
            - Verdict must be exactly SUCCESS or FAILURE (no partial).
            - If deliverable is missing, empty, or clearly unrelated: FAILURE.
            - If deliverable reasonably satisfies the request: SUCCESS.
            - Confidence: 0.0 (total uncertainty) to 1.0 (certain).

            Respond with valid JSON only — no markdown, no extra text:
            {"verdict": "SUCCESS", "confidence": 0.85, "reasoning": "brief explanation, max 2 sentences"}
            """.formatted(
                taskDescription,
                taskCategory,
                deliverableUrl != null ? deliverableUrl : "not provided",
                deliverableHash != null ? deliverableHash : "not provided",
                content
        );
    }

    private JudgeResult parseResponse(String raw) {
        try {
            String json = raw.strip()
                .replaceAll("(?s)^```json\\s*", "")
                .replaceAll("(?s)^```\\s*", "")
                .replaceAll("(?s)\\s*```$", "");

            JsonNode node = objectMapper.readTree(json);
            String verdictStr = node.get("verdict").asText();
            OutcomeVerdict verdict = "SUCCESS".equalsIgnoreCase(verdictStr)
                ? OutcomeVerdict.SUCCESS
                : OutcomeVerdict.FAILURE;
            BigDecimal confidence = BigDecimal.valueOf(node.get("confidence").asDouble());
            String reasoning = node.get("reasoning").asText();

            return new JudgeResult(verdict, confidence, reasoning);
        } catch (Exception e) {
            log.error("Failed to parse LLM Judge response: {}", raw, e);
            return new JudgeResult(OutcomeVerdict.FAILURE, BigDecimal.ZERO, "Failed to parse judge response");
        }
    }
}
