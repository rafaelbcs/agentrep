package br.com.agentrep.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import br.com.agentrep.dto.DisputeRequest;
import br.com.agentrep.dto.DisputeResolveRequest;
import br.com.agentrep.dto.DisputeResponse;
import br.com.agentrep.service.DisputeService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping
    public ResponseEntity<DisputeResponse> openDispute(
            @Valid @RequestBody DisputeRequest request,
            @AuthenticationPrincipal String walletAddress) {
        return ResponseEntity.status(201).body(disputeService.open(request, walletAddress));
    }

    @GetMapping("/{disputeId}")
    public ResponseEntity<DisputeResponse> getDispute(@PathVariable UUID disputeId) {
        return ResponseEntity.ok(disputeService.getById(disputeId));
    }

    @PostMapping("/{disputeId}/resolve")
    public ResponseEntity<DisputeResponse> resolveDispute(
            @PathVariable UUID disputeId,
            @Valid @RequestBody DisputeResolveRequest request,
            @AuthenticationPrincipal String walletAddress) {
        return ResponseEntity.ok(disputeService.resolve(disputeId, request, walletAddress));
    }
}
