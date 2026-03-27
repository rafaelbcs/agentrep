package br.com.agentrep.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import br.com.agentrep.repository.IdempotencyKeyRepository;

/**
 * Previne re-submissão duplicada de outcomes via header Idempotency-Key.
 * Chave ausente/em branco é ignorada (warning no log) — não bloqueia no MVP.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;

    /**
     * Verifica se a chave já foi usada. Se não, registra e permite.
     * @throws DuplicateRequestException se a chave já existe
     */
    public void checkAndRegister(String key) {
        if (key == null || key.isBlank()) {
            log.debug("Idempotency-Key ausente — request permitido sem garantia de idempotência");
            return;
        }

        if (repository.existsByKey(key)) {
            throw new DuplicateRequestException("Idempotency-Key já utilizada: " + key);
        }

        repository.saveKey(key);
    }

    public static class DuplicateRequestException extends RuntimeException {
        public DuplicateRequestException(String message) {
            super(message);
        }
    }
}
