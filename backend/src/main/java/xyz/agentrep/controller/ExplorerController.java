package xyz.agentrep.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.agentrep.model.Agent;
import xyz.agentrep.service.AgentService;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/explore")
@RequiredArgsConstructor
public class ExplorerController {

    private final AgentService agentService;

    @GetMapping
    public ResponseEntity<Page<Agent>> explore(
            @RequestParam(defaultValue = "all") String category,
            @RequestParam(defaultValue = "0") BigDecimal minScore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(agentService.explore(category, minScore,
            PageRequest.of(page, size, Sort.by("score").descending())));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<Page<Agent>> leaderboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(agentService.findLeaderboard(
            PageRequest.of(page, size, Sort.by("score").descending())));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Agent>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(agentService.search(q,
            PageRequest.of(page, size, Sort.by("score").descending())));
    }
}
