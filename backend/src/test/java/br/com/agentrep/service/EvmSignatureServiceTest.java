package br.com.agentrep.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

class EvmSignatureServiceTest {

    EvmSignatureService service;

    // Chave privada de teste (Hardhat account #0 — nunca usar em produção)
    static final String TEST_PRIVATE_KEY =
        "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80";

    Credentials credentials;
    String expectedAddress;

    @BeforeEach
    void setUp() {
        service     = new EvmSignatureService();
        credentials = Credentials.create(TEST_PRIVATE_KEY);
        expectedAddress = credentials.getAddress().toLowerCase();
    }

    @Test
    void validSignature_passes() {
        String message   = "AgentRep:test-message";
        String signature = sign(message);

        assertThatNoException()
            .isThrownBy(() -> service.verify(message, signature, expectedAddress));
    }

    @Test
    void wrongSigner_throwsSecurityException() {
        String message   = "AgentRep:test-message";
        String signature = sign(message);
        String wrongAddr = "0x0000000000000000000000000000000000000001";

        assertThatThrownBy(() -> service.verify(message, signature, wrongAddr))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("Signature mismatch");
    }

    @Test
    void tamperedMessage_throwsSecurityException() {
        String original  = "AgentRep:original";
        String tampered  = "AgentRep:tampered";
        String signature = sign(original);

        assertThatThrownBy(() -> service.verify(tampered, signature, expectedAddress))
            .isInstanceOf(SecurityException.class);
    }

    @Test
    void nullSignature_isIgnored() {
        assertThatNoException()
            .isThrownBy(() -> service.verify("any-message", null, expectedAddress));
    }

    @Test
    void blankSignature_isIgnored() {
        assertThatNoException()
            .isThrownBy(() -> service.verify("any-message", "", expectedAddress));
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private String sign(String message) {
        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
        Sign.SignatureData sig = Sign.signPrefixedMessage(msgBytes,
            ECKeyPair.create(Numeric.toBigInt(TEST_PRIVATE_KEY)));
        byte[] sigBytes = new byte[65];
        System.arraycopy(sig.getR(), 0, sigBytes, 0,  32);
        System.arraycopy(sig.getS(), 0, sigBytes, 32, 32);
        sigBytes[64] = sig.getV()[0];
        return Numeric.toHexString(sigBytes);
    }
}
