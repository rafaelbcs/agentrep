package br.com.agentrep.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.agentrep.dto.OutcomeRequest;
import br.com.agentrep.dto.OutcomeResponse;
import br.com.agentrep.model.*;
import br.com.agentrep.repository.AgentRepository;
import br.com.agentrep.repository.OutcomeRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutcomeService {

    private final OutcomeRepository outcomeRepository;
    private final AgentRepository agentRepository;
    private final AgentService agentService;
    private final LlmJudgeService llmJudgeService;
    private final OnChainService onChainService;
    private final WebhookService webhookService;
    private final EvmSignatureService evmSignatureService;
    private final MetricsService metricsService;

    private static final List<String> VALID_CATEGORIES = AgentService.VALID_CATEGORIES;

    @Transactional
    public OutcomeResponse register(OutcomeRequest request, String requesterWallet) {
        if (!VALID_CATEGORIES.contains(request.getTaskCategory())) {
            throw new IllegalArgumentException("Invalid category: " + request.getTaskCategory()
                + ". Valid: " + VALID_CATEGORIES);
        }

        // Valida assinatura EVM se presente (fail-open: ausente gera warning, não bloqueia)
        if (request.getRequesterSignature() != null && !request.getRequesterSignature().isBlank()) {
            String signedMessage = "AgentRep:outcome:" + request.getContractorAgentAddress().toLowerCase()
                + ":" + request.getDeliverableHash();
            evmSignatureService.verify(signedMessage, request.getRequesterSignature(),
                request.getRequesterAgentAddress());
        }

        Agent contractor = agentRepository.findByWalletAddress(request.getContractorAgentAddress().toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("Contractor agent not found"));

        Agent requester = agentRepository.findByWalletAddress(request.getRequesterAgentAddress().toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("Requester agent not found"));

        Outcome outcome = Outcome.builder()
            .contractorAgent(contractor)
            .requesterAgent(requester)
            .taskDescription(request.getTaskDescription())
            .taskCategory(request.getTaskCategory())
            .deliverableUrl(request.getDeliverableUrl())
            .deliverableHash(request.getDeliverableHash())
            .deliverableContent(request.getDeliverableContent())
            .valueUsdc(request.getValueUsdc())
            .requesterTxHash(request.getTxHash())
            .requesterSignature(request.getRequesterSignature())
            .status(OutcomeStatus.EVALUATING)
            .build();

        outcome = outcomeRepository.save(outcome);

        evaluateAsync(outcome.getId());

        return OutcomeResponse.builder()
            .outcomeId(outcome.getId())
            .status(OutcomeStatus.EVALUATING)
            .estimatedResolutionSeconds(30)
            .build();
    }

    @Async
    @Transactional
    public void evaluateAsync(UUID outcomeId) {
        try {
            Outcome outcome = outcomeRepository.findById(outcomeId).orElseThrow();

            LlmJudgeService.JudgeResult result = llmJudgeService.evaluate(
                outcome.getTaskDescription(),
                outcome.getTaskCategory(),
                outcome.getDeliverableUrl(),
                outcome.getDeliverableHash(),
                outcome.getDeliverableContent()
            );

            outcome.setVerdict(result.verdict());
            outcome.setLlmConfidence(result.confidence());
            outcome.setLlmReasoning(result.reasoning());
            outcome.setStatus(OutcomeStatus.RESOLVED);
            outcomeRepository.save(outcome);

            metricsService.recordOutcome(result.verdict() == OutcomeVerdict.SUCCESS);

            AgentScoreUpdate scoreUpdate = updateAgentScore(outcome.getContractorAgent().getId());
            log.info("Outcome {} resolved by LLM Judge: {} (confidence={})",
                outcomeId, result.verdict(), result.confidence());

            // Notify subscribers — non-blocking
            UUID contractorId = outcome.getContractorAgent().getId();
            webhookService.dispatch(contractorId, WebhookService.EVENT_OUTCOME_RESOLVED, java.util.Map.of(
                "outcomeId", outcomeId.toString(),
                "verdict",   result.verdict().name(),
                "confidence", result.confidence()
            ));
            webhookService.dispatch(contractorId, WebhookService.EVENT_SCORE_UPDATED, java.util.Map.of(
                "score",         scoreUpdate.score(),
                "totalOutcomes", scoreUpdate.totalOutcomes(),
                "successCount",  scoreUpdate.successCount()
            ));

            // Sync to blockchain (non-blocking — failure does not affect outcome)
            var txHash = onChainService.registerOutcome(
                outcome, scoreUpdate.score(), scoreUpdate.totalOutcomes(), scoreUpdate.successCount());
            metricsService.recordOnchainSync(txHash.isPresent());
            txHash.ifPresent(hash -> {
                outcome.setOnChainTxHash(hash);
                outcome.setOnChainRegisteredAt(java.time.Instant.now());
                outcomeRepository.save(outcome);
                log.info("Outcome {} recorded onchain: {}", outcomeId, hash);
            });
        } catch (Exception e) {
            log.error("Failed to evaluate outcome {}", outcomeId, e);
        }
    }

    public record AgentScoreUpdate(BigDecimal score, int totalOutcomes, int successCount) {}

    @Transactional
    public AgentScoreUpdate updateAgentScore(UUID agentId) {
        var outcomes = outcomeRepository.findByContractorAgentIdOrderByCreatedAtDesc(
            agentId, org.springframework.data.domain.PageRequest.of(0, 10000));

        long total     = outcomes.getTotalElements();
        long successes = outcomes.getContent().stream()
            .filter(o -> o.getVerdict() == OutcomeVerdict.SUCCESS)
            .count();

        if (total == 0) return new AgentScoreUpdate(BigDecimal.ZERO, 0, 0);

        BigDecimal successRate = BigDecimal.valueOf(successes)
            .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
        BigDecimal score = successRate.multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);

        agentService.updateScoreAndTier(agentId, score, (int) total, successRate);
        return new AgentScoreUpdate(score, (int) total, (int) successes);
    }

    @Transactional(readOnly = true)
    public OutcomeResponse getById(UUID outcomeId) {
        Outcome outcome = outcomeRepository.findById(outcomeId)
            .orElseThrow(() -> new IllegalArgumentException("Outcome not found: " + outcomeId));

        return OutcomeResponse.builder()
            .outcomeId(outcome.getId())
            .status(outcome.getStatus())
            .verdict(outcome.getVerdict())
            .llmJudgeReasoning(outcome.getLlmReasoning())
            .llmConfidence(outcome.getLlmConfidence())
            .onChainTx(outcome.getOnChainTxHash())
            .build();
    }
}
