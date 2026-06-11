import SwiftUI

/// Row of 5 stars used to display and select a course rating
struct StarRatingView: View {
    let rating: Int
    let isSubmitting: Bool
    let onRatingSelected: (Int) -> Void

    var body: some View {
        HStack(spacing: Spacing.spacing1) {
            ForEach(1...5, id: \.self) { star in
                Image(systemName: star <= rating ? "star.fill" : "star")
                    .font(.title2)
                    .foregroundColor(star <= rating ? .yellow : .secondary)
                    .onTapGesture {
                        guard !isSubmitting else { return }
                        onRatingSelected(star)
                    }
                    .accessibilityLabel("\(star) estrella\(star == 1 ? "" : "s")")
                    .accessibilityAddTraits(.isButton)
            }
        }
        .opacity(isSubmitting ? 0.5 : 1.0)
        .disabled(isSubmitting)
    }
}

// MARK: - Previews
#Preview("Interactive") {
    StarRatingView(rating: 3, isSubmitting: false) { stars in
        print("Selected \(stars) stars")
    }
    .padding()
}

#Preview("Submitting") {
    StarRatingView(rating: 3, isSubmitting: true) { _ in }
        .padding()
}
