import Foundation

/// Represents the lifecycle of a rating submission
enum RatingState: Equatable {
    case idle
    case loading
    case success
    case error
}
