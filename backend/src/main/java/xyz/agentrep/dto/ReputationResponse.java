package xyz.agentrep.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.agentrep.model.AgentTier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReputationResponse {
    private String agentAddress;
    private int score;
    private AgentTier tier;
    private int totalOutcomes;
    private BigDecimal successRate;
    private BigDecimal totalValueTransacted;
    private Instant firstSeenAt;
    private Instant lastUpdatedAt;
    private Map<String, CategoryScoreDTO> categories;
    private boolean onChainVerified;
    private String chainTxUrl;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryScoreDTO {
        private int score;
        private int count;
    }
}
