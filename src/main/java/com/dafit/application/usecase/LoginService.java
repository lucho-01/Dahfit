package com.dafit.application.usecase;

import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.ClaimCode;
import com.dafit.domain.model.UserCredentials;
import com.dafit.domain.port.in.LoginUseCase;
import com.dafit.domain.port.out.ClaimCodeRepository;
import com.dafit.domain.port.out.CredentialsRepository;
import com.dafit.domain.port.out.PasswordHasher;
import com.dafit.domain.port.out.TokenIssuer;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Caso de uso: login.
 *
 * <p>El mismo mensaje para email desconocido y contrasena incorrecta evita
 * enumerar cuentas. <b>R-16:</b> el coste de BCrypt se paga tambien cuando el
 * email no existe, para no filtrar la existencia de la cuenta por tiempos.
 *
 * <p><b>R-02:</b> una cuenta pendiente de reclamacion solo inicia sesion con su
 * codigo de un solo uso; al entrar queda reclamada y el codigo se consume.
 */
public class LoginService implements LoginUseCase {

    private static final String INVALID = "credenciales invalidas";
    /** Hash BCrypt de una contrasena aleatoria descartada: solo consume el tiempo del coste. */
    private static final String DUMMY_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final CredentialsRepository credentials;
    private final ClaimCodeRepository claimCodes;
    private final PasswordHasher hasher;
    private final TokenIssuer tokens;

    public LoginService(CredentialsRepository credentials,
                        ClaimCodeRepository claimCodes,
                        PasswordHasher hasher,
                        TokenIssuer tokens) {
        this.credentials = credentials;
        this.claimCodes = claimCodes;
        this.hasher = hasher;
        this.tokens = tokens;
    }

    @Override
    public Result login(Command command) {
        String email;
        try {
            email = UserCredentials.normalizeEmail(command.email());
        } catch (DomainException.Validation ex) {
            throw new DomainException.Unauthorized(INVALID);
        }

        UserCredentials stored = credentials.findByEmail(email).orElse(null);

        if (stored == null) {
            hasher.matches(command.password(), DUMMY_HASH);
            throw new DomainException.Unauthorized(INVALID);
        }
        if (command.password() == null || !hasher.matches(command.password(), stored.passwordHash())) {
            throw new DomainException.Unauthorized(INVALID);
        }

        if (!stored.isClaimed()) {
            claim(stored, command.claimCode());
        }

        Optional<TokenIssuer.TokenData> tokenOpt =
                tokens.issue(stored.userId(), stored.email(), Instant.now());
        TokenIssuer.TokenData token = tokenOpt.orElseThrow();
        return new Result(token.value(), "Bearer", token.expiresAt(), stored.userId());
    }

    private void claim(UserCredentials stored, String claimCode) {
        Instant now = Instant.now();
        ClaimCode pending = claimCodes.findByUserId(stored.userId());
        if (pending == null) {
            throw new DomainException.Forbidden(
                    "la cuenta esta pendiente de reclamacion; pide un codigo nuevo");
        }
        if (!pending.isValidAt(now)) {
            throw new DomainException.Forbidden("el codigo de reclamacion ha caducado");
        }
        if (claimCode == null || !pending.codeHash().equals(sha256(claimCode))) {
            throw new DomainException.Forbidden("codigo de reclamacion invalido");
        }
        credentials.save(stored.claim(now));
        claimCodes.deleteByUserId(stored.userId());
    }

    /** Hash SHA-256 en hexadecimal: mismo calculo que usa el alta de credenciales. */
    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 no disponible", impossible);
        }
    }

}
