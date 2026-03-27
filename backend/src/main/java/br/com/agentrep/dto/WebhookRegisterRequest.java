package br.com.agentrep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class WebhookRegisterRequest {

    @NotBlank
    @Pattern(regexp = "^https?://.+", message = "URL must start with http:// or https://")
    private String url;

    @NotEmpty(message = "At least one event is required")
    private Set<String> events;

    /** Optional custom secret. If blank, one is generated. */
    @Size(min = 16, max = 64)
    private String secret;
}
