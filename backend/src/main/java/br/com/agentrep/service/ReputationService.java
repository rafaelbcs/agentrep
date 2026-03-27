package br.com.agentrep.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.agentrep.dto.ReputationResponse;
import br.com.agentrep.model.Agent;
import br.com.agentrep.model.OutcomeVerdict;
import br.com.agentrep.repository.AgentRepository;
import br.com.agentrep.repository.OutcomeRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReputationService {

    private final AgentRepository agentRepository;
    private final OutcomeRepository outcomeRepository;

    @Cacheable(value = "reputation", key = "#agentAddress")
    @Transactional(readOnly = true)
    public ReputationResponse getReputation(String agentAddress, String paymentProof) {
        Agent agent = agentRepository.findByWalletAddress(agentAddress.toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentAddress));

        return buildReputationResponse(agent);
    }

    @Transactional(readOnly = true)
    public List<ReputationResponse> getBulkReputation(List<String> addresses, String paymentProof) {
        return addresses.stream()
            .map(addr -> agentRepository.findByWalletAddress(addr.toLowerCase()).orElse(null))
            .filter(a -> a != null)
            .map(this::buildReputationResponse)
            .toList();
    }

    private ReputationResponse buildReputationResponse(Agent agent) {
        Map<String, ReputationResponse.CategoryScoreDTO> categories = buildCategoryScores(agent);

        int score = agent.getScore() != null
            ? agent.getScore().setScale(0, RoundingMode.HALF_UP).intValue()
            : 0;

        return ReputationResponse.builder()
            .agentAddress(agent.getWalletAddress())
            .score(score)
            .tier(agent.getTier())
            .totalOutcomes(agent.getTotalOutcomes() != null ? agent.getTotalOutcomes() : 0)
            .successRate(agent.getSuccessRate() != null ? agent.getSuccessRate() : BigDecimal.ZERO)
            .totalValueTransacted(BigDecimal.ZERO)
            .firstSeenAt(agent.getCreatedAt())
            .lastUpdatedAt(agent.getUpdatedAt())
            .categories(categories)
            .onChainVerified(Boolean.TRUE.equals(agent.getOnChainSynced()))
            .chainTxUrl("https://basescan.org/address/" + agent.getWalletAddress())
            .build();
    }

    private Map<String, ReputationResponse.CategoryScoreDTO> buildCategoryScores(Agent agent) {
        Map<String, ReputationResponse.CategoryScoreDTO> result = new HashMap<>();
        var outcomes = outcomeRepository.findByContractorAgentIdOrderByCreatedAtDesc(agent.getId(),
            org.springframework.data.domain.PageRequest.of(0, 1000));

        Map<String, long[]> byCategory = new HashMap<>();
        for (var outcome : outcomes.getContent()) {
            if (outcome.getTaskCategory() == null || outcome.getVerdict() == null) continue;
            byCategory.computeIfAbsent(outcome.getTaskCategory(), k -> new long[]{0, 0});
            long[] counts = byCategory.get(outcome.getTaskCategory());
            counts[0]++;
            if (outcome.getVerdict() == OutcomeVerdict.SUCCESS) counts[1]++;
        }

        byCategory.forEach((cat, counts) -> {
            int catScore = counts[0] > 0 ? (int) ((counts[1] * 100) / counts[0]) : 0;
            result.put(cat, ReputationResponse.CategoryScoreDTO.builder()
                .score(catScore)
                .count((int) counts[0])
                .build());
        });

        return result;
    }
}
