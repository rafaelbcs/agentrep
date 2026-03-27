package br.com.agentrep.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outcomes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Outcome {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contractor_agent_id")
    private Agent contractorAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_agent_id")
    private Agent requesterAgent;

    @Column(name = "task_description", nullable = false, columnDefinition = "TEXT")
    private String taskDescription;

    @Column(name = "task_category", length = 50)
    private String taskCategory;

    @Column(name = "deliverable_url", columnDefinition = "TEXT")
    private String deliverableUrl;

    @Column(name = "deliverable_hash", length = 66)
    private String deliverableHash;

    @Column(name = "deliverable_content", columnDefinition = "TEXT")
    private String deliverableContent;

    @Column(name = "value_usdc", precision = 18, scale = 6)
    private BigDecimal valueUsdc;

    @Column(name = "requester_tx_hash", length = 66)
    private String requesterTxHash;

    @Column(name = "requester_signature", columnDefinition = "TEXT")
    private String requesterSignature;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private OutcomeStatus status = OutcomeStatus.EVALUATING;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OutcomeVerdict verdict;

    @Column(name = "llm_confidence", precision = 4, scale = 3)
    private BigDecimal llmConfidence;

    @Column(name = "llm_reasoning", columnDefinition = "TEXT")
    private String llmReasoning;

    @Column(name = "on_chain_tx_hash", length = 66)
    private String onChainTxHash;

    @Column(name = "on_chain_registered_at")
    private Instant onChainRegisteredAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
