import SwiftUI

/// Displays the interactive star rating control plus the aggregated rating stats.
///
/// The stats block is only shown when `ratingStats.totalRatings > 0` — there is
/// no placeholder text for the empty state.
struct RatingSectionView: View {
    let ratingStats: RatingStats?
    let userRating: Int?
    let ratingState: RatingState
    let errorMessage: String?
    let onRatingSelected: (Int) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.spacing2) {
            Text("Califica este curso")
                .font(.title3)
                .foregroundColor(.primary)

            HStack(spacing: Spacing.spacing3) {
                StarRatingView(
                    rating: userRating ?? 0,
                    isSubmitting: ratingState == .loading,
                    onRatingSelected: onRatingSelected
                )

                if ratingState == .loading {
                    ProgressView()
                }
            }

            switch ratingState {
            case .loading:
                Text("Guardando…")
                    .font(.bodyRegular)
                    .foregroundColor(.secondary)
            case .success:
                Text("¡Calificación guardada!")
                    .font(.bodyRegular)
                    .foregroundColor(.successGreen)
            case .error:
                Text(errorMessage ?? "Ocurrió un error al guardar tu calificación")
                    .font(.bodyRegular)
                    .foregroundColor(.errorRed)
            case .idle:
                EmptyView()
            }

            if let stats = ratingStats, stats.totalRatings > 0 {
                Text("\(String(format: "%.1f", stats.averageRating)) (\(stats.totalRatings) valoraciones)")
                    .font(.bodyRegular)
                    .foregroundColor(.secondary)
            }
        }
    }
}

// MARK: - Previews
#Preview("With ratings") {
    RatingSectionView(
        ratingStats: RatingStats(averageRating: 4.35, totalRatings: 142),
        userRating: 4,
        ratingState: .idle,
        errorMessage: nil,
        onRatingSelected: { _ in }
    )
    .padding()
}

#Preview("Without ratings") {
    RatingSectionView(
        ratingStats: RatingStats(averageRating: 0.0, totalRatings: 0),
        userRating: nil,
        ratingState: .idle,
        errorMessage: nil,
        onRatingSelected: { _ in }
    )
    .padding()
}

#Preview("Error state") {
    RatingSectionView(
        ratingStats: RatingStats(averageRating: 4.0, totalRatings: 10),
        userRating: 2,
        ratingState: .error,
        errorMessage: "No se pudo guardar la calificación",
        onRatingSelected: { _ in }
    )
    .padding()
}
