import XCTest
import Combine
@testable import PlatziFlixiOS

@MainActor
final class CourseDetailViewModelTests: XCTestCase {

    private let testCourse = Course.mockCourses[0]
    private var cancellables: Set<AnyCancellable> = []

    override func tearDown() {
        cancellables.removeAll()
        super.tearDown()
    }

    func test_initialState_hasNoRatingDataAndIsLoading() {
        let viewModel = CourseDetailViewModel(course: testCourse, ratingRepository: FakeRatingRepository())

        XCTAssertTrue(viewModel.isLoading)
        XCTAssertNil(viewModel.ratingStats)
        XCTAssertNil(viewModel.userRating)
    }

    func test_loadData_populatesRatingStatsAndUserRating() async {
        let repository = FakeRatingRepository()
        repository.ratingStatsResult = .success(RatingStats(averageRating: 4.35, totalRatings: 142))
        repository.userRatingResult = .success(CourseRating(id: 1, courseId: testCourse.id, userId: 1, rating: 4))

        let viewModel = CourseDetailViewModel(course: testCourse, ratingRepository: repository)
        await viewModel.loadData()

        XCTAssertFalse(viewModel.isLoading)
        XCTAssertEqual(viewModel.ratingStats, RatingStats(averageRating: 4.35, totalRatings: 142))
        XCTAssertEqual(viewModel.userRating, 4)
    }

    func test_submitRating_appliesOptimisticUpdateBeforeRepositoryResponds() async {
        let repository = FakeRatingRepository()
        let viewModel = CourseDetailViewModel(course: testCourse, ratingRepository: repository)
        await viewModel.loadData()

        viewModel.submitRating(5)

        XCTAssertEqual(viewModel.userRating, 5)
        XCTAssertEqual(viewModel.ratingState, .loading)
    }

    func test_submitRating_rollsBackUserRatingOnError() async throws {
        let repository = FakeRatingRepository()
        repository.userRatingResult = .success(CourseRating(id: 1, courseId: testCourse.id, userId: 1, rating: 3))
        repository.upsertResult = .failure(TestError.network)

        let viewModel = CourseDetailViewModel(course: testCourse, ratingRepository: repository)
        await viewModel.loadData()

        let errorExpectation = expectation(description: "rating state becomes error")
        viewModel.$ratingState
            .dropFirst()
            .sink { state in
                if state == .error {
                    errorExpectation.fulfill()
                }
            }
            .store(in: &cancellables)

        viewModel.submitRating(5)

        await fulfillment(of: [errorExpectation], timeout: 2)

        XCTAssertEqual(viewModel.userRating, 3)
        XCTAssertEqual(viewModel.ratingState, .error)
    }

    func test_submitRating_transitionsFromSuccessToIdleAfterDelay() async throws {
        let repository = FakeRatingRepository()
        repository.ratingStatsResult = .success(RatingStats(averageRating: 4.0, totalRatings: 10))

        let viewModel = CourseDetailViewModel(course: testCourse, ratingRepository: repository)
        await viewModel.loadData()

        let successExpectation = expectation(description: "rating state becomes success")
        let idleExpectation = expectation(description: "rating state resets to idle after success")
        var sawSuccess = false

        viewModel.$ratingState
            .dropFirst()
            .sink { state in
                switch state {
                case .success:
                    sawSuccess = true
                    successExpectation.fulfill()
                case .idle where sawSuccess:
                    idleExpectation.fulfill()
                default:
                    break
                }
            }
            .store(in: &cancellables)

        viewModel.submitRating(5)

        await fulfillment(of: [successExpectation], timeout: 2)
        await fulfillment(of: [idleExpectation], timeout: 3)
    }
}

// MARK: - Test Doubles

private enum TestError: Error {
    case network
}

private final class FakeRatingRepository: RatingRepository {
    var ratingStatsResult: Result<RatingStats, Error> = .success(RatingStats(averageRating: 0.0, totalRatings: 0))
    var userRatingResult: Result<CourseRating?, Error> = .success(nil)
    var upsertResult: Result<CourseRating, Error>?

    func getRatingStats(courseId: Int) async throws -> RatingStats {
        try ratingStatsResult.get()
    }

    func getUserRating(courseId: Int, userId: Int) async throws -> CourseRating? {
        try userRatingResult.get()
    }

    func upsertRating(courseId: Int, userId: Int, stars: Int) async throws -> CourseRating {
        if let upsertResult {
            return try upsertResult.get()
        }
        return CourseRating(id: 1, courseId: courseId, userId: userId, rating: stars)
    }
}
