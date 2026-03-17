package xyz.agentrep.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import xyz.agentrep.dto.WebhookRegisterRequest;
import xyz.agentrep.dto.WebhookResponse;
import xyz.agentrep.service.WebhookService;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('AGENT')")
public class WebhookController {

    private final WebhookService webhookService;

    /** Register a new webhook for the authenticated agent. Returns the secret once. */
    @PostMapping
    public ResponseEntity<WebhookResponse> register(@Valid @RequestBody WebhookRegisterRequest request,
                                                    Principal principal) {
        return ResponseEntity.status(201).body(
            webhookService.register(request, principal.getName())
        );
    }

    /** List all webhooks for the authenticated agent. Secret is NOT returned. */
    @GetMapping
    public List<WebhookResponse> list(Principal principal) {
        return webhookService.listByAgent(principal.getName());
    }

    /** Delete a webhook by ID. Only the owning agent can delete. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Principal principal) {
        webhookService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
