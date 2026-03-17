package xyz.agentrep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import xyz.agentrep.model.DisputeVerdict;

@Data
public class DisputeResolveRequest {

    @NotNull
    private DisputeVerdict verdict;

    @NotBlank
    private String reason;
}
