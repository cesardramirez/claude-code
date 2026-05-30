# Plan de Implementación: Sistema de Ratings Mobile (Android + iOS)

## Metadatos

| Campo | Valor |
|-------|-------|
| **Fecha** | 2026-05-29 |
| **Estado** | Pendiente de implementación |
| **Plataformas** | Android (Kotlin + Jetpack Compose) · iOS (Swift + SwiftUI) |
| **Estimación total** | ~43 horas (incluye 20% contingencia) |
| **Dependencias** | Backend 100% completo · Frontend Web 100% completo |

---

## 1. Contexto y Estado Actual

El sistema de ratings de cursos (1–5 estrellas) ya está completamente implementado en backend y frontend web. La fase mobile es el único componente pendiente.

### Estado por componente

| Componente | Estado | Notas |
|------------|--------|-------|
| Backend FastAPI | ✅ Completo | 7 endpoints funcionando |
| Frontend Next.js | ✅ Completo | RatingSection + tests unitarios + E2E |
| Android | ❌ Pendiente | Sin pantalla de detalle ni soporte de ratings |
| iOS | ❌ Pendiente | Sin pantalla de detalle ni soporte de ratings |

### Endpoints disponibles en la API

| Operación | Endpoint | Método | Notas |
|-----------|----------|--------|-------|
| Crear o actualizar rating | `POST /courses/{id}/ratings` | POST | Upsert — body `{user_id, rating}` |
| Obtener todos los ratings | `GET /courses/{id}/ratings` | GET | No usado en mobile MVP |
| Obtener estadísticas | `GET /courses/{id}/ratings/stats` | GET | Devuelve promedio + distribución |
| Obtener rating del usuario | `GET /courses/{id}/ratings/user/{user_id}` | GET | HTTP 204 si no ha calificado |
| Actualizar rating | `PUT /courses/{id}/ratings/{user_id}` | PUT | No usado (upsert via POST) |
| Eliminar rating | `DELETE /courses/{id}/ratings/{user_id}` | DELETE | Solo para tests/limpieza |

---

## 2. Análisis de Impacto por Plataforma

### 2.1 Android

**Archivos nuevos a crear**

| Ruta | Tipo | Propósito |
|------|------|-----------|
| `data/entities/RatingDTO.kt` | Data class | DTOs de rating y stats de la API |
| `data/mappers/RatingMapper.kt` | Object | DTO → modelo de dominio |
| `domain/models/CourseRating.kt` | Data class | Modelo de dominio |
| `domain/models/RatingStats.kt` | Data class | Estadísticas de dominio |
| `domain/repositories/RatingRepository.kt` | Interface | Contrato del repositorio |
| `data/repositories/RemoteRatingRepository.kt` | Class | Implementación con Retrofit |
| `presentation/coursedetail/state/CourseDetailUiState.kt` | Data class | Estado de UI para detalle |
| `presentation/coursedetail/viewmodel/CourseDetailViewModel.kt` | ViewModel | Lógica de detalle + ratings |
| `presentation/coursedetail/components/StarRatingBar.kt` | Composable | Estrellas interactivas |
| `presentation/coursedetail/components/RatingSection.kt` | Composable | Sección completa de ratings |
| `presentation/coursedetail/screen/CourseDetailScreen.kt` | Composable | Pantalla de detalle del curso |
| `presentation/coursedetail/viewmodel/CourseDetailViewModelTest.kt` | Test | Tests del ViewModel |

**Archivos existentes a modificar**

| Ruta | Cambio |
|------|--------|
| `data/network/ApiService.kt` | Agregar 3 endpoints de ratings |
| `di/AppModule.kt` | Proveer `RatingRepository` y `CourseDetailViewModel` |
| `MainActivity.kt` | Implementar `NavHost` y navegación a detalle |

### 2.2 iOS

**Archivos nuevos a crear**

