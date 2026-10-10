package com.shopsmart.controller;

import com.shopsmart.ai.RecommendationService;
import com.shopsmart.dto.Dtos.KnowledgeRequest;
import com.shopsmart.dto.Dtos.KnowledgeResponse;
import com.shopsmart.rag.KnowledgeService;
import com.shopsmart.dto.Dtos.RecommendationRequest;
import com.shopsmart.dto.Dtos.RecommendationResponse;
import com.shopsmart.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {
    private final RecommendationService recommendations;
    private final KnowledgeService knowledge;

    @PostMapping("/recommendations")
    public RecommendationResponse recommend(@AuthenticationPrincipal User u, @Valid @RequestBody RecommendationRequest r) {
        return recommendations.recommend(u, r.query().trim());
    }

    @PostMapping("/knowledge/ask")
    public KnowledgeResponse ask(@Valid @RequestBody KnowledgeRequest r) {
        return knowledge.ask(r.question().trim());
    }
}
