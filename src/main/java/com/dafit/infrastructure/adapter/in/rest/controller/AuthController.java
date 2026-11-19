package com.dafit.infrastructure.adapter.in.rest.controller;

import com.dafit.domain.port.in.LoginUseCase;
import com.dafit.infrastructure.adapter.in.rest.dto.request.LoginRequest;
import com.dafit.infrastructure.adapter.in.rest.dto.response.LoginResponse;
import com.dafit.infrastructure.adapter.in.rest.mapper.RequestMapper;
import com.dafit.infrastructure.adapter.in.rest.mapper.ResponseMapper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Login: unico endpoint publico que emite tokens. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginUseCase login;

    public AuthController(LoginUseCase login) {
        this.login = login;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return ResponseMapper.login(login.login(RequestMapper.toCommand(request)));
    }
}
