package com.dafit.persistence;

import com.dafit.domain.model.AlgorithmVersion;
import com.dafit.domain.model.AssessmentStatus;
import com.dafit.domain.model.Badge;
import com.dafit.domain.model.BenchmarkCategory;
import com.dafit.domain.model.BenchmarkPoint;
import com.dafit.domain.model.BenchmarkTable;
import com.dafit.domain.model.BodyMetric;
import com.dafit.domain.model.E1RM;
import com.dafit.domain.model.Equipment;
import com.dafit.domain.model.Exercise;
import com.dafit.domain.model.ExerciseAssessment;
import com.dafit.domain.model.GlobalAssessment;
import com.dafit.domain.model.Kg;
import com.dafit.domain.model.LoadMode;
import com.dafit.domain.model.MuscleAssessment;
import com.dafit.domain.model.MuscleCoefficient;
import com.dafit.domain.model.MuscleGroup;
import com.dafit.domain.model.Percentile;
import com.dafit.domain.model.Points;
import com.dafit.domain.model.PrimaryTest;
import com.dafit.domain.model.RelativeStrength;
import com.dafit.domain.model.Reps;
import com.dafit.domain.model.Rir;
import com.dafit.domain.model.Routine;
import com.dafit.domain.model.RoutineExercise;
import com.dafit.domain.model.SetType;
import com.dafit.domain.model.Sex;
import com.dafit.domain.model.StrengthRank;
import com.dafit.domain.model.Streak;
import com.dafit.domain.model.SyncBatchHistory;
import com.dafit.domain.model.SyncOutcome;
import com.dafit.domain.model.SyncResult;
import com.dafit.domain.model.SyncTarget;
import com.dafit.domain.model.User;
import com.dafit.domain.model.UserBadge;
import com.dafit.domain.model.UserRole;
import com.dafit.domain.model.WeightUnit;
import com.dafit.domain.model.Workout;
import com.dafit.domain.model.WorkoutSet;
import com.dafit.domain.model.WorkoutStatus;
import com.dafit.domain.model.XpEvent;
import com.dafit.domain.model.XpEventType;
import com.dafit.domain.port.out.BenchmarkTableRepository;
import com.dafit.domain.port.out.BadgeRepository;
import com.dafit.domain.port.out.BodyMetricRepository;
import com.dafit.domain.port.out.ExerciseAssessmentRepository;
import com.dafit.domain.port.out.ExerciseRepository;
import com.dafit.domain.port.out.GlobalAssessmentRepository;
import com.dafit.domain.port.out.MuscleAssessmentRepository;
import com.dafit.domain.port.out.MuscleCoefficientRepository;
import com.dafit.domain.port.out.PrimaryTestRepository;
import com.dafit.domain.port.out.RoutineRepository;
import com.dafit.domain.port.out.StreakRepository;
import com.dafit.domain.port.out.SyncBatchRepository;
import com.dafit.domain.port.out.UserRepository;
import com.dafit.domain.port.out.WorkoutRepository;
import com.dafit.domain.port.out.XpEventRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integracion de los adaptadores JPA reales.
 *
 * <p>Corre con perfil {@code jpa}: los {@code InMemory*} estan apagados y los
 * {@code *JpaAdapter} activos. Hibernate genera el esquema en H2 (modo
 * PostgreSQL) porque lo que se verifica aqui es el mapeo entidad <-> dominio y
 * las consultas derivadas de Spring Data, no el DDL.
 *
 * <p>El DDL de PostgreSQL se valida en el arranque real con {@code ddl-auto:
 * validate} contra Neon. Las dos comprobaciones son complementarias: esta
 * atrapa un error de consulta o de mapeo en cada {@code mvn test}; aquella
 * atrapa una divergencia entre entidad y migracion.
 *
 * <p>Cada prueba escribe, vacia la sesion de Hibernate y vuelve a leer: si el
 * dato solo estuviera en el contexto de persistencia, el test fallaria.
 */
@SpringBootTest(properties = {
        "spring.sql.init.mode=never",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "dafit.database.verify-on-startup=true"
})
@ActiveProfiles("jpa")
@Transactional
class JpaPersistenceIntegrationTest {

