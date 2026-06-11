# Code Review: Mobile Ratings (Fases 2-6, Android + iOS)

**Versión**: 1.0
**Fecha**: 2026-06-10
**Scope**: Mobile — Android (Kotlin + Jetpack Compose) e iOS (Swift + SwiftUI), Fases 2-6 de `05_mobile_ratings_implementation_plan.md`
**Método**: Revisión multi-ángulo (7 ángulos finder vía agentes + verificación 1-voto recall-biased sobre el código actual)
**Hallazgos**: 10 (2 altos, 4 medios, 4 bajos)
**Estado**: Pendiente de remediación

---

## Resumen Ejecutivo

Esta revisión analiza el código (sin commitear al momento de la revisión) que implementa las Fases 2-6 del sistema de ratings de cursos en las apps móviles nativas: repositorios de datos, ViewModels con actualización optimista, UI de rating, navegación a la pantalla de detalle y tests unitarios.

No se encontraron vulnerabilidades de seguridad ni crashes garantizados. Los hallazgos son principalmente **condiciones de carrera en la actualización optimista**, **errores que se calculan pero nunca se muestran al usuario**, y **deuda arquitectónica** (acoplamiento dominio-navegación, IDs de usuario hardcodeados, llamadas de red secuenciales evitables).

### Hallazgos Principales

| # | Hallazgo | Plataforma | Severidad | Categoría |
|---|----------|------------|-----------|-----------|
| 1 | Race condition en `submitRating`: rollback tardío sobrescribe una calificación más reciente | Android | 🟠 Alta | Correctness |
| 2 | Race condition en `submitRating` (mismo patrón) | iOS | 🟠 Alta | Correctness |
| 3 | Error de `loadRatingStats()` se guarda en `error` pero nunca se muestra (ratingState IDLE/SUCCESS) | Android | 🟡 Media | Correctness |
| 4 | Mismo problema de error silencioso (`errorMessage`) | iOS | 🟡 Media | Correctness |
| 5 | `ZStack` + `NavigationLink` invisible sobre `CourseCardView(onTap: nil)` — accesibilidad/tap-target ambiguos | iOS | 🟡 Media | Correctness/A11y |
| 6 | Modelo de dominio `Course` anotado `@Serializable` solo para navegación type-safe | Android | 🟡 Media | Altitude |
| 7 | `HARDCODED_USER_ID` / `AppConstants.hardcodedUserId` dispersos sin abstracción de sesión | Android/iOS | 🟢 Baja | Altitude/Reuse |
| 8 | `loadRatingStats()` y `loadUserRating()` se ejecutan secuencialmente en vez de en paralelo | Android/iOS | 🟢 Baja | Efficiency |
| 9 | `key = "course_detail_${course.id}"` en `viewModel<CourseDetailViewModel>()` es redundante | Android | 🟢 Baja | Simplification |
| 10 | `upsertRating` valida `response.code() == 201` exacto en vez de `isSuccessful` | Android | 🟢 Baja | Altitude |

---

## Metodología

La revisión se ejecutó en dos fases:

1. **Búsqueda de candidatos** — 7 agentes independientes, cada uno con un ángulo distinto:
   - **A. Line-by-line**: lectura línea por línea del diff y de las funciones que lo contienen.
   - **B. Removed-behavior**: para cada línea eliminada, verificar que el invariante que garantizaba se restablezca en otro lugar.
   - **C. Cross-file tracer**: para cada función nueva/modificada, validar llamadores, callees, formas de retorno y firmas.
   - **Reuse**: detectar lógica nueva que reimplementa helpers existentes.
   - **Simplification**: detectar complejidad/estado/código innecesario.
   - **Efficiency**: detectar trabajo redundante o secuencial evitable.
   - **Altitude**: detectar parches frágiles vs. soluciones a la profundidad correcta.

2. **Verificación 1-voto (recall-biased)** — cada candidato se contrastó contra el código actual (lectura directa de los archivos finales). Se descartó únicamente lo **provablemente imposible**; lo demás se mantuvo como `CONFIRMED`/`PLAUSIBLE`.

