package com.dafit.infrastructure.config;

import com.dafit.application.usecase.AuthorizationSupport;
import com.dafit.application.usecase.AwardXpService;
import com.dafit.application.usecase.CreateRoutineService;
import com.dafit.application.usecase.EvaluateExerciseService;
import com.dafit.application.usecase.EvaluateGlobalService;
import com.dafit.application.usecase.EvaluateMuscleService;
import com.dafit.application.usecase.EvaluateUserService;
import com.dafit.application.usecase.GetAssessmentSummaryService;
import com.dafit.application.usecase.GetBodyMetricHistoryService;
import com.dafit.application.usecase.GetGamificationService;
import com.dafit.application.usecase.GetRoutinesService;
import com.dafit.application.usecase.GetWorkoutHistoryService;
import com.dafit.application.usecase.ListExercisesService;
import com.dafit.application.usecase.LoginService;
import com.dafit.application.usecase.RecordBodyMetricService;
import com.dafit.application.usecase.RegisterCredentialsService;
import com.dafit.application.usecase.RegisterExerciseService;
import com.dafit.application.usecase.ResolveBodyweightService;
import com.dafit.application.usecase.SyncBatchService;
import com.dafit.application.usecase.UserProfileService;
import com.dafit.application.usecase.WorkoutSessionService;
import com.dafit.domain.model.AlgorithmVersion;
import com.dafit.domain.model.RankScale;
import com.dafit.domain.port.in.AwardWorkoutXpUseCase;
import com.dafit.domain.port.in.CreateRoutineUseCase;
import com.dafit.domain.port.in.EvaluateExerciseUseCase;
import com.dafit.domain.port.in.EvaluateGlobalUseCase;
import com.dafit.domain.port.in.EvaluateMuscleUseCase;
import com.dafit.domain.port.in.EvaluateUserUseCase;
import com.dafit.domain.port.in.GetAssessmentSummaryUseCase;
import com.dafit.domain.port.in.GetBodyMetricHistoryUseCase;
import com.dafit.domain.port.in.GetGamificationUseCase;
import com.dafit.domain.port.in.GetRoutinesUseCase;
import com.dafit.domain.port.in.GetUserUseCase;
import com.dafit.domain.port.in.GetWorkoutHistoryUseCase;
import com.dafit.domain.port.in.ListExercisesUseCase;
import com.dafit.domain.port.in.LoginUseCase;
import com.dafit.domain.port.in.RecordBodyMetricUseCase;
import com.dafit.domain.port.in.RegisterCredentialsUseCase;
import com.dafit.domain.port.in.RegisterExerciseUseCase;
import com.dafit.domain.port.in.RegisterUserUseCase;
import com.dafit.domain.port.in.ResolveBodyweightUseCase;
import com.dafit.domain.port.in.SyncBatchUseCase;
import com.dafit.domain.port.in.WorkoutSessionUseCase;
import com.dafit.domain.port.out.BadgeRepository;
import com.dafit.domain.port.out.BenchmarkTableRepository;
import com.dafit.domain.port.out.BodyMetricRepository;
import com.dafit.domain.port.out.ClaimCodeRepository;
import com.dafit.domain.port.out.CredentialsRepository;
import com.dafit.domain.port.out.ExerciseAssessmentRepository;
import com.dafit.domain.port.out.ExerciseRepository;
import com.dafit.domain.port.out.GlobalAssessmentRepository;
import com.dafit.domain.port.out.MuscleAssessmentRepository;
import com.dafit.domain.port.out.MuscleCoefficientRepository;
import com.dafit.domain.port.out.PasswordHasher;
import com.dafit.domain.port.out.PrimaryTestRepository;
import com.dafit.domain.port.out.RoutineRepository;
import com.dafit.domain.port.out.StreakRepository;
import com.dafit.domain.port.out.SyncBatchRepository;
import com.dafit.domain.port.out.TokenIssuer;
import com.dafit.domain.port.out.UserRepository;
import com.dafit.domain.port.out.WorkoutRepository;
import com.dafit.domain.port.out.XpEventRepository;
import com.dafit.domain.service.BadgeEvaluator;
import com.dafit.domain.service.BestSetSelector;
import com.dafit.domain.service.E1RmStrategyRegistry;
import com.dafit.domain.service.GlobalRankCalculator;
import com.dafit.domain.service.MuscleScoreCalculator;
import com.dafit.domain.service.OutlierDetector;
import com.dafit.domain.service.PercentileInterpolator;
import com.dafit.domain.service.XpCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