    private static final Instant T0 = Instant.parse("2026-01-15T10:00:00Z");
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Autowired
    private UserRepository users;

    @Autowired
    private BodyMetricRepository bodyMetrics;

    @Autowired
    private ExerciseRepository exercises;

    @Autowired
    private WorkoutRepository workouts;

    @Autowired
    private ExerciseAssessmentRepository exerciseAssessments;

    @Autowired
    private MuscleAssessmentRepository muscleAssessments;

    @Autowired
    private GlobalAssessmentRepository globalAssessments;

    @Autowired
    private BenchmarkTableRepository benchmarkTables;

    @Autowired
    private RoutineRepository routines;

    @Autowired
    private MuscleCoefficientRepository coefficients;

    @Autowired
    private PrimaryTestRepository primaryTests;

    @Autowired
    private XpEventRepository xpEvents;

    @Autowired
    private StreakRepository streaks;

    @Autowired
    private SyncBatchRepository syncBatches;

    @Autowired
    private BadgeRepository badges;

    @Autowired
    private EntityManager entityManager;

    // --- usuarios ----------------------------------------------------------

    @Test
    void guardaYRecuperaUsuarioConSuZonaHoraria() {
        UUID userId = UUID.randomUUID();
        users.save(new User(userId, "Ana", UserRole.ATHLETE, Sex.FEMALE,
                LocalDate.of(1995, 4, 2), Kg.ofKg(new BigDecimal("61.5")),
                WeightUnit.LB, BOGOTA, true, T0));
        flushAndClear();

        User loaded = users.findById(userId).orElseThrow();

        assertThat(loaded.displayName()).isEqualTo("Ana");
        assertThat(loaded.role()).isEqualTo(UserRole.ATHLETE);
        assertThat(loaded.sex()).isEqualTo(Sex.FEMALE);
        assertThat(loaded.birthDate()).isEqualTo(LocalDate.of(1995, 4, 2));
        assertThat(loaded.bodyweightKg().value()).isEqualByComparingTo("61.5");
        assertThat(loaded.preferredUnit()).isEqualTo(WeightUnit.LB);
        assertThat(loaded.timeZone()).isEqualTo(BOGOTA);
        assertThat(loaded.benchmarkConsent()).isTrue();
        assertThat(loaded.createdAt()).isEqualTo(T0);
    }

    @Test
    void elPesoCorporalOpcionalSigueSiendoNulo() {
        UUID userId = UUID.randomUUID();
        users.save(new User(userId, "Sin peso", UserRole.COACH, Sex.UNSPECIFIED,
                null, null, WeightUnit.KG, BOGOTA, false, T0));
        flushAndClear();

        User loaded = users.findById(userId).orElseThrow();

        // C-15: opcional es nulo, no cero.
        assertThat(loaded.bodyweightKg()).isNull();
        assertThat(loaded.birthDate()).isNull();
    }

    // --- historico corporal ------------------------------------------------

    @Test
    void historicoCorporalOrdenaYEncuentraLaPesadaMasCercana() {
        UUID userId = UUID.randomUUID();
        insertUser(userId);

        bodyMetrics.save(metric(userId, "80.0", LocalDate.of(2026, 1, 10), T0));
        bodyMetrics.save(metric(userId, "81.5", LocalDate.of(2026, 1, 13), T0.plus(Duration.ofDays(3))));
        bodyMetrics.save(metric(userId, "83.0", LocalDate.of(2026, 1, 20), T0.plus(Duration.ofDays(10))));
        flushAndClear();

        // R-11: la cercania se mide por dia de pesada. La referencia es el 19
        // de enero: la pesada del 18 se subio ese dia pero es del 13; la del 20
        // esta a un dia y es la correcta (antes se elegia la del 13).
        Optional<BodyMetric> closest = bodyMetrics.findClosestTo(
                userId, LocalDate.of(2026, 1, 19), 2);

        assertThat(closest).isPresent();
        assertThat(closest.orElseThrow().weightKg().value()).isEqualByComparingTo("83.0");

        List<BodyMetric> history = bodyMetrics.findHistory(userId);
        assertThat(history).extracting(metric -> metric.weightKg().value().stripTrailingZeros().toPlainString())
                .containsExactly("83", "81.5", "80");
    }

