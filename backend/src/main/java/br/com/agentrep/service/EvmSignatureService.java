package br.com.agentrep.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Valida assinaturas EVM (Ethereum personal_sign / eth_sign prefixado).
 * Wallets assinam com o prefixo: "\x19Ethereum Signed Message:\n{len}{message}".
 *
 * Assinatura ausente/em branco é ignorada (warning no log) — não bloqueia no MVP.
 */
@Service
@Slf4j
public class EvmSignatureService {

    /**
     * Verifica se {@code signature} foi produzida por {@code expectedAddress}
     * ao assinar {@code message} com personal_sign.
     *
     * @throws SecurityException se a assinatura não bater com o endereço esperado
     */
    public void verify(String message, String signature, String expectedAddress) {
        if (signature == null || signature.isBlank()) {
            log.warn("requesterSignature ausente — outcome aceito sem verificação EVM ({})", expectedAddress);
            return;
        }

        try {
            byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
            byte[] sigBytes = Numeric.hexStringToByteArray(signature);

            if (sigBytes.length != 65) {
                throw new SecurityException("Signature inválida: comprimento incorreto (" + sigBytes.length + " bytes)");
            }

            Sign.SignatureData sigData = new Sign.SignatureData(
                new byte[]{sigBytes[64]},          // v
                Arrays.copyOfRange(sigBytes, 0, 32),  // r
                Arrays.copyOfRange(sigBytes, 32, 64)  // s
            );

            BigIntegerWrapper key = recoverKey(msgBytes, sigData);
            String recovered = ("0x" + Keys.getAddress(key.value())).toLowerCase();
            String expected  = expectedAddress.toLowerCase();

            if (!recovered.equals(expected)) {
                throw new SecurityException(
                    "Signature mismatch: expected " + expected + " but recovered " + recovered
                );
            }

        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new SecurityException("Erro ao verificar assinatura EVM: " + e.getMessage(), e);
        }
    }

    private BigIntegerWrapper recoverKey(byte[] msgBytes, Sign.SignatureData sigData) throws Exception {
        java.math.BigInteger key = Sign.signedPrefixedMessageToKey(msgBytes, sigData);
        return new BigIntegerWrapper(key);
    }

    private record BigIntegerWrapper(java.math.BigInteger value) {}
}
