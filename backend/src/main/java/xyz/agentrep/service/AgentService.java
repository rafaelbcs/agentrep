package xyz.agentrep.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.agentrep.dto.AgentRegisterRequest;
import xyz.agentrep.dto.AgentRegisterResponse;
import xyz.agentrep.model.Agent;
import xyz.agentrep.model.AgentTier;
import xyz.agentrep.repository.AgentRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {

    public static final List<String> VALID_CATEGORIES = List.of(
        "code-review", "data-analysis", "research", "content",
        "infra", "finance", "trading", "legal", "ops"
    );

    private final AgentRepository agentRepository;
    private final ApiKeyService apiKeyService;
    private final PasswordEncoder passwordEncoder;
    private final OnChainService onChainService;

    @Transactional
    public AgentRegisterResponse register(AgentRegisterRequest request) {
        if (agentRepository.existsByWalletAddress(request.getAgentAddress())) {
            throw new IllegalArgumentException("Agent with this wallet address already registered");
        }

        String rawApiKey = apiKeyService.generateApiKey();
        String hashedApiKey = apiKeyService.hashApiKey(rawApiKey);

        java.util.Set<String> cats = new java.util.HashSet<>();
        if (request.getCategories() != null) {
            for (String cat : request.getCategories()) {
                if (!VALID_CATEGORIES.contains(cat)) {
                    throw new IllegalArgumentException(
                        "Invalid category: " + cat + ". Valid: " + VALID_CATEGORIES);
                }
            }
            cats.addAll(request.getCategories());
        }

        Agent agent = Agent.builder()
            .walletAddress(request.getAgentAddress().toLowerCase())
            .name(request.getName())
            .description(request.getDescription())
            .ownerEmail(request.getOwnerEmail())
            .apiKeyHash(hashedApiKey)
            .tier(AgentTier.UNKNOWN)
            .score(BigDecimal.ZERO)
            .totalOutcomes(0)
            .successRate(BigDecimal.ZERO)
            .categories(cats)
            .onChainSynced(false)
            .build();

        agent = agentRepository.save(agent);
        log.info("Registered new agent: {} ({})", agent.getName(), agent.getWalletAddress());

        // Sync to blockchain (non-blocking — failure does not block registration)
        final UUID agentId = agent.getId();
        final String walletAddress = agent.getWalletAddress();
        onChainService.registerAgent(walletAddress).ifPresent(txHash -> {
            agentRepository.findById(agentId).ifPresent(a -> {
                a.setOnChainSynced(true);
                agentRepository.save(a);
            });
            log.info("Agent {} registered onchain: {}", walletAddress, txHash);
        });

        return AgentRegisterResponse.builder()
            .agentId(agent.getId())
            .walletAddress(agent.getWalletAddress())
            .apiKey(rawApiKey)
            .moltbookSkillSnippet(buildMoltbookSnippet(agent.getWalletAddress()))
            .build();
    }

    @Transactional(readOnly = true)
    public Agent findById(UUID id) {
        return agentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + id));
    }

    @Transactional(readOnly = true)
    public Agent findByWalletAddress(String address) {
        return agentRepository.findByWalletAddress(address.toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + address));
    }

    @Transactional(readOnly = true)
    public Page<Agent> findLeaderboard(Pageable pageable) {
        return agentRepository.findLeaderboard(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Agent> explore(String category, BigDecimal minScore, Pageable pageable) {
        if (category != null && !category.isBlank() && !category.equals("all")) {
            return agentRepository.findByMinScore(minScore != null ? minScore : BigDecimal.ZERO, pageable);
        }
        return agentRepository.findByMinScore(minScore != null ? minScore : BigDecimal.ZERO, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Agent> search(String q, Pageable pageable) {
        return agentRepository.searchByNameOrDescription(q, pageable);
    }

    @Transactional
    public void updateScoreAndTier(UUID agentId, BigDecimal newScore, int totalOutcomes, BigDecimal successRate) {
        agentRepository.findById(agentId).ifPresent(agent -> {
            agent.setScore(newScore);
            agent.setTotalOutcomes(totalOutcomes);
            agent.setSuccessRate(successRate);
            agent.setTier(AgentTier.fromScore(newScore, totalOutcomes));
            agentRepository.save(agent);
        });
    }

    private String buildMoltbookSnippet(String walletAddress) {
        return """
            {
              "skill": "agentrep-reputation",
              "endpoint": "https://api.agentrep.xyz/api/v1/reputation/%s",
              "description": "Query AgentRep reputation score (x402 paywall: $0.001 USDC)",
              "version": "1.0"
            }
            """.formatted(walletAddress);
    }

    @Transactional(readOnly = true)
    public List<Agent> findByWalletAddresses(List<String> addresses) {
        return addresses.stream()
            .map(String::toLowerCase)
            .map(addr -> agentRepository.findByWalletAddress(addr).orElse(null))
            .filter(a -> a != null)
            .toList();
    }
}
