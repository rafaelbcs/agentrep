package xyz.agentrep.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.agentrep.model.Webhook;

import java.util.List;
import java.util.UUID;

public interface WebhookRepository extends JpaRepository<Webhook, UUID> {

    List<Webhook> findByAgentIdOrderByCreatedAtDesc(UUID agentId);

    @Query("SELECT w FROM Webhook w JOIN w.events e " +
           "WHERE w.agent.id = :agentId AND w.active = true AND e = :event")
    List<Webhook> findActiveByAgentIdAndEvent(@Param("agentId") UUID agentId,
                                              @Param("event") String event);
}
