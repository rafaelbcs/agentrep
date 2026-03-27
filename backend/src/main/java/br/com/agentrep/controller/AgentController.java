package br.com.agentrep.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import br.com.agentrep.dto.AgentRegisterRequest;
import br.com.agentrep.dto.AgentRegisterResponse;
import br.com.agentrep.service.AgentService;

@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PostMapping("/register")
    public ResponseEntity<AgentRegisterResponse> register(@Valid @RequestBody AgentRegisterRequest request) {
        return ResponseEntity.status(201).body(agentService.register(request));
    }
}
