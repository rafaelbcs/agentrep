package br.com.agentrep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class AgentRegisterRequest {

    @NotBlank
    @Pattern(regexp = "^0x[a-fA-F0-9]{40}$", message = "Invalid EVM wallet address")
    private String agentAddress;

    @NotBlank
    private String name;

    private String description;

    private String ownerEmail;

    private List<String> categories;
}
