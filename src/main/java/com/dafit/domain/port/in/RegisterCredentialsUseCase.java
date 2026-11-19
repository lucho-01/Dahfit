package com.dafit.domain.port.in;

import java.util.UUID;

/**
 * Puerto de entrada: asignar email y contrasena a un usuario existente.
 *
 * <p><b>R-02 (reclamacion de cuentas):</b>
 * <ul>
 *   <li>Volver a fijar la contrasena de una cuenta ya reclamada exige estar
 *       autenticado como ese usuario (o ser ADMIN). Antes bastaba conocer el Id
 *       de un atleta creado por un entrenador para quedarse con su cuenta.</li>
 *   <li>Cuando el alta la hace otra persona (un coach o un admin, o una peticion
 *       anonima con la seguridad encendida) la cuenta queda "pendiente de
 *       reclamacion": el login no la acepta hasta que su duenio entre con un
 *       codigo de un solo uso, que se devuelve una unica vez.</li>
 * </ul>
 */
public interface RegisterCredentialsUseCase {

    Result register(Command command);

    /**
     * @param actorId  usuario autenticado ({@code null} si el API esta abierto)
     * @param claimCode codigo anterior, obligatorio para reemplazar uno vigente
     * @return la cuenta creada y, solo si quedo pendiente, el codigo de reclamacion
     */
    record Command(UUID actorId, UUID userId, String email, String password, String claimCode) {
    }

    /**
     * @param claimCode codigo de reclamacion en claro; {@code null} si la cuenta
     *                  la creo su propio duenio
     */
    record Result(UUID userId, boolean pendingClaim, String claimCode) {
    }
}