| Ruta | Tipo | Propósito |
|------|------|-----------|
| `Data/Entities/RatingDTO.swift` | Struct Codable | DTOs de rating y stats |
| `Data/Mapper/RatingMapper.swift` | Struct | DTO → modelo de dominio |
| `Domain/Models/CourseRating.swift` | Struct | Modelo de dominio |
| `Domain/Models/RatingStats.swift` | Struct | Estadísticas de dominio |
| `Domain/Repositories/RatingRepositoryProtocol.swift` | Protocol | Contrato del repositorio |
| `Data/Repositories/RatingAPIEndpoints.swift` | Enum | Definición de endpoints (conforme a `APIEndpoint`) |
| `Data/Repositories/RemoteRatingRepository.swift` | Final class | Implementación con `NetworkManager` |
| `Presentation/ViewModels/CourseDetailViewModel.swift` | ObservableObject | Lógica de detalle + ratings |
| `Presentation/Views/StarRatingView.swift` | View | Estrellas interactivas |
| `Presentation/Views/RatingSectionView.swift` | View | Sección completa de ratings |
| `Presentation/Views/CourseDetailView.swift` | View | Vista de detalle del curso |
| `PlatziFlixiOSTests/CourseDetailViewModelTests.swift` | Tests | Tests del ViewModel |

**Archivos existentes a modificar**

| Ruta | Cambio |
|------|--------|
| `Presentation/Views/CourseListView.swift` | Agregar `NavigationLink` a `CourseDetailView` |
| `Presentation/ViewModels/CourseListViewModel.swift` | `selectCourse` dispara navegación real |

---

## 3. Diagramas de Arquitectura

### 3.1 Android

```
┌─────────────────────────────────────────────┐
│              Presentation Layer             │
│                                             │
│  CourseDetailScreen (Composable)            │
│    ├── CourseDetailViewModel (ViewModel)    │
│    │     ├── uiState: StateFlow<UiState>    │
│    │     ├── loadRatingStats()              │
│    │     ├── loadUserRating()               │
│    │     └── submitRating(stars: Int)       │
│    ├── RatingSection (Composable)           │
│    └── StarRatingBar (Composable)           │
└────────────────────┬────────────────────────┘
                     │ injected via AppModule
┌────────────────────▼────────────────────────┐
│              Domain Layer                   │
│                                             │
│  RatingRepository (interface)               │
│    ├── getRatingStats(courseId)             │
│    ├── getUserRating(courseId, userId)      │
│    └── upsertRating(courseId, userId, stars)│
│                                             │
│  Models: CourseRating, RatingStats          │
└────────────────────┬────────────────────────┘
                     │ implements
┌────────────────────▼────────────────────────┐
│               Data Layer                    │
│                                             │
│  RemoteRatingRepository                     │
│    └── ApiService (Retrofit)                │
│         ├── POST /courses/{id}/ratings      │
│         ├── GET  /courses/{id}/ratings/stats│
│         └── GET  /courses/{id}/ratings/     │
│                  user/{user_id}             │
│                                             │
│  RatingMapper: RatingDTO → CourseRating     │
└─────────────────────────────────────────────┘
```

### 3.2 iOS

```
┌─────────────────────────────────────────────┐
│              Presentation Layer             │
│                                             │
│  CourseDetailView (SwiftUI View)            │
│    ├── CourseDetailViewModel (@StateObject) │
│    │     ├── @Published userRating: Int?    │
│    │     ├── @Published ratingStats         │
│    │     ├── @Published ratingState         │
│    │     ├── loadRatingStats()              │
│    │     ├── loadUserRating()               │
│    │     └── submitRating(stars: Int)       │
│    ├── RatingSectionView (View)             │
│    └── StarRatingView (View)                │
└────────────────────┬────────────────────────┘
                     │ injected via init
┌────────────────────▼────────────────────────┐
│              Domain Layer                   │
│                                             │
│  RatingRepositoryProtocol (protocol)        │
│    ├── getRatingStats(courseId:) async      │
│    ├── getUserRating(courseId:userId:) async│
│    └── upsertRating(courseId:userId:        │
│                     stars:) async           │
│                                             │
│  Models: CourseRating, RatingStats          │
└────────────────────┬────────────────────────┘
                     │ implements
┌────────────────────▼────────────────────────┐
│               Data Layer                    │
│                                             │
│  RemoteRatingRepository                     │
│    └── NetworkManager (shared)              │
│         ├── POST /courses/{id}/ratings      │
│         ├── GET  /courses/{id}/ratings/stats│
│         └── GET  /courses/{id}/ratings/     │
│                  user/{user_id}             │
│                                             │
│  RatingAPIEndpoints (enum: APIEndpoint)     │
│  RatingMapper: RatingDTO → CourseRating     │
└─────────────────────────────────────────────┘
```

