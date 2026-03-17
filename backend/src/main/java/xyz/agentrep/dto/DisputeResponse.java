package xyz.agentrep.dto;

import lombok.Builder;
import lombok.Data;
import xyz.agentrep.model.DisputeStatus;
import xyz.agentrep.model.DisputeVerdict;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class DisputeResponse {
    private UUID disputeId;
    private UUID outcomeId;
    private DisputeStatus status;
    private DisputeVerdict resolvedVerdict;
    private String resolvedReason;
    private String requiredCounterpartyStakeUsdc;
    private Instant deadline;
    private Instant resolvedAt;
}
