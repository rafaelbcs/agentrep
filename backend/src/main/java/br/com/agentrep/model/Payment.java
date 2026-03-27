package br.com.agentrep.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tx_hash", nullable = false, unique = true, length = 66)
    private String txHash;

    @Column(name = "payer_address", length = 42)
    private String payerAddress;

    @Column(name = "amount_usdc", precision = 18, scale = 6)
    private BigDecimal amountUsdc;

    @Column(length = 255)
    private String endpoint;

    @Builder.Default
    private Boolean used = false;

    @Column(name = "validated_at")
    @Builder.Default
    private Instant validatedAt = Instant.now();
}
