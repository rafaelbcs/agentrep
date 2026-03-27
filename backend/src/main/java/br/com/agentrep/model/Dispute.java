package br.com.agentrep.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "disputes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outcome_id", nullable = false)
    private Outcome outcome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opened_by_agent_id", nullable = false)
    private Agent openedByAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DisputeStatus status = DisputeStatus.OPEN;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "evidence_url", columnDefinition = "TEXT")
    private String evidenceUrl;

    @Column(name = "stake_payment_tx_hash", length = 66)
    private String stakePaymentTxHash;

    @Column(name = "counterparty_stake_tx_hash", length = 66)
    private String counterpartyStakeTxHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolved_verdict", length = 30)
    private DisputeVerdict resolvedVerdict;

    @Column(name = "resolved_reason", columnDefinition = "TEXT")
    private String resolvedReason;

    @Column(name = "resolved_by", length = 42)
    private String resolvedBy;

    @Column(name = "deadline")
    private Instant deadline;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
