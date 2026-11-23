package com.dafit;

import com.dafit.domain.port.in.CreateRoutineUseCase;
import com.dafit.domain.port.in.EvaluateExerciseUseCase;
import com.dafit.domain.port.in.EvaluateGlobalUseCase;
import com.dafit.domain.port.in.EvaluateMuscleUseCase;
import com.dafit.domain.port.in.GetWorkoutHistoryUseCase;
import com.dafit.domain.port.in.ListExercisesUseCase;
import com.dafit.domain.port.in.RecordBodyMetricUseCase;
import com.dafit.domain.port.in.RegisterExerciseUseCase;
import com.dafit.domain.port.in.RegisterUserUseCase;
import com.dafit.domain.port.in.ResolveBodyweightUseCase;
import com.dafit.domain.port.in.WorkoutSessionUseCase;
import com.dafit.domain.port.out.BenchmarkTableRepository;
import com.dafit.domain.port.out.BodyMetricRepository;
import com.dafit.domain.port.out.ExerciseAssessmentRepository;
import com.dafit.domain.port.out.ExerciseRepository;
import com.dafit.domain.port.out.GlobalAssessmentRepository;
import com.dafit.domain.port.out.MuscleAssessmentRepository;
import com.dafit.domain.port.out.MuscleCoefficientRepository;
import com.dafit.domain.port.out.PrimaryTestRepository;
import com.dafit.domain.port.out.RoutineRepository;
import com.dafit.domain.port.out.SyncBatchRepository;
import com.dafit.domain.port.out.UserRepository;
import com.dafit.domain.port.out.WorkoutRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que el hexagono se ensambla: contexto arriba y exactamente un
 * adaptador por puerto de salida.
 *
 * <p>Si alguien anade un segundo adaptador para un puerto, este test falla y
 * obliga a decidir cual queda activo. Es deliberado: dos adaptadores
 * del mismo puerto producen un error de Spring confuso en el arranque.
 *
 * <p>Corre con perfil {@code test} (H2 + adaptadores JPA): los tests
 * nunca tocan la base real.
 */
@SpringBootTest(properties = {
        "spring.sql.init.mode=never",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
class DafitApplicationTests {

    private static final List<Class<?>> INPUT_PORTS = List.of(
            RegisterUserUseCase.class,
            RecordBodyMetricUseCase.class,
            ResolveBodyweightUseCase.class,
            ListExercisesUseCase.class,
            RegisterExerciseUseCase.class,
            EvaluateExerciseUseCase.class,
            EvaluateMuscleUseCase.class,
            EvaluateGlobalUseCase.class,
            WorkoutSessionUseCase.class,
            GetWorkoutHistoryUseCase.class,
            CreateRoutineUseCase.class);

    private static final List<Class<?>> OUTPUT_PORTS = List.of(
            UserRepository.class,
            BodyMetricRepository.class,
            ExerciseRepository.class,
            MuscleCoefficientRepository.class,
            PrimaryTestRepository.class,
            BenchmarkTableRepository.class,
            ExerciseAssessmentRepository.class,
            MuscleAssessmentRepository.class,
            GlobalAssessmentRepository.class,
            WorkoutRepository.class,
            RoutineRepository.class,
            SyncBatchRepository.class);

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @Test
    void allInputPortsHaveExactlyOneImplementation() {
        for (Class<?> port : INPUT_PORTS) {
            assertThat(context.getBeansOfType(port))
                    .as("implementaciones de %s", port.getSimpleName())
                    .hasSize(1);
        }
    }

    @Test
    void allOutputPortsHaveExactlyOneAdapter() {
        for (Class<?> port : OUTPUT_PORTS) {
            assertThat(context.getBeansOfType(port))
                    .as("adaptadores de %s", port.getSimpleName())
                    .hasSize(1);
        }
    }

    @Test
    void bothAlgorithmVersionsAreRegistered() {
        var registry = context.getBean(com.dafit.domain.service.E1RmStrategyRegistry.class);

        assertThat(registry.versions())
                .containsExactlyInAnyOrder(
                        com.dafit.domain.model.AlgorithmVersion.EPLEY_V1,
                        com.dafit.domain.model.AlgorithmVersion.BRZYCKI_V1);
    }
}
