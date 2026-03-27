package br.com.agentrep.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnChainCircuitBreakerTest {

    @Mock Web3j web3j;

    OnChainService service;
    CircuitBreaker circuitBreaker;

    static final String PRIVATE_KEY =
        "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80";
    static final String CONTRACT =
        "0x5FbDB2315678afecb367f032d93F642f64180aa3";

    @BeforeEach
    void setUp() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .minimumNumberOfCalls(4)
            .slidingWindowSize(4)
            .waitDurationInOpenState(Duration.ofSeconds(1))
            .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        circuitBreaker = registry.circuitBreaker("onchain");

        service = new OnChainService(web3j, PRIVATE_KEY, CONTRACT, 31337L, circuitBreaker);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void circuitOpens_afterThresholdFailures() throws Exception {
        // Simula falha no RPC (nonce request lança IOException)
        org.web3j.protocol.core.Request nonceRequest = mock(org.web3j.protocol.core.Request.class);
        doThrow(new IOException("RPC unavailable")).when(nonceRequest).send();
        doReturn(nonceRequest).when(web3j).ethGetTransactionCount(any(), any());

        // 4 chamadas — todas falham
        for (int i = 0; i < 4; i++) {
            Optional<String> result = service.registerAgent("0xabc");
            assertThat(result).isEmpty();
        }

        // Circuit deve estar OPEN agora
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void openCircuit_returnsFallbackImmediately() throws Exception {
        // Força circuit aberto
        circuitBreaker.transitionToOpenState();

        // Não deve chamar o RPC
        Optional<String> result = service.registerAgent("0xabc");

        assertThat(result).isEmpty();
        verifyNoInteractions(web3j);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void successfulCall_doesNotOpenCircuit() throws Exception {
        org.web3j.protocol.core.Request nonceRequest = mock(org.web3j.protocol.core.Request.class);
        EthGetTransactionCount nonceMock = mock(EthGetTransactionCount.class);
        when(nonceMock.getTransactionCount()).thenReturn(BigInteger.ZERO);
        when(nonceRequest.send()).thenReturn(nonceMock);
        doReturn(nonceRequest).when(web3j).ethGetTransactionCount(any(), any());

        org.web3j.protocol.core.Request txRequest = mock(org.web3j.protocol.core.Request.class);
        EthSendTransaction txMock = mock(EthSendTransaction.class);
        when(txMock.hasError()).thenReturn(false);
        when(txMock.getTransactionHash()).thenReturn("0xabc123");
        when(txRequest.send()).thenReturn(txMock);
        doReturn(txRequest).when(web3j).ethSendRawTransaction(any());

        Optional<String> result = service.registerAgent("0xdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef");

        assertThat(result).isPresent();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
