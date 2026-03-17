package xyz.agentrep.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xyz.agentrep.model.Agent;
import xyz.agentrep.model.AgentTier;

import java.util.Optional;
import java.util.UUID;

public interface AgentRepository extends JpaRepository<Agent, UUID> {

    Optional<Agent> findByWalletAddress(String walletAddress);

    boolean existsByWalletAddress(String walletAddress);

    Page<Agent> findByTierOrderByScoreDesc(AgentTier tier, Pageable pageable);

    @Query("SELECT a FROM Agent a WHERE a.score >= :minScore ORDER BY a.score DESC")
    Page<Agent> findByMinScore(@Param("minScore") java.math.BigDecimal minScore, Pageable pageable);

    @Query("SELECT a FROM Agent a WHERE a.score >= :minScore AND :category MEMBER OF a.categories ORDER BY a.score DESC")
    Page<Agent> findByMinScoreAndCategory(@Param("minScore") java.math.BigDecimal minScore,
                                           @Param("category") String category,
                                           Pageable pageable);

    @Query("SELECT a FROM Agent a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(a.description) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY a.score DESC")
    Page<Agent> searchByNameOrDescription(@Param("q") String q, Pageable pageable);

    @Query("SELECT a FROM Agent a ORDER BY a.score DESC")
    Page<Agent> findLeaderboard(Pageable pageable);
}
