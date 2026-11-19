package com.dafit.infrastructure.adapter.in.rest.controller;

import com.dafit.domain.port.in.RegisterCredentialsUseCase;
import com.dafit.infrastructure.adapter.in.rest.dto.request.RegisterCredentialsRequest;
import com.dafit.infrastructure.adapter.in.rest.dto.response.RegisterCredentialsResponse;
import com.dafit.infrastructure.adapter.in.rest.mapper.RequestMapper;
import com.dafit.infrastructure.adapter.in.rest.security.Actors;
import com.dafit.infrastructure.config.DafitProperties;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Credenciales de un usuario.
 *
 * <p>Va aparte del alta de perfil: un entrenador puede crear atletas sin login,
 * y un usuario puede anadir email/contrasena despues.
 *
 * <p><b>R-02:</b> si la cuenta la crea otra persona, la respuesta incluye el
 * codigo de reclamacion (una sola vez). Antes el controlador devolvia 204 y el
 * codigo se perdia, con lo que la cuenta quedaba inutilizable. El perfil creado
 * por su propio duenio sigue respondiendo 204.
 */
@RestController
@RequestMapping("/api/users/{userId}/credentials")
public class CredentialsController {

    private final RegisterCredentialsUseCase registerCredentials;
    private final DafitProperties properties;

    public CredentialsController(RegisterCredentialsUseCase registerCredentials,
                                 DafitProperties properties) {
        this.registerCredentials = registerCredentials;
        this.properties = properties;
    }

    @PostMapping
    public ResponseEntity<RegisterCredentialsResponse> register(
            Authentication authentication,
            @PathVariable UUID userId,
            @Valid @RequestBody RegisterCredentialsRequest request) {

        UUID actorId = Actors.actorId(authentication);
        // Modo abierto (seguridad apagada): el flujo es de confianza y el
        // usuario actua como duenio de su propia cuenta. Con la seguridad
        // encendida, un actor nulo significa que la cuenta la crea otro.
        if (actorId == null && !properties.security().enabled()) {
            actorId = userId;
        }

        RegisterCredentialsUseCase.Result result = registerCredentials.register(
                RequestMapper.toCommand(actorId, userId, request));

        if (!result.pendingClaim()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(new RegisterCredentialsResponse(
                result.userId(), true, result.claimCode()));
    }
}
