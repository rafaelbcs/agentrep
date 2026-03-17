package xyz.agentrep.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.agentrep.model.Outcome;
import xyz.agentrep.model.OutcomeStatus;

import java.util.List;
import java.util.UUID;

public interface OutcomeRepository extends JpaRepository<Outcome, UUID> {

    Page<Outcome> findByContractorAgentIdOrderByCreatedAtDesc(UUID agentId, Pageable pageable);

    List<Outcome> findByStatus(OutcomeStatus status);

    @Query("SELECT o FROM Outcome o WHERE o.contractorAgent.id = :agentId AND o.taskCategory = :category ORDER BY o.createdAt DESC")
    Page<Outcome> findByAgentAndCategory(@Param("agentId") UUID agentId,
                                          @Param("category") String category,
                                          Pageable pageable);

    long countByContractorAgentIdAndVerdictIsNotNull(UUID agentId);
}
