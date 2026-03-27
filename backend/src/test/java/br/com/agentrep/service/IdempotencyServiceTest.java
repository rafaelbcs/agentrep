package br.com.agentrep.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import br.com.agentrep.repository.IdempotencyKeyRepository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock IdempotencyKeyRepository repository;

    IdempotencyService service;

    @BeforeEach
    void setUp() {
        service = new IdempotencyService(repository);
    }

    @Test
    void newKey_isRegisteredAndAllowed() {
        when(repository.existsByKey(anyString())).thenReturn(false);

        assertThatNoException().isThrownBy(() -> service.checkAndRegister("key-abc-123"));

        verify(repository).saveKey("key-abc-123");
    }

    @Test
    void duplicateKey_throwsDuplicateRequestException() {
        when(repository.existsByKey("key-abc-123")).thenReturn(true);

        assertThatThrownBy(() -> service.checkAndRegister("key-abc-123"))
            .isInstanceOf(IdempotencyService.DuplicateRequestException.class)
            .hasMessageContaining("key-abc-123");

        verify(repository, never()).saveKey(anyString());
    }

    @Test
    void nullKey_isIgnored() {
        assertThatNoException().isThrownBy(() -> service.checkAndRegister(null));

        verifyNoInteractions(repository);
    }

    @Test
    void blankKey_isIgnored() {
        assertThatNoException().isThrownBy(() -> service.checkAndRegister("  "));

        verifyNoInteractions(repository);
    }
}
