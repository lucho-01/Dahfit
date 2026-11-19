# DAFIT — Bitácora de desarrollo

**Última actualización:** 2 de noviembre de 2026
**Estado:** Fase 3 completada — persistencia real (PostgreSQL/Neon + Flyway + JPA). 55 tests en verde.

---

## 1. Estado del build

```powershell
cd C:\Users\LUIS\dafit
.\mvnw.cmd -q -B compile     # OK
.\mvnw.cmd -B test           # 55 tests, 0 fallos
```

- Java 21 (Corretto 21.0.12)
- Spring Boot 3.5.16
- Maven Wrapper 3.3.4 (no requiere Maven instalado)
- **167 clases Java** en `main`, todas compilando.
- **55 tests**: 7 arquitectura + 4 contexto + 19 persistencia JPA + 13 aceptación + 7 borde + 5 integración REST.

---

## 2. Estructura (hexágono global único)

```
com.dafit
├── DafitApplication.java
│
├── domain/                              ← Java puro. Sin Spring, sin JPA.
│   ├── model/             (35)          Entidades y objetos de valor
│   ├── exception/         (1)           DomainException + 5 subtipos anidados
│   ├── port/
│   │   ├── in/            (11)          Puertos de entrada (casos de uso)
│   │   └── out/           (11)          Puertos de salida (repositorios)
│   └── service/           (16)          Servicios de dominio
│
├── application/
│   └── usecase/           (11)          Implementación de los puertos de entrada
│
└── infrastructure/
    ├── adapter/
    │   ├── in/rest/
    │   │   ├── controller/ (7)          UserController, ExerciseController, ...
    │   │   ├── dto/        (17)         Requests (validados) + Responses + Error
    │   │   └── mapper/     (2)          RequestMapper, ResponseMapper
    │   └── out/persistence/
    │       ├── entity/     (15)         Entidades JPA (14 de dominio + dafit_schema_info)
    │       ├── repository/ (12)         Spring Data JPA
    │       ├── adapter/    (22)         11 *JpaAdapter (@Profile("!test"))
    │       │                            11 InMemory*    (@Profile("test"))
    │       └── DatabaseConnectivityCheck.java
    └── config/             (5)          DafitProperties, DomainServiceConfig, UseCaseConfig,
                                         DatabaseStartupVerifier, DotEnvEnvironmentPostProcessor
```

**Dependencias permitidas:**

```
infrastructure → application → domain
infrastructure → domain
application    → domain
domain         → nada
```

---

## 3. Inventario de lo construido

### `domain/model` (35)

| Grupo | Clases |
|---|---|
| Compartidos | `Kg`, `Reps`, `Rir`, `Percentile`, `Points` |
| Usuario | `User`, `BodyMetric`, `Sex`, `WeightUnit`, `UserRole` |
| Catálogo | `Exercise`, `MuscleGroup`, `LoadMode`, `Equipment`, `MuscleCoefficient`, `PrimaryTest` |
| Motor | `E1RM`, `RelativeStrength`, `AlgorithmVersion`, `AssessmentStatus`, `StrengthRank`, `RankScale`, `BenchmarkCategory`, `BenchmarkPoint`, `BenchmarkTable`, `Assessment`, `ExerciseAssessment`, `MuscleAssessment`, `GlobalAssessment` |
| Entrenamiento | `Workout`, `WorkoutSet`, `WorkoutStatus`, `SetType`, `Routine`, `RoutineExercise` |

### `domain/service` (16)

| Servicio | Regla que implementa |
|---|---|
| `E1RmStrategy` | Puerto del algoritmo (Strategy versionado) |
| `EpleyV1` | §7.2 + **C-01** (caso base `reps == 1`) |
| `BrzyckiV1` | Alternativa, demuestra el versionado |
| `E1RmStrategyRegistry` | Selección por `AlgorithmVersion` |
| `PercentileInterpolator` | §7.2, sin extrapolar fuera de P5/P95 |
| `MuscleCoefficientResolver` | §7.4, cobertura (C-14) |
| `MuscleAggregationStrategy` | Puerto de agregación |
| `MaxPointsAggregation` | **C-09 aplicada** (monótona) |
| `WeightedMeanAggregation` | Fórmula original, solo referencia |
| `MuscleContribution` | Aporte ejercicio → músculo |
| `MuscleScoreCalculator` | §7.4 + C-14 |
| `GlobalRankCalculator` | §7.4, exige los 8 músculos |
| `ImprovementCalculator` | §7.5 + C-11 (candidato vs confirmado) |
| `OutlierDetector` | C-16 (carga atípica, salto de e1RM) |
| `BestSetSelector` | §7.1, mejor serie = mayor e1RM |
| `VolumeCalculator` | Volumen solo de series de trabajo |

