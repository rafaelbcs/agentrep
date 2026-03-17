package xyz.agentrep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class DisputeRequest {

    @NotNull
    private UUID outcomeId;

    @NotBlank
    private String reason;

    private String evidenceUrl;

    @NotBlank
    private String stakePaymentTxHash;
}
