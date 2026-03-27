package br.com.agentrep.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import br.com.agentrep.dto.WebhookRegisterRequest;
import br.com.agentrep.dto.WebhookResponse;
import br.com.agentrep.model.Agent;
import br.com.agentrep.model.Webhook;
import br.com.agentrep.repository.AgentRepository;
import br.com.agentrep.repository.WebhookRepository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock WebhookRepository webhookRepository;
    @Mock AgentRepository   agentRepository;

    WebhookService service;

    @BeforeEach
    void setUp() {
        service = new WebhookService(webhookRepository, agentRepository, new ObjectMapper());
    }

    // ─── register ─────────────────────────────────────────────────────────────

    @Test
    void register_generatesSecretWhenNotProvided() {
        var agent = agentWithWallet("0xabc");
        when(agentRepository.findByWalletAddress("0xabc")).thenReturn(Optional.of(agent));

        var saved = Webhook.builder()
            .id(UUID.randomUUID()).agent(agent)
            .url("https://example.com/hook")
            .secret("generated")
            .events(Set.of(WebhookService.EVENT_OUTCOME_RESOLVED))
            .active(true).build();
        when(webhookRepository.save(any())).thenReturn(saved);

        var req = new WebhookRegisterRequest();
        req.setUrl("https://example.com/hook");
        req.setEvents(Set.of(WebhookService.EVENT_OUTCOME_RESOLVED));

        WebhookResponse resp = service.register(req, "0xabc");

        assertThat(resp.getUrl()).isEqualTo("https://example.com/hook");
        assertThat(resp.getSecret()).isNotBlank();
        assertThat(resp.getEvents()).contains(WebhookService.EVENT_OUTCOME_RESOLVED);

        ArgumentCaptor<Webhook> captor = ArgumentCaptor.forClass(Webhook.class);
        verify(webhookRepository).save(captor.capture());
        assertThat(captor.getValue().getSecret()).hasSize(64); // 32 bytes hex
    }

    @Test
    void register_usesProvidedSecret() {
        var agent = agentWithWallet("0xabc");
        when(agentRepository.findByWalletAddress("0xabc")).thenReturn(Optional.of(agent));

        var saved = Webhook.builder()
            .id(UUID.randomUUID()).agent(agent)
            .url("https://x.com").secret("my-secret-value-1234567890123456")
            .events(Set.of(WebhookService.EVENT_SCORE_UPDATED))
            .active(true).build();
        when(webhookRepository.save(any())).thenReturn(saved);

        var req = new WebhookRegisterRequest();
        req.setUrl("https://x.com");
        req.setEvents(Set.of(WebhookService.EVENT_SCORE_UPDATED));
        req.setSecret("my-secret-value-1234567890123456");

        service.register(req, "0xabc");

        ArgumentCaptor<Webhook> captor = ArgumentCaptor.forClass(Webhook.class);
        verify(webhookRepository).save(captor.capture());
        assertThat(captor.getValue().getSecret()).isEqualTo("my-secret-value-1234567890123456");
    }

    @Test
    void register_rejectsInvalidEvent() {
        var req = new WebhookRegisterRequest();
        req.setUrl("https://x.com");
        req.setEvents(Set.of("invalid.event"));

        assertThatThrownBy(() -> service.register(req, "0xabc"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid event");
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_rejectsWrongOwner() {
        var agent = agentWithWallet("0xowner");
        var wh = Webhook.builder().id(UUID.randomUUID()).agent(agent)
            .url("https://x.com").secret("s").events(Set.of()).active(true).build();
        when(webhookRepository.findById(wh.getId())).thenReturn(Optional.of(wh));

        assertThatThrownBy(() -> service.delete(wh.getId(), "0xattacker"))
            .isInstanceOf(SecurityException.class);

        verify(webhookRepository, never()).delete(any());
    }

    // ─── HMAC signing ─────────────────────────────────────────────────────────

    @Test
    void sign_producesStableDeterministicSignature() throws Exception {
        String sig1 = WebhookService.sign("mysecret", "hello");
        String sig2 = WebhookService.sign("mysecret", "hello");
        assertThat(sig1).isEqualTo(sig2).startsWith("sha256=").hasSize(71);
    }

    @Test
    void sign_changeWithDifferentSecret() throws Exception {
        assertThat(WebhookService.sign("secret1", "payload"))
            .isNotEqualTo(WebhookService.sign("secret2", "payload"));
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private Agent agentWithWallet(String wallet) {
        return Agent.builder()
            .id(UUID.randomUUID())
            .walletAddress(wallet)
            .apiKeyHash("hash")
            .build();
    }
}