> **Candidato refutado**: se descartó la hipótesis de que `RemoteRatingRepository.kt:44` lanzaría una `EOFException` de Gson al recibir un `204 No Content` en `getUserRating`. Retrofit (`OkHttpCall.parseResponse()`) tiene un caso especial para `code == 204 || code == 205` que devuelve `Response.success(null, rawResponse)` **sin invocar el converter**, por lo que la rama `response.code() == 204 -> Result.success(null)` (línea 46) sí es alcanzable y correcta.

---

## Hallazgos Detallados

### Hallazgo 1 — Race condition en `submitRating` (Android)

- **Severidad**: 🟠 Alta
- **Archivo**: `Mobile/PlatziFlixAndroid/app/src/main/java/com/espaciotiago/platziflixandroid/presentation/coursedetail/viewmodel/CourseDetailViewModel.kt:54`
- **Categoría**: Correctness (Angle A)

**Descripción**

```kotlin
fun submitRating(stars: Int) {
    val previousRating = _uiState.value.userRating   // <- capturado por cada llamada

    _uiState.update {
        it.copy(userRating = stars, ratingState = RatingState.LOADING, error = null)
    }

    viewModelScope.launch {
        ratingRepository.upsertRating(course.id, HARDCODED_USER_ID, stars)
            .onSuccess { ... }
            .onFailure { exception ->
                _uiState.update {
                    it.copy(userRating = previousRating, ratingState = RatingState.ERROR, ...)
                }
            }
    }
}
```

`previousRating` se captura del estado **optimista** (potencialmente no confirmado) en el momento de cada llamada. Si dos llamadas a `submitRating` se solapan y completan en orden inverso al de inicio, el rollback de la primera puede sobrescribir el resultado ya confirmado de la segunda.

**Escenario de Fallo**

1. Usuario tiene `rating = 3`.
2. Toca estrella 5 → llamada **A**: `previousRating = 3`, UI = `5 / LOADING`.
3. Antes de que A responda, toca estrella 2 → llamada **B**: `previousRating = 5` (valor optimista de A, aún no confirmado), UI = `2 / LOADING`.
4. B responde con éxito primero → UI = `2 / SUCCESS`, persistido correctamente en backend.
5. A falla (red lenta) → `onFailure` hace `userRating = previousRating = 5` y `ratingState = ERROR`.
6. **Resultado**: la UI muestra `5` con un mensaje de error, aunque el backend tiene `2` guardado correctamente — el usuario ve un valor que nunca fue persistido y un error sobre una operación que sí tuvo éxito.

**Recomendación**

Ignorar el resultado de una llamada a `upsertRating` si ya existe una llamada más reciente en curso (p. ej. usando un `requestId`/`Job` incrementable y comparando antes de aplicar el rollback), o cancelar (`Job.cancel()`) la llamada anterior al iniciar una nueva.

---

### Hallazgo 2 — Race condition en `submitRating` (iOS)

- **Severidad**: 🟠 Alta
- **Archivo**: `Mobile/PlatziFlixiOS/PlatziFlixiOS/Presentation/ViewModels/CourseDetailViewModel.swift:43`
- **Categoría**: Correctness (Angle A)

**Descripción**

Mismo patrón que el Hallazgo 1: `previousRating` se captura al inicio de cada `Task` lanzado por `submitRating`, sin coordinación entre `Task`s concurrentes.

```swift
func submitRating(_ stars: Int) {
    let previousRating = userRating   // <- capturado por cada Task

    userRating = stars
    ratingState = .loading
    errorMessage = nil

    Task {
        do {
            _ = try await ratingRepository.upsertRating(...)
            await loadRatingStats()
            ratingState = .success
            ...
        } catch {
            userRating = previousRating
            ratingState = .error
            errorMessage = "No se pudo guardar la calificación"
        }
    }
}
```

**Escenario de Fallo**

Doble tap rápido en estrellas distintas: el primer `Task` (más lento) falla **después** de que el segundo `Task` (más rápido) tuvo éxito. El `catch` del primero restaura `userRating` al valor que ÉL capturó (el valor optimista del primer tap, no el original ni el confirmado por el segundo) y marca `ratingState = .error`, mostrando un valor y un error incorrectos pese a que la calificación más reciente sí se guardó en el backend.

**Recomendación**

Misma estrategia que el Hallazgo 1: descartar el resultado de un `Task` de `submitRating` si ya hay uno más reciente en curso (p. ej. guardando una referencia al `Task` actual y cancelando el anterior, o comparando un contador de "intento" antes de aplicar `userRating = previousRating`).

