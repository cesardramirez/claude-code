import SwiftUI

/// Detail screen for a course: shows its thumbnail, name, description and rating section
struct CourseDetailView: View {
    @StateObject private var viewModel: CourseDetailViewModel

    init(course: Course) {
        _viewModel = StateObject(wrappedValue: CourseDetailViewModel(course: course))
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.spacing4) {
                AsyncImage(url: URL(string: viewModel.course.thumbnail)) { image in
                    image
                        .resizable()
                        .aspectRatio(16/9, contentMode: .fill)
                } placeholder: {
                    RoundedRectangle(cornerRadius: Radius.radiusMedium)
                        .fill(Color(.systemGray5))
                        .aspectRatio(16/9, contentMode: .fit)
                        .overlay(
                            Image(systemName: "photo")
                                .font(.title2)
                                .foregroundColor(.secondary)
                        )
                }
                .frame(height: 220)
                .clipped()
                .cornerRadius(Radius.radiusMedium)
                .accessibilityLabel("Imagen del curso \(viewModel.course.name)")

                Text(viewModel.course.name)
                    .font(.title1)
                    .foregroundColor(.primary)

                Text(viewModel.course.description)
                    .font(.bodyRegular)
                    .foregroundColor(.secondary)

                if viewModel.isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                        .padding(.top, Spacing.spacing4)
                } else {
                    RatingSectionView(
                        ratingStats: viewModel.ratingStats,
                        userRating: viewModel.userRating,
                        ratingState: viewModel.ratingState,
                        errorMessage: viewModel.errorMessage,
                        onRatingSelected: { stars in
                            viewModel.submitRating(stars)
                        }
                    )
                }
            }
            .padding(Spacing.spacing4)
        }
        .navigationTitle(viewModel.course.name)
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            Task {
                await viewModel.loadData()
            }
        }
    }
}

// MARK: - Previews
#Preview {
    NavigationView {
        CourseDetailView(course: Course.mockCourses[0])
    }
}
