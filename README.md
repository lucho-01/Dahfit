# DAFIT

Aplicación de seguimiento de entrenamiento con rangos de fuerza y gamificación.

**Stack:** Java 21 · Spring Boot 3.5 · PostgreSQL (Neon) + Flyway · arquitectura hexagonal.

---

## Requisitos

- JDK 21
- No hace falta instalar Maven: el proyecto incluye el wrapper.
- Un `.env` con `DATABASE_URL`, `DATABASE_USER` y `DATABASE_PASSWORD` (ver `.env.example`).

## Compilar

```powershell
.\mvnw.cmd clean compile
```

## Tests

```powershell
.\mvnw.cmd test
```

La suite corre con el perfil `test` (H2 + adaptadores en memoria) y nunca toca
Neon. Los adaptadores JPA reales se prueban con el perfil `jpa` sobre H2.

## Ejecutar

```powershell
.\mvnw.cmd spring-boot:run
```

- API: `http://localhost:8080/api`
- Salud: `http://localhost:8080/actuator/health`
- Contrato OpenAPI: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Endpoints principales

| Método | Ruta |
|---|---|
| POST | `/api/users` · `/api/users/{id}/credentials` |
| GET | `/api/users/{id}` · `/api/users/{id}/body-metrics` · `/api/users/{id}/assessments` |
| POST | `/api/users/{id}/body-metrics` |
| GET/POST | `/api/exercises` · `/api/exercises/{id}` |
| GET/POST | `/api/routines` · `/api/routines/{id}` · `/api/routines/{id}/duplicate` · `/api/routines/{id}/assign` |
| GET | `/api/users/{id}/routines` |
| POST | `/api/workouts` · `/api/workouts/{id}/sets` · `/api/workouts/{id}/finish` |
| GET | `/api/workouts/{id}` · `/api/users/{id}/workouts` · `/api/users/{id}/workouts/current` |
| POST | `/api/users/{id}/exercises/{exerciseId}/assessments` · `/api/users/{id}/muscles/{muscle}/assessment` · `/api/users/{id}/global-assessment` |
| GET | `/api/users/{id}/gamification` |
| POST | `/api/sync/batch` |
| POST | `/api/auth/login` |

## Seguridad

El API está abierto por defecto para no romper la integración del frontend.
Para exigir JWT:

```bash
DAFIT_SECURITY_ENABLED=true
DAFIT_JWT_SECRET=<32 o más bytes aleatorios>
```

Con la seguridad encendida quedan públicos el login, el alta de usuario, el
alta de credenciales, la salud y OpenAPI. El resto exige
`Authorization: Bearer <token>`.

## Base de datos

Flyway es la única fuente de verdad del esquema (`src/main/resources/db/migration`):

- `V1` esquema inicial
- `V2` catálogo semilla (16 ejercicios, coeficientes y pruebas primarias)
- `V3` gamificación (XP, rachas, insignias)
- `V4` credenciales

No se siembran tablas de percentiles: no hay fuente licenciada. Mientras no se
cargue una, las evaluaciones responden `SIN_FUENTE`, que es el estado correcto.

## Arquitectura

Hexágono único con tres capas y dependencias unidireccionales:

```
infrastructure → application → domain
infrastructure → domain
application    → domain
domain         → nada
```

```
src/main/java/com/dafit
├── domain/                  Núcleo. Java puro: sin Spring, sin JPA.
│   ├── model/               Entidades y objetos de valor
│   ├── exception/           Excepciones de negocio
│   ├── port/in/             Puertos de entrada (casos de uso)
│   ├── port/out/            Puertos de salida (repositorios, APIs)
│   └── service/             Servicios de dominio
├── application/
│   └── usecase/             Implementación de los puertos de entrada
└── infrastructure/
    ├── adapter/
    │   ├── in/rest/         controller → dto → mapper
    │   └── out/persistence/ entity → repository → adapter
    ├── config/              Beans de Spring
    └── security/            JWT + BCrypt
```

La regla de dependencias la verifica ArchUnit en cada build
(`HexagonalArchitectureTest`).

## Documentación

- `docs/BITACORA.md` — estado del desarrollo, decisiones y próximos pasos
- `../DAHIAN/docs/DAHIAN_01_Analisis_Critico.md` — análisis de la especificación
- `../DAHIAN/docs/DAHIAN_02_Correcciones_Propuestas.md` — 20 correcciones (C-01 a C-20)
- `../DAHIAN/AGENTS.md` — memoria del proyecto