---

### Hallazgo 3 — Error de `loadRatingStats()` calculado pero nunca mostrado (Android)

- **Severidad**: 🟡 Media
- **Archivo**: `Mobile/PlatziFlixAndroid/app/src/main/java/com/espaciotiago/platziflixandroid/presentation/coursedetail/viewmodel/CourseDetailViewModel.kt:86`
- **Categoría**: Correctness (Angle C)

**Descripción**

```kotlin
private suspend fun loadRatingStats() {
    ratingRepository.getRatingStats(course.id)
        .onSuccess { stats -> _uiState.update { it.copy(ratingStats = stats) } }
        .onFailure { exception ->
            _uiState.update { it.copy(error = exception.message ?: "No se pudieron cargar las estadísticas") }
        }
}
```

`RatingSection.kt` solo renderiza `error` cuando `ratingState == RatingState.ERROR`:

```kotlin
when (ratingState) {
    RatingState.LOADING -> Text("Guardando…", ...)
    RatingState.SUCCESS -> Text("¡Calificación guardada!", ...)
    RatingState.ERROR -> Text(error ?: "Ocurrió un error al guardar tu calificación", ...)
    RatingState.IDLE -> Unit   // <- error no se muestra aquí
}
```

**Escenario de Fallo**

- **Carga inicial**: al abrir `CourseDetailScreen`, `init { loadRatingStats(); loadUserRating(); ... }` se ejecuta con `ratingState = IDLE` (valor por defecto). Si `GET /courses/{id}/ratings/stats` falla por un error transitorio de red, `uiState.error` queda seteado pero `ratingState` permanece `IDLE` → la rama `IDLE -> Unit` no muestra nada. El usuario ve la sección de rating sin estadísticas, indistinguible de un curso que simplemente no tiene calificaciones aún.
- **Tras un submit exitoso**: si `submitRating` tiene éxito pero la posterior `loadRatingStats()` falla, `error` queda seteado pero `ratingState = SUCCESS` se aplica justo después, por lo que se muestra "¡Calificación guardada!" y el error de refresco de estadísticas queda enmascarado.

**Recomendación**

Separar el campo `error` en dos conceptos (p. ej. `submitError: String?` para errores de `submitRating`, mostrado solo en `ratingState == ERROR`; y `statsLoadError: String?` para errores de carga, con su propio renderizado independiente del `ratingState`), o limpiar/loguear explícitamente los errores de carga que no deban bloquear la UI.

---

### Hallazgo 4 — Error de `loadRatingStats()` calculado pero nunca mostrado (iOS)

- **Severidad**: 🟡 Media
- **Archivo**: `Mobile/PlatziFlixiOS/PlatziFlixiOS/Presentation/ViewModels/CourseDetailViewModel.swift:76`
- **Categoría**: Correctness (Angle C)

**Descripción**

Mismo problema que el Hallazgo 3, en su contraparte iOS:

```swift
private func loadRatingStats() async {
    do {
        ratingStats = try await ratingRepository.getRatingStats(courseId: course.id)
    } catch {
        errorMessage = "No se pudieron cargar las estadísticas"
    }
}
```

`RatingSectionView.swift` solo muestra `errorMessage` cuando `ratingState == .error`:

```swift
switch ratingState {
case .loading: Text("Guardando…")...
case .success: Text("¡Calificación guardada!")...
case .error:   Text(errorMessage ?? "Ocurrió un error al guardar tu calificación")...
case .idle:    EmptyView()
}
```

**Escenario de Fallo**

`loadData()` llama `loadRatingStats()` (falla → `errorMessage = "No se pudieron cargar las estadísticas"`) y luego `loadUserRating()` (éxito). `ratingState` permanece `.idle`, así que `case .idle: EmptyView()` no muestra el error. Igual que en Android, en `submitRating` un fallo de `loadRatingStats()` tras un upsert exitoso queda enmascarado porque `ratingState = .success` se asigna inmediatamente después.

**Recomendación**

Misma que el Hallazgo 3, adaptada a iOS: diferenciar el error de carga de estadísticas del error de envío de rating, y/o mostrarlo independientemente del `ratingState`.

---

### Hallazgo 5 — `ZStack` + `NavigationLink` invisible sobre `CourseCardView(onTap: nil)` (iOS)

