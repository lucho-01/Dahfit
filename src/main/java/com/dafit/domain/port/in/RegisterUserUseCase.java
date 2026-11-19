package com.dafit.domain.port.in;

import com.dafit.domain.model.Kg;
import com.dafit.domain.model.Sex;
import com.dafit.domain.model.UserRole;
import com.dafit.domain.model.WeightUnit;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/** Puerto de entrada: alta de usuario (§3, MVP obligatorio). */
public interface RegisterUserUseCase {

    UUID register(Command command);

    /**
     * @param selfSignup el adaptador lo marca cuando la peticion no esta
     *                   autenticada y la seguridad esta activa. En ese caso no
     *                   se permiten roles distintos de ATHLETE (R-02).
     */
    record Command(
            String displayName,
            UserRole role,
            Sex sex,
            LocalDate birthDate,
            Kg weightKg,
            WeightUnit preferredUnit,
            ZoneId timeZone,
            boolean benchmarkConsent,
            boolean selfSignup
    ) {
    }
}