---

## 4. Contratos de Datos

### 4.1 DTOs (wire format — snake_case de la API)

**`RatingResponseDTO`** — respuesta de `POST`, `GET ratings`, `GET user rating`:
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

**`RatingStatsDTO`** — respuesta de `GET /courses/{id}/ratings/stats`:
```json
{
  "average_rating": 4.35,
  "total_ratings": 142,
  "rating_distribution": {
    "1": 5,
    "2": 10,
    "3": 25,
    "4": 50,
    "5": 52
  }
}
```

> **Nota**: Las claves de `rating_distribution` son strings en JSON estándar aunque representen enteros. Declarar como `Map<String, Int>` (Kotlin) / `[String: Int]` (Swift) en el DTO y convertir en el mapper.

**`RatingRequestDTO`** — body para `POST /courses/{id}/ratings`:
```json
{
  "user_id": 1,
  "rating": 4
}
```

### 4.2 Modelos de Dominio

**`CourseRating`**:
```
id:        Int
courseId:  Int
userId:    Int
rating:    Int       // 1..5
createdAt: String    // ISO 8601
updatedAt: String    // ISO 8601
```

**`RatingStats`**:
```
averageRating: Double   // 0.0..5.0
totalRatings:  Int      // >= 0
```

> `rating_distribution` no se expone en el modelo de dominio para el MVP.

### 4.3 Endpoints utilizados en mobile

Solo tres de los siete endpoints son necesarios para el MVP:

| Operación | Endpoint | Notas |
|-----------|----------|-------|
| Cargar stats | `GET /courses/{id}/ratings/stats` | Al abrir pantalla de detalle |
| Cargar rating del usuario | `GET /courses/{id}/ratings/user/1` | HTTP 204 = sin rating previo |
| Crear o actualizar rating | `POST /courses/{id}/ratings` | Upsert; body `{user_id: 1, rating: N}` |

---

## 5. Orden de Implementación

El orden garantiza que cada componente puede compilarse y probarse independientemente antes de continuar.

```
DTOs → Modelos de dominio → Mapper → Interface de repositorio
→ Implementación de repositorio → UiState / ViewModel
→ StarRating (componente visual) → RatingSection → DetailScreen
→ Navegación → Tests del ViewModel
```

### 5.1 Android — fases detalladas

**Fase 1 — Data Layer** (sin dependencias externas)

1. Crear `RatingDTO.kt` con `RatingResponseDTO`, `RatingStatsDTO`, `RatingRequestDTO`
2. Crear `CourseRating.kt` y `RatingStats.kt` en domain/models
3. Crear `RatingMapper.kt`
4. Extender `ApiService.kt` con los tres endpoints:
   - `@POST("courses/{courseId}/ratings") suspend fun upsertRating(...)`
   - `@GET("courses/{courseId}/ratings/stats") suspend fun getRatingStats(...)`
   - `@GET("courses/{courseId}/ratings/user/{userId}") suspend fun getUserRating(...)`
5. Crear `RatingRepository.kt` (interface)
6. Crear `RemoteRatingRepository.kt`

**Fase 2 — ViewModel** (depende de Fase 1)

