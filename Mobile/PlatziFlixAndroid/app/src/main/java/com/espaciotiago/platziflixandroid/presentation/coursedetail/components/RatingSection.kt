package com.espaciotiago.platziflixandroid.presentation.coursedetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.espaciotiago.platziflixandroid.domain.models.RatingStats
import com.espaciotiago.platziflixandroid.presentation.coursedetail.state.RatingState
import com.espaciotiago.platziflixandroid.ui.theme.PlatziFlixAndroidTheme
import com.espaciotiago.platziflixandroid.ui.theme.Spacing
import java.util.Locale

/**
 * Displays the interactive star rating control plus the aggregated rating stats.
 *
 * The stats block is only shown when [ratingStats] has at least one rating —
 * there is no placeholder text for the empty state.
 */
@Composable
fun RatingSection(
    ratingStats: RatingStats?,
    userRating: Int?,
    ratingState: RatingState,
    error: String?,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        Text(
            text = "Califica este curso",
            style = MaterialTheme.typography.titleMedium
        )

        StarRatingBar(
            rating = userRating ?: 0,
            onRatingChange = onRatingChange,
            enabled = ratingState != RatingState.LOADING
        )

        when (ratingState) {
            RatingState.LOADING -> Text(
                text = "Guardando…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            RatingState.SUCCESS -> Text(
                text = "¡Calificación guardada!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            RatingState.ERROR -> Text(
                text = error ?: "Ocurrió un error al guardar tu calificación",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )

            RatingState.IDLE -> Unit
        }

        if (ratingStats != null && ratingStats.totalRatings > 0) {
            Text(
                text = String.format(Locale.getDefault(), "%.1f (%d)", ratingStats.averageRating, ratingStats.totalRatings),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RatingSectionPreview() {
    PlatziFlixAndroidTheme {
        RatingSection(
            ratingStats = RatingStats(averageRating = 4.35, totalRatings = 142),
            userRating = 4,
            ratingState = RatingState.IDLE,
            error = null,
            onRatingChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RatingSectionEmptyPreview() {
    PlatziFlixAndroidTheme {
        RatingSection(
            ratingStats = RatingStats(averageRating = 0.0, totalRatings = 0),
            userRating = null,
            ratingState = RatingState.IDLE,
            error = null,
            onRatingChange = {}
        )
    }
}
