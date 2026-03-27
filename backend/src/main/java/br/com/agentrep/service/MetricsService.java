package br.com.agentrep.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Métricas customizadas do AgentRep expostas via Prometheus.
 *
 * Contadores disponíveis em /actuator/prometheus:
 *   agentrep_outcomes_total{verdict}
 *   agentrep_disputes_total{event}
 *   agentrep_llm_judge_total{result}
 *   agentrep_onchain_sync_total{result}
 *
 * Timers:
 *   agentrep_llm_judge_duration_seconds
 */
@Service
@Slf4j
public class MetricsService {

    // Contadores
    private final Counter outcomesSuccess;
    private final Counter outcomesFailure;
    private final Counter disputesOpened;
    private final Counter disputesExpired;
    private final Counter disputesResolved;
    private final Counter llmJudgeSuccess;
    private final Counter llmJudgeFallback;
    private final Counter onchainSyncSuccess;
    private final Counter onchainSyncFailure;

    // Timer
    private final Timer llmJudgeTimer;

    public MetricsService(MeterRegistry registry) {
        outcomesSuccess   = Counter.builder("agentrep.outcomes")
            .tag("verdict", "SUCCESS").description("Outcomes judged SUCCESS").register(registry);
        outcomesFailure   = Counter.builder("agentrep.outcomes")
            .tag("verdict", "FAILURE").description("Outcomes judged FAILURE").register(registry);

        disputesOpened    = Counter.builder("agentrep.disputes")
            .tag("event", "opened").description("Disputes opened").register(registry);
        disputesExpired   = Counter.builder("agentrep.disputes")
            .tag("event", "expired").description("Disputes expired without resolution").register(registry);
        disputesResolved  = Counter.builder("agentrep.disputes")
            .tag("event", "resolved").description("Disputes resolved").register(registry);

        llmJudgeSuccess   = Counter.builder("agentrep.llm_judge")
            .tag("result", "success").description("LLM Judge calls succeeded").register(registry);
        llmJudgeFallback  = Counter.builder("agentrep.llm_judge")
            .tag("result", "fallback").description("LLM Judge calls fell back to FAILURE").register(registry);

        onchainSyncSuccess = Counter.builder("agentrep.onchain_sync")
            .tag("result", "success").description("On-chain sync succeeded").register(registry);
        onchainSyncFailure = Counter.builder("agentrep.onchain_sync")
            .tag("result", "failure").description("On-chain sync failed").register(registry);

        llmJudgeTimer = Timer.builder("agentrep.llm_judge.duration")
            .description("LLM Judge evaluation latency").register(registry);
    }

    public void recordOutcome(boolean success) {
        if (success) outcomesSuccess.increment();
        else         outcomesFailure.increment();
    }

    public void recordDisputeOpened()   { disputesOpened.increment(); }
    public void recordDisputeExpired()  {
        disputesExpired.increment();
        log.warn("ALERT: dispute expired without resolution — check DisputeService scheduler");
    }
    public void recordDisputeResolved() { disputesResolved.increment(); }

    public void recordLlmJudge(boolean success) {
        if (success) llmJudgeSuccess.increment();
        else         llmJudgeFallback.increment();
    }

    public void recordOnchainSync(boolean success) {
        if (success) onchainSyncSuccess.increment();
        else {
            onchainSyncFailure.increment();
            log.warn("ALERT: on-chain score sync failed — verify RPC and circuit breaker state");
        }
    }

    public Timer.Sample startLlmTimer() {
        return Timer.start();
    }

    public void stopLlmTimer(Timer.Sample sample) {
        sample.stop(llmJudgeTimer);
    }
}