7. Crear `CourseDetailUiState.kt`:
   - Campos: `isLoading`, `courseName`, `courseDescription`, `courseThumbnail`, `courseId`
   - Campos de ratings: `ratingStats: RatingStats?`, `userRating: Int?`, `ratingState: RatingState`, `error: String?`
   - Enum: `RatingState { IDLE, LOADING, SUCCESS, ERROR }`
8. Crear `CourseDetailViewModel.kt`:
   - `courseId: Int` como argumento de factory
   - Llama `loadRatingStats()` y `loadUserRating()` en `init`
   - `submitRating(stars: Int)` con actualización optimista

**Fase 3 — UI** (depende de Fase 2)

9. Crear `StarRatingBar.kt`: `Row` de 5 `Icon` (Material Icons), parámetros `rating: Int`, `onRatingChange: (Int) -> Unit`, `enabled: Boolean`
10. Crear `RatingSection.kt`: promedio, total, `StarRatingBar` interactivo, feedback de estado
11. Crear `CourseDetailScreen.kt`: thumbnail, nombre, descripción + `RatingSection`
12. Actualizar `AppModule.kt`: factory method `provideCourseDetailViewModel(courseId)`
13. Actualizar `MainActivity.kt`: `NavHost` con rutas `"courses"` y `"course/{courseId}"`

**Fase 4 — Tests** (depende de Fase 2)

14. Crear `CourseDetailViewModelTest.kt` con mocks de `RatingRepository`

### 5.2 iOS — fases detalladas

**Fase 1 — Data Layer** (sin dependencias externas)

1. Crear `RatingDTO.swift`: `RatingResponseDTO`, `RatingStatsDTO`, `RatingRequestDTO` como `Codable structs`
2. Crear `CourseRating.swift` y `RatingStats.swift`
3. Crear `RatingMapper.swift`
4. Crear `RatingRepositoryProtocol.swift`
5. Crear `RatingAPIEndpoints.swift` como `enum` conformando `APIEndpoint`
6. Crear `RemoteRatingRepository.swift` usando `NetworkManager.shared`

**Fase 2 — ViewModel** (depende de Fase 1)

7. Crear `CourseDetailViewModel.swift`:
   - `@MainActor` con `@Published` para thread safety
   - `@Published var userRating: Int?`
   - `@Published var ratingStats: RatingStats?`
   - `@Published var ratingState: RatingState`
   - `loadData()` ejecuta en `Task { }`
   - `submitRating(stars: Int)` con actualización optimista

**Fase 3 — UI** (depende de Fase 2)

8. Crear `StarRatingView.swift`: `HStack` de 5 `Image(systemName:)` ("star.fill" / "star"), `onTapGesture` por estrella, `.disabled(isSubmitting)`
9. Crear `RatingSectionView.swift`: promedio, total, `StarRatingView`, estados de UI, `accessibilityLabel` en cada estrella
10. Crear `CourseDetailView.swift`: inicializada con `course: Course`, thumbnail con `AsyncImage`, `RatingSectionView`, carga en `.onAppear`
11. Actualizar `CourseListView.swift`: envolver `CourseCardView` en `NavigationLink` hacia `CourseDetailView(course:)`

**Fase 4 — Tests** (depende de Fase 2)

12. Crear `CourseDetailViewModelTests.swift`

---

## 6. Criterios de Aceptación

### 6.1 Android

**Data Layer**
- `RatingDTO` deserializa correctamente JSON con `@SerializedName`
- `RatingStatsDTO` mapea `rating_distribution` como `Map<String, Int>`
- `RemoteRatingRepository.getUserRating` devuelve `Result.success(null)` para HTTP 204
- `RemoteRatingRepository` devuelve `Result.failure(Exception)` para errores de red o 4xx/5xx

**ViewModel**
- Estado inicial: `isLoading = true`, `ratingStats = null`, `userRating = null`
- Tras carga exitosa: `isLoading = false`, `ratingStats` poblado
- `submitRating(3)`: actualiza `userRating = 3` antes del request (optimista)
- Si el request falla: revierte a rating anterior y establece `ratingState = ERROR`
- `ratingState == SUCCESS` se resetea a `IDLE` tras 2 segundos

