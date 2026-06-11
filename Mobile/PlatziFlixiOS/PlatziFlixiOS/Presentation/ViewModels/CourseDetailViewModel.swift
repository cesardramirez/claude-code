import Foundation

/// ViewModel responsible for managing the course detail and rating state
@MainActor
final class CourseDetailViewModel: ObservableObject {

    // MARK: - Published Properties
    @Published var userRating: Int?
    @Published var ratingStats: RatingStats?
    @Published var ratingState: RatingState = .idle
    @Published var isLoading: Bool = true
    @Published var errorMessage: String?

    // MARK: - Properties
    let course: Course
    private let ratingRepository: RatingRepository

    // MARK: - Initialization
    init(course: Course, ratingRepository: RatingRepository = RemoteRatingRepository()) {
        self.course = course
        self.ratingRepository = ratingRepository
    }

    // MARK: - Public Methods

    /// Loads the rating stats and the current user's rating for the course
    func loadData() async {
        isLoading = true

        await loadRatingStats()
        await loadUserRating()

        isLoading = false
    }

    /// Submits a new rating using the optimistic update pattern:
    /// 1. Saves the previous rating in case a rollback is needed
    /// 2. Immediately reflects the new rating and a `.loading` state
    /// 3. Persists the rating in the background
    /// 4a. On success: refreshes the stats, shows `.success` and resets to `.idle` after a delay
    /// 4b. On failure: rolls back to the previous rating and shows `.error` (no auto-reset)
    func submitRating(_ stars: Int) {
        let previousRating = userRating

        userRating = stars
        ratingState = .loading
        errorMessage = nil

        Task {
            do {
                _ = try await ratingRepository.upsertRating(
                    courseId: course.id,
                    userId: AppConstants.hardcodedUserId,
                    stars: stars
                )

                await loadRatingStats()

                ratingState = .success
                try? await Task.sleep(nanoseconds: AppConstants.ratingSuccessResetDelayNanoseconds)
                ratingState = .idle
            } catch {
                userRating = previousRating
                ratingState = .error
                errorMessage = "No se pudo guardar la calificación"
            }
        }
    }

    // MARK: - Private Methods

    private func loadRatingStats() async {
        do {
            ratingStats = try await ratingRepository.getRatingStats(courseId: course.id)
        } catch {
            errorMessage = "No se pudieron cargar las estadísticas"
        }
    }

    private func loadUserRating() async {
        do {
            let rating = try await ratingRepository.getUserRating(courseId: course.id, userId: AppConstants.hardcodedUserId)
            userRating = rating?.rating
        } catch {
            errorMessage = "No se pudo cargar tu calificación"
        }
    }
}
