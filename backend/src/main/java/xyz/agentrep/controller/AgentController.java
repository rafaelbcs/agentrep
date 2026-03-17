package xyz.agentrep.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.agentrep.dto.AgentRegisterRequest;
import xyz.agentrep.dto.AgentRegisterResponse;
import xyz.agentrep.service.AgentService;

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
