package com.espaciotiago.platziflixandroid.presentation.coursedetail.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.espaciotiago.platziflixandroid.presentation.coursedetail.components.RatingSection
import com.espaciotiago.platziflixandroid.presentation.coursedetail.state.CourseDetailUiState
import com.espaciotiago.platziflixandroid.presentation.coursedetail.state.RatingState
import com.espaciotiago.platziflixandroid.presentation.coursedetail.viewmodel.CourseDetailViewModel
import com.espaciotiago.platziflixandroid.presentation.courses.components.LoadingIndicator
import com.espaciotiago.platziflixandroid.ui.theme.CornerRadius
import com.espaciotiago.platziflixandroid.ui.theme.PlatziFlixAndroidTheme
import com.espaciotiago.platziflixandroid.ui.theme.Spacing
import com.espaciotiago.platziflixandroid.domain.models.RatingStats

/**
 * Screen that displays the details of a course, including its rating section.
 *
 * @param viewModel ViewModel that exposes [CourseDetailUiState] and handles rating submission
 * @param onBack Callback invoked when the user taps the back navigation icon
 * @param modifier Modifier for styling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    viewModel: CourseDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.courseName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingIndicator(
                message = "Cargando curso...",
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            )
        } else {
            CourseDetailContent(
                uiState = uiState,
                onRatingChange = viewModel::submitRating,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun CourseDetailContent(
    uiState: CourseDetailUiState,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(uiState.courseThumbnail)
                .crossfade(true)
                .build(),
            contentDescription = "Thumbnail de ${uiState.courseName}",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(CornerRadius.large)),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = android.R.drawable.ic_menu_gallery),
            error = painterResource(id = android.R.drawable.ic_menu_gallery)
        )

        Text(
            text = uiState.courseName,
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = uiState.courseDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RatingSection(
            ratingStats = uiState.ratingStats,
            userRating = uiState.userRating,
            ratingState = uiState.ratingState,
            error = uiState.error,
            onRatingChange = onRatingChange
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CourseDetailContentPreview() {
    PlatziFlixAndroidTheme {
        CourseDetailContent(
            uiState = CourseDetailUiState(
                isLoading = false,
                courseId = 4,
                courseName = "Curso de React.js",
                courseDescription = "Aprende React.js desde cero hasta avanzado.",
                courseThumbnail = "https://via.placeholder.com/300x200",
                ratingStats = RatingStats(averageRating = 4.35, totalRatings = 142),
                userRating = 4,
                ratingState = RatingState.IDLE,
                error = null
            ),
            onRatingChange = {}
        )
    }
}
