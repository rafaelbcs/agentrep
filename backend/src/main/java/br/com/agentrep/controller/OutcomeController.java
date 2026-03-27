package br.com.agentrep.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import br.com.agentrep.dto.OutcomeRequest;
import br.com.agentrep.dto.OutcomeResponse;
import br.com.agentrep.service.IdempotencyService;
import br.com.agentrep.service.OutcomeService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/outcome")
@RequiredArgsConstructor
public class OutcomeController {

    private final OutcomeService outcomeService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    public ResponseEntity<OutcomeResponse> registerOutcome(
            @Valid @RequestBody OutcomeRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal String walletAddress) {
        idempotencyService.checkAndRegister(idempotencyKey);
        return ResponseEntity.status(202).body(outcomeService.register(request, walletAddress));
    }

    @GetMapping("/{outcomeId}")
    public ResponseEntity<OutcomeResponse> getOutcome(@PathVariable UUID outcomeId) {
        return ResponseEntity.ok(outcomeService.getById(outcomeId));
    }
}
