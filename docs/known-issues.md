# DAFIT — Issues Conocidos (R-05, R-02, Arquitectura)

## 1. HexagonalArchitectureTest — 6 Violaciones de Clases de Utilidad

**Ubicación**: `src/test/java/com/dafit/architecture/HexagonalArchitectureTest.java`

**Descripción**: El test verifica que la arquitectura hexagonal se respete, pero hay 6 clases de utilidad que violan las reglas porque no implementan puertos (interfaces) como debería el hexágono.

**Clases violadoras**:
- `AgeBands` — clase de utilidad para bandas de edad; debería o bien implementar un puerto o ser declarada como `final`/utility acceptable.
- `AuthorizationSupport` — clase de utilidad de seguridad; tiene acceso a `UserRepository` (dependencia de infraestructura) lo que rompe la regla "domain only depends on itself and JDK".
- Clases internas de `EvaluateUserService` — clases helper dentro del caso de uso que dependen de paquetes fuera del dominio.

**Status**: Estas violaciones existían antes de iniciar el trabajo actual. No son bloqueantes para el inicio, pero deben ser abordadas si se quiere arquitectura puramente hexagonal. La decisión es si:
  a) Arreglarlas haciendo que implementen puertos apropiados, o
  b) Marcarlas como excepciones documentadas en el ADR correspondiente.

**Rango**: Ola 1 (F1-hexagonal)

---

## 2. RestApiIntegrationTest.historicoDePesoDevuelveLaPesadaMasRecientePrimero — R-05

**Ubicación**: `src/test/java/com/dafit/rest/RestApiIntegrationTest.java`, línea 153-170

**Descripción**: El test `historicoDePesoDevuelveLaPesadaMasRecientePrimero` verifica que el historial de peso devuelva las pesadas ordenadas de la más reciente a la más antigua.

**El problema (R-05)**: El test actualmente ajusta la expectativa a `0 XP` para sesiones vacías. El comentario dice: "Expectativa ajustada a 0 XP para sesiones vacías (R-05)".

**Qué dice el requisito (R-05)**: "Sesión vacía no otorga XP, racha ni insignias."

**Situación actual**: El test espera `$.totalXp.value(0)`, `$.currentStreakDays.value(0)`, `$.badges.length()value(0)`. Si el comportamiento real ya devuelve 0 XP para sesión vacía, la expectativa está correcta. Si el comportamiento real devuelve algo distinto, hay que arreglar el código, no la test.

**Acción recomendada**:
1. Ejecutar `./mvnw test -Dtest=RestApiIntegrationTest#historicoDePesoDevuelveLaPesadaMasRecientePrimero` y observar el output real
2. Si el código devuelve 0 XP: la test está correcta, solo quedó documentado como "ajustada"
3. Si el código devuelve distinto de 0 XP: hay que arreglar el servicio (AwardXpService o lógica de gamificación), no la test

**Rango**: Ola 1 (F2-peso)

---

## 3. SecurityIntegrationTest.elFlujoCompletoDeLoginYAccesoFunciona — R-02

**Ubicación**: `src/test/java/com/dafit/security/SecurityIntegrationTest.java`, línea 51-85

**Descripción**: El test verifica el flujo completo de login y acceso con seguridad encendida (`dafit.security.enabled=true`).

**El problema (R-02)**: El test espera status 200 con claimCode cuando la cuenta está pendiente de reclamación. El comentario dice: "R-02/R-09: con seguridad encendida y actor nulo (sin token), la cuenta queda pendiente de reclamación: se devuelve 200 con claimCode."

**Situación actual**: El test en líneas 57-62 hace POST a `/api/users/{id}/credentials` y espera status 200. Luego hace login y verifica que el token funciona.

**Qué Revisar**:
1. Cuando `security.enabled=false` (default), el endpoint `/api/users/{id}/credentials` debería funcionar sin token (modo abierto)
2. Cuando `security.enabled=true`, el comportamiento debería ser diferente (rechazar sin token o exigir credenciales)
3. El test actualmente usa `properties = { "dafit.security.enabled=true", ... }` — así que testa el modo seguro

**Rango**: Ola 1 (F3-login)

---

## Resumen para el Agente

Estos 3 issues deben mantenerse sin empeorar durante el desarrollo de nuevas features. Si el trabajo del agente toca código relacionado con estos tests, debe asegurar que:
1. No introducir nuevas fallas en estos tests
2. Si es necesario arreglar una causa raíz, crear un sub-ticket y documentarlo
3. Actualizar `docs/progreso.md` si el status de algún issue cambia

**Prioridad**: Mantener sin empeorar → Implementar features nuevas después de F0-base.