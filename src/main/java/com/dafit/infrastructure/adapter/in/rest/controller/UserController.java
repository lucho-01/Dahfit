package com.dafit.infrastructure.adapter.in.rest.controller;

import com.dafit.application.usecase.AuthorizationSupport;
import com.dafit.domain.port.in.GetUserUseCase;
import com.dafit.domain.port.in.RegisterUserUseCase;
import com.dafit.infrastructure.adapter.in.rest.dto.request.RegisterUserRequest;
import com.dafit.infrastructure.adapter.in.rest.dto.response.IdResponse;
import com.dafit.infrastructure.adapter.in.rest.dto.response.UserResponse;
import com.dafit.infrastructure.adapter.in.rest.mapper.RequestMapper;
import com.dafit.infrastructure.adapter.in.rest.mapper.ResponseMapper;
import com.dafit.infrastructure.adapter.in.rest.security.Actors;
import com.dafit.infrastructure.config.DafitProperties;
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

import java.util.UUID;

/** Alta y consulta de usuarios (MVP obligatorio). */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final RegisterUserUseCase registerUser;
    private final GetUserUseCase getUser;
    private final AuthorizationSupport authorization;
    private final DafitProperties properties;

    public UserController(RegisterUserUseCase registerUser,
                          GetUserUseCase getUser,
                          AuthorizationSupport authorization,
                          DafitProperties properties) {
        this.registerUser = registerUser;
        this.getUser = getUser;
        this.authorization = authorization;
        this.properties = properties;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse register(Authentication authentication,
                               @Valid @RequestBody RegisterUserRequest request) {
        UUID actorId = Actors.actorId(authentication);
        // R-02: el alta publica solo puede crear atletas. Un actor autenticado
        // que no sea ADMIN tampoco puede fabricarse un coach o un admin.
        boolean selfSignup = actorId == null
                ? properties.security().enabled()
                : !authorization.isAdmin(actorId);
        return IdResponse.of(registerUser.register(
                RequestMapper.toCommand(request, selfSignup)));
    }

    @GetMapping("/{userId}")
    public UserResponse byId(Authentication authentication, @PathVariable UUID userId) {
        return ResponseMapper.user(getUser.byId(Actors.actorId(authentication), userId));
    }
}
