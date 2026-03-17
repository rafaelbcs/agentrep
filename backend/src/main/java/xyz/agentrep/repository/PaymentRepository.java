package xyz.agentrep.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.agentrep.model.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByTxHash(String txHash);

    boolean existsByTxHashAndUsedTrue(String txHash);
}
