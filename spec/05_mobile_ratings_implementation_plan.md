# Plan de Implementación Mobile - Sistema de Ratings (Android + iOS)

**Versión**: 1.0  
**Fecha**: 2026-05-30  
**Alcance**: Mobile — Android (Kotlin + Jetpack Compose) · iOS (Swift + SwiftUI)  
**Estimación**: ~43 horas (incluye 20% contingencia)  
**Prerequisito**: Leer `/spec/04_mobile_ratings_implementation_plan.md`  
**Dependencias**: Backend 100% completo · Frontend Web 100% completo

---

## Contexto y Estado Actual

El sistema de ratings de cursos (1–5 estrellas) ya está completamente implementado en backend y frontend web. La fase mobile es el único componente pendiente.

| Componente | Estado | Notas |
|------------|--------|-------|
| Backend FastAPI | ✅ Completo | 7 endpoints funcionando |
| Frontend Next.js | ✅ Completo | RatingSection + tests unitarios + E2E |
| Android | ❌ Pendiente | Sin pantalla de detalle ni soporte de ratings |
| iOS | ❌ Pendiente | Sin pantalla de detalle ni soporte de ratings |

---

## Correcciones al Spec Basadas en el Código Real

> Análisis realizado sobre el código fuente actual del backend (`Backend/app/`) y del frontend (`Frontend/src/`). Estas correcciones tienen precedencia sobre el spec de arquitectura `04_`.

| Ítem | Spec `04_` | Código real |
|------|-----------|-------------|
| `POST /ratings` status code | No especificado | Siempre `201 Created`, incluso en update por upsert |
| `GET .../user/{user_id}` sin rating | `204 No Content` | Backend emite `204` — mobile debe manejar `204` (el frontend maneja `404` por diferencia en su cliente HTTP) |
| `average_rating` sin ratings | No especificado | Devuelve `0.0` (nunca `null`) |
| `rating_distribution` claves | Claves string | Confirmado: JSON serializa como `"1"`, `"2"`, ..., `"5"` |
| Estado de error en UI | No especificado | **No se auto-resetea** — persiste hasta que el usuario vuelve a calificar |
| Texto "Sin valoraciones" | Texto explícito | Frontend **oculta el bloque** cuando `totalRatings == 0`; no muestra texto alternativo |
| Formato del promedio | No especificado | Una cifra decimal: `"4.5"` + total entre paréntesis |

---

## Contratos de Datos (Verificados contra el Código Real)

### DTOs — wire format (snake_case de la API)

**`RatingRequestDTO`** — body para `POST /courses/{id}/ratings`:
```json
{ "user_id": 1, "rating": 4 }
```
Validaciones en el backend: `user_id > 0`, `rating` entre `1` y `5` inclusive. Incumplimiento devuelve `422`.

**`RatingResponseDTO`** — respuesta de `POST` y `GET .../user/{userId}`:
```json
{
  "id": 1,
  "course_id": 4,
  "user_id": 1,
  "rating": 4,
  "created_at": "2025-05-29T10:00:00",
  "updated_at": "2025-05-29T10:00:00"
}
```
`created_at` y `updated_at` son **strings ISO 8601** (no timestamps numéricos).

**`RatingStatsDTO`** — respuesta de `GET /courses/{id}/ratings/stats`:
```json
{
  "average_rating": 4.35,
  "total_ratings": 142,
  "rating_distribution": { "1": 5, "2": 10, "3": 25, "4": 50, "5": 52 }
}
```
Las claves de `rating_distribution` son strings en JSON. Siempre incluye las 5 claves aunque el valor sea `0`.

### Comportamientos HTTP Críticos para Mobile

| Endpoint | Status exitoso | Caso especial |
|----------|---------------|---------------|
| `POST /courses/{id}/ratings` | `201` (siempre, incluso en upsert) | — |
| `GET /courses/{id}/ratings/stats` | `200` | `average_rating = 0.0` cuando no hay ratings |
| `GET /courses/{id}/ratings/user/{userId}` | `200` con body | `204 No Content` sin body si el usuario no ha calificado |

---

## Patrón de Actualización Optimista (Verificado en `RatingSection.tsx`)

Ambas plataformas deben replicar este flujo exacto:

```
1. Guardar:  previousRating = userRating
2. Aplicar:  userRating = newStars  (inmediato, antes de la respuesta de red)
             ratingState = LOADING
3. POST al backend
4a. Éxito:   Obtener stats actualizadas (GET /ratings/stats)
             ratingState = SUCCESS
             Esperar 2000ms
             ratingState = IDLE
4b. Error:   userRating = previousRating  (rollback)
             ratingState = ERROR  (permanece hasta el próximo intento del usuario)
```

> **Importante**: los stats (promedio/total) **no** se actualizan optimistamente — se esperan a la respuesta del servidor. El estado `ERROR` no se resetea automáticamente.

