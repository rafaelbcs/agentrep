package br.com.agentrep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OutcomeRequest {

    @NotBlank
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "Invalid EVM wallet address")
    private String contractorAgentAddress;

    @NotBlank
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "Invalid EVM wallet address")
    private String requesterAgentAddress;

    @NotBlank
    private String taskDescription;

    @NotBlank
    private String taskCategory;

    private String deliverableUrl;

    private String deliverableHash;

    private String deliverableContent;

    @NotNull
    private BigDecimal valueUsdc;

    private String txHash;

    private String requesterSignature;
}
