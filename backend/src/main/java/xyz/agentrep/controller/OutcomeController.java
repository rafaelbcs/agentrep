package xyz.agentrep.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import xyz.agentrep.dto.OutcomeRequest;
import xyz.agentrep.dto.OutcomeResponse;
import xyz.agentrep.service.OutcomeService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/outcome")
@RequiredArgsConstructor
public class OutcomeController {

    private final OutcomeService outcomeService;

    @PostMapping
    public ResponseEntity<OutcomeResponse> registerOutcome(
            @Valid @RequestBody OutcomeRequest request,
            @AuthenticationPrincipal String walletAddress) {
        return ResponseEntity.status(202).body(outcomeService.register(request, walletAddress));
    }

    @GetMapping("/{outcomeId}")
    public ResponseEntity<OutcomeResponse> getOutcome(@PathVariable UUID outcomeId) {
        return ResponseEntity.ok(outcomeService.getById(outcomeId));
    }
}