---

## FASE 1 — Data Layer: DTOs y Modelos de Dominio

**Estimación**: 1.5 h por plataforma  
**Dependencias**: ninguna

### Android

1. Crear `data/entities/RatingDTO.kt`:
   - `RatingResponseDTO` con todos los campos usando `@SerializedName`
   - `RatingStatsDTO` con `ratingDistribution: Map<String, Int>`
   - `RatingRequestDTO` para el body del POST

2. Crear `domain/models/CourseRating.kt`: modelo de dominio sin `ratingDistribution`

3. Crear `domain/models/RatingStats.kt`: solo `averageRating: Double` y `totalRatings: Int`

4. Crear `data/mappers/RatingMapper.kt`: conversión DTO → dominio

**Criterio de aceptación**: los DTOs deserializan correctamente los JSON de ejemplo de la sección anterior.

### iOS

1. Crear `Data/Entities/RatingDTO.swift`:
   - `RatingResponseDTO`: struct Codable con `CodingKeys` en snake_case
   - `RatingStatsDTO` con `ratingDistribution: [String: Int]`
   - `RatingRequestDTO`: struct Encodable

2. Crear `Domain/Models/CourseRating.swift`: modelo de dominio

3. Crear `Domain/Models/RatingStats.swift`: solo `averageRating` y `totalRatings`

4. Crear `Data/Mapper/RatingMapper.swift`: struct con métodos de conversión DTO → dominio

**Criterio de aceptación**: los structs deserializan los JSON de ejemplo, incluyendo `rating_distribution` con claves string.

---

## FASE 2 — Data Layer: Repositorio y Endpoints de Red

**Estimación**: 2.5 h por plataforma  
**Dependencias**: Fase 1 completada

### Android

1. Definir `domain/repositories/RatingRepository.kt` (interface):
   - `getRatingStats(courseId: Int): Result<RatingStats>`
   - `getUserRating(courseId: Int, userId: Int): Result<CourseRating?>`
   - `upsertRating(courseId: Int, userId: Int, stars: Int): Result<CourseRating>`

2. Extender `data/network/ApiService.kt` con los tres endpoints:
   - `@POST("courses/{courseId}/ratings")`
   - `@GET("courses/{courseId}/ratings/stats")`
   - `@GET("courses/{courseId}/ratings/user/{userId}")`

3. Crear `data/repositories/RemoteRatingRepository.kt`:
   - `getUserRating`: verificar `response.code() == 204` **antes** de llamar a `body()` — devolver `Result.success(null)` en ese caso
   - `upsertRating`: `201` es el único código de éxito esperado
   - `getRatingStats`: `average_rating = 0.0` es válido, no tratar como error

4. Registrar `RatingRepository` en `di/AppModule.kt`

**Criterio de aceptación**: `getUserRating` en un curso sin ratings devuelve `Result.success(null)` sin lanzar excepción.

### iOS

1. Definir `Domain/Repositories/RatingRepositoryProtocol.swift`:
   - `getRatingStats(courseId:) async throws -> RatingStats`
   - `getUserRating(courseId:userId:) async throws -> CourseRating?`
   - `upsertRating(courseId:userId:stars:) async throws -> CourseRating`

2. Crear `Data/Repositories/RatingAPIEndpoints.swift` como enum conformando `APIEndpoint` — siguiendo el patrón existente en `CourseAPIEndpoints`

3. Crear `Data/Repositories/RemoteRatingRepository.swift` usando `NetworkManager.shared`:
   - `getUserRating`: capturar `NetworkError.requestFailed(statusCode: 204)` y devolver `nil`
   - `upsertRating`: esperar `201` como respuesta exitosa
   - `getRatingStats`: `0.0` en `average_rating` es válido

**Criterio de aceptación**: `getUserRating` en curso sin ratings devuelve `nil` sin propagar error.

---

## FASE 3 — Presentation: ViewModel

**Estimación**: 3 h por plataforma  
**Dependencias**: Fase 2 completada

### Android

1. Crear `presentation/coursedetail/state/CourseDetailUiState.kt`:
   - Enum `RatingState { IDLE, LOADING, SUCCESS, ERROR }`
   - Data class `CourseDetailUiState` con: `isLoading`, `courseName`, `courseDescription`, `courseThumbnail`, `courseId`, `ratingStats: RatingStats?`, `userRating: Int?`, `ratingState`, `error: String?`

