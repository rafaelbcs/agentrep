package br.com.agentrep.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.agentrep.dto.WebhookRegisterRequest;
import br.com.agentrep.dto.WebhookResponse;
import br.com.agentrep.model.Agent;
import br.com.agentrep.model.Webhook;
import br.com.agentrep.repository.AgentRepository;
import br.com.agentrep.repository.WebhookRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    /** Supported event names */
    public static final String EVENT_OUTCOME_RESOLVED = "outcome.resolved";
    public static final String EVENT_SCORE_UPDATED    = "score.updated";

    private static final List<String> VALID_EVENTS = List.of(
        EVENT_OUTCOME_RESOLVED, EVENT_SCORE_UPDATED
    );

    private static final int    MAX_RETRIES    = 3;
    private static final long   RETRY_DELAY_MS = 1_000;
    private static final int    TIMEOUT_SECS   = 10;

    private final WebhookRepository webhookRepository;
    private final AgentRepository   agentRepository;
    private final ObjectMapper      objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(TIMEOUT_SECS))
        .build();

    // ─── Management ───────────────────────────────────────────────────────────

    @Transactional
    public WebhookResponse register(WebhookRegisterRequest request, String walletAddress) {
        validateEvents(request.getEvents());

        Agent agent = agentRepository.findByWalletAddress(walletAddress.toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("Agent not found for wallet: " + walletAddress));

        String secret = (request.getSecret() != null && !request.getSecret().isBlank())
            ? request.getSecret()
            : generateSecret();

        Webhook webhook = Webhook.builder()
            .agent(agent)
            .url(request.getUrl())
            .secret(secret)
            .events(request.getEvents())
            .active(true)
            .build();

        webhook = webhookRepository.save(webhook);
        log.info("Webhook registered: {} → {} events={}", webhook.getId(), webhook.getUrl(), webhook.getEvents());

        return toResponse(webhook, secret);
    }

    @Transactional
    public void delete(UUID webhookId, String walletAddress) {
        Webhook webhook = webhookRepository.findById(webhookId)
            .orElseThrow(() -> new IllegalArgumentException("Webhook not found: " + webhookId));

        if (!webhook.getAgent().getWalletAddress().equalsIgnoreCase(walletAddress)) {
            throw new SecurityException("Webhook does not belong to this agent");
        }

        webhookRepository.delete(webhook);
        log.info("Webhook deleted: {}", webhookId);
    }

    @Transactional(readOnly = true)
    public List<WebhookResponse> listByAgent(String walletAddress) {
        Agent agent = agentRepository.findByWalletAddress(walletAddress.toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("Agent not found"));

        return webhookRepository.findByAgentIdOrderByCreatedAtDesc(agent.getId())
            .stream()
            .map(w -> toResponse(w, null))
            .toList();
    }

    // ─── Dispatch ─────────────────────────────────────────────────────────────

    /**
     * Finds all active webhooks for the agent subscribed to the given event
     * and sends the payload asynchronously with HMAC-SHA256 signature + retries.
     */
    @Async
    @Transactional(readOnly = true)
    public void dispatch(UUID agentId, String event, Map<String, Object> data) {
        List<Webhook> targets = webhookRepository.findActiveByAgentIdAndEvent(agentId, event);
        if (targets.isEmpty()) return;

        Map<String, Object> payload = Map.of(
            "event",     event,
            "agentId",   agentId.toString(),
            "timestamp", Instant.now().toString(),
            "data",      data
        );

        String body;
        try {
            body = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Failed to serialize webhook payload for event {}", event, e);
            return;
        }

        for (Webhook wh : targets) {
            sendWithRetry(wh, body);
        }
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    private void sendWithRetry(Webhook webhook, String body) {
        String signature;
        try {
            signature = sign(webhook.getSecret(), body);
        } catch (Exception e) {
            log.error("Failed to sign webhook payload for {}", webhook.getId(), e);
            return;
        }

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(webhook.getUrl()))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECS))
                    .header("Content-Type", "application/json")
                    .header("X-AgentRep-Signature", signature)
                    .header("X-AgentRep-Webhook-Id", webhook.getId().toString())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

                HttpResponse<Void> response = httpClient.send(req, HttpResponse.BodyHandlers.discarding());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    log.info("Webhook {} delivered on attempt {} → HTTP {}", webhook.getId(), attempt, response.statusCode());
                    return;
                }

                log.warn("Webhook {} attempt {}/{} failed → HTTP {}", webhook.getId(), attempt, MAX_RETRIES, response.statusCode());

            } catch (Exception e) {
                log.warn("Webhook {} attempt {}/{} error: {}", webhook.getId(), attempt, MAX_RETRIES, e.getMessage());
            }

            if (attempt < MAX_RETRIES) {
                sleep(RETRY_DELAY_MS * (long) Math.pow(2, attempt - 1));
            }
        }

        log.error("Webhook {} failed after {} attempts — giving up", webhook.getId(), MAX_RETRIES);
    }

    /** HMAC-SHA256: sha256=<hex> */
    static String sign(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload.getBytes()));
    }

    private static String generateSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private void validateEvents(java.util.Set<String> events) {
        for (String e : events) {
            if (!VALID_EVENTS.contains(e)) {
                throw new IllegalArgumentException("Invalid event: " + e + ". Valid: " + VALID_EVENTS);
            }
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    private WebhookResponse toResponse(Webhook w, String secret) {
        return WebhookResponse.builder()
            .id(w.getId())
            .url(w.getUrl())
            .events(w.getEvents())
            .active(w.isActive())
            .createdAt(w.getCreatedAt())
            .secret(secret)   // null except on creation
            .build();
    }
}
