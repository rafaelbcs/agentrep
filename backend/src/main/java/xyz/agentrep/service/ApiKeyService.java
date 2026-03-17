package xyz.agentrep.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import xyz.agentrep.model.Agent;
import xyz.agentrep.repository.AgentRepository;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private static final String PREFIX = "arep_live_";
    private static final int KEY_BYTES = 32;

    private final AgentRepository agentRepository;
    private final PasswordEncoder passwordEncoder;

    public String generateApiKey() {
        byte[] bytes = new byte[KEY_BYTES];
        new SecureRandom().nextBytes(bytes);
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashApiKey(String rawApiKey) {
        return passwordEncoder.encode(rawApiKey);
    }

    public Optional<Agent> findAgentByApiKey(String rawApiKey) {
        return agentRepository.findAll().stream()
            .filter(agent -> passwordEncoder.matches(rawApiKey, agent.getApiKeyHash()))
            .findFirst();
    }
}