2. Crear `presentation/coursedetail/viewmodel/CourseDetailViewModel.kt`:
   - Recibe `courseId: Int` vía factory
   - En `init`: lanza `loadRatingStats()` y `loadUserRating()` en coroutine
   - `submitRating(stars: Int)`:
     1. Guarda `previousRating = uiState.userRating`
     2. Emite `userRating = stars` + `ratingState = LOADING` inmediatamente
     3. Llama `upsertRating` en background
     4. Si falla: restaura `previousRating` + emite `ratingState = ERROR`
     5. Si exitoso: obtiene stats actualizadas + emite `SUCCESS` → delay 2000ms → emite `IDLE`
   - Estado `ERROR` no se resetea automáticamente

3. Centralizar constantes en el companion object:
   - `HARDCODED_USER_ID = 1`
   - `RATING_SUCCESS_RESET_DELAY_MS = 2000L`

4. Registrar factory en `di/AppModule.kt`

**Criterio de aceptación**: la actualización optimista revierte correctamente ante un error de red simulado.

### iOS

1. Crear `Presentation/ViewModels/CourseDetailViewModel.swift`:
   - Clase `@MainActor` que conforma `ObservableObject`
   - `@Published var userRating: Int?`
   - `@Published var ratingStats: RatingStats?`
   - `@Published var ratingState: RatingState`
   - `@Published var isLoading: Bool`
   - `@Published var errorMessage: String?`
   - `loadData()` ejecutado en `Task { }` — no bloquea el hilo principal
   - `submitRating(stars: Int)` con el patrón optimista descrito arriba:
     - Éxito: `ratingState = .success` → `Task.sleep(2_000_000_000)` → `ratingState = .idle`
     - Error: restaurar rating previo + `ratingState = .error` (sin auto-reset)

2. Centralizar constantes en `AppConstants`:
   - `hardcodedUserId = 1`
   - `ratingSuccessResetDelayNanoseconds: UInt64 = 2_000_000_000`

**Criterio de aceptación**: todas las actualizaciones de `@Published` ocurren en el hilo principal gracias a `@MainActor`.

---

## FASE 4 — UI: Componentes Visuales de Rating

**Estimación**: 6 h por plataforma  
**Dependencias**: Fase 3 completada

### Android

1. Crear `presentation/coursedetail/components/StarRatingBar.kt` (Composable):
   - `Row` de 5 `Icon` (Material Icons), alternando lleno/vacío según `rating: Int`
   - Parámetros: `rating: Int`, `onRatingChange: (Int) -> Unit`, `enabled: Boolean`
   - Con `enabled = false`: opacidad reducida + sin respuesta a interacciones

2. Crear `presentation/coursedetail/components/RatingSection.kt` (Composable):
   - Bloque de stats visible **solo cuando** `totalRatings > 0` (sin texto alternativo cuando es 0)
   - Formato del promedio: `"X.X"` + total entre paréntesis
   - `StarRatingBar` interactivo con `enabled = ratingState != LOADING`
   - Feedback textual por estado: "Guardando…" / "¡Calificación guardada!" / mensaje de error

3. Crear `presentation/coursedetail/screen/CourseDetailScreen.kt` (Composable):
   - Thumbnail del curso, nombre, descripción
   - Integra `RatingSection`

**Criterio de aceptación**: el bloque de stats no aparece si `ratingStats.totalRatings == 0`.

### iOS

1. Crear `Presentation/Views/StarRatingView.swift`:
   - `HStack` de 5 `Image(systemName:)` alternando `"star.fill"` (amarillo) / `"star"` (gris)
   - `onTapGesture` individual por estrella
   - `.disabled(isSubmitting)` cuando está en estado loading
   - `accessibilityLabel` por estrella: `"1 estrella"`, `"2 estrellas"`, etc.

2. Crear `Presentation/Views/RatingSectionView.swift`:
   - `ProgressView` durante estado `.loading`
   - Bloque de stats visible **solo cuando** `totalRatings > 0` (sin texto alternativo)
   - Formato: `"X.X"` + `"(N valoraciones)"`
   - Feedback por estado: "Guardando…" / "¡Calificación guardada!" / mensaje de error

3. Crear `Presentation/Views/CourseDetailView.swift`:
   - Inicializada con `course: Course`
   - `AsyncImage` para thumbnail siguiendo el patrón de `CourseCardView`
   - Carga datos en `.onAppear { Task { await viewModel.loadData() } }`
   - Integra `RatingSectionView`

**Criterio de aceptación**: `StarRatingView` renderiza 5 íconos llenos hasta el valor de `rating` y vacíos el resto.

---

## FASE 5 — Navegación

**Estimación**: 2.5 h Android · 1 h iOS  
**Dependencias**: Fase 4 completada

### Android

1. Agregar `@Serializable` al modelo `Course` para serializar el objeto completo en el backstack (evita un request extra al abrir el detalle)

2. Actualizar `MainActivity.kt`:
   - Implementar `NavHost` con rutas `"courses"` y `"course/{courseId}"`
   - Conectar el `onCourseClick = {}` vacío actual para navegar a `CourseDetailScreen`

