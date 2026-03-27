package br.com.agentrep.dto;

import lombok.Builder;
import lombok.Data;
import br.com.agentrep.model.OutcomeStatus;
import br.com.agentrep.model.OutcomeVerdict;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class OutcomeResponse {
    private UUID outcomeId;
    private OutcomeStatus status;
    private OutcomeVerdict verdict;
    private String llmJudgeReasoning;
    private BigDecimal llmConfidence;
    private String scoreImpact;
    private String onChainTx;
    private int estimatedResolutionSeconds;
}
