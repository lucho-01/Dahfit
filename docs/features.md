# DAFIT — Lista de Features (FDD)

Esta es la lista de features por construir, en el orden sugerido.
**Aquí no hay reglas de negocio**: cada feature se decide con la persona al escribir su especificación.
El agente trabaja en `main` directo. Tú decides push.

## Orden sugerido (olas)

### Ola 1 - Base y Seguridad (features críticas)
| Id | Feature | Quién | Descripción |
|----|---------|-------|-------------|
| **F0** | **Base compilable y tests verdes** | Sistema | Lograr que `./mvnw compile` pase y los 88 tests tengan los 3 fallos conocidos sin empeorar. Este es el bloqueo actual. |
| **F1** | Arreglar HexagonalArchitectureTest | Sistema | Corregir las 6 violaciones de clases de utilidad (AgeBands, AuthorizationSupport, clases internas de EvaluateUserService) que no implementan puertos. |
| **F2** | Corregir RestApiIntegrationTest.historicoDePeso | Sistema | Revisar expectativa R-05: sesión vacía y XP, racha e insignias. Ajustar comportamiento o expectativa al requisito real. |
| **F3** | Corregir SecurityIntegrationTest.elFlujoCompletoDeLoginYAccesoFunciona | Sistema | Revisar R-02: flujo login con cuenta pendiente de reclamación. Ajustar status expectation. |

### Ola 2 - Dominio y Casos de Uso
| Id | Feature | Quién | Descripción |
|----|---------|-------|-------------|
| **F10** | Implementar EvaluateUserService completo | Agente | Caso de uso completo: evaluar músculos y global, con refresh de sets. Requiere puertos out implementados. |
| **F11** | Implementar AwardXpService | Agente | XP, racha e insignias al cerrar sesión. Usa XpCalculator, BadgeEvaluator, Streak. |
| **F12** | Implementar servicios de métricas corporales | Agente | RecordBodyMetricService, GetBodyMetricHistoryService. |
| **F13** | Implementar servicios de rutinas | Agente | CreateRoutineService, GetRoutinesService. |

### Ola 3 - API REST y Controladores
| Id | Feature | Quién | Descripción |
|----|---------|-------|-------------|
| **F20** | Controlador REST: Users | Agente | Endpoints: crear usuario, obtener perfil, body-metrics, gamification. |
| **F21** | Controlador REST: Exercises | Agente | Endpoints: catálogo, evaluaciones por ejercicio. |
| **F22** | Controlador REST: Workouts | Agente | Endpoints: iniciar sesión, añadir series, finalizar, historial. |
| **F23** | Controlador REST: Routines | Agente | Endpoints: crear consultar rutinas. |
| **F24** | Controlador REST: Auth/Seguridad | Agente | Login, credenciales, JWT cuando security.enabled=true. |

### Ola 4 - Características Pendientes (después de base verde)
| Id | Feature | Quién | Descripción |
|----|---------|-------|-------------|
| **F30** | Panel de progreso visual | Agente | Endpoints y DTOs para mostrar progreso de fuerza. |
| **F31** | Rangos y percentiles avanzados | Agente | Clasificación Elite/Brutal, cálculos adicionales. |
| **F32** | Informe de evaluaciones globales | Agente | Resumen de todas las evaluaciones por usuario. |

## Notas
- **F0 es el bloqueo actual**: el proyecto no compila por paquetes faltantes (`domain.port.out`, `infrastructure.adapter.out.persistence`).
- **3 fallos conocidos** deben mantenerse sin empeorar:
  1. HexagonalArchitectureTest: 6 violaciones de clases de utilidad
  2. RestApiIntegrationTest: expectativa R-05 ajustada a 0 XP sesiones vacías
  3. SecurityIntegrationTest: flujo login cuenta pendiente reclamación (R-02)
- El agente implementa directamente en `src/main/java/` y `src/test/java/`.
- Tú decides cuándo hacer `git add`, `git commit`, `git push origin main`.
- Cada feature se especifica en `docs/specs/` antes de implementar.