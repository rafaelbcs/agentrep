package xyz.agentrep.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.agentrep.dto.ReputationResponse;
import xyz.agentrep.service.ReputationService;

@RestController
@RequestMapping("/api/v1/widget")
@RequiredArgsConstructor
public class WidgetController {

    private final ReputationService reputationService;

    @GetMapping("/agent/{address}")
    public ResponseEntity<ReputationResponse> widgetData(@PathVariable String address) {
        return ResponseEntity.ok(reputationService.getReputation(address, null));
    }
}
