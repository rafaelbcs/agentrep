package xyz.agentrep.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import xyz.agentrep.model.OutcomeVerdict;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for LlmJudgeService — calls the real Claude API.
 * Requires ANTHROPIC_API_KEY set in the environment or .env.
 *
 * Run with: mvn test -Dtest=LlmJudgeServiceIT -pl backend
 */
@SpringBootTest(classes = {
    xyz.agentrep.config.SpringAiTestConfig.class
})
@ActiveProfiles("test")
class LlmJudgeServiceIT {

    @Autowired
    private LlmJudgeService judgeService;

    @Test
    void shouldReturnSuccessForClearDeliverable() {
        var result = judgeService.evaluate(
            "Write a Python function that reverses a string",
            "code-review",
            "https://gist.github.com/example/abc123",
            "sha256:abc123def456",
            """
            def reverse_string(s: str) -> str:
                \"\"\""Reverses the input string.\"\"\"
                return s[::-1]

            # Tests
            assert reverse_string("hello") == "olleh"
            assert reverse_string("") == ""
            print("All tests passed!")
            """
        );

        System.out.println("=== SUCCESS CASE ===");
        System.out.println("Verdict   : " + result.verdict());
        System.out.println("Confidence: " + result.confidence());
        System.out.println("Reasoning : " + result.reasoning());

        assertThat(result.verdict()).isEqualTo(OutcomeVerdict.SUCCESS);
        assertThat(result.confidence()).isBetween(
            java.math.BigDecimal.valueOf(0.5),
            java.math.BigDecimal.ONE
        );
        assertThat(result.reasoning()).isNotBlank();
    }

    @Test
    void shouldReturnFailureForEmptyDeliverable() {
        var result = judgeService.evaluate(
            "Analyze market trends for Q1 2025 and provide a 3-page report with charts",
            "data-analysis",
            null,
            null,
            null
        );

        System.out.println("=== FAILURE CASE ===");
        System.out.println("Verdict   : " + result.verdict());
        System.out.println("Confidence: " + result.confidence());
        System.out.println("Reasoning : " + result.reasoning());

        assertThat(result.verdict()).isEqualTo(OutcomeVerdict.FAILURE);
        assertThat(result.reasoning()).isNotBlank();
    }
}