3. Actualizar `di/AppModule.kt` con la factory del `CourseDetailViewModel` que recibe `courseId`

**Criterio de aceptación**: tap en una `CourseCard` abre `CourseDetailScreen` con el `courseId` correcto; el Back nativo regresa a la lista.

### iOS

1. Actualizar `Presentation/Views/CourseListView.swift`:
   - Envolver `CourseCardView` en `NavigationLink(destination: CourseDetailView(course: course))` dentro del `NavigationView` existente

2. El botón Back nativo de iOS funciona sin configuración adicional

**Criterio de aceptación**: `NavigationLink` navega a `CourseDetailView`; el Back regresa a la lista sin configuración extra.

---

## FASE 6 — Tests del ViewModel

**Estimación**: 2.5 h por plataforma  
**Dependencias**: Fase 3 completada (puede hacerse en paralelo con Fases 4 y 5)

### Android

Crear `presentation/coursedetail/viewmodel/CourseDetailViewModelTest.kt`:

| Caso de test | Descripción |
|---|---|
| Estado inicial | `isLoading = true`, `ratingStats = null`, `userRating = null` |
| Carga exitosa | Tras respuestas OK: `isLoading = false`, `ratingStats` y `userRating` poblados |
| Actualización optimista | `userRating` cambia **antes** de que el mock de repositorio responda |
| Rollback en error | `userRating` revierte al valor anterior si `upsertRating` lanza excepción |
| Transición SUCCESS → IDLE | `ratingState == SUCCESS` pasa a `IDLE` después de 2 segundos (usar `TestCoroutineScheduler`) |

### iOS

Crear `PlatziFlixiOSTests/CourseDetailViewModelTests.swift`:

| Caso de test | Descripción |
|---|---|
| Estado inicial | `isLoading = true`, `ratingStats = nil`, `userRating = nil` |
| Carga exitosa | `loadData()` puebla `ratingStats` y `userRating` con datos del mock |
| Actualización optimista | `userRating` se actualiza antes de que el mock de repositorio responda |
| Rollback en error | `userRating` revierte si el mock de `upsertRating` lanza error |
| Transición SUCCESS → IDLE | `ratingState` vuelve a `.idle` tras 2 segundos (usar `XCTestExpectation` o clock controlable) |

---

## Orden de Ejecución y Dependencias

```
Fase 1 (DTOs / Modelos)
    └─→ Fase 2 (Repositorio)
            └─→ Fase 3 (ViewModel)
                    ├─→ Fase 4 (UI)
                    │       └─→ Fase 5 (Navegación)
                    └─→ Fase 6 (Tests)  ← paralela con Fases 4 y 5
```

---

## Archivos de Referencia

| Propósito | Ruta |
|-----------|------|
| Schemas Pydantic (request/response exactos) | `Backend/app/schemas/rating.py` |
| Modelo SQLAlchemy + constraints | `Backend/app/models/course_rating.py` |
| Endpoints con semántica HTTP completa | `Backend/app/main.py` |
| Manejo del 204 en cliente web | `Frontend/src/services/ratingsApi.ts` |
| Patrón de actualización optimista | `Frontend/src/components/CourseDetail/RatingSection.tsx` |
| Tipos TypeScript de ratings | `Frontend/src/types/rating.ts` |
| Patrón de repositorio Android | `Mobile/PlatziFlixAndroid/app/src/main/java/.../data/repositories/RemoteCourseRepository.kt` |
| Patrón de repositorio iOS | `Mobile/PlatziFlixiOS/PlatziFlixiOS/Data/Repositories/RemoteCourseRepository.swift` |
| Patrón de endpoints iOS | `Mobile/PlatziFlixiOS/PlatziFlixiOS/Data/Repositories/CourseAPIEndpoints.swift` |
| Errores de red iOS | `Mobile/PlatziFlixiOS/PlatziFlixiOS/Services/NetworkError.swift` |
| Tests de ViewModel Android de referencia | `Mobile/PlatziFlixAndroid/app/src/test/.../CourseListViewModelTest.kt` |

---

## Estimación de Esfuerzo

| Fase | Android | iOS |
|------|---------|-----|
| Fase 1 — DTOs y Modelos | 1.5 h | 1.5 h |
| Fase 2 — Repositorio y Red | 2.5 h | 2.5 h |
| Fase 3 — ViewModel | 3.0 h | 3.0 h |
| Fase 4 — UI | 6.0 h | 6.5 h |
| Fase 5 — Navegación | 2.5 h | 1.0 h |
| Fase 6 — Tests | 2.5 h | 2.5 h |
| **Total base** | **18.0 h** | **17.0 h** |
| + 20% contingencia | 3.6 h | 3.4 h |
| **Total con contingencia** | **~21.6 h** | **~20.4 h** |

**Total proyecto: ~42 h**
