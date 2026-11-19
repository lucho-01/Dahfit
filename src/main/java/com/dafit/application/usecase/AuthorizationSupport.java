package com.dafit.application.usecase;

import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.User;
import com.dafit.domain.model.UserRole;
import com.dafit.domain.port.out.UserRepository;

import java.util.UUID;

/**
 * Reglas de acceso por propietario compartidas por los casos de uso (R-03).
 *
 * <p>Antes, los controladores tomaban el {@code userId} solo de la ruta: con la
 * seguridad encendida cualquier token valido podia leer o modificar recursos de
 * otro usuario. La comprobacion vive aqui, en la capa de aplicacion, y no en el
 * adaptador REST, para que valga igual cuando el caso de uso lo invoque un CLI,
 * un job o un test.
 *
 * <p>Convenciones:
 * <ul>
 *   <li>{@code actorId == null} significa "peticion anonima" (el API esta
 *       abierto con {@code dafit.security.enabled=false}); se permite, porque el
 *       modo abierto es una decision deliberada de configuracion.</li>
 *   <li>Con actor, solo el propio usuario o un {@code ADMIN} acceden.</li>
 *   <li>Un {@code COACH} solo accede a los atletas que tienen una rutina suya
 *       asignada ({@code ownsRoutineOf}), lo que exige cargar las rutinas y por
 *       eso no se mezcla con el acceso general.</li>
 *   <li>Se responde {@code Forbidden} (403) para dar un mensaje claro en
 *       desarrollo; en produccion puede cambiarse a {@code NotFound} para no
 *       revelar la existencia del recurso.</li>
 * </ul>
 */
public final class AuthorizationSupport {

    private final UserRepository users;

    public AuthorizationSupport(UserRepository users) {
        this.users = users;
    }

    /**
     * Exactamente el mismo usuario (o un ADMIN).
     *
     * @throws DomainException.Forbidden si el actor es otro usuario
     */
    public void checkSameUser(UUID actorId, UUID targetUserId) {
        if (actorId == null) {
            return;
        }
        if (actorId.equals(targetUserId)) {
            return;
        }
        if (isAdmin(actorId)) {
            return;
        }
        throw new DomainException.Forbidden(
                "el token no corresponde al usuario " + targetUserId);
    }

    /**
     * Acceso a un dato "propio por defecto": el propio usuario, un ADMIN o un
     * entrenador. Un coach comparte el catalogo de rutinas y puede crear
     * rutinas para sus atletas; las rutinas asignadas se consultan aparte.
     */
    public void checkAthleteAccess(UUID actorId, UUID targetUserId) {
        if (actorId == null || actorId.equals(targetUserId) || isAdmin(actorId) || isCoach(actorId)) {
            return;
        }
        throw new DomainException.Forbidden(
                "el token no corresponde al usuario " + targetUserId);
    }

    /** El actor es el propietario de la rutina o un ADMIN. */
    public void checkOwner(UUID actorId, UUID ownerId) {
        if (actorId == null || actorId.equals(ownerId) || isAdmin(actorId)) {
            return;
        }
        throw new DomainException.Forbidden("solo el propietario de la rutina puede modificarla");
    }

    /** El actor puede operar sobre el atleta: si es coach, debe ser su entrenador. */
    public void checkCoachScope(UUID actorId, UUID athleteId, boolean trainsWithActor) {
        if (actorId == null || actorId.equals(athleteId) || isAdmin(actorId)) {
            return;
        }
        if (isCoach(actorId) && trainsWithActor) {
            return;
        }
        throw new DomainException.Forbidden(
                "el token no puede operar sobre el usuario " + athleteId);
    }

    public boolean isAdmin(UUID userId) {
        return users.findById(userId)
                .map(User::isAdmin)
                .orElse(false);
    }

    public boolean isCoach(UUID userId) {
        return users.findById(userId)
                .map(User::isCoach)
                .orElse(false);
    }
}