    @Test
    void sinPesadaDentroDeLaVentanaNoHayCandidata() {
        UUID userId = UUID.randomUUID();
        insertUser(userId);
        bodyMetrics.save(metric(userId, "80.0", LocalDate.of(2026, 1, 10), T0));
        flushAndClear();

        assertThat(bodyMetrics.findClosestTo(userId, LocalDate.of(2026, 1, 10).plusDays(60), 7))
                .isEmpty();
    }

    // --- catalogo ----------------------------------------------------------

    @Test
    void elCatalogoConservaMusculosYElFactorDePesoCorporal() {
        UUID barbellId = UUID.randomUUID();
        UUID pullUpId = UUID.randomUUID();

        exercises.save(exercise(barbellId, "Press banca", Equipment.BARBELL, LoadMode.TOTAL,
                Set.of(MuscleGroup.CHEST, MuscleGroup.TRICEPS), null));
        exercises.save(exercise(pullUpId, "Dominada", Equipment.BODYWEIGHT, LoadMode.BODYWEIGHT,
                Set.of(MuscleGroup.BACK, MuscleGroup.BICEPS), Exercise.DEFAULT_BODYWEIGHT_FACTOR));
        flushAndClear();

        Exercise barbell = exercises.findById(barbellId).orElseThrow();
        assertThat(barbell.muscles()).containsExactlyInAnyOrder(MuscleGroup.CHEST, MuscleGroup.TRICEPS);
        // C-15: un ejercicio de barra no moviliza peso corporal, no es "1".
        assertThat(barbell.bodyweightFactor()).isNull();

        Exercise pullUp = exercises.findById(pullUpId).orElseThrow();
        assertThat(pullUp.bodyweightFactor()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(pullUp.loadMode().isBodyweightBased()).isTrue();
    }

    @Test
    void laBusquedaFiltraPorMusculoEquipoYEvaluabilidad() {
        UUID chestId = UUID.randomUUID();
        UUID backId = UUID.randomUUID();
        UUID inactiveId = UUID.randomUUID();

        exercises.save(exercise(chestId, "Press banca", Equipment.BARBELL, LoadMode.TOTAL,
                Set.of(MuscleGroup.CHEST), null));
        exercises.save(exercise(backId, "Remo", Equipment.BARBELL, LoadMode.TOTAL,
                Set.of(MuscleGroup.BACK), null));
        exercises.save(new Exercise(inactiveId, "Retirado", Equipment.MACHINE, LoadMode.MACHINE_STACK,
                false, Set.of(MuscleGroup.CHEST), true, null, false));
        flushAndClear();

        assertThat(exercises.search(Set.of(MuscleGroup.CHEST), null, false))
                .extracting(Exercise::id)
                .containsExactly(chestId);

        assertThat(exercises.search(null, Equipment.BARBELL, true))
                .extracting(Exercise::id)
                .containsExactlyInAnyOrder(chestId, backId);

        assertThat(exercises.findAllActive())
                .extracting(Exercise::id)
                .containsExactlyInAnyOrder(chestId, backId);
    }

    // --- entrenamientos ----------------------------------------------------

    @Test
    void reenviarLaMismaSerieNoDuplicaYActualizaSusValores() {
        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        insertUser(userId);
        insertExercise(exerciseId);

        UUID workoutId = UUID.randomUUID();
        UUID setId = UUID.randomUUID();
        workouts.save(new Workout(workoutId, userId, LocalDate.of(2026, 1, 15), BOGOTA,
                T0, null, WorkoutStatus.IN_PROGRESS,
                List.of(set(workoutId, setId, exerciseId, 1, "100", 5)), null, null));
        flushAndClear();

        assertThat(workouts.findById(workoutId).orElseThrow().sets()).hasSize(1);

        // C-17: mismo clientUuid -> la serie se actualiza, no se duplica.
        workouts.save(new Workout(workoutId, userId, LocalDate.of(2026, 1, 15), BOGOTA,
                T0, null, WorkoutStatus.IN_PROGRESS,
                List.of(set(workoutId, setId, exerciseId, 1, "102.5", 6)), "corregida", null));
        flushAndClear();

        Workout reloaded = workouts.findById(workoutId).orElseThrow();
        assertThat(reloaded.sets()).hasSize(1);
        assertThat(reloaded.sets().get(0).load().value()).isEqualByComparingTo("102.5");
        assertThat(reloaded.sets().get(0).reps()).isEqualTo(new Reps(6));
        assertThat(reloaded.notes()).isEqualTo("corregida");
    }

    @Test
    void lasConsultasDeSesionesRespetanEstadoFechaYLimite() {
        UUID userId = UUID.randomUUID();
        insertUser(userId);

        UUID day14 = UUID.randomUUID();
        UUID day15 = UUID.randomUUID();
        Instant start14 = T0.minus(Duration.ofDays(1));

        workouts.save(new Workout(day14, userId, LocalDate.of(2026, 1, 14), BOGOTA,
                start14, start14.plus(Duration.ofMinutes(50)), WorkoutStatus.COMPLETED,
                List.of(), null, null));
        workouts.save(new Workout(day15, userId, LocalDate.of(2026, 1, 15), BOGOTA,
                T0, T0.plus(Duration.ofMinutes(55)), WorkoutStatus.COMPLETED,
                List.of(), null, null));
        flushAndClear();

        assertThat(workouts.findCompletedBetween(userId,
                LocalDate.of(2026, 1, 14), LocalDate.of(2026, 1, 15)))
                .hasSize(2);

        assertThat(workouts.findRecent(userId, 1))
                .singleElement()
                .extracting(Workout::localDate)
                .isEqualTo(LocalDate.of(2026, 1, 15));

        assertThat(workouts.findRecent(userId, 0)).isEmpty();
        assertThat(workouts.findInProgress(userId)).isEmpty();
        assertThat(workouts.countCompleted(userId)).isEqualTo(2);
    }

    @Test
    void unaSesionAbiertaSeEncuentraPorUsuario() {
        UUID userId = UUID.randomUUID();
        insertUser(userId);
        UUID workoutId = UUID.randomUUID();

        workouts.save(new Workout(workoutId, userId, LocalDate.of(2026, 1, 15), BOGOTA,
                T0, null, WorkoutStatus.IN_PROGRESS, List.of(), null, null));
        flushAndClear();

        assertThat(workouts.findInProgress(userId))
                .get()
                .extracting(Workout::id)
                .isEqualTo(workoutId);
    }

    // --- evaluaciones ------------------------------------------------------

    @Test
    void laEvaluacionDeEjercicioConservaOpcionalesYVersionDeAlgoritmo() {
        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        insertUser(userId);
        insertExercise(exerciseId);

        exerciseAssessments.save(assessment(userId, exerciseId, AssessmentStatus.SIN_FUENTE, T0));
        UUID officialId = UUID.randomUUID();
        exerciseAssessments.save(new ExerciseAssessment(
                officialId, userId, exerciseId, AssessmentStatus.OK,
                Optional.of(Points.of(500)), Optional.of(StrengthRank.AVANZADO),
                Optional.of(E1RM.of(Kg.ofKg(new BigDecimal("120")))), Optional.of(new RelativeStrength(new BigDecimal("1.5"))),
                Optional.of(Percentile.of(50)), AlgorithmVersion.BRZYCKI_V1,
                null, Optional.of(Kg.ofKg(new BigDecimal("80"))), null,
                T0.plus(Duration.ofHours(1))));
        flushAndClear();

        ExerciseAssessment latest = exerciseAssessments.findLatestFor(userId, exerciseId).orElseThrow();

        assertThat(latest.id()).isEqualTo(officialId);
        assertThat(latest.points()).contains(Points.of(500));
        assertThat(latest.rank()).contains(StrengthRank.AVANZADO);
        assertThat(latest.e1rm().orElseThrow().value().value()).isEqualByComparingTo("120");
        assertThat(latest.percentile().orElseThrow().value()).isEqualByComparingTo("50");
        assertThat(latest.algorithmVersion()).isEqualTo(AlgorithmVersion.BRZYCKI_V1);
        assertThat(latest.bodyweightUsed().orElseThrow().value()).isEqualByComparingTo("80");
    }

    @Test
    void soloSeDevuelveLaUltimaEvaluacionPorEjercicio() {
        UUID userId = UUID.randomUUID();
        UUID pressId = UUID.randomUUID();
        UUID rowId = UUID.randomUUID();
        insertUser(userId);
        insertExercise(pressId);
        insertExercise(rowId);

        exerciseAssessments.save(assessment(userId, pressId, AssessmentStatus.SIN_DATOS, T0));
        exerciseAssessments.save(assessment(userId, pressId, AssessmentStatus.SIN_FUENTE, T0.plus(Duration.ofHours(2))));
        exerciseAssessments.save(assessment(userId, rowId, AssessmentStatus.SIN_DATOS, T0.plus(Duration.ofHours(1))));
        flushAndClear();

        List<ExerciseAssessment> latest = exerciseAssessments.findLatestForUser(userId);

        assertThat(latest).hasSize(2);
        assertThat(latest).filteredOn(assessment -> assessment.exerciseId().equals(pressId))
                .singleElement()
                .extracting(ExerciseAssessment::status)
                .isEqualTo(AssessmentStatus.SIN_FUENTE);
    }

    @Test
    void laEvaluacionMuscularConservaCoberturaYEjerciciosEvaluados() {
        UUID userId = UUID.randomUUID();
        UUID exerciseA = UUID.randomUUID();
        UUID exerciseB = UUID.randomUUID();
        insertUser(userId);

        muscleAssessments.save(new MuscleAssessment(
                UUID.randomUUID(), userId, MuscleGroup.CHEST, AssessmentStatus.OK,
                Optional.of(Points.of(620)), Optional.of(StrengthRank.BESTIA),
                new BigDecimal("0.750"), List.of(exerciseA, exerciseB), 2, T0));
        muscleAssessments.save(new MuscleAssessment(
                UUID.randomUUID(), userId, MuscleGroup.CHEST, AssessmentStatus.COBERTURA_INSUFICIENTE,
                Optional.empty(), Optional.empty(), new BigDecimal("0.100"), List.of(), 0,
                T0.plus(Duration.ofDays(1))));
        flushAndClear();

        MuscleAssessment latest = muscleAssessments.findLatestFor(userId, MuscleGroup.CHEST).orElseThrow();

        assertThat(latest.status()).isEqualTo(AssessmentStatus.COBERTURA_INSUFICIENTE);
        assertThat(latest.coverage()).isEqualByComparingTo("0.100");

        assertThat(muscleAssessments.findLatestForUser(userId))
                .singleElement()
                .satisfies(assessment -> assertThat(assessment.exercisesEvaluated()).isEmpty());
    }

    @Test
    void laEvaluacionMuscularRecuperaLaListaDeEjercicios() {
        UUID userId = UUID.randomUUID();
        UUID exerciseA = UUID.randomUUID();
        UUID exerciseB = UUID.randomUUID();
        insertUser(userId);

        muscleAssessments.save(new MuscleAssessment(
                UUID.randomUUID(), userId, MuscleGroup.BACK, AssessmentStatus.OK,
                Optional.of(Points.of(700)), Optional.of(StrengthRank.ELITE),
                new BigDecimal("0.800"), List.of(exerciseA, exerciseB), 2, T0));
        flushAndClear();

        MuscleAssessment loaded = muscleAssessments.findLatestFor(userId, MuscleGroup.BACK).orElseThrow();

        assertThat(loaded.exercisesEvaluated()).containsExactlyInAnyOrder(exerciseA, exerciseB);
        assertThat(loaded.primaryTestsEvaluated()).isEqualTo(2);
        assertThat(loaded.points()).contains(Points.of(700));
    }

    @Test
    void seRecuperaElRangoGeneralMasReciente() {
        UUID userId = UUID.randomUUID();
        insertUser(userId);

        globalAssessments.save(new GlobalAssessment(UUID.randomUUID(), userId,
                AssessmentStatus.COBERTURA_INSUFICIENTE, Optional.empty(), Optional.empty(),
                0, 8, T0));
        globalAssessments.save(new GlobalAssessment(UUID.randomUUID(), userId,
                AssessmentStatus.OK, Optional.of(Points.of(750)), Optional.of(StrengthRank.ELITE),
                8, 8, T0.plus(Duration.ofDays(2))));
        flushAndClear();

        GlobalAssessment latest = globalAssessments.findLatestFor(userId).orElseThrow();

        assertThat(latest.status()).isEqualTo(AssessmentStatus.OK);
        assertThat(latest.points()).contains(Points.of(750));
        assertThat(latest.musclesEvaluated()).isEqualTo(8);
        assertThat(latest.musclesRequired()).isEqualTo(8);
    }

    // --- tablas de referencia ----------------------------------------------

    @Test
    void laTablaDeReferenciaConservaSusPuntosYSeEligePorBanda() {
        UUID exerciseId = UUID.randomUUID();
        insertExercise(exerciseId);

        BenchmarkCategory male = BenchmarkCategory.of(Sex.MALE);
        benchmarkTables.save(table(UUID.randomUUID(), exerciseId, "ACSM", "v1", male, null));
        benchmarkTables.save(table(UUID.randomUUID(), exerciseId, "ACSM", "v1", male,
                Kg.ofKg(new BigDecimal("80"))));
        flushAndClear();

        // Banda exacta: gana la tabla especifica.
        assertThat(benchmarkTables.findFor(exerciseId, male, Kg.ofKg(new BigDecimal("80"))))
                .get()
                .satisfies(table -> {
                    assertThat(table.bodyweightKg().value()).isEqualByComparingTo("80");
                    assertThat(table.points()).extracting(point -> point.percentile().value().stripTrailingZeros().toPlainString())
                            .containsExactly("50", "80");
                });

        // Sin banda exacta: se usa la tabla generica, nunca otra categoria.
        assertThat(benchmarkTables.findFor(exerciseId, male, Kg.ofKg(new BigDecimal("92"))))
                .get()
                .satisfies(table -> assertThat(table.bodyweightKg()).isNull());

        assertThat(benchmarkTables.findSiblings(exerciseId, male))
                .extracting(table -> table.bodyweightKg() == null
                        ? "sin-banda"
                        : table.bodyweightKg().value().stripTrailingZeros().toPlainString())
                .containsExactly("sin-banda", "80");
    }

    @Test
    void noSeMezclanCategoriasDeSexo() {
        UUID exerciseId = UUID.randomUUID();
        insertExercise(exerciseId);
        benchmarkTables.save(table(UUID.randomUUID(), exerciseId, "ACSM", "v1",
                BenchmarkCategory.of(Sex.FEMALE), null));
        flushAndClear();

        assertThat(benchmarkTables.findFor(exerciseId, BenchmarkCategory.of(Sex.MALE), null)).isEmpty();
    }

    @Test
    void persistePercentilesDeBenchmarkConDosDecimales() {
        UUID exerciseId = UUID.randomUUID();
        insertExercise(exerciseId);
        BenchmarkTable table = new BenchmarkTable(
                UUID.randomUUID(), exerciseId, "ACSM", "v1",
                BenchmarkCategory.of(Sex.MALE), null, true, false,
                List.of(BenchmarkPoint.of(50.25, "90"), BenchmarkPoint.of(80.75, "130")));

        benchmarkTables.save(table);
        flushAndClear();

        BenchmarkTable loaded = benchmarkTables.findById(table.id()).orElseThrow();

        assertThat(loaded.points()).extracting(point -> point.percentile().value())
                .containsExactly(new BigDecimal("50.25"), new BigDecimal("80.75"));
    }

    @Test
    void persisteElHistorialDeLoteConElOrdenYResultadoPorElemento() {
        UUID userId = UUID.randomUUID();
        SyncBatchHistory history = new SyncBatchHistory(
                UUID.randomUUID(), userId, T0,
                List.of(
                        SyncResult.applied("set-1", SyncTarget.WORKOUT_SET),
                        SyncResult.rejected("finish-1", SyncTarget.WORKOUT_FINISH, "sesion invalida")));

        syncBatches.save(history);
        flushAndClear();

        SyncBatchHistory loaded = syncBatches.findById(history.id()).orElseThrow();

        assertThat(loaded.userId()).isEqualTo(userId);
        assertThat(loaded.receivedAt()).isEqualTo(T0);
        assertThat(loaded.results()).containsExactlyElementsOf(history.results());
        assertThat(syncBatches.findByUserId(userId)).containsExactly(history);
    }

    // --- rutinas -----------------------------------------------------------

    @Test
    void laRutinaConservaElOrdenYSeListaPorPropietarioYDestinatario() {
        UUID coachId = UUID.randomUUID();
        UUID athleteId = UUID.randomUUID();
        insertUser(coachId);
        insertUser(athleteId);

        UUID exerciseA = UUID.randomUUID();
        UUID exerciseB = UUID.randomUUID();
        UUID exerciseC = UUID.randomUUID();
        insertExercise(exerciseA);
        insertExercise(exerciseB);
        insertExercise(exerciseC);
        UUID routineId = UUID.randomUUID();
        Routine routine = new Routine(routineId, coachId, athleteId, "Fuerza base",
                List.of(routineExercise(exerciseA, 3), routineExercise(exerciseB, 1),
                        routineExercise(exerciseC, 2)),
                T0);
        routines.save(routine);
        flushAndClear();

        Routine loaded = routines.findById(routineId).orElseThrow();

        assertThat(loaded.exercises()).extracting(RoutineExercise::order).containsExactly(1, 2, 3);
        assertThat(loaded.exercises()).extracting(RoutineExercise::targetSets).containsOnly(4);
        assertThat(routines.findOwnedBy(coachId)).singleElement()
                .extracting(Routine::id).isEqualTo(routineId);
        assertThat(routines.findAssignedTo(athleteId)).singleElement()
                .extracting(Routine::id).isEqualTo(routineId);
        assertThat(routines.findOwnedBy(athleteId)).isEmpty();
    }

    // --- datos de configuracion --------------------------------------------

    @Test
    void losCoeficientesSeFiltranPorAprobacionYVersion() {
        UUID exerciseId = UUID.randomUUID();
        insertExercise(exerciseId);

        coefficients.saveAll(List.of(
                new MuscleCoefficient(exerciseId, MuscleGroup.CHEST, new BigDecimal("1.000"), "v1", true),
                new MuscleCoefficient(exerciseId, MuscleGroup.TRICEPS, new BigDecimal("0.500"), "v1", true),
                new MuscleCoefficient(exerciseId, MuscleGroup.SHOULDERS, new BigDecimal("0.250"), "v1", false),
                new MuscleCoefficient(exerciseId, MuscleGroup.CHEST, new BigDecimal("1.000"), "v2", false)));
        flushAndClear();

        assertThat(coefficients.findApproved()).hasSize(2);
        assertThat(coefficients.findByVersion("v1")).hasSize(3);
        assertThat(coefficients.findByVersion("v2"))
                .singleElement()
                .extracting(MuscleCoefficient::approved)
                .isEqualTo(false);
    }

    @Test
    void lasPruebasPrimariasSeGuardanYSeLeenCompletas() {
        UUID pressId = UUID.randomUUID();
        UUID rowId = UUID.randomUUID();
        insertExercise(pressId);
        insertExercise(rowId);

        primaryTests.saveAll(List.of(
                new PrimaryTest(MuscleGroup.CHEST, pressId, true),
                new PrimaryTest(MuscleGroup.BACK, rowId, true)));
        flushAndClear();

        assertThat(primaryTests.findAll())
                .extracting(PrimaryTest::muscle)
                .containsExactlyInAnyOrder(MuscleGroup.CHEST, MuscleGroup.BACK);
    }

    // --- gamificacion ------------------------------------------------------

    @Test
    void elLedgerDeXpSumaPuntosYEsIdempotentePorClave() {
        UUID userId = UUID.randomUUID();
        insertUser(userId);

        xpEvents.save(new XpEvent(UUID.randomUUID(), userId, XpEventType.SESSION_COMPLETED,
                50, LocalDate.of(2026, 1, 15), T0, "workout:1"));
        xpEvents.save(new XpEvent(UUID.randomUUID(), userId, XpEventType.FIRST_SESSION_OF_WEEK,
                20, LocalDate.of(2026, 1, 15), T0, "first-week:2026-01-12"));
        flushAndClear();

        assertThat(xpEvents.existsBySourceKey(userId, "workout:1")).isTrue();
        assertThat(xpEvents.existsBySourceKey(userId, "workout:2")).isFalse();
        assertThat(xpEvents.totalPoints(userId)).isEqualTo(70);
        assertThat(xpEvents.findBetween(userId, LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 18)))
                .hasSize(2);
        assertThat(xpEvents.findRecent(userId, 1)).hasSize(1);
    }