### `infrastructure/config` (5)

- `DafitProperties` — `@ConfigurationProperties(prefix="dafit")`, record inmutable.
- `DomainServiceConfig` — 16 servicios de dominio como `@Bean`. `RankScale` por `dafit.engine.rank-scale`, agregación por `dafit.assessment.muscle-aggregation`.
- `UseCaseConfig` — 11 casos de uso como `@Bean`, tipados por su **puerto de entrada**.
- `DatabaseStartupVerifier` — abre una conexión real al arrancar (condicionado a `dafit.database.verify-on-startup`).
- `DotEnvEnvironmentPostProcessor` — carga `.env` (registrado en `META-INF/spring.factories`). Precedencia: variables de entorno reales > `.env` > `application.yml`.

### `infrastructure/adapter/out/persistence` (JPA, producción)

| Pieza | Contenido |
|---|---|
| `entity/` (15) | `UserEntity`, `BodyMetricEntity`, `ExerciseEntity`, `MuscleCoefficientEntity`, `PrimaryTestEntity`, `BenchmarkTableEntity`, `BenchmarkPointEntity`, `WorkoutEntity`, `WorkoutSetEntity`, `RoutineEntity`, `RoutineExerciseEntity`, `ExerciseAssessmentEntity`, `MuscleAssessmentEntity`, `GlobalAssessmentEntity`, `SchemaInfoEntity` |
| `repository/` (12) | Interfaces Spring Data; las consultas derivadas se validan al arrancar el contexto |
| `adapter/` (11) | `*JpaAdapter` con `@Profile("!test")`, misma semántica que los `InMemory*` |
| `DatabaseConnectivityCheck` | Consulta real (`version()`, base, usuario, versión de motor) para el log y el health |

**Perfiles** (un solo adaptador por puerto, siempre):

| Perfil | Persistencia | Base |
|---|---|---|
| `test` | `InMemory*` | H2 en memoria, Flyway desactivado |
| `jpa` (solo tests) | `*JpaAdapter` | H2 en memoria con esquema generado por Hibernate |
| por defecto (prod/local) | `*JpaAdapter` | PostgreSQL (Neon) + Flyway |

### `infrastructure/adapter/out/persistence/adapter` (11, en memoria)

`InMemoryUserRepository`, `InMemoryBodyMetricRepository`, `InMemoryExerciseRepository`,
`InMemoryMuscleCoefficientRepository`, `InMemoryPrimaryTestRepository`,
`InMemoryBenchmarkTableRepository`, `InMemoryExerciseAssessmentRepository`,
`InMemoryMuscleAssessmentRepository`, `InMemoryGlobalAssessmentRepository`,
`InMemoryWorkoutRepository`, `InMemoryRoutineRepository`.

> Son los adaptadores del perfil `test`. La lógica de persistencia real está en
> los `*JpaAdapter`; ambos comparten puerto y semántica, y el perfil decide cuál
> se ensambla. No se toca dominio ni casos de uso.

### `infrastructure/adapter/in/rest` (REST completo)

| Controller | Endpoints |
|---|---|
| `UserController` | `POST /api/users` |
| `ExerciseController` | `GET /api/exercises`, `POST /api/exercises` |
| `BodyMetricController` | `POST /api/users/{id}/body-metrics` |
| `AssessmentController` | `POST .../exercises/{id}/assessments`, `POST .../muscles/{m}/assessment`, `POST .../global-assessment` |
| `WorkoutController` | `POST /api/workouts`, `POST /api/workouts/{id}/sets`, `POST /api/workouts/{id}/finish`, `GET /api/users/{id}/workouts` |
| `RoutineController` | `POST /api/routines`, `POST .../duplicate`, `POST .../assign` |

