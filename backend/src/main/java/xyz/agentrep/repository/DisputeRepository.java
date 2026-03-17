package xyz.agentrep.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.agentrep.model.Dispute;
import xyz.agentrep.model.DisputeStatus;

import java.util.List;
import java.util.UUID;

public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    List<Dispute> findByOutcomeId(UUID outcomeId);

    List<Dispute> findByStatus(DisputeStatus status);
}
