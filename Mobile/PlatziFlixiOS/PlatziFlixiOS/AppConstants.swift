import Foundation

/// App-wide constants shared across the presentation layer
enum AppConstants {
    /// Hardcoded user id used until authentication is implemented
    static let hardcodedUserId = 1

    /// Delay before resetting the rating state from `.success` back to `.idle`
    static let ratingSuccessResetDelayNanoseconds: UInt64 = 2_000_000_000
}