- DTOs de entrada con **Bean Validation** (`@NotBlank`, `@NotNull`, `@DecimalMin`, …).
- DTOs de salida como `record`; `Optional` del dominio → `null` (omitidos por `non_null`).
- `ApiExceptionHandler` traduce `DomainException` → HTTP: `Validation`→400, `NotFound`→404, `Forbidden`→403, `NotEligible`/`InsufficientData`→422.

---

## 4. Decisiones ya aplicadas en el código

| ID | Cómo quedó implementado |
|---|---|
| **C-01** | `EpleyV1`: `reps == 1` devuelve la carga; sin el 3,33 % inflado |
| **C-03** | RIR **no** entra en la fórmula; solo filtra elegibilidad (opción A) |
| **C-04** | `Rir`: registrable 0-5, oficial ≤ 4 |
| **C-05** | `ResolveBodyweightService`: ventana ±7 días, sin fallback a peso lejano |
| **C-09** | `MaxPointsAggregation` es la estrategia por defecto |
| **C-12** | `Kg` escala 4 HALF_UP; `Percentile` escala 2; `Points` entero |
| **C-13** | `ExerciseAssessment` guarda `algorithmVersion` + `benchmarkTableId` |
| **C-14** | `MuscleScoreCalculator` exige prueba primaria y cobertura mínima |
| **C-15** | `AssessmentStatus` con 8 estados; nunca 0 por dato ausente |
| **C-17** | `WorkoutSet.id` es el `clientUuid`; `Workout.addSet` es idempotente |
| **C-18** | `Workout` guarda `localDate` + `timeZone` por separado |

### Decisiones de esquema (Fase 3)

- **Flyway es la única fuente de verdad.** `ddl-auto: validate` en producción:
  si una entidad se separa de la migración, la app no arranca.
- `V1__initial_schema.sql` replica los enums con `CHECK`: un valor fuera de
  catálogo falla en la base, no en la aplicación.
- **Sin `UNIQUE (user_id, measured_on)` en `body_metrics`**: el dominio dice que
  cada pesada es una fila nueva; el histórico puede tener varias el mismo día.
- **`exercises.bodyweight_factor` es NULL** en ejercicios que no movilizan peso
  corporal. Poner `1.000` por defecto inventaría un dato (C-15) y rompía el alta
  de ejercicios de barra.
- `dafit_schema_info` existe para tener algo real contra lo que verificar la
  conexión antes de que el esquema de negocio esté completo.
- Se usa el host **pooler** de Neon para la app y el directo solo si Flyway
  tropieza con los *advisory locks*.

### Nota importante sobre C-09

La propuesta original decía `puntos_musculo = MAX(puntos_ejercicio × 10)`. **Era un error de redacción**: los puntos ya están en escala 0-1000; multiplicar por 10 los desborda.

Lo implementado es `MAX(puntos_ejercicio)`. Consecuencia útil: **con un solo ejercicio evaluado, el resultado coincide con la fórmula del documento**, así que las pruebas de aceptación son válidas. El coeficiente decide qué ejercicios cuentan y mide cobertura (C-14), no escala la nota.

---

## 5. Tests (55)

| Clase | Casos | Qué cubre |
|---|---|---|
| `AcceptanceCriteriaTest` | 13 | Las 12 filas de la §11 (una se desdobla en 2), traducción literal |
| `EngineEdgeCasesTest` | 7 | `reps=1`, cortes 199/200 y 949/950, P5/P95, tabla no monótona, RIR=5 |
| `HexagonalArchitectureTest` | 7 | Reglas ArchUnit (7) |
| `DafitApplicationTests` | 4 | Contexto arriba; 1 impl. por puerto; ambas versiones de algoritmo |
| `JpaPersistenceIntegrationTest` | 19 | Adaptadores JPA reales sobre H2 (modo PostgreSQL): escritura, vaciado de sesión y relectura |
| `RestApiIntegrationTest` | 5 | Flujo HTTP end-to-end sobre adaptadores en memoria |

`JpaPersistenceIntegrationTest` cubre los 11 puertos: usuario (con y sin peso),
histórico corporal (orden y pesada más cercana), catálogo (músculos, factor de
peso corporal, filtros), entrenamiento (idempotencia C-17, consultas por estado,
fecha y límite), evaluaciones (última por ejercicio/músculo, opcionales),
tablas de referencia (puntos, banda exacta vs genérica, categorías), rutinas
(orden y listados) y datos de configuración (coeficientes y pruebas primarias).