**UI**
- `StarRatingBar` renderiza 5 íconos: llenos hasta `rating`, vacíos el resto
- `onRatingChange` se invoca con el índice (1–5) al hacer tap
- Con `enabled = false`: sin respuesta a interacciones, opacidad reducida
- `RatingSection` muestra `"X.X / 5"` y `"(N valoraciones)"`
- `RatingSection` muestra `"Sin valoraciones aún"` cuando `totalRatings == 0`

**Navegación**
- Tap en `CourseCard` navega a `CourseDetailScreen` con el `courseId` correcto
- El botón Back nativo de Android regresa a la lista de cursos

### 6.2 iOS

**Data Layer**
- `RatingResponseDTO` es `Codable` con `CodingKeys` en snake_case
- `RatingStatsDTO` mapea `rating_distribution` como `[String: Int]`
- `RemoteRatingRepository.getUserRating` devuelve `nil` para HTTP 204 (captura `NetworkError.requestFailed(statusCode: 204)`)

**ViewModel**
- `@MainActor` garantiza que las actualizaciones de `@Published` ocurren en el hilo principal
- `loadData()` no bloquea el hilo principal (usa `Task { }`)
- `submitRating(stars:)` implementa actualización optimista: actualiza `userRating` antes de esperar respuesta de red
- `ratingState` vuelve a `.idle` 2 segundos después de `.success` usando `Task.sleep`

**UI**
- `StarRatingView` usa `Image(systemName: "star.fill")` (amarillo) y `"star"` (gris)
- `accessibilityLabel` en cada estrella: `"1 estrella"`, `"2 estrellas"`, ...
- `RatingSectionView` muestra `ProgressView` durante `.loading`
- `CourseDetailView` usa `AsyncImage` para la thumbnail (patrón de `CourseCardView`)

**Navegación**
- `NavigationLink` en `CourseListView` navega dentro del `NavigationView` existente
- El botón Back nativo de iOS funciona sin configuración adicional

---

## 7. Riesgos y Mitigaciones

### Riesgo 1 — HTTP 204 en `getUserRating`
**Descripción**: Retrofit y el `NetworkManager` de iOS fallan al deserializar un body vacío en HTTP 204.

**Mitigación Android**: En `RemoteRatingRepository`, verificar `response.code() == 204` antes de intentar deserializar. Devolver `Result.success(null)`.

**Mitigación iOS**: Capturar `NetworkError.requestFailed(statusCode: 204)` y devolver `nil` en lugar de relanzar.

---

### Riesgo 2 — `rating_distribution` con claves string
**Descripción**: JSON no soporta claves enteras. La API devuelve `{"1": 5, "2": 10, ...}`. Declarar como `Map<Int, Int>` / `[Int: Int]` causaría fallo silencioso o excepción en deserialización.

**Mitigación**: Declarar como `Map<String, Int>` (Kotlin) / `[String: Int]` (Swift) en el DTO. Convertir en el mapper o ignorar el campo en MVP.

---

### Riesgo 3 — Navegación sin pantalla de detalle
**Descripción**: `MainActivity.kt` tiene `onCourseClick = {}` vacío. No existe ninguna pantalla de detalle aún.

**Mitigación**: Implementar `CourseDetailScreen` completa antes de conectar la navegación. Pasar `courseId` como `Int` argumento de navegación (evita necesidad de `@Parcelize`).

---

### Riesgo 4 — userId hardcodeado = 1
**Descripción**: En un servidor compartido, todos los usuarios sobreescriben el mismo rating.

**Mitigación**: Centralizar en una constante:

```kotlin
// Android
const val HARDCODED_USER_ID = 1
const val RATING_SUCCESS_RESET_DELAY_MS = 2000L
```

```swift
// iOS
enum AppConstants {
    static let hardcodedUserId = 1
    static let ratingSuccessResetDelayNanoseconds: UInt64 = 2_000_000_000
}
```

