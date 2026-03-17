package xyz.agentrep.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import xyz.agentrep.service.OnChainService;

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
    public OnChainService onChainService(Web3j web3j) {
        var service = new OnChainService(
            web3j,
            props.getDeployerPrivateKey(),
            props.getContractAddress(),
            props.getChainId()
        );
        if (service.isEnabled()) {
            log.info("OnChainService ENABLED — contract: {}", props.getContractAddress());
        } else {
            log.warn("OnChainService DISABLED — set DEPLOYER_PRIVATE_KEY and CONTRACT_ADDRESS to enable");
        }
        return service;
    }
}
