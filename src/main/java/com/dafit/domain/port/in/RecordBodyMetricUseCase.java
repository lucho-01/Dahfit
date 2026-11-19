package com.dafit.domain.port.in;

import com.dafit.domain.model.Kg;

import java.time.LocalDate;
import java.util.UUID;

/** Puerto de entrada: registrar una pesada (§5, perfil - peso historico). */
public interface RecordBodyMetricUseCase {

    /**
     * @param actorId usuario autenticado ({@code null} con el API abierto)
     * @param date    {@code null} = hoy en la zona horaria del usuario
     */
    UUID record(Command command);

    record Command(UUID actorId, UUID userId, Kg weightKg, LocalDate date) {
    }
}
