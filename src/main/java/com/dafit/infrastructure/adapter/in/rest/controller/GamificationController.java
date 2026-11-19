package com.dafit.infrastructure.adapter.in.rest.controller;

import com.dafit.domain.port.in.GetGamificationUseCase;
import com.dafit.infrastructure.adapter.in.rest.dto.response.GamificationResponse;
import com.dafit.infrastructure.adapter.in.rest.mapper.ResponseMapper;
import com.dafit.infrastructure.adapter.in.rest.security.Actors;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** XP, nivel, racha e insignias del usuario (C-19). */
@RestController
@RequestMapping("/api/users/{userId}/gamification")
public class GamificationController {

    private final GetGamificationUseCase gamification;

    public GamificationController(GetGamificationUseCase gamification) {
        this.gamification = gamification;
    }

    @GetMapping
    public GamificationResponse summary(Authentication authentication,
                                        @PathVariable UUID userId) {
        return ResponseMapper.gamification(gamification.summaryFor(
                Actors.actorId(authentication), userId));
    }
}
