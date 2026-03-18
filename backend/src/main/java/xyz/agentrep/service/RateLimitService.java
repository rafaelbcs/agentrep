package xyz.agentrep.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Rate limiting por wallet usando Redis INCR + EXPIRE (sliding window de 1 minuto).
 * Fail-open: se Redis estiver indisponível, a requisição é permitida.
 */
@Service
@Slf4j
public class RateLimitService {

    public static final String ENDPOINT_OUTCOME  = "outcome";
    public static final String ENDPOINT_DISPUTE  = "dispute";
    public static final String ENDPOINT_REGISTER = "register";

    private final StringRedisTemplate redis;
    private final int limitOutcome;
    private final int limitDispute;
    private final int limitRegister;

    public RateLimitService(
        StringRedisTemplate redis,
        @Value("${agentrep.rate-limit.outcome-per-minute:10}")  int limitOutcome,
        @Value("${agentrep.rate-limit.dispute-per-minute:5}")   int limitDispute,
        @Value("${agentrep.rate-limit.register-per-minute:3}")  int limitRegister
    ) {
        this.redis         = redis;
        this.limitOutcome  = limitOutcome;
        this.limitDispute  = limitDispute;
        this.limitRegister = limitRegister;
    }

    /**
     * Verifica e consome 1 token para o par (wallet, endpoint).
     * @return true se permitido, false se limite excedido
     */
    public boolean isAllowed(String walletAddress, String endpoint) {
        try {
            String key = "rl:" + endpoint + ":" + walletAddress.toLowerCase();
            Long count = redis.opsForValue().increment(key);

            if (count == null) return true; // fail open

            if (count == 1L) {
                // Primeira chamada nesta janela — define expiração de 1 minuto
                redis.expire(key, 60L, TimeUnit.SECONDS);
            }

            int limit = limitFor(endpoint);
            if (count > limit) {
                log.warn("Rate limit exceeded: wallet={} endpoint={} count={} limit={}", walletAddress, endpoint, count, limit);
                return false;
            }
            return true;

        } catch (Exception e) {
            log.error("Rate limit check failed (Redis error) — failing open: {}", e.getMessage());
            return true; // fail open
        }
    }

    private int limitFor(String endpoint) {
        return switch (endpoint) {
            case ENDPOINT_OUTCOME  -> limitOutcome;
            case ENDPOINT_DISPUTE  -> limitDispute;
            case ENDPOINT_REGISTER -> limitRegister;
            default                -> limitOutcome;
        };
    }
}
