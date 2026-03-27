package br.com.agentrep.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AgentRegisterResponse {
    private UUID agentId;
    private String walletAddress;
    private String apiKey;
    private String moltbookSkillSnippet;
}
