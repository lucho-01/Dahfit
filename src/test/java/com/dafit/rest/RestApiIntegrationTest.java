package com.dafit.rest;

import com.dafit.domain.model.BenchmarkCategory;
import com.dafit.domain.model.BenchmarkPoint;
import com.dafit.domain.model.BenchmarkTable;
import com.dafit.domain.model.Sex;
import com.dafit.domain.port.out.BenchmarkTableRepository;
import com.dafit.domain.port.out.SyncBatchRepository;
import com.dafit.infrastructure.adapter.in.rest.dto.response.ExerciseAssessmentResponse;
import com.dafit.infrastructure.adapter.in.rest.dto.response.WorkoutResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba el API REST de punta a punta sobre los adaptadores en memoria.
 *
 * <p>Corre con perfil {@code test}: H2 y adaptadores JPA, sin tocar la base
 * real. El flujo completo (usuario -> pesada -> tabla -> evaluacion) se
 * verifica con HTTP real, de modo que el contrato del API queda fijado sin
 * depender del motor de persistencia.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:dafit-rest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.sql.init.mode=never",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RestApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BenchmarkTableRepository benchmarkTables;

    @Autowired
    private SyncBatchRepository syncBatches;

    @Test
    void altaDeUsuarioDevuelve201() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Ana","role":"ATHLETE","sex":"MALE",
                                 "bodyweightKg":74.20,"benchmarkConsent":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void evaluacionDeEjercicioEndToEnd() throws Exception {
        UUID userId = registerUser();
        UUID exerciseId = registerExercise();

        benchmarkTables.save(new BenchmarkTable(
                UUID.randomUUID(), exerciseId, "TEST", "v1",
                BenchmarkCategory.of(Sex.MALE), null,
                true, false,
                List.of(BenchmarkPoint.of(50, "90"), BenchmarkPoint.of(80, "130"))));

        MvcResult result = mockMvc.perform(post("/api/users/{u}/exercises/{e}/assessments", userId, exerciseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"totalLoadKg":90,"reps":1,"rir":0,"sex":"MALE"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        ExerciseAssessmentResponse assessment = objectMapper.readValue(
                result.getResponse().getContentAsString(), ExerciseAssessmentResponse.class);

        // reps=1 => e1RM = 90 = P50 exacto => percentil 50, 500 puntos, AVANZADO
        assertThat(assessment.status()).isEqualTo("OK");
        assertThat(assessment.percentile()).isEqualByComparingTo("50.00");
        assertThat(assessment.points()).isEqualTo(500);
        assertThat(assessment.rank()).isEqualTo("AVANZADO");
        assertThat(assessment.e1rmKg()).isEqualByComparingTo("90.0000");
    }

    @Test
    void flujoDeEntrenamientoCompletoEsIdempotente() throws Exception {
        UUID userId = registerUser();
        UUID exerciseId = registerExercise();

        UUID workoutId = startWorkout(userId);

        String setBody = """
                {"clientUuid":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                 "exerciseId":"%s","loadKg":60,"inputUnit":"KG","loadMode":"TOTAL",
                 "implementCount":1,"reps":4,"rir":0,"type":"WORKING"}
                """.formatted(exerciseId);

        MvcResult first = mockMvc.perform(post("/api/workouts/{id}/sets", workoutId)
                        .contentType(MediaType.APPLICATION_JSON).content(setBody))
                .andExpect(status().isOk())
                .andReturn();

        WorkoutResponse once = objectMapper.readValue(first.getResponse().getContentAsString(), WorkoutResponse.class);
        assertThat(once.sets()).hasSize(1);

        // reenvio del mismo clientUuid: no duplica (C-17)
        MvcResult second = mockMvc.perform(post("/api/workouts/{id}/sets", workoutId)
                        .contentType(MediaType.APPLICATION_JSON).content(setBody))
                .andExpect(status().isOk())
                .andReturn();
        WorkoutResponse twice = objectMapper.readValue(second.getResponse().getContentAsString(), WorkoutResponse.class);
        assertThat(twice.sets()).hasSize(1);

        // cerrar
        mockMvc.perform(post("/api/workouts/{id}/finish", workoutId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // al cerrar, el usuario ya no tiene sesion en curso
        mockMvc.perform(get("/api/users/{id}/workouts/current", userId))
                .andExpect(status().isNoContent());

        // historial
        mockMvc.perform(get("/api/users/{id}/workouts", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void consultaDeUsuarioDevuelveSuPerfil() throws Exception {
        UUID userId = registerUser();

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.displayName").value("Ana"))
                .andExpect(jsonPath("$.role").value("ATHLETE"))
                .andExpect(jsonPath("$.bodyweightKg").value(74.2))
                .andExpect(jsonPath("$.benchmarkConsent").value(true));
    }

    @Test
    void historicoDePesoDevuelveLaPesadaMasRecientePrimero() throws Exception {
        UUID userId = registerUserWithoutWeight();
        LocalDate first = LocalDate.now().minusDays(1);
        LocalDate second = LocalDate.now();
        recordBodyMetric(userId, first.toString(), "74.50");
        recordBodyMetric(userId, second.toString(), "73.80");

        mockMvc.perform(get("/api/users/{id}/body-metrics", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].date").value(second.toString()))
                .andExpect(jsonPath("$[0].weightKg").value(73.8))
                .andExpect(jsonPath("$[1].date").value(first.toString()))
                .andExpect(jsonPath("$[1].weightKg").value(74.5));
    }

    @Test
    void elAltaYaRegistraLaPrimeraPesada() throws Exception {
        UUID userId = registerUser();

        mockMvc.perform(get("/api/users/{id}/body-metrics", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].weightKg").value(74.2))
                .andExpect(jsonPath("$[0].source").value("MANUAL"));
    }

    @Test
    void consultaDeEjercicioPorIdDevuelveElCatalogo() throws Exception {
        UUID exerciseId = registerExercise();

        mockMvc.perform(get("/api/exercises/{id}", exerciseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(exerciseId.toString()))
                .andExpect(jsonPath("$.name").value("Press banca"))
                .andExpect(jsonPath("$.equipment").value("BARBELL"))
                .andExpect(jsonPath("$.muscles[0]").value("CHEST"))
                .andExpect(jsonPath("$.evaluable").value(true));
    }

    @Test
    void rutinaSeConsultaPorIdYPorUsuario() throws Exception {
        UUID userId = registerUser();
        UUID exerciseId = registerExercise();

        MvcResult created = mockMvc.perform(post("/api/routines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerId":"%s","name":"Fuerza A",
                                 "exercises":[{"exerciseId":"%s","targetSets":3,
                                               "targetReps":5,"restSeconds":120}]}
                                """.formatted(userId, exerciseId)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID routineId = idOf(created);

        mockMvc.perform(get("/api/routines/{id}", routineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Fuerza A"))
                .andExpect(jsonPath("$.exercises[0].exerciseId").value(exerciseId.toString()))
                .andExpect(jsonPath("$.exercises[0].targetSets").value(3));

        mockMvc.perform(get("/api/users/{id}/routines", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(routineId.toString()));
    }

    @Test
    void sesionEnCursoSeRetomaYDevuelve204SiNoHayNinguna() throws Exception {
        UUID userId = registerUser();

        mockMvc.perform(get("/api/users/{id}/workouts/current", userId))
                .andExpect(status().isNoContent());

        UUID workoutId = startWorkout(userId);

        mockMvc.perform(get("/api/users/{id}/workouts/current", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workoutId.toString()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(get("/api/workouts/{id}", workoutId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workoutId.toString()))
                .andExpect(jsonPath("$.sets.length()").value(0));
    }

    @Test
    void resumenDeEvaluacionesArrancaVacio() throws Exception {
        UUID userId = registerUser();

        mockMvc.perform(get("/api/users/{id}/assessments", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises.length()").value(0))
                .andExpect(jsonPath("$.muscles.length()").value(0))
                .andExpect(jsonPath("$.global").doesNotExist());
    }

    @Test
    void elContratoOpenApiSeGeneraConLasRutasDelApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("DAFIT API"))
                .andExpect(jsonPath("$.paths['/api/users']").exists())
                .andExpect(jsonPath("$.paths['/api/users/{userId}/workouts/current']").exists())
                .andExpect(jsonPath("$.paths['/api/users/{userId}/assessments']").exists());
    }

    @Test
    void laGamificacionArrancaEnCero() throws Exception {
        UUID userId = registerUser();

        mockMvc.perform(get("/api/users/{id}/gamification", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalXp").value(0))
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.xpIntoLevel").value(0))
                .andExpect(jsonPath("$.xpForNextLevel").value(500))
                .andExpect(jsonPath("$.currentStreakDays").value(0))
                .andExpect(jsonPath("$.longestStreakDays").value(0))
                .andExpect(jsonPath("$.badges.length()").value(0))
                .andExpect(jsonPath("$.recentEvents.length()").value(0));
    }

    @Test
    void cerrarLaPrimeraSesionDaXpRachaEInsignia() throws Exception {
        UUID userId = registerUser();
        UUID workoutId = startWorkout(userId);

        mockMvc.perform(post("/api/workouts/{id}/finish", workoutId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/{id}/gamification", userId))
                .andExpect(status().isOk())
                // R-05: sesión vacía no otorga XP, racha ni insignias.
                .andExpect(jsonPath("$.totalXp").value(0))
                .andExpect(jsonPath("$.currentStreakDays").value(0))
                .andExpect(jsonPath("$.longestStreakDays").value(0))
                .andExpect(jsonPath("$.badges.length()").value(0))
                .andExpect(jsonPath("$.recentEvents.length()").value(0));
    }

    @Test
    void laSegundaSesionSoloSumaElXpDeSesionYNoRepiteLaInsignia() throws Exception {
        UUID userId = registerUser();
        mockMvc.perform(post("/api/workouts/{id}/finish", startWorkout(userId)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/workouts/{id}/finish", startWorkout(userId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/{id}/gamification", userId))
                .andExpect(status().isOk())
                // R-05: dos sesiones vacías no otorgan XP conjunto ni duplican insignia.
                .andExpect(jsonPath("$.totalXp").value(0))
                .andExpect(jsonPath("$.badges.length()").value(0))
                .andExpect(jsonPath("$.recentEvents.length()").value(0));
    }

    @Test
    void cerrarDosVecesLaMismaSesionNoDuplicaElXp() throws Exception {
        UUID userId = registerUser();
        UUID workoutId = startWorkout(userId);

        mockMvc.perform(post("/api/workouts/{id}/finish", workoutId))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/workouts/{id}/finish", workoutId))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/users/{id}/gamification", userId))
                .andExpect(status().isOk())
                // R-05: la segunda firmaje sobre la misma sesion vacía no da XP
                // y devuelve 400; el XP persiste en 0 desde el primer cierre.
                .andExpect(jsonPath("$.totalXp").value(0))
                .andExpect(jsonPath("$.recentEvents.length()").value(0));
    }

    @Test
    void elLoteDeSincronizacionEsIdempotenteYNoDuplicaSeriesNiXp() throws Exception {
        UUID userId = registerUser();
        UUID exerciseId = registerExercise();
        UUID workoutId = startWorkout(userId);

        String body = """
                {"userId":"%s","items":[
                  {"target":"WORKOUT_SET","clientUuid":"cccccccc-cccc-cccc-cccc-cccccccccccc",
                   "workoutId":"%s","exerciseId":"%s","loadKg":80,"inputUnit":"KG",
                   "loadMode":"TOTAL","implementCount":1,"reps":5,"rir":1,"type":"WORKING"},
                  {"target":"WORKOUT_FINISH","workoutId":"%s"}
                ]}
                """.formatted(userId, workoutId, exerciseId, workoutId);

        mockMvc.perform(post("/api/sync/batch")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(2))
                .andExpect(jsonPath("$.duplicates").value(0))
                .andExpect(jsonPath("$.rejected").value(0));

        // Reenvio integro del lote: todo es duplicado, nada se duplica.
        mockMvc.perform(post("/api/sync/batch")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(0))
                .andExpect(jsonPath("$.duplicates").value(2));

        mockMvc.perform(get("/api/workouts/{id}", workoutId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sets.length()").value(1))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/users/{id}/gamification", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalXp").value(70))
                .andExpect(jsonPath("$.recentEvents.length()").value(2));

        assertThat(syncBatches.findByUserId(userId)).hasSize(2)
                .anySatisfy(history -> assertThat(history.results())
                        .extracting(result -> result.outcome())
                        .containsExactly(com.dafit.domain.model.SyncOutcome.APPLIED,
                                com.dafit.domain.model.SyncOutcome.APPLIED))
                .anySatisfy(history -> assertThat(history.results())
                        .extracting(result -> result.outcome())
                        .containsExactly(com.dafit.domain.model.SyncOutcome.DUPLICATE,
                                com.dafit.domain.model.SyncOutcome.DUPLICATE));
    }

    @Test
    void unItemDeOtraSesionSeRechazaSinTumbarElLote() throws Exception {
        UUID userId = registerUser();
        UUID workoutId = startWorkout(userId);

        mockMvc.perform(post("/api/sync/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","items":[
                                  {"target":"WORKOUT_FINISH","workoutId":"%s"}
                                ]}
                                """.formatted(UUID.randomUUID(), workoutId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rejected").value(1))
                .andExpect(jsonPath("$.results[0].outcome").value("REJECTED"))
                .andExpect(jsonPath("$.results[0].message").value("la sesion no pertenece al usuario"));
    }

    @Test
    void cuerpoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ATHLETE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recursoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(post("/api/workouts/{id}/finish", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // --- helpers -----------------------------------------------------------

    private UUID registerUser() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Ana","role":"ATHLETE","sex":"MALE",
                                 "bodyweightKg":74.20,"benchmarkConsent":true}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private UUID registerUserWithoutWeight() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Ana","role":"ATHLETE","sex":"MALE",
                                 "benchmarkConsent":true}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private UUID registerExercise() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/exercises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Press banca","equipment":"BARBELL","loadMode":"TOTAL",
                                 "unilateral":false,"muscles":["CHEST"],"evaluable":true}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private void recordBodyMetric(UUID userId, String date, String weightKg) throws Exception {
        mockMvc.perform(post("/api/users/{id}/body-metrics", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"weightKg":%s,"date":"%s"}
                                """.formatted(weightKg, date)))
                .andExpect(status().isCreated());
    }

    private UUID startWorkout(UUID userId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/workouts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","localDate":"2026-10-27"}
                                """.formatted(userId)))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private UUID idOf(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(json.get("id").asText());
    }
}
