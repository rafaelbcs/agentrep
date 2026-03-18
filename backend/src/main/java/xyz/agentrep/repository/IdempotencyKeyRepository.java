package xyz.agentrep.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IdempotencyKeyRepository {

    private final JdbcTemplate jdbc;

    public IdempotencyKeyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean existsByKey(String key) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM idempotency_keys WHERE key = ?",
            Integer.class, key
        );
        return count != null && count > 0;
    }

    public void saveKey(String key) {
        jdbc.update("INSERT INTO idempotency_keys (key) VALUES (?)", key);
    }
}
