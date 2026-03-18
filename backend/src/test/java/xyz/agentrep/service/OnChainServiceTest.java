package xyz.agentrep.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import xyz.agentrep.model.Outcome;
import xyz.agentrep.model.OutcomeVerdict;
import xyz.agentrep.model.Agent;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class OnChainServiceTest {

    @Mock Web3j web3j;

    private static final String VALID_CONTRACT  = "0x1234567890abcdef1234567890abcdef12345678";
    private static final String ZERO_CONTRACT   = "0x0000000000000000000000000000000000000000";
    private static final String VALID_PRIV_KEY  =
        "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80"; // Hardhat #0
    private static final long   CHAIN_ID        = 84532L;

    // ─── disabled mode ────────────────────────────────────────────────────────

    @Test
    void isDisabled_whenContractIsZeroAddress() {
        var service = new OnChainService(web3j, VALID_PRIV_KEY, ZERO_CONTRACT, CHAIN_ID, null);
        assertThat(service.isEnabled()).isFalse();
    }

    @Test
    void isDisabled_whenPrivateKeyIsBlank() {
        var service = new OnChainService(web3j, "", VALID_CONTRACT, CHAIN_ID, null);
        assertThat(service.isEnabled()).isFalse();
    }

    @Test
    void registerAgent_returnsEmpty_whenDisabled() {
        var service = new OnChainService(web3j, "", VALID_CONTRACT, CHAIN_ID, null);
        Optional<String> result = service.registerAgent("0xabc");
        assertThat(result).isEmpty();
        verifyNoInteractions(web3j);
    }

    @Test
    void registerOutcome_returnsEmpty_whenDisabled() {
        var service = new OnChainService(web3j, "", VALID_CONTRACT, CHAIN_ID, null);
        Optional<String> result = service.registerOutcome(buildOutcome(), BigDecimal.valueOf(85), 10, 8);
        assertThat(result).isEmpty();
        verifyNoInteractions(web3j);
    }

    // ─── enabled mode — happy path ─────────────────────────────────────────────

    @Test
    void registerAgent_returnsTxHash_onSuccess() throws Exception {
        var service = new OnChainService(web3j, VALID_PRIV_KEY, VALID_CONTRACT, CHAIN_ID, null);
        mockSuccessfulTransaction("0xdeadbeef01");

        Optional<String> result = service.registerAgent("0xabcdef1234567890abcdef1234567890abcdef12");

        assertThat(result).isPresent().hasValue("0xdeadbeef01");
    }

    @Test
    void registerOutcome_returnsTxHash_onSuccess() throws Exception {
        var service = new OnChainService(web3j, VALID_PRIV_KEY, VALID_CONTRACT, CHAIN_ID, null);
        mockSuccessfulTransaction("0xdeadbeef02");

        Optional<String> result = service.registerOutcome(buildOutcome(), BigDecimal.valueOf(87.32), 10, 9);

        assertThat(result).isPresent().hasValue("0xdeadbeef02");
    }

    @Test
    void recordDispute_returnsTxHash_onSuccess() throws Exception {
        var service = new OnChainService(web3j, VALID_PRIV_KEY, VALID_CONTRACT, CHAIN_ID, null);
        mockSuccessfulTransaction("0xdeadbeef03");

        Optional<String> result = service.recordDispute(
            "0xabcdef1234567890abcdef1234567890abcdef12", UUID.randomUUID());

        assertThat(result).isPresent();
    }

    // ─── error handling ────────────────────────────────────────────────────────

    @Test
    void registerAgent_returnsEmpty_onIoException() throws Exception {
        var service = new OnChainService(web3j, VALID_PRIV_KEY, VALID_CONTRACT, CHAIN_ID, null);
        mockNonceThrows(new IOException("RPC unavailable"));

        Optional<String> result = service.registerAgent("0xabcdef1234567890abcdef1234567890abcdef12");

        assertThat(result).isEmpty();
    }

    @Test
    void registerAgent_returnsEmpty_whenTxHasError() throws Exception {
        var service = new OnChainService(web3j, VALID_PRIV_KEY, VALID_CONTRACT, CHAIN_ID, null);
        mockTransactionWithError("execution reverted");

        Optional<String> result = service.registerAgent("0xabcdef1234567890abcdef1234567890abcdef12");

        assertThat(result).isEmpty();
    }

    @Test
    void neverThrows_onAnyFailure() {
        var service = new OnChainService(web3j, VALID_PRIV_KEY, VALID_CONTRACT, CHAIN_ID, null);
        // web3j is mocked but not configured — will throw NullPointerException internally
        assertThatCode(() -> service.registerAgent("0xabc")).doesNotThrowAnyException();
        assertThatCode(() -> service.registerOutcome(buildOutcome(), BigDecimal.ZERO, 0, 0))
            .doesNotThrowAnyException();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockSuccessfulTransaction(String txHash) throws Exception {
        // Mock nonce — use raw Request to avoid wildcard capture issues
        org.web3j.protocol.core.Request nonceReq = mock(org.web3j.protocol.core.Request.class);
        EthGetTransactionCount nonceResp = mock(EthGetTransactionCount.class);
        when(nonceResp.getTransactionCount()).thenReturn(BigInteger.ONE);
        when(nonceReq.send()).thenReturn(nonceResp);
        doReturn(nonceReq).when(web3j).ethGetTransactionCount(any(), any());

        // Mock send tx
        org.web3j.protocol.core.Request txReq = mock(org.web3j.protocol.core.Request.class);
        EthSendTransaction txResp = mock(EthSendTransaction.class);
        when(txResp.hasError()).thenReturn(false);
        when(txResp.getTransactionHash()).thenReturn(txHash);
        when(txReq.send()).thenReturn(txResp);
        doReturn(txReq).when(web3j).ethSendRawTransaction(any());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockNonceThrows(Exception ex) throws Exception {
        org.web3j.protocol.core.Request nonceReq = mock(org.web3j.protocol.core.Request.class);
        when(nonceReq.send()).thenThrow(ex);
        doReturn(nonceReq).when(web3j).ethGetTransactionCount(any(), any());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockTransactionWithError(String errorMessage) throws Exception {
        org.web3j.protocol.core.Request nonceReq = mock(org.web3j.protocol.core.Request.class);
        EthGetTransactionCount nonceResp = mock(EthGetTransactionCount.class);
        when(nonceResp.getTransactionCount()).thenReturn(BigInteger.ONE);
        when(nonceReq.send()).thenReturn(nonceResp);
        doReturn(nonceReq).when(web3j).ethGetTransactionCount(any(), any());

        org.web3j.protocol.core.Request txReq = mock(org.web3j.protocol.core.Request.class);
        EthSendTransaction txResp = mock(EthSendTransaction.class);
        org.web3j.protocol.core.Response.Error error = mock(org.web3j.protocol.core.Response.Error.class);
        when(error.getMessage()).thenReturn(errorMessage);
        when(txResp.hasError()).thenReturn(true);
        when(txResp.getError()).thenReturn(error);
        when(txReq.send()).thenReturn(txResp);
        doReturn(txReq).when(web3j).ethSendRawTransaction(any());
    }

    private Outcome buildOutcome() {
        Agent contractor = Agent.builder()
            .id(UUID.randomUUID())
            .walletAddress("0xabcdef1234567890abcdef1234567890abcdef12")
            .build();
        Agent requester = Agent.builder()
            .id(UUID.randomUUID())
            .walletAddress("0x9876543210fedcba9876543210fedcba98765432")
            .build();
        return Outcome.builder()
            .id(UUID.randomUUID())
            .contractorAgent(contractor)
            .requesterAgent(requester)
            .taskCategory("code-review")
            .verdict(OutcomeVerdict.SUCCESS)
            .build();
    }
}
