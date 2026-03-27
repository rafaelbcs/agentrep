package br.com.agentrep.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import br.com.agentrep.dto.ReputationResponse;
import br.com.agentrep.service.ReputationService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reputation")
@RequiredArgsConstructor
public class ReputationController {

    private final ReputationService reputationService;

    @GetMapping("/{agentAddress}")
    public ResponseEntity<ReputationResponse> getReputation(
            @PathVariable String agentAddress,
            @RequestHeader(value = "X-Payment-Proof", required = false) String paymentProof) {
        return ResponseEntity.ok(reputationService.getReputation(agentAddress, paymentProof));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<ReputationResponse>> getBulkReputation(
            @RequestBody List<String> addresses,
            @RequestHeader(value = "X-Payment-Proof", required = false) String paymentProof) {
        return ResponseEntity.ok(reputationService.getBulkReputation(addresses, paymentProof));
    }
}
