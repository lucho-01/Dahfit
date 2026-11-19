package com.dafit.application.usecase;

import com.dafit.application.usecase.AuthorizationSupport;
import com.dafit.domain.model.BodyMetric;
import com.dafit.domain.port.in.GetBodyMetricHistoryUseCase;
import com.dafit.domain.port.out.BodyMetricRepository;

import java.util.List;
import java.util.UUID;

/** Caso de uso: historico de peso corporal. */
public class GetBodyMetricHistoryService implements GetBodyMetricHistoryUseCase {

    private final BodyMetricRepository bodyMetrics;
    private final AuthorizationSupport authorization;

    public GetBodyMetricHistoryService(BodyMetricRepository bodyMetrics,
                                       AuthorizationSupport authorization) {
        this.bodyMetrics = bodyMetrics;
        this.authorization = authorization;
    }

    @Override
    public List<BodyMetric> history(UUID actorId, UUID userId) {
        authorization.checkSameUser(actorId, userId);
        return bodyMetrics.findHistory(userId);
    }
}
