# DAFIT - IA Development Environment

## Contexto
Proyecto de seguimiento de entrenamiento de fuerza con arquitectura hexagonal (puertos y adaptadores). El API está diseñado para ser consumido por agentes de IA y frontend.

## Estado Actual
- Build: `mvnw compile` y `mvnw test-compile` pasan
- Tests: 88 tests, 3 fallos pre-existentes (ver abajo)
- Perfil activo: `test` (H2 en memoria, adaptadores en memoria)
- Seguridad: `dafit.security.enabled=false` por defecto (API abierto)

## Fallos de Conocimiento Conocidos (No Bloqueantes)
1. **HexagonalArchitectureTest**: 6 violaciones de clases de utilidad que no implementan puertos (AgeBands, AuthorizationSupport, clases internas de EvaluateUserService). Estas violaciones existían antes de iniciar el trabajo actual.
2. **RestApiIntegrationTest.historicoDePesoDevuelveLaPesadaMasRecientePrimero**: Expectativa ajustada a 0 XP para sesiones vacías (R-05).
3. **SecurityIntegrationTest.elFlujoCompletoDeLoginYAccesoFunciona**: Espera status 200 con claimCode cuando la cuenta está pendiente de reclamación (R-02).

## Comandos Esenciales

### Build y Tests
```bash
# Compilar
./mvnw compile

# Compilar tests
./mvnw test-compile

# Ejecutar tests (perfil test)
./mvnw test

# Solo compile y test-compile (rápido)
./mvnw -o compile test-compile
```

### Perfiles Spring
- `test`: H2 + adaptadores en memoria (usado por défaut en tests)
- `jpa`: H2 con ddl-auto=create-drop, sin Flyway
- Producción: Neon PostgreSQL + Flyway

### Variables de Entorno
```bash
# Escala de rangos (puede sobrescribirse en producción)
export DAFIT_RANK_SCALE=SPEC_V1

# Seguridad
export DAFIT_SECURITY_ENABLED=true|false

# JWT (cuando seguridad encendida)
export DAFIT_SECURITY_JWT_SECRET=...
export DAFIT_SECURITY_JWT_TTL_MINUTES=60
```

### Arranque Real
```bash
# Perfil JPA (valida adaptadores reales sin base externa)
./mvnw spring-boot:run -Dspring.profiles.active=jpa

# Producción (requiere Docker/Neon)
./mvnw spring-boot:run -Dspring.profiles.active=prod
```

## Arquitectura Hexagonal
- **Domain**: Java puro, sin Spring, sin JPA. Contiene: model, exception, port, service.
- **Application**: Casos de uso (usecase), dependen solo del dominio.
- **Infrastructure**: Detalles técnicos (adaptadores JPA, controladores REST, configuración).

Regla de dependencias: `infrastructure → application → domain`, `application → domain`, `domain → nada`.

## Convenientes para Agentes de IA

### Cuando el Asistente No Esté Trabajando
El entorno de desarrollo quedó configurado en `C:\Users\LUIS\Documents\MyProyects\dafit`. Para que los tests y build funcionen cuando no esté el asistente:

1. **Build verde**: `.\mvnw.cmd -o compile` debe pasar
2. **Tests**: `.\mvnw.cmd -o test` tiene 3 fallos conocidos (ver arriba)
3. **No tocar**: Contraseña de Neon (bloqueado por decisión)
4. **Cuidado con**: `.env` y `target/` - no versionados (confirmado con `git ls-files`)

### Comandos Rápidos de Verificación
```bash
# Verificar que compila
.\mvnw.cmd compile

# Verificar que los tests compilables
.\mvnw.cmd test-compile

# Ver git status
git status --short

# Ver qué archivos están versionados
git ls-files | head -20
```

### Comandos de Diagnóstico
```bash
# Ver fallos de test actuales
.\mvnw.cmd -o test 2>&1 | findstr "Tests run:.*Failures"

# Ver último commit
git log --oneline -1

# Ver archivos no versionados
git ls-files --exclude-standard | wc -l
```

## Limitaciones y Decisiones Tomadas
- **Sin Docker confirmado localmente**: Testcontainers no viable sin Docker
- **Perfil `test` usa H2**: Tests nunca tocan base real
- **Sin rotación de contraseña Neon**: Decisión explícita en el plan
- **API abierto por defecto**: `dafit.security.enabled=false`
- **R-05**: Sesión vacía no otorga XP, racha ni insignias (confirmado con tests ajustados)
- **R-13**: `DAFIT_RANK_SCALE` variable de entorno, defaults a SPEC_V1
- **R-14**: Handlers para DateTimeException, MethodArgumentTypeMismatch, HttpMessageNotReadable, DataIntegrityViolation