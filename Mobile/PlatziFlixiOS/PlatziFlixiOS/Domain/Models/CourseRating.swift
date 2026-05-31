import Foundation

struct CourseRating: Identifiable, Equatable {
    let id: Int
    let courseId: Int
    let userId: Int
    let rating: Int
}
