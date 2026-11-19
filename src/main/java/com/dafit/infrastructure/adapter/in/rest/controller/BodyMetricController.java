package com.dafit.infrastructure.adapter.in.rest.controller;

import com.dafit.domain.port.in.GetBodyMetricHistoryUseCase;
import com.dafit.domain.port.in.RecordBodyMetricUseCase;
import com.dafit.infrastructure.adapter.in.rest.dto.request.RecordBodyMetricRequest;
import com.dafit.infrastructure.adapter.in.rest.dto.response.BodyMetricResponse;
import com.dafit.infrastructure.adapter.in.rest.dto.response.IdResponse;
import com.dafit.infrastructure.adapter.in.rest.mapper.RequestMapper;
import com.dafit.infrastructure.adapter.in.rest.mapper.ResponseMapper;
import com.dafit.infrastructure.adapter.in.rest.security.Actors;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Historico de peso corporal. */
@RestController
@RequestMapping("/api/users/{userId}/body-metrics")
public class BodyMetricController {

    private final RecordBodyMetricUseCase recordBodyMetric;
    private final GetBodyMetricHistoryUseCase history;

    public BodyMetricController(RecordBodyMetricUseCase recordBodyMetric,
                                GetBodyMetricHistoryUseCase history) {
        this.recordBodyMetric = recordBodyMetric;
        this.history = history;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse record(Authentication authentication,
                             @PathVariable UUID userId,
                             @Valid @RequestBody RecordBodyMetricRequest request) {
        return IdResponse.of(recordBodyMetric.record(
                RequestMapper.toCommand(Actors.actorId(authentication), userId, request)));
    }

    /** Mas reciente primero. Lista vacia si el usuario no ha registrado ninguna pesada. */
    @GetMapping
    public List<BodyMetricResponse> history(Authentication authentication,
                                            @PathVariable UUID userId) {
        return history.history(Actors.actorId(authentication), userId).stream()
                .map(ResponseMapper::bodyMetric)
                .toList();
    }
}
