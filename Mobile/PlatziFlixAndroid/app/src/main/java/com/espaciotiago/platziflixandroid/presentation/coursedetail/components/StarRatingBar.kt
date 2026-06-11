package com.espaciotiago.platziflixandroid.presentation.coursedetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.espaciotiago.platziflixandroid.ui.theme.PlatziFlixAndroidTheme
import com.espaciotiago.platziflixandroid.ui.theme.Spacing

private val FilledStarColor = Color(0xFFFFC107)
private const val DisabledAlpha = 0.4f

/**
 * Row of 5 stars used to display and select a course rating.
 *
 * @param rating Current rating (0-5). Stars up to this value are rendered as filled.
 * @param onRatingChange Callback invoked with the tapped star value (1-5)
 * @param enabled Whether the bar responds to taps. When `false`, stars are shown
 * with reduced opacity and ignore interactions.
 */
@Composable
fun StarRatingBar(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(modifier = modifier) {
        for (star in 1..5) {
            val isFilled = star <= rating
            Icon(
                imageVector = if (isFilled) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = "$star estrella${if (star != 1) "s" else ""}",
                tint = if (isFilled) FilledStarColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(36.dp)
                    .padding(end = Spacing.extraSmall)
                    .alpha(if (enabled) 1f else DisabledAlpha)
                    .clickable(enabled = enabled) { onRatingChange(star) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StarRatingBarPreview() {
    PlatziFlixAndroidTheme {
        StarRatingBar(rating = 3, onRatingChange = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun StarRatingBarDisabledPreview() {
    PlatziFlixAndroidTheme {
        StarRatingBar(rating = 4, onRatingChange = {}, enabled = false)
    }
}