/**
 * Registro de los casos de uso y de las reglas de autorizacion como beans de Spring.
 *
 * <p>Cada caso de uso se declara con el tipo del <b>puerto de entrada</b>, no con
 * el de la implementacion. Asi cualquier consumidor (REST, CLI, tests) depende de
 * la interfaz y el acoplamiento queda en esta unica clase de configuracion.
 *
 * <p>{@link AuthorizationSupport} es compartido: las reglas de "solo el duenio"
 * tienen que ser las mismas en todos los casos de uso (R-03).
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public AuthorizationSupport authorizationSupport(UserRepository users) {
        return new AuthorizationSupport(users);
    }

    // --- perfil ------------------------------------------------------------

    // UserProfileService implementa DOS puertos de entrada. Se construye dentro
    // de cada bean y no como bean propio: exponerlo tambien por su tipo concreto
    // creaba dos candidatos para cada puerto y Spring no sabia cual inyectar.

    /**
     * UserProfileService implementa DOS puertos de entrada (GetUserUseCase y
     * RegisterUserUseCase). NO se expone como bean propio para evitar que
     * Spring vea dos candidatos y lance NoUniqueBeanDefinitionException.
     * Se construye dentro de cada bean que lo necesita.
     */
    @Bean
    public GetUserUseCase getUserUseCase(UserRepository users,
                                         BodyMetricRepository bodyMetrics,
                                         AuthorizationSupport authorization) {
        UserProfileService service = new UserProfileService(users, bodyMetrics, authorization);
        return service;
    }

    /**
     * R-09: el alta escribe el perfil y su pesada inicial. Sin una transaccion
     * que abarque ambas, un fallo en la segunda dejaba un usuario sin el peso
     * que la fuerza relativa necesita.
     */
    @Bean
    public RegisterUserUseCase registerUserUseCase(UserRepository users,
                                                  BodyMetricRepository bodyMetrics,
                                                  AuthorizationSupport authorization,
                                                  PlatformTransactionManager transactions) {
        UserProfileService service = new UserProfileService(users, bodyMetrics, authorization);
        TransactionTemplate tx = new TransactionTemplate(transactions);
        return command -> tx.execute(status -> service.register(command));
    }

    @Bean
    public GetBodyMetricHistoryUseCase getBodyMetricHistoryUseCase(
            BodyMetricRepository bodyMetrics, AuthorizationSupport authorization) {
        return new GetBodyMetricHistoryService(bodyMetrics, authorization);
    }

    /**
     * R-09: la pesada y el peso del perfil se escriben juntos o no se escribe
     * ninguno. El decorador vive en infraestructura para no meter Spring en la
     * capa de aplicacion.
     */
    @Bean
    public RecordBodyMetricUseCase recordBodyMetricUseCase(
            BodyMetricRepository bodyMetrics,
            UserRepository users,
            AuthorizationSupport authorization,
            PlatformTransactionManager transactions) {
        RecordBodyMetricUseCase core = new RecordBodyMetricService(bodyMetrics, users, authorization);
        TransactionTemplate tx = new TransactionTemplate(transactions);
        return command -> tx.execute(status -> core.record(command));
    }

    @Bean
    public ResolveBodyweightUseCase resolveBodyweightUseCase(
            BodyMetricRepository bodyMetrics, UserRepository users, DafitProperties properties,
            Clock clock) {
        return new ResolveBodyweightService(
                bodyMetrics, users, properties.assessment().bodyweightLookupDays(), clock);
    }

    // --- catalogo ----------------------------------------------------------

    @Bean
    public ListExercisesUseCase listExercisesUseCase(ExerciseRepository exercises) {
        return new ListExercisesService(exercises);
    }

    @Bean
    public RegisterExerciseUseCase registerExerciseUseCase(ExerciseRepository exercises) {
        return new RegisterExerciseService(exercises);
    }

    // --- motor de evaluacion ------------------------------------------------

    @Bean
    public EvaluateExerciseUseCase evaluateExerciseUseCase(
            E1RmStrategyRegistry strategies,
            PercentileInterpolator interpolator,
            RankScale rankScale,
            BenchmarkTableRepository benchmarks,
            ExerciseAssessmentRepository assessments,
            ResolveBodyweightUseCase bodyweights,
            OutlierDetector outliers,
            UserRepository users,
            ExerciseRepository exercises,
            WorkoutRepository workouts,
            AuthorizationSupport authorization,
            DafitProperties properties) {

        return new EvaluateExerciseService(
                strategies, interpolator, rankScale, benchmarks, assessments, bodyweights,
                outliers, users, exercises, workouts, authorization,
                properties.engine().defaultAlgorithm());
    }

    @Bean
    public EvaluateMuscleUseCase evaluateMuscleUseCase(
            MuscleScoreCalculator calculator,
            ExerciseAssessmentRepository exerciseAssessments,
            MuscleAssessmentRepository muscleAssessments,
            MuscleCoefficientRepository coefficients,
            PrimaryTestRepository primaryTests,
            AuthorizationSupport authorization) {

        return new EvaluateMuscleService(
                calculator, exerciseAssessments, muscleAssessments, coefficients, primaryTests,
                authorization);
    }

    @Bean
    public EvaluateGlobalUseCase evaluateGlobalUseCase(
            GlobalRankCalculator calculator,
            MuscleAssessmentRepository muscleAssessments,
            GlobalAssessmentRepository globalAssessments,
            AuthorizationSupport authorization) {

        return new EvaluateGlobalService(calculator, muscleAssessments, globalAssessments, authorization);
    }

    /**
     * Orquestador del boton "evaluar mi fuerza": recalcula las series del
     * historial y compone musculo y global (R-15).
     */
    @Bean
    public EvaluateUserUseCase evaluateUserService(
            EvaluateExerciseUseCase exerciseUseCase,
            EvaluateMuscleUseCase muscleUseCase,
            EvaluateGlobalUseCase globalUseCase,
            WorkoutRepository workouts,
            MuscleCoefficientRepository coefficients,
            UserRepository users,
            BestSetSelector bestSets,
            AuthorizationSupport authorization,
            DafitProperties properties) {

        AlgorithmVersion defaultAlgorithm = properties.engine().defaultAlgorithm();
        return new EvaluateUserService(
                exerciseUseCase,
                muscleUseCase,
                globalUseCase,
                workouts,
                coefficients,
                users,
                bestSets,
                authorization,
                defaultAlgorithm);
    }

    // --- entrenamiento ------------------------------------------------------

    @Bean
    public WorkoutSessionUseCase workoutSessionUseCase(
            WorkoutRepository workouts,
            UserRepository users,
            ExerciseRepository exercises,
            PrimaryTestRepository primaryTests,
            AuthorizationSupport authorization) {
        return new WorkoutSessionService(workouts, users, exercises, primaryTests, authorization);
    }

    @Bean
    public GetWorkoutHistoryUseCase getWorkoutHistoryUseCase(
            WorkoutRepository workouts, AuthorizationSupport authorization) {
        return new GetWorkoutHistoryService(workouts, authorization);
    }

    @Bean
    public GetRoutinesUseCase getRoutinesUseCase(
            RoutineRepository routines, AuthorizationSupport authorization) {
        return new GetRoutinesService(routines, authorization);
    }

    @Bean
    public CreateRoutineUseCase createRoutineUseCase(
            RoutineRepository routines,
            ExerciseRepository exercises,
            UserRepository users,
            AuthorizationSupport authorization) {
        return new CreateRoutineService(routines, exercises, users, authorization);
    }

    @Bean
    public GetAssessmentSummaryUseCase getAssessmentSummaryUseCase(
            ExerciseAssessmentRepository exerciseAssessments,
            MuscleAssessmentRepository muscleAssessments,
            GlobalAssessmentRepository globalAssessments,
            AuthorizationSupport authorization) {

        return new GetAssessmentSummaryService(
                exerciseAssessments, muscleAssessments, globalAssessments, authorization);
    }

    // --- gamificacion -------------------------------------------------------

    @Bean
    public AwardWorkoutXpUseCase awardWorkoutXpUseCase(
            XpEventRepository xpEvents,
            StreakRepository streaks,
            BadgeRepository badges,
            WorkoutRepository workouts,
            XpCalculator xpCalculator,
            BadgeEvaluator badgeEvaluator,
            DafitProperties properties,
            PlatformTransactionManager transactions) {

        DafitProperties.Gamification gamification = properties.gamification();
        AwardWorkoutXpUseCase core = new AwardXpService(
                xpEvents, streaks, badges, workouts, xpCalculator, badgeEvaluator,
                gamification.sessionCompletedXp(), gamification.firstSessionOfWeekXp());
        // R-09: eventos, racha e insignias en una sola transaccion. Calcular
        // antes de escribir evita estados parciales; la transaccion evita que
        // un fallo entre dos guardados deje el XP a medias.
        TransactionTemplate tx = new TransactionTemplate(transactions);
        return (workout, at) -> tx.execute(status -> core.onWorkoutCompleted(workout, at));
    }

    @Bean
    public GetGamificationUseCase getGamificationUseCase(
            XpEventRepository xpEvents,
            StreakRepository streaks,
            BadgeRepository badges,
            AuthorizationSupport authorization) {

        return new GetGamificationService(xpEvents, streaks, badges, authorization);
    }

    // --- sincronizacion offline --------------------------------------------

    @Bean
    public SyncBatchUseCase syncBatchUseCase(
            WorkoutRepository workouts,
            WorkoutSessionUseCase session,
            AwardWorkoutXpUseCase awardXp,
            AuthorizationSupport authorization,
            SyncBatchRepository syncBatches,
            Clock clock) {

        return new SyncBatchService(
                workouts, session, awardXp, authorization, syncBatches, clock);
    }

    // --- seguridad ----------------------------------------------------------

    @Bean
    public RegisterCredentialsUseCase registerCredentialsUseCase(
            UserRepository users,
            CredentialsRepository credentials,
            ClaimCodeRepository claimCodes,
            PasswordHasher hasher,
            AuthorizationSupport authorization,
            PlatformTransactionManager transactions) {

        RegisterCredentialsUseCase core = new RegisterCredentialsService(
                users, credentials, claimCodes, hasher, authorization);
        // R-09: credenciales y codigo de reclamacion se guardan juntos. Si
        // fallara el codigo, la cuenta quedaria bloqueada sin forma de entrar.
        TransactionTemplate tx = new TransactionTemplate(transactions);
        return command -> tx.execute(status -> core.register(command));
    }

    @Bean
    public LoginUseCase loginUseCase(
            CredentialsRepository credentials,
            ClaimCodeRepository claimCodes,
            PasswordHasher hasher,
            TokenIssuer tokens) {

        return new LoginService(credentials, claimCodes, hasher, tokens);
    }
}