> La §11 del documento tiene **12 casos** (no 22): el "22" de las notas previas
> era un desglose. Aquí se traducen 1:1 y se añaden aserciones por resultado.

---

## 6. Lo que falta

### A. Módulos nuevos del dominio

- [x] **Gamificacion** (Fase 5): `XpEvent`, `XpRule`, `XpGrant`, `XpLevel`, `Badge`, `Streak`, `XpCalculator` (topes C-19), `BadgeEvaluator`
- [x] **Sync** (Fase 5): `SyncItem`, `SyncBatch`, `SyncResult`, `SyncOutcome`, `SyncTarget`, `SyncBatchService`

### B. Persistencia real — **hecho en Fase 3**

- [x] `adapter/out/persistence/entity` — 14 entidades JPA + `SchemaInfoEntity`
- [x] `adapter/out/persistence/repository` — 12 repositorios Spring Data
- [x] `adapter/out/persistence/adapter` — 11 adaptadores JPA de los 11 puertos
- [x] Mapeo entidad ↔ dominio **en el propio adaptador** (sin capa `mapper`
      aparte: el mapeo es corto y vive junto a la única implementación que lo usa)
- [x] PostgreSQL (Neon) + Flyway (`src/main/resources/db/migration/V1__initial_schema.sql`)
- [x] Perfil `test` (memoria) vs `jpa`/producción (JPA), un adaptador por puerto
- [x] Verificación de conexión real al arrancar + `ddl-auto: validate`

Pendiente de esta capa, cuando haya datos reales:

- [x] Catalogo semilla cargado en Fase 4 (V2__seed_catalog.sql): 16 ejercicios, coeficientes v1 y 8 pruebas primarias.
- [ ] Tablas de referencia (percentiles): siguen vacias a proposito porque no hay fuente licenciada. Sin ellas las evaluaciones devuelven `SIN_FUENTE`.
- [ ] Rotar la contraseña de `neondb_owner` (se compartió por chat) y confirmar
      que `.env` es el único sitio donde vive.
- [ ] Backup/restauración y política de migraciones para producción.

### C. Pendiente de decisión del producto

- [ ] **C-02** ventana de repeticiones (1-10 vs 2-6)
- [ ] **C-08** ejercicios de peso corporal (dominadas): rama `reps` vs reasignar prueba de espalda
- [ ] **C-10** recalibración de etiquetas (existe `RankScale.recalibratedV1()` listo, sin activar)
- [ ] **C-11** umbrales de mejora
- [ ] **C-20** ¿el MVP lleva rangos?

### D. Seguridad

- [x] **JWT** (Fase 5): credenciales BCrypt + login + filtro Bearer, apagado por defecto (`dafit.security.enabled`).

---

## 7. Por dónde seguir

1. **Carga del catálogo** — sin ejercicios y tablas de referencia cargadas, el
   motor no puede emitir rangos (`SIN_FUENTE`). Es lo primero que hace falta
   para que los datos de la app sirvan de algo.
2. **Gamificacion + Sync** - hechos en Fase 5.
3. **JWT** — al final.

---

## 8. Notas de diseño a recordar

- **Ningún `@Service` en `domain`.** Los servicios de dominio son clases planas; se registran como `@Bean` en `infrastructure/config`.
- **`application/usecase` son clases planas** (sin `@Service`); sus beans se declaran en `UseCaseConfig`. Si en el futuro llevan `@Transactional`, se anota en esta capa, no en el dominio.
- **Los puertos no reflejan SQL.** Si un método del puerto huele a `findBy...And...`, se filtró el adaptador.
- **`Assessment` es `sealed`** con tres implementaciones: `ExerciseAssessment`, `MuscleAssessment`, `GlobalAssessment`.
- **Sin dato ≠ 0 puntos.** Siempre `Optional.empty()` + `AssessmentStatus`.
- **Los DTO y las entidades JPA son los únicos con getters/setters.** El dominio usa `record` inmutable a propósito.
- **Un solo adaptador por puerto activo.** `DafitApplicationTests` falla si hay dos; es deliberado.
- **La regla de negocio no se esconde en SQL.** La elección de tabla por banda de
  peso corporal se resuelve en `BenchmarkTableJpaAdapter`, no en un `WHERE`.
