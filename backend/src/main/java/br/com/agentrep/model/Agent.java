package br.com.agentrep.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "agents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "wallet_address", nullable = false, unique = true, length = 42)
    private String walletAddress;

    @Column(length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "owner_email")
    private String ownerEmail;

    @Column(name = "api_key_hash", nullable = false)
    private String apiKeyHash;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private AgentTier tier = AgentTier.UNKNOWN;

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal score = BigDecimal.ZERO;

    @Column(name = "total_outcomes")
    @Builder.Default
    private Integer totalOutcomes = 0;

    @Column(name = "success_rate", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal successRate = BigDecimal.ZERO;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "agent_categories", joinColumns = @JoinColumn(name = "agent_id"))
    @Column(name = "category")
    @Builder.Default
    private Set<String> categories = new HashSet<>();

    @Column(name = "on_chain_synced")
    @Builder.Default
    private Boolean onChainSynced = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