Marcar como deuda técnica hasta que se implemente autenticación.

---

### Riesgo 5 — Carga del detalle del curso en Android
**Descripción**: Se necesita decidir si reusar el objeto `Course` pasado desde la lista o hacer un nuevo request al endpoint `GET /courses/{slug}`.

**Recomendación**: Pasar el objeto `Course` completo desde `CourseListScreen` usando serialización JSON en el backstack (agregar `@Serializable` al modelo). Evita un request adicional. Si se necesita detalle completo con lessons, añadir `getCourseBySlug(slug)` al repositorio Android siguiendo el patrón de iOS.

---

## 8. Estimación de Esfuerzo

### Android

| Fase | Tarea | Horas |
|------|-------|-------|
| Data Layer | DTOs + Modelos + Mapper | 1.5 |
| Data Layer | ApiService + RemoteRatingRepository | 2.0 |
| Domain | RatingRepository interface | 0.5 |
| Presentation | CourseDetailUiState | 0.5 |
| Presentation | CourseDetailViewModel | 3.0 |
| UI | StarRatingBar Composable | 2.0 |
| UI | RatingSection Composable | 2.0 |
| UI | CourseDetailScreen | 2.0 |
| Config | AppModule + NavHost + MainActivity | 2.5 |
| Tests | CourseDetailViewModelTest | 2.5 |
| **Total Android** | | **18.5 h** |

### iOS

| Fase | Tarea | Horas |
|------|-------|-------|
| Data Layer | DTOs + Modelos + Mapper | 1.5 |
| Data Layer | RatingAPIEndpoints + RemoteRatingRepository | 2.0 |
| Domain | RatingRepositoryProtocol | 0.5 |
| Presentation | CourseDetailViewModel | 3.0 |
| UI | StarRatingView | 2.0 |
| UI | RatingSectionView | 2.0 |
| UI | CourseDetailView | 2.5 |
| Navegación | CourseListView → NavigationLink | 1.0 |
| Tests | CourseDetailViewModelTests | 2.5 |
| **Total iOS** | | **17.0 h** |

| Plataforma | Horas base | + 20% contingencia | **Total** |
|------------|-----------|-------------------|-----------|
| Android | 18.5 h | 3.7 h | **22.2 h** |
| iOS | 17.0 h | 3.4 h | **20.4 h** |
| **Proyecto** | **35.5 h** | **7.1 h** | **~43 h** |

---

## 9. Patrón de Actualización Optimista

Ambas plataformas deben replicar el mismo patrón implementado en `RatingSection.tsx`:

1. Guardar el rating actual antes del request
2. Actualizar inmediatamente el estado local (`userRating = newStars`)
3. Lanzar el request en background
4. Si falla: revertir al valor guardado y mostrar mensaje de error
5. Si exitoso: mantener el nuevo valor y mostrar feedback de éxito por 2 segundos

---

## 10. Archivos de Referencia

| Propósito | Ruta |
|-----------|------|
| Contratos exactos de request/response | `Backend/app/schemas/rating.py` |
| Semántica HTTP de cada endpoint (incl. 204) | `Backend/app/main.py` |
| Manejo del 204 en cliente | `Frontend/src/services/ratingsApi.ts` |
| Patrón de actualización optimista | `Frontend/src/components/CourseDetail/RatingSection.tsx` |
| Patrón de repositorio Android | `Mobile/PlatziFlixAndroid/app/src/main/java/.../data/repositories/RemoteCourseRepository.kt` |
| Patrón de repositorio iOS | `Mobile/PlatziFlixiOS/PlatziFlixiOS/Data/Repositories/RemoteCourseRepository.swift` |
| Patrón de test ViewModel Android | `Mobile/PlatziFlixAndroid/app/src/test/.../CourseListViewModelTest.kt` |
| Casos de error iOS | `Mobile/PlatziFlixiOS/PlatziFlixiOS/Services/NetworkError.swift` |
