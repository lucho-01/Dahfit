# DAFIT - Skills and Conventions

## Propósito
Documentación de skills, convenciones y lineamientos para cuándo el asistente de IA no esté disponible. Este archivo complementa a `AGENTS.md` con información técnica más específica.

## Arquitectura y Patrones

### Arquitectura Hexagonal (Puertos y Adaptadores)
- **Principio**: El dominio (capas internas) no debe conocer detalles de frameworks o bases de datos.
- **Dependencias permitidas**:
  - `infrastructure → application → domain`
  - `infrastructure → domain`
  - `application → domain`
  - `domain → nada`
- **Herramienta de verificación**: `HexagonalArchitectureTest` con ArchUnit (se ejecuta en cada build).

### Casos de Uso (Use Cases)
- Cada caso de uso implementa un **puerto de entrada** (interfaz en `domain.port.in`).
- La implementación vive en `application.usecase`, pero se declara como bean en `infrastructure.config.UseCaseConfig`.
- Los consumidores (REST, CLI, tests) dependen de la interfaz, no de la implementación.

### Excepciones Notables en el Paquete `application.usecase`
Estas clases viven en `com.dafit.application.usecase` pero **no** son casos de uso en el sentido hexagonal:
- `AgeBands`: Utilidad de etiquetado de edades (R-04)
- `AuthorizationSupport`: Reglas de acceso compartidas (R-03)
- Clases internas de `EvaluateUserService` (`MuscleStep`, `UserStep`): Resultado de evaluaciones musculares

Estas clases fueron movidas temporalmente al paquete `application.usecase` y el test de arquitectura las报告 (6 violaciones conocidas). Idealmente deberían moverse a `com.dafit.application.service`.

### Servicios de Dominio
- En `com.dafit.domain.service`: calculadores de e1RM, rangos, puntos, volumen, mejora, outliers.
- Son clases planas, sin `@Service`, instanciables sin contexto Spring.

## Patrones de Codificación

### Manejo de Fechas y RIR (Reglas de Negocio Importantes)

**R-10: Último día antes de guardar**
- En `RecordBodyMetricService`: Se lee el último día ANTES de guardar la pesada.
- Nunca se guarda primero y luego compara (bug anterior: la comparación nunca era cierta).

**R-11: Medida por `measuredOn`**
- `BodyMetricRepository.findClosestTo(userId, LocalDate, windowDays)` mide distancia por `measuredOn`, no por `recordedAt`.
- Esto evita que una sincronización offline de una pesada vieja cuente como "reciente".

**R-06: RIR ausente no se inventa como 0**
- `SyncItem` admite `load` y `rir` nulos.
- `SyncBatchService` rechaza el ítem si `rir` es nulo (con mensaje: "falta el RIR de la serie").
- En `RequestMapper.toSyncBatch`: `item.rir() == null ? null : Rir.of(item.rir())`.

### Excepciones y Manejo de Errores (R-14)
- `DateTimeException`: zona horaria o fecha ISO inválida → 400 con mensaje.
- `MethodArgumentTypeMismatchException`: segmento de ruta inválido (ej. UUID mal formado) → 400.
- `HttpMessageNotReadableException`: JSON mal formado o enum desconocido → 400.
- `DataIntegrityViolationException`: restricción de base (email duplicado, check) → 409 CONFLICT.

### Convenios de Nomenclatura
- **Records**: Para DTOs inmutables y valor objects (`ClaimCode`, `UserCredentials`, `WorkoutSet`, `Kg`, `E1RM`).
- **Records vs Clases**: Records para datos, clases para lógica de negocio con comportamiento.
- **Nombres de variables**: `camelCase` para locales, `SCREAMING_SNAKE_CASE` para constantes.
- **Escalas**: `Kg.SCALE = 3`, `RelativeStrength.SCALE = 4`, `Percentile.SCALE = 2`.

## Comandos y Configuración del Entorno

### Maven Profiles
```bash
# Perfil test (default en tests)
./mvnw test -Dspring.profiles.active=test

# Perfil jpa (valida adaptadores sin base externa)
./mvnw spring-boot:run -Dspring.profiles.active=jpa

# Compile rápido sin ejecutar tests
./mvnw compile -DskipTests
```

### Variables de Entorno Importantes
| Variable | Default | Descripción |
|----------|---------|-------------|
| `DAFIT_RANK_SCALE` | `SPEC_V1` | Puede sobrescribirse con env para cambiar etiquetas |
| `DAFIT_SECURITY_ENABLED` | `false` | API abierto por defecto |
| `DAFIT_SECURITY_JWT_SECRET` | `secreto-de-pruebas-con-mas-de-32-bytes-2026` | Para tests con seguridad encendida |
| `DAFIT_SECURITY_JWT_TTL_MINUTES` | `60` | Tiempo de vida del token |

### Estructura de Paquetes
```
com.dafit
├── domain/           # Java puro, sin Spring
│   ├── model/        # Entidades y value objects (records)
│   ├── exception/    # Excepciones de dominio
│   ├── port/         # Puertos (interfaces) de entrada y salida
│   │   ├── in/       # Casos de uso (UseCase, LoginUseCase, etc.)
│   │   └── out/      # Repositorios, hasheadores, etc.
│   └── service/      # Servicios de dominio puros (sin @Service)
├── application/      # Lógica de casos de uso
│   └── usecase/      # Implementaciones de casos de uso
│   └── service/      # Servicios auxiliares (AgeBands, AuthorizationSupport)
├── infrastructure/   # Detalles tecnológicos
│   ├── adapter/      # Adaptadores (JPA, memoria, REST, etc.)
│   │   ├── in/       # Controladores, mappers
│   │   └── out/      # Entidades JPA, repositorios, adaptadores JPA
│   └── config/       # Beans de Spring, configuración
├── application/      # Capa de aplicación
│   └── usecase/      # Casos de uso
└── docs/             # Documentación
```

## Cuándo Mover una Clase de Paquete

Una clase debe considerarse para mudanza a `com.dafit.application.service` cuando:
1. No es un caso de uso (no implementa un puerto de entrada).
2. Contiene lógica compartida usada por múltiples casos de uso.
3. Tiene múltiples implementaciones o podría tenerlas (como `AuthorizationSupport`).

Ejemplos ya mudados:
- `AgeBands` → `com.dafit.application.service.AgeBands`
- `AuthorizationSupport` → `com.dafit.application.service.AuthorizationSupport`

## Próximos Pasos para el Entorno de IA

1. **AGENTS.md**: Hecho - contiene contexto, estado de tests, comandos esenciales.
2. **SKILLS.md**: Hecho - contiene arquitectura, patrones, convenios de codificación.
3. **docs/ai/**: Pendiente - documentación detallada de flujos específicos.
4. **Tests**: 3 fallos conocidos documentados, no bloquean el desarrollo.
5. **Build**: Verde para compile y test-compile.