- **Severidad**: 🟡 Media
- **Archivo**: `Mobile/PlatziFlixiOS/PlatziFlixiOS/Presentation/Views/CourseListView.swift:123`
- **Categoría**: Correctness / Accesibilidad (Angle B)

**Descripción**

Como parte de la Fase 5 (navegación a detalle), se eliminó `CourseListViewModel.selectCourse(_:)` y se reemplazó la navegación por:

```swift
ZStack {
    CourseCardView(course: course)

    NavigationLink(destination: CourseDetailView(course: course)) {
        EmptyView()
    }
    .opacity(0)
}
.accessibilityAddTraits(.isButton)
```

`CourseCardView` (`CourseCardView.swift:14-66`) sigue siendo en sí mismo un `Button` con:

```swift
.accessibilityHint("Doble toque para ver los detalles del curso")
.accessibilityAction(named: "Ver curso") {
    onTap?()
}
```

Pero ahora se instancia con `onTap: nil` (valor por defecto), por lo que ese `Button`, su `accessibilityHint` y su `accessibilityAction` son efectivamente no-ops, mientras que el `NavigationLink` invisible es el único elemento que realmente navega.

**Escenario de Fallo**

Un usuario de VoiceOver enfoca la tarjeta de curso; escucha la etiqueta "Curso: X" y el hint "Doble toque para ver los detalles del curso" provenientes de `CourseCardView`. Al activarla con doble toque, `onTap?()` es `nil` y no ocurre nada — la promesa de navegación del hint queda incumplida. Además, dos controles tocables (`CourseCardView`'s `Button` y el `NavigationLink` con `.opacity(0)`) se superponen en el mismo frame, lo que puede producir elementos de accesibilidad duplicados/confusos o competencia por el gesto de tap.

**Recomendación**

Eliminar la dualidad de controles: o bien (a) quitar el `Button`/`accessibilityHint`/`accessibilityAction` de `CourseCardView` cuando `onTap == nil` y dejar que el `NavigationLink` sea el único elemento accesible (ajustando su `accessibilityLabel`/`accessibilityHint` para que coincidan con los de la tarjeta), o (b) mantener `CourseCardView` como único control y usar `NavigationLink(isActive:)`/`navigationDestination(for:)` disparado desde `onTap`, evitando el patrón de "invisible NavigationLink" superpuesto.

---

### Hallazgo 6 — `Course` (dominio) anotado `@Serializable` solo para navegación (Android)

- **Severidad**: 🟡 Media
- **Archivo**: `Mobile/PlatziFlixAndroid/app/src/main/java/com/espaciotiago/platziflixandroid/domain/models/Course.kt:12`
- **Categoría**: Altitude

**Descripción**

```kotlin
/**
 * Domain model representing a Course.
 *
 * Marked as [Serializable] so it can be passed directly as a type-safe
 * Navigation Compose route argument (avoids an extra request to load the
 * course detail screen).
 */
@Serializable
data class Course(
    val id: Int,
    val name: String,
    val description: String,
    val thumbnail: String,
    val slug: String
)
```

El modelo de dominio `Course` se acopla a `kotlinx.serialization`/Navigation Compose para poder usarse en `composable<Course>` y `backStackEntry.toRoute<Course>()` (`MainActivity.kt:62-68`). Esta decisión ya está documentada como desviación deliberada en `05_mobile_ratings_implementation_plan.md` (evita una request extra al abrir el detalle).

**Escenario de Fallo**

Si en el futuro `Course` necesita un campo no trivialmente serializable (p. ej. un `LocalDate`, un objeto de valor anidado, o un campo que no debería viajar en la navegación/deep link), agregarlo rompe la compilación de `composable<Course>` o exige un `@Serializer` custom — forzando a elegir entre romper la navegación o dividir el modelo.

**Recomendación**

Aceptado como trade-off documentado para esta fase. Si el modelo `Course` crece en complejidad, considerar introducir una ruta dedicada (`CourseDetailRoute(courseId: Int, name: String, description: String, thumbnail: String, slug: String)`) desacoplada del modelo de dominio, manteniendo el beneficio de evitar una request extra sin acoplar `Course` a `kotlinx.serialization`.

---

### Hallazgo 7 — `HARDCODED_USER_ID` disperso sin abstracción de sesión

- **Severidad**: 🟢 Baja
- **Archivos**:
  - `Mobile/PlatziFlixAndroid/app/src/main/java/com/espaciotiago/platziflixandroid/presentation/coursedetail/viewmodel/CourseDetailViewModel.kt:101`
  - `Mobile/PlatziFlixiOS/PlatziFlixiOS/AppConstants.swift`
- **Categoría**: Altitude / Reuse

**Descripción**

```kotlin
companion object {
    const val HARDCODED_USER_ID = 1
    const val RATING_SUCCESS_RESET_DELAY_MS = 2000L
}
```

En Android, `HARDCODED_USER_ID` vive como constante privada del companion object de `CourseDetailViewModel`. En iOS existe un `AppConstants.swift` centralizado con `hardcodedUserId`, pero no hay equivalente en Android ni una abstracción de sesión (`SessionManager`/`AuthRepository`) en ninguna de las dos plataformas.

**Escenario de Fallo**

Cuando se implemente autenticación real, hay que localizar y editar manualmente cada ViewModel que use el ID hardcodeado (hoy solo `CourseDetailViewModel` en Android, pero el patrón se replicará). No existe un punto único que el compilador obligue a actualizar, por lo que es fácil olvidar una pantalla y dejarla operando con el usuario hardcodeado tras agregar login.

**Recomendación**

Centralizar `HARDCODED_USER_ID` en un objeto `Constants`/`AppConfig` Android equivalente a `AppConstants.swift`, y dejar un `// TODO(auth):` explícito en ambas plataformas apuntando a un futuro `SessionManager`/`AuthRepository` que reemplace estas constantes.

---

### Hallazgo 8 — `loadRatingStats()`/`loadUserRating()` secuenciales

- **Severidad**: 🟢 Baja
- **Archivos**:
  - `Mobile/PlatziFlixAndroid/app/src/main/java/com/espaciotiago/platziflixandroid/presentation/coursedetail/viewmodel/CourseDetailViewModel.kt:38`
  - `Mobile/PlatziFlixiOS/PlatziFlixiOS/Presentation/ViewModels/CourseDetailViewModel.swift:30`
- **Categoría**: Efficiency

**Descripción**

Android (`init`):
```kotlin
viewModelScope.launch {
    loadRatingStats()
    loadUserRating()
    _uiState.update { it.copy(isLoading = false) }
}
```

iOS (`loadData()`):
```swift
func loadData() async {
    isLoading = true
    await loadRatingStats()
    await loadUserRating()
    isLoading = false
}
```

Ambas son llamadas GET independientes (estadísticas del curso vs. rating del usuario actual) ejecutadas secuencialmente.

**Escenario de Fallo**

La pantalla de detalle muestra el spinner de carga durante aproximadamente la **suma** de la latencia de ambas requests en lugar del **máximo** (p. ej. ~200ms por request se convierten en ~400ms en una conexión lenta), ya que la segunda llamada HTTP solo comienza cuando la primera termina por completo.

**Recomendación**

- Android: lanzar ambas en `async { }` dentro de `viewModelScope` y esperar con `awaitAll()`.
- iOS: usar `async let stats = loadRatingStats(); async let rating = loadUserRating()` y `await (stats, rating)`.

---

### Hallazgo 9 — `key = "course_detail_${course.id}"` redundante

- **Severidad**: 🟢 Baja
- **Archivo**: `Mobile/PlatziFlixAndroid/app/src/main/java/com/espaciotiago/platziflixandroid/MainActivity.kt:65`
- **Categoría**: Simplification

**Descripción**

```kotlin
composable<Course> { backStackEntry ->
    val course = backStackEntry.toRoute<Course>()
    val courseDetailViewModel = viewModel<CourseDetailViewModel>(
        key = "course_detail_${course.id}"
    ) {
        AppModule.provideCourseDetailViewModel(course)
    }
    ...
}
```

Cada `composable<Course>` ya obtiene su propio `NavBackStackEntry` con su propio `ViewModelStore` por cada llamada a `navController.navigate(course)` (no usa `launchSingleTop`), por lo que `viewModel<CourseDetailViewModel> { ... }` sin `key` ya crearía una instancia aislada por pantalla.

**Escenario de Fallo**

No hay un fallo funcional — la `key` no cambia el comportamiento actual. El costo es de mantenibilidad: el patrón es copy-pasteable y otro desarrollador podría replicarlo en pantallas futuras pensando que es obligatorio para diferenciar ViewModels, generando confusión sobre cuándo una `key` explícita es realmente necesaria (p. ej. múltiples ViewModels del mismo tipo dentro de UNA misma entrada de backstack).

**Recomendación**

Eliminar el parámetro `key` salvo que se identifique un caso real que lo requiera.

---

### Hallazgo 10 — `upsertRating` valida `response.code() == 201` exacto

- **Severidad**: 🟢 Baja
- **Archivo**: `Mobile/PlatziFlixAndroid/app/src/main/java/com/espaciotiago/platziflixandroid/data/repositories/RemoteRatingRepository.kt:72`
- **Categoría**: Altitude

**Descripción**

```kotlin
override suspend fun upsertRating(courseId: Int, userId: Int, stars: Int): Result<CourseRating> {
    return withContext(Dispatchers.IO) {
        try {
            val response = apiService.upsertRating(...)
            if (response.code() == 201) {
                ...
            } else {
                Result.failure(Exception("Failed to upsert rating: ${response.code()} ${response.message()}"))
            }
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}
```

El resto del repositorio (`getRatingStats`, `getUserRating`, y `RemoteCourseRepository.getAllCourses`) usa `response.isSuccessful` (rango 200-299). `upsertRating` hardcodea `== 201`, que coincide con el contrato documentado ("`POST /courses/{id}/ratings` siempre `201`, incluso en upsert" — `05_mobile_ratings_implementation_plan.md`), pero es más frágil que el resto del código.

**Escenario de Fallo**

Si el backend alguna vez devuelve `200 OK` para una actualización vs. `201 Created` para una creación (cambio de contrato de upsert plausible), el cliente Android trataría una actualización exitosa como `Result.failure`, mostrando un error de "no se pudo guardar" al usuario aunque el rating sí se haya persistido.

**Recomendación**

Mantener tal como está mientras el contrato documentado siga siendo "siempre 201"; si el backend cambia ese contrato, actualizar a `response.isSuccessful` junto con la actualización del spec `01_backend_ratings_implementation_plan.md`.

---

## Plan de Acción

| # | Hallazgo | Severidad | Acción | Estado |
|---|----------|-----------|--------|--------|
| 1 | Race condition `submitRating` (Android) | 🟠 Alta | Descartar resultados de llamadas obsoletas (cancelación/comparación de intento) | 🔴 Pendiente |
| 2 | Race condition `submitRating` (iOS) | 🟠 Alta | Descartar resultados de `Task`s obsoletos | 🔴 Pendiente |
| 3 | Error de stats silencioso (Android) | 🟡 Media | Separar `error` de submit vs. carga, o mostrar independientemente del `ratingState` | 🔴 Pendiente |
| 4 | Error de stats silencioso (iOS) | 🟡 Media | Idem en `errorMessage`/`RatingSectionView` | 🔴 Pendiente |
| 5 | `ZStack`/`NavigationLink` accesibilidad (iOS) | 🟡 Media | Unificar control tocable y accesibilidad en `CourseListView` | 🔴 Pendiente |
| 6 | `Course` `@Serializable` acoplado a navegación (Android) | 🟡 Media | Aceptado como trade-off; revisar si `Course` crece en complejidad | 🟢 Aceptado |
| 7 | `HARDCODED_USER_ID` disperso | 🟢 Baja | Centralizar constante + TODO de auth en ambas plataformas | 🔴 Pendiente |
| 8 | Carga secuencial de stats/user rating | 🟢 Baja | Paralelizar con `async`/`async let` | 🔴 Pendiente |
| 9 | `key` redundante en `viewModel()` (Android) | 🟢 Baja | Eliminar parámetro `key` | 🔴 Pendiente |
| 10 | `upsertRating` chequea `== 201` exacto | 🟢 Baja | Mantener; revisar si cambia el contrato del backend | 🟢 Aceptado |

---

## Historial de Cambios

| Versión | Fecha | Autor | Cambios |
|---------|-------|-------|---------|
| 1.0 | 2026-06-10 | Code Review (Claude Code) | Documento inicial — revisión de Fases 2-6 mobile (Android + iOS) |
