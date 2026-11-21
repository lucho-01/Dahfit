package com.dafit.application.usecase;

import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.ClaimCode;
import com.dafit.domain.model.UserCredentials;
import com.dafit.domain.port.in.RegisterCredentialsUseCase;
import com.dafit.domain.port.out.ClaimCodeRepository;
import com.dafit.domain.port.out.CredentialsRepository;
import com.dafit.domain.port.out.PasswordHasher;
import com.dafit.domain.port.out.UserRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Caso de uso: asignar credenciales a un usuario existente (R-02).
 *
 * <p>Reglas:
 * <ol>
 *   <li>El usuario debe existir.</li>
 *   <li>Email unico y cliente-side validado; la contrasena 8-72.</li>
 *   <li>Reemplazar una cuenta ya reclamada exige ser su duenio o ADMIN. Esta es
 *       la regla que cierra la reclamacion de cuentas ajena.</li>
 *   <li>Reemplazar una cuenta PENDIENTE de otra persona exige el codigo de
 *       reclamacion vigente (reemitir uno caducado no lo exige).</li>
 *   <li>Si la cuenta la crea alguien que no es su duenio, queda pendiente y se
 *       reemite un codigo que se devuelve una unica vez.</li>
 * </ol>
 */
public class RegisterCredentialsService implements RegisterCredentialsUseCase {

    public static final int MIN_PASSWORD_LENGTH = 8;
    /** BCrypt ignora bytes a partir del 72: se rechaza en vez de truncar en silencio. */
    public static final int MAX_PASSWORD_LENGTH = 72;
    public static final int CLAIM_CODE_TTL_MINUTES = 30;

    private final UserRepository users;
    private final CredentialsRepository credentials;
    private final ClaimCodeRepository claimCodes;
    private final PasswordHasher hasher;
    private final AuthorizationSupport authorization;
    private final SecureRandom random = new SecureRandom();

    public RegisterCredentialsService(UserRepository users,
                                      CredentialsRepository credentials,
                                      ClaimCodeRepository claimCodes,
                                      PasswordHasher hasher,
                                      AuthorizationSupport authorization) {
        this.users = users;
        this.credentials = credentials;
        this.claimCodes = claimCodes;
        this.hasher = hasher;
        this.authorization = authorization;
    }

    @Override
    public Result register(Command command) {
        UUID userId = command.userId();
        users.findById(userId)
                .orElseThrow(() -> new DomainException.NotFound("usuario no encontrado: " + userId));

        String email = UserCredentials.normalizeEmail(command.email());
        validatePassword(command.password());

        Instant now = Instant.now();
        boolean self = command.actorId() != null && command.actorId().equals(userId);
        boolean admin = command.actorId() != null && authorization.isAdmin(command.actorId());

        UserCredentials existing = credentials.findByUserId(userId).orElse(null);
        if (existing != null && !self && !admin) {
            applyReplacementRules(existing, command.claimCode(), now);
        }

        credentials.findByEmail(email)
                .filter(other -> !other.userId().equals(userId))
                .ifPresent(other -> {
                    throw new DomainException.Conflict("el email ya esta registrado");
                });

        // El duenio reclama su cuenta en el acto; si la crea otro (coach, admin
        // o un registro anonimo con la seguridad encendida), queda pendiente.
        boolean claimed = self;

        credentials.save(new UserCredentials(
                userId, email, hasher.hash(command.password()), now, claimed ? now : null));

        if (claimed) {
            claimCodes.deleteByUserId(userId);
            return new Result(userId, false, null);
        }

        String code = newCode();
        claimCodes.save(new ClaimCode(hash(code), ClaimCode.defaultExpiry(now, CLAIM_CODE_TTL_MINUTES)));
        return new Result(userId, true, code);
    }

    private void applyReplacementRules(UserCredentials existing, String providedCode, Instant now) {
        if (existing.isClaimed()) {
            throw new DomainException.Forbidden(
                    "la cuenta ya pertenece a su titular; solo su duenio puede cambiar la contrasena");
        }
        ClaimCode stored = claimCodes.findByUserId(existing.userId());
        if (stored == null) {
            throw new DomainException.Conflict("la cuenta esta pendiente de reclamacion");
        }
        if (stored.canBeReplacedAt(now)) {
            return;
        }
        if (providedCode == null || providedCode.isBlank()
                || !stored.codeHash().equals(hash(providedCode))) {
            throw new DomainException.Forbidden("codigo de reclamacion invalido");
        }
    }

    private String newCode() {
        byte[] bytes = new byte[18];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String code) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(
                    digest.digest(code.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 no disponible", impossible);
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new DomainException.Validation(
                    "la contrasena debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new DomainException.Validation(
                    "la contrasena no puede superar " + MAX_PASSWORD_LENGTH + " caracteres");
        }
    }
}
