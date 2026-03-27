package br.com.agentrep.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.agentrep.dto.DisputeRequest;
import br.com.agentrep.dto.DisputeResolveRequest;
import br.com.agentrep.dto.DisputeResponse;
import br.com.agentrep.model.*;
import br.com.agentrep.repository.AgentRepository;
import br.com.agentrep.repository.DisputeRepository;
import br.com.agentrep.repository.OutcomeRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final OutcomeRepository outcomeRepository;
    private final AgentRepository agentRepository;
    private final OutcomeService outcomeService;

    @Value("${agentrep.x402.dispute-stake-usdc:0.50}")
    private String disputeStakeUsdc;

    @Transactional
    public DisputeResponse open(DisputeRequest request, String walletAddress) {
        Outcome outcome = outcomeRepository.findById(request.getOutcomeId())
            .orElseThrow(() -> new IllegalArgumentException("Outcome not found"));

        if (outcome.getStatus() != OutcomeStatus.RESOLVED) {
            throw new IllegalStateException("Can only dispute resolved outcomes");
        }

        Agent opener = agentRepository.findByWalletAddress(walletAddress.toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("Agent not found"));

        boolean isParty = opener.getId().equals(outcome.getContractorAgent().getId())
            || opener.getId().equals(outcome.getRequesterAgent().getId());
        if (!isParty) {
            throw new IllegalStateException("Only parties of the outcome can open a dispute");
        }

        outcome.setStatus(OutcomeStatus.DISPUTED);
        outcomeRepository.save(outcome);

        Dispute dispute = Dispute.builder()
            .outcome(outcome)
            .openedByAgent(opener)
            .status(DisputeStatus.OPEN)
            .reason(request.getReason())
            .evidenceUrl(request.getEvidenceUrl())
            .stakePaymentTxHash(request.getStakePaymentTxHash())
            .deadline(Instant.now().plus(48, ChronoUnit.HOURS))
            .build();

        dispute = disputeRepository.save(dispute);
        log.info("Dispute opened: {} for outcome {}", dispute.getId(), outcome.getId());

        return toResponse(dispute);
    }

    @Transactional
    public DisputeResponse resolve(UUID disputeId, DisputeResolveRequest request, String resolverWallet) {
        Dispute dispute = disputeRepository.findById(disputeId)
            .orElseThrow(() -> new IllegalArgumentException("Dispute not found"));

        if (dispute.getStatus() == DisputeStatus.RESOLVED) {
            throw new IllegalStateException("Dispute already resolved");
        }

        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setResolvedVerdict(request.getVerdict());
        dispute.setResolvedReason(request.getReason());
        dispute.setResolvedBy(resolverWallet);
        dispute.setResolvedAt(Instant.now());
        disputeRepository.save(dispute);

        Outcome outcome = dispute.getOutcome();
        OutcomeVerdict newVerdict = request.getVerdict() == DisputeVerdict.REQUESTER_WINS
            ? OutcomeVerdict.FAILURE
            : OutcomeVerdict.SUCCESS;
        outcome.setVerdict(newVerdict);
        outcome.setStatus(OutcomeStatus.RESOLVED);
        outcomeRepository.save(outcome);

        outcomeService.updateAgentScore(outcome.getContractorAgent().getId());
        log.info("Dispute {} resolved: {}", disputeId, request.getVerdict());

        return toResponse(dispute);
    }

    @Transactional(readOnly = true)
    public DisputeResponse getById(UUID disputeId) {
        return toResponse(disputeRepository.findById(disputeId)
            .orElseThrow(() -> new IllegalArgumentException("Dispute not found")));
    }

    private DisputeResponse toResponse(Dispute dispute) {
        return DisputeResponse.builder()
            .disputeId(dispute.getId())
            .outcomeId(dispute.getOutcome().getId())
            .status(dispute.getStatus())
            .resolvedVerdict(dispute.getResolvedVerdict())
            .resolvedReason(dispute.getResolvedReason())
            .requiredCounterpartyStakeUsdc(disputeStakeUsdc)
            .deadline(dispute.getDeadline())
            .resolvedAt(dispute.getResolvedAt())
            .build();
    }
}
