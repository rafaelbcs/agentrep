package br.com.agentrep.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "agentrep.blockchain")
@Getter
@Setter
public class BlockchainProperties {
    private String rpcUrl = "https://sepolia.base.org";
    private String contractAddress = "0x0000000000000000000000000000000000000000";
    private String deployerPrivateKey = "";
    private long chainId = 84532L;
}
