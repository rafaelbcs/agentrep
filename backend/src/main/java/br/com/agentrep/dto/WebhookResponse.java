package br.com.agentrep.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class WebhookResponse {
    private UUID id;
    private String url;
    private Set<String> events;
    private boolean active;
    private Instant createdAt;
    /** Only returned on creation */
    private String secret;
}