- **Los `@Transactional` viven en los adaptadores JPA**, no en los casos de uso:
  la unidad de trabajo es un detalle de persistencia.

---

## 9. Fase 4 - API lista para el frontend (2026-11-05)

### 9.1 Endpoints de lectura anadidos

Todos son GET sin cuerpo. Los 404 usan el mismo `ErrorResponse` que el resto.

| Metodo | Ruta | Uso en el frontend |
|---|---|---|
| GET | `/api/users/{userId}` | perfil del usuario |
| GET | `/api/users/{userId}/body-metrics` | historico de peso, mas reciente primero |
| GET | `/api/users/{userId}/assessments` | ultima evaluacion por ejercicio y musculo + ultimo global |
| GET | `/api/exercises/{exerciseId}` | ficha de un ejercicio del catalogo |
| GET | `/api/routines/{routineId}` | detalle de una rutina |
| GET | `/api/users/{userId}/routines` | rutinas poseidas o asignadas, sin duplicados |
| GET | `/api/workouts/{workoutId}` | detalle de una sesion con sus series |
| GET | `/api/users/{userId}/workouts/current` | sesion abierta; **204** si no hay ninguna |

`RoutineController` paso a `@RequestMapping("/api")` para que las rutas de
consulta y las de creacion/duplicacion/asignacion convivan sin prefijos
duplicados.

### 9.2 Contrato OpenAPI

- `springdoc-openapi-starter-webmvc-ui` 2.8.9.
- Contrato generado del codigo en `/v3/api-docs` y UI en `/swagger-ui.html`.
- `OpenApiConfig` fija titulo, version y las notas de contrato que el frontend
  necesita (204 de sesion en curso, estados `SIN_FUENTE`, forma de los errores).
- Test `elContratoOpenApiSeGeneraConLasRutasDelApi` falla si springdoc deja de
  exponer las rutas nuevas.

### 9.3 Catalogo semilla (`V2__seed_catalog.sql`)

- 16 ejercicios, 2 por musculo, con IDs fijos y reproducibles.
- 16 coeficientes `v1` aprobados (pesos de diseno de DAFIT, no fisiologia
  medida) y 8 pruebas primarias, una por musculo.
- `exercises.bodyweight_factor` queda NULL salvo en dominadas y fondos (1.000).
- **No se siembran `benchmark_tables`**: no hay fuente licenciada de percentiles
  (DAHIAN_01, linea 265). Hasta que se cargue una, las evaluaciones devuelven
  `SIN_FUENTE` y es el comportamiento correcto.
- Migracion aplicada en Neon: `version 2`, verificada en vivo con
  `/actuator/health` = UP, `/api/exercises` y `/v3/api-docs`.

### 9.4 Estado de la suite

- 63 tests en verde: 7 arquitectura + 4 contexto + 19 JPA + 13 aceptacion +
  7 borde + 13 REST.
- Los tests REST cubren ahora perfil, historico de peso, ficha de ejercicio,
  rutinas, sesion en curso (200/204), resumen de evaluaciones y contrato OpenAPI.

### 9.5 Siguiente

1. Cargar tablas de referencia con licencia (unico bloqueo real para los rangos).
2. Gamificacion + Sync - hechos en Fase 5.
3. Encender JWT cuando el frontend integre el login.

---

## 10. Fase 5 - Gamificacion y sincronizacion offline (2026-11-05)

### 10.1 Gamificacion (C-19)

Reglas implementadas, con los numeros de `application.yml`:

| Concepto | Valor |
|---|---|
| XP por sesion completada | 50 |
| Bonus primera sesion de la semana ISO | 20 |
| Revision de ciclo (reservado) | 100 |
| Tope diario | 200 XP |
| Tope semanal | 500 XP |
| Maximo de sesiones que dan XP al dia | 2 |

- `XpCalculator` aplica idempotencia por `sourceKey`, limite de sesiones,
  tope diario y tope semanal. Si solo cabe una parte del XP, otorga la parte
  que cabe; si no cabe nada, no crea evento.
- `XpEvent` es un ledger inmutable. La base impone
  `UNIQUE (user_id, source_key)`.
