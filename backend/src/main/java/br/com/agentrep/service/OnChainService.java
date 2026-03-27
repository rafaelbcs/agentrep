package br.com.agentrep.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.utils.Numeric;
import br.com.agentrep.model.Outcome;
import br.com.agentrep.model.OutcomeVerdict;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Calls AgentRepRegistry smart contract on Base L2.
 * Non-blocking: all methods return Optional.empty() on failure — never throws.
 * Disabled automatically when DEPLOYER_PRIVATE_KEY or CONTRACT_ADDRESS are not configured.
 */
@Slf4j
public class OnChainService {

    private static final String ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";
    private static final BigInteger GAS_LIMIT = BigInteger.valueOf(300_000L);
    private static final BigInteger GAS_PRICE = BigInteger.valueOf(1_000_000_000L); // 1 gwei

    private final Web3j          web3j;
    private final Credentials    credentials;
    private final String         contractAddress;
    private final long           chainId;
    private final boolean        enabled;
    private final CircuitBreaker circuitBreaker;

    public OnChainService(Web3j web3j, String privateKey, String contractAddress, long chainId,
                          CircuitBreaker circuitBreaker) {
        this.web3j          = web3j;
        this.contractAddress = contractAddress;
        this.chainId         = chainId;
        this.circuitBreaker  = circuitBreaker;

        boolean hasKey      = privateKey != null && !privateKey.isBlank();
        boolean hasContract = contractAddress != null
                              && !contractAddress.isBlank()
                              && !contractAddress.equalsIgnoreCase(ZERO_ADDRESS);

        if (hasKey && hasContract) {
            this.credentials = Credentials.create(privateKey);
            this.enabled     = true;
        } else {
            this.credentials = null;
            this.enabled     = false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Calls registerAgent(address) on AgentRepRegistry.
     */
    public Optional<String> registerAgent(String agentWallet) {
        if (!enabled) return Optional.empty();
        try {
            Function fn = new Function(
                "registerAgent",
                List.of(new Address(agentWallet)),
                List.of()
            );
            return send(fn);
        } catch (Exception e) {
            log.error("onchain registerAgent failed for {}: {}", agentWallet, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Calls registerOutcome(...) on AgentRepRegistry.
     * @param outcome       resolved outcome entity
     * @param newScore      agent's new score (0–100.00)
     * @param totalOutcomes total resolved outcomes for agent
     * @param successCount  number of SUCCESS outcomes
     */
    public Optional<String> registerOutcome(
        Outcome outcome,
        BigDecimal newScore,
        int totalOutcomes,
        int successCount
    ) {
        if (!enabled) return Optional.empty();
        try {
            boolean success      = outcome.getVerdict() == OutcomeVerdict.SUCCESS;
            String  category     = outcome.getTaskCategory() != null ? outcome.getTaskCategory() : "";
            byte[]  categoryHash = keccak256Bytes(category);
            byte[]  outcomeId    = uuidToBytes32(outcome.getId());
            // Category score same as global for MVP (no per-category aggregation onchain yet)
            int catScore         = scoreToUint32(newScore);
            int catCount         = totalOutcomes;

            Function fn = new Function(
                "registerOutcome",
                Arrays.asList(
                    new Address(outcome.getContractorAgent().getWalletAddress()),
                    new Uint32(scoreToUint32(newScore)),
                    new Uint32(totalOutcomes),
                    new Uint32(successCount),
                    new Bytes32(categoryHash),
                    new Uint32(catScore),
                    new Uint32(catCount),
                    new Bytes32(outcomeId),
                    new Bool(success),
                    new Address(outcome.getRequesterAgent().getWalletAddress())
                ),
                List.of()
            );
            return send(fn);
        } catch (Exception e) {
            log.error("onchain registerOutcome failed for outcome {}: {}", outcome.getId(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Calls recordDispute(address, bytes32) on AgentRepRegistry.
     */
    public Optional<String> recordDispute(String agentWallet, UUID outcomeId) {
        if (!enabled) return Optional.empty();
        try {
            Function fn = new Function(
                "recordDispute",
                Arrays.asList(
                    new Address(agentWallet),
                    new Bytes32(uuidToBytes32(outcomeId))
                ),
                List.of()
            );
            return send(fn);
        } catch (Exception e) {
            log.error("onchain recordDispute failed for {}: {}", outcomeId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Calls recordDisputeResolution(bytes32, bool) on AgentRepRegistry.
     */
    public Optional<String> recordDisputeResolution(UUID outcomeId, boolean requesterWins) {
        if (!enabled) return Optional.empty();
        try {
            Function fn = new Function(
                "recordDisputeResolution",
                Arrays.asList(
                    new Bytes32(uuidToBytes32(outcomeId)),
                    new Bool(requesterWins)
                ),
                List.of()
            );
            return send(fn);
        } catch (Exception e) {
            log.error("onchain recordDisputeResolution failed for {}: {}", outcomeId, e.getMessage());
            return Optional.empty();
        }
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    private Optional<String> send(Function function) {
        if (circuitBreaker != null && circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
            log.warn("Circuit breaker OPEN — skipping onchain call: {}", function.getName());
            return Optional.empty();
        }
        try {
            return circuitBreaker != null
                ? circuitBreaker.executeCheckedSupplier(() -> doSend(function))
                : doSend(function);
        } catch (Throwable e) {
            log.error("onchain send failed via circuit breaker (fn={}): {}", function.getName(), e.getMessage());
            return Optional.empty();
        }
    }

    // doSend deixa exceções propagarem para o circuit breaker registrá-las
    private Optional<String> doSend(Function function) throws Exception {
        String encoded = FunctionEncoder.encode(function);

        BigInteger nonce = web3j
            .ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.LATEST)
            .send()
            .getTransactionCount();

        RawTransaction tx = RawTransaction.createTransaction(
            nonce,
            GAS_PRICE,
            GAS_LIMIT,
            contractAddress,
            BigInteger.ZERO,
            encoded
        );

        byte[] signed   = TransactionEncoder.signMessage(tx, chainId, credentials);
        String hexValue = Numeric.toHexString(signed);

        EthSendTransaction response = web3j.ethSendRawTransaction(hexValue).send();

        if (response.hasError()) {
            throw new RuntimeException("onchain tx error: " + response.getError().getMessage());
        }

        String txHash = response.getTransactionHash();
        log.info("onchain tx sent: {} (fn={})", txHash, function.getName());
        return Optional.of(txHash);
    }

    // ─── Utils ────────────────────────────────────────────────────────────────

    /** Converts BigDecimal score (0–100.00) to uint32 scaled ×100 (e.g. 87.32 → 8732). */
    private static int scoreToUint32(BigDecimal score) {
        if (score == null) return 0;
        return score.multiply(BigDecimal.valueOf(100))
                    .intValue();
    }

    /** Packs a UUID into a 32-byte array (right-aligned, zero-padded left). */
    private static byte[] uuidToBytes32(UUID uuid) {
        byte[] result = new byte[32];
        ByteBuffer bb = ByteBuffer.wrap(result, 16, 16);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return result;
    }

    /** Returns keccak256 of a UTF-8 string as 32-byte array. */
    private static byte[] keccak256Bytes(String input) {
        byte[] inputBytes = input.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return org.web3j.crypto.Hash.sha3(inputBytes);
    }
}
