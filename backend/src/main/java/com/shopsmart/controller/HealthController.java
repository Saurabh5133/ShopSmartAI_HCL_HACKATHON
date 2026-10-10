package com.shopsmart.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HealthController {
    private final JdbcTemplate jdbc;

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "UP");
        try {
            jdbc.queryForObject("select 1", Integer.class);
            m.put("database", "UP");
        } catch (Exception e) {
            m.put("database", "DOWN");
        }
        m.put("timestamp", Instant.now().toString());
        return m;
    }
}