- `Streak` cuenta dias consecutivos y conserva el record al cortarse: no hay
  castigo ni resta de XP.
- `BadgeEvaluator`: `PRIMER_ENTRENAMIENTO` (1 sesion), `RACHA_4` (4 dias),
  `DIEZ_ENTRENAMIENTOS` (10 sesiones). No se repiten.
- `XpLevel`: 500 XP por nivel.
- El XP **no** toca el motor de fuerza: son puertos y tablas separados.

Endpoint nuevo: `GET /api/users/{userId}/gamification` con XP total, nivel,
racha, insignias y ultimos 10 eventos.

El XP se otorga al cerrar la sesion, despues de persistirla. Si el otorgamiento
fallara, el entrenamiento ya esta guardado.

### 10.2 Sincronizacion offline (C-17)

`POST /api/sync/batch` aplica un lote de hasta 200 items en orden:

```json
{"userId":"...","items":[
  {"target":"WORKOUT_SET","clientUuid":"...","workoutId":"...","exerciseId":"...",
   "loadKg":80,"inputUnit":"KG","loadMode":"TOTAL","implementCount":1,
   "reps":5,"rir":1,"type":"WORKING"},
  {"target":"WORKOUT_FINISH","workoutId":"..."}
]}
```

- Respuesta 200 siempre; el resultado va por item:
  `APPLIED`, `DUPLICATE` (ya estaba: el cliente puede borrarlo de la cola) o
  `REJECTED` con mensaje.
- `DUPLICATE` de una serie se detecta antes de comprobar si la sesion esta
  cerrada, para que un reintento viejo no genere un falso error.
- Un item malo no tumba el lote.
- Un item de una sesion ajena se rechaza.
- El cierre dispara el mismo otorgamiento de XP que el endpoint normal y es
  idempotente por `workoutId`.

### 10.3 Persistencia

- `V3__gamification.sql`: `xp_events`, `streaks`, `user_badges`.
- 3 entidades JPA, 3 repositorios Spring Data, 3 adaptadores JPA y sus 3
  equivalentes en memoria.
- `WorkoutRepository.countCompleted` alimenta las insignias.
- Migracion aplicada en Neon: `version 3`.

### 10.4 Verificacion

- **88 tests en verde**: 7 arquitectura + 4 contexto + 13 aceptacion +
  7 borde + 10 gamificacion + 22 JPA + 19 REST + 6 seguridad.
- En vivo contra Neon: lote de sync aplicado (2/2), reenvio del mismo lote
  (0 aplicados, 2 duplicados), XP 70, racha 1, 1 insignia, sesion COMPLETED
  con 1 serie y 400 kg de volumen.

### 10.5 JWT y credenciales (V4)

Implementado y probado, **apagado por defecto** para que el contrato que usa el
frontend hoy siga funcionando durante la integracion:

- `POST /api/users/{userId}/credentials` asigna email y contrasena (BCrypt,
  8-72 caracteres). Va aparte del alta para que un entrenador pueda crear
  atletas sin login.
- `POST /api/auth/login` devuelve un JWT HS256 (`Bearer`, 60 min).
- `dafit.security.enabled=false`: todo publico (estado actual).
- `dafit.security.enabled=true`: exige `Authorization: Bearer <token>` en todo
  salvo login, alta de usuario, alta de credenciales, salud y OpenAPI.
- `DAFIT_JWT_SECRET` es obligatorio con la seguridad encendida (minimo 32
  bytes); si falta, la aplicacion falla al arrancar en vez de firmar con una
  clave debil.
- Sesiones sin estado, CSRF desactivado, 401 para token ausente/invalido y 409
  para email duplicado.
- El email se normaliza (minusculas, sin espacios) y el mismo mensaje
  "credenciales invalidas" evita enumerar cuentas.
- OpenAPI declara el esquema `bearerAuth`.

Verificado en vivo contra Neon con la seguridad encendida: sin token 401,
login 200 con Bearer, perfil con token 200 y contrasena incorrecta 401. Con la
configuracion por defecto, `/api/exercises` sigue respondiendo 200.

Para encenderla:

```bash
DAFIT_SECURITY_ENABLED=true
DAFIT_JWT_SECRET=<32+ bytes aleatorios>
```

Pendiente para produccion: refresh token/rotacion, verificacion de email y
limite de intentos de login.


