package xyz.agentrep.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import xyz.agentrep.service.OnChainService;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class BlockchainConfig {

    private final BlockchainProperties props;

    @Bean
    public Web3j web3j() {
        log.info("Initializing Web3j → RPC: {}, chainId: {}", props.getRpcUrl(), props.getChainId());
        return Web3j.build(new HttpService(props.getRpcUrl()));
    }

    @Bean
    public CircuitBreaker onChainCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .minimumNumberOfCalls(5)
            .slidingWindowSize(10)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .build();
        return CircuitBreakerRegistry.of(config).circuitBreaker("onchain");
    }

    @Bean
    public OnChainService onChainService(Web3j web3j, CircuitBreaker onChainCircuitBreaker) {
        var service = new OnChainService(
            web3j,
            props.getDeployerPrivateKey(),
            props.getContractAddress(),
            props.getChainId(),
            onChainCircuitBreaker
        );
        if (service.isEnabled()) {
            log.info("OnChainService ENABLED — contract: {}", props.getContractAddress());
        } else {
            log.warn("OnChainService DISABLED — set DEPLOYER_PRIVATE_KEY and CONTRACT_ADDRESS to enable");
        }
        return service;
    }
}