    @Test
    void laRachaSeGuardaPorUsuarioYConservaElRecord() {
        UUID userId = UUID.randomUUID();
        insertUser(userId);

        streaks.save(new Streak(userId, 3, 7, LocalDate.of(2026, 1, 15)));
        flushAndClear();

        Streak loaded = streaks.findByUserId(userId).orElseThrow();
        assertThat(loaded.currentDays()).isEqualTo(3);
        assertThat(loaded.longestDays()).isEqualTo(7);
        assertThat(loaded.lastActivityDate()).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    @Test
    void lasInsigniasSeRecuperanSinDuplicados() {
        UUID userId = UUID.randomUUID();
        insertUser(userId);

        badges.save(new UserBadge(UUID.randomUUID(), userId, Badge.PRIMER_ENTRENAMIENTO, T0));
        badges.save(new UserBadge(UUID.randomUUID(), userId, Badge.RACHA_4, T0.plus(Duration.ofDays(3))));
        flushAndClear();

        assertThat(badges.findByUserId(userId)).hasSize(2);
        assertThat(badges.badgesOf(userId))
                .containsExactlyInAnyOrder(Badge.PRIMER_ENTRENAMIENTO, Badge.RACHA_4);
    }

    // --- helpers -----------------------------------------------------------

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private void insertUser(UUID userId) {
        users.save(new User(userId, "Atleta " + userId, UserRole.ATHLETE, Sex.MALE,
                LocalDate.of(1990, 1, 1), Kg.ofKg(new BigDecimal("80")), WeightUnit.KG,
                BOGOTA, false, T0));
    }

    private void insertExercise(UUID exerciseId) {
        exercises.save(exercise(exerciseId, "Ejercicio " + exerciseId, Equipment.BARBELL,
                LoadMode.TOTAL, Set.of(MuscleGroup.CHEST), null));
    }

    private static Exercise exercise(UUID id, String name, Equipment equipment, LoadMode loadMode,
                                     Set<MuscleGroup> muscles, BigDecimal bodyweightFactor) {
        return new Exercise(id, name, equipment, loadMode, false, muscles, true,
                bodyweightFactor, true);
    }

    private static BodyMetric metric(UUID userId, String weightKg, LocalDate date, Instant recordedAt) {
        return new BodyMetric(UUID.randomUUID(), userId, Kg.ofKg(new BigDecimal(weightKg)),
                date, recordedAt, BodyMetric.SOURCE_MANUAL);
    }

    private static WorkoutSet set(UUID workoutId, UUID setId, UUID exerciseId, int order,
                                  String loadKg, int reps) {
        return new WorkoutSet(setId, workoutId, exerciseId, order,
                Kg.ofKg(new BigDecimal(loadKg)), WeightUnit.KG, LoadMode.TOTAL, 1,
                new Reps(reps), new Rir(1), SetType.WORKING, null,
                T0.plus(Duration.ofMinutes(order * 3L)), null, null);
    }

    private static ExerciseAssessment assessment(UUID userId, UUID exerciseId,
                                                 AssessmentStatus status, Instant assessedAt) {
        return new ExerciseAssessment(UUID.randomUUID(), userId, exerciseId, status,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), AlgorithmVersion.EPLEY_V1, null, Optional.empty(), null,
                assessedAt);
    }

    private static BenchmarkTable table(UUID id, UUID exerciseId, String source, String version,
                                        BenchmarkCategory category, Kg bodyweightKg) {
        return new BenchmarkTable(id, exerciseId, source, version, category, bodyweightKg,
                true, false,
                List.of(BenchmarkPoint.of(50, "90"), BenchmarkPoint.of(80, "130")));
    }

    private static RoutineExercise routineExercise(UUID exerciseId, int order) {
        return new RoutineExercise(exerciseId, order, 4, new Reps(8), 90);
    }
}
