package xyz.agentrep.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    RateLimitService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        // limits: outcome=10/min, dispute=5/min, register=3/min
        service = new RateLimitService(redisTemplate, 10, 5, 3);
    }

    @Test
    void underLimit_allowed() {
        when(valueOps.increment(anyString())).thenReturn(1L);

        assertThat(service.isAllowed("0xabc", RateLimitService.ENDPOINT_OUTCOME)).isTrue();
    }

    @Test
    void atLimit_allowed() {
        when(valueOps.increment(anyString())).thenReturn(10L); // exatamente no limite

        assertThat(service.isAllowed("0xabc", RateLimitService.ENDPOINT_OUTCOME)).isTrue();
    }

    @Test
    void overLimit_blocked() {
        when(valueOps.increment(anyString())).thenReturn(11L); // acima do limite

        assertThat(service.isAllowed("0xabc", RateLimitService.ENDPOINT_OUTCOME)).isFalse();
    }

    @Test
    void firstRequest_setsExpiry() {
        when(valueOps.increment(anyString())).thenReturn(1L);

        service.isAllowed("0xabc", RateLimitService.ENDPOINT_OUTCOME);

        // Deve chamar expire ao criar a chave (count == 1)
        verify(redisTemplate).expire(anyString(), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    void subsequentRequests_doNotResetExpiry() {
        when(valueOps.increment(anyString())).thenReturn(5L); // já existia

        service.isAllowed("0xabc", RateLimitService.ENDPOINT_OUTCOME);

        verify(redisTemplate, never()).expire(anyString(), anyLong(), any());
    }

    @Test
    void differentEndpoints_useSeparateCounters() {
        when(valueOps.increment(contains("outcome"))).thenReturn(11L); // bloqueado
        when(valueOps.increment(contains("dispute"))).thenReturn(1L);  // liberado

        assertThat(service.isAllowed("0xabc", RateLimitService.ENDPOINT_OUTCOME)).isFalse();
        assertThat(service.isAllowed("0xabc", RateLimitService.ENDPOINT_DISPUTE)).isTrue();
    }

    @Test
    void differentWallets_useSeparateCounters() {
        when(valueOps.increment(contains("0xabc"))).thenReturn(11L); // bloqueado
        when(valueOps.increment(contains("0xdef"))).thenReturn(1L);  // liberado

        assertThat(service.isAllowed("0xabc", RateLimitService.ENDPOINT_OUTCOME)).isFalse();
        assertThat(service.isAllowed("0xdef", RateLimitService.ENDPOINT_OUTCOME)).isTrue();
    }

    @Test
    void redisError_allowsByDefault() {
        when(valueOps.increment(anyString())).thenThrow(new RuntimeException("Redis down"));

        // Fail open — não bloqueia se Redis estiver fora
        assertThat(service.isAllowed("0xabc", RateLimitService.ENDPOINT_OUTCOME)).isTrue();
    }
}